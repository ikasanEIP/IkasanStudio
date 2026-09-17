package org.ikasan.studio.intellij.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.intellij.openapi.application.*;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.command.undo.*;
import com.intellij.openapi.project.Project;
import org.ikasan.studio.core.ai.LiveModelSnapshot;
import org.ikasan.studio.core.ai.ModelProposalTest;
import org.ikasan.studio.core.generation.GenerationRequest;
import org.ikasan.studio.intellij.project.GeneratedProjectSynchronizer;
import org.ikasan.studio.intellij.project.StudioProjectFiles;
import org.ikasan.studio.ui.UiContext;
import org.ikasan.studio.ui.component.properties.ComponentPropertiesPanel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;

import java.nio.file.Path;
import java.nio.file.Files;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class StudioAiServiceTest {
    @TempDir Path directory;
    private static final ObjectMapper JSON = new ObjectMapper();

    @Test void previewApplyUndoRedoAndReloadProtection() throws Exception { exercise(false); }
    @Test void failedPersistenceRollsBackWithoutUndoRegistration() throws Exception { exercise(true); }

    private void exercise(boolean failSave) throws Exception {
        Project project = mock(Project.class);
        UiContext context = mock(UiContext.class);
        var model = ModelProposalTest.model();
        when(project.getService(UiContext.class)).thenReturn(context);
        when(project.getName()).thenReturn("test-project");
        when(context.getIkasanModule()).thenReturn(model);
        when(context.getPipsiIkasanModel()).thenReturn(mock(GeneratedProjectSynchronizer.class));
        when(context.getLatestGeneration()).thenReturn(CompletableFuture.completedFuture(null));
        var properties = mock(ComponentPropertiesPanel.class);
        when(context.getPropertiesPanel()).thenReturn(properties);
        var before = LiveModelSnapshot.capture(model);
        StudioAiService service = new StudioAiService(project);
        AtomicReference<StudioAiService.Proposal> reviewed = new AtomicReference<>();
        try (var applications = mockStatic(ApplicationManager.class);
             var paths = mockStatic(PathManager.class);
             var modalities = mockStatic(ModalityState.class);
             var commands = mockStatic(CommandProcessor.class);
             var undos = mockStatic(UndoManager.class);
             var files = mockStatic(StudioProjectFiles.class);
             var dialogs = mockConstruction(StudioAiProposalDialog.class, (dialog, construction) ->
                     reviewed.set((StudioAiService.Proposal) construction.arguments().get(2)))) {
            Application app = mock(Application.class);
            modalities.when(ModalityState::any).thenReturn(mock(ModalityState.class));
            // Mockito records this call for stubbing; its return value is intentionally ignored.
            //noinspection ResultOfMethodCallIgnored
            applications.when(ApplicationManager::getApplication).thenReturn(app);
            doAnswer(call -> { call.getArgument(0, Runnable.class).run(); return null; })
                    .when(app).invokeAndWait(any(Runnable.class), any(ModalityState.class));
            doAnswer(call -> { call.getArgument(0, Runnable.class).run(); return null; }).when(app).invokeLater(any(Runnable.class));
            paths.when(PathManager::getSystemPath).thenReturn(directory.toString());
            CommandProcessor command = mock(CommandProcessor.class);
            commands.when(CommandProcessor::getInstance).thenReturn(command);
            doAnswer(call -> { call.getArgument(1, Runnable.class).run(); return null; })
                    .when(command).executeCommand(eq(project), any(Runnable.class), anyString(), isNull());
            UndoManager undo = mock(UndoManager.class);
            undos.when(() -> UndoManager.getInstance(project)).thenReturn(undo);
            CompletableFuture<Void> generated = new CompletableFuture<>();
            if (failSave) files.when(() -> StudioProjectFiles.refreshCodeFromModel(project, GenerationRequest.full())).thenThrow(new IllegalStateException("Save failed"));
            else files.when(() -> StudioProjectFiles.refreshCodeFromModel(project, GenerationRequest.full())).thenReturn(generated);
            String config = service.start();
            assertThat(config).contains("python3", "--connection").doesNotContain("Bearer");
            // Exercise the actual HTTP/stdio transport against this server, without any IDE model request.
            var args = JSON.readTree(config).path("mcpServers").path("ikasan-studio").path("args");
            // This connection file belongs to the local JUnit temporary directory.
            //noinspection UseOptimizedEelFunctions
            var connection = JSON.readTree(Files.readString(Path.of(args.get(2).asText())));
            var http = java.net.http.HttpClient.newHttpClient();
            var endpoint = java.net.URI.create(connection.path("url").asText());
            var unauthenticated = java.net.http.HttpRequest.newBuilder(endpoint)
                    .POST(java.net.http.HttpRequest.BodyPublishers.ofString("{}"));
            assertThat(http.send(unauthenticated.build(), java.net.http.HttpResponse.BodyHandlers.discarding()).statusCode()).isEqualTo(403);
            var browser = java.net.http.HttpRequest.newBuilder(endpoint)
                    .header("Authorization", "Bearer " + connection.path("token").asText())
                    .header("Origin", "https://example.com")
                    .POST(java.net.http.HttpRequest.BodyPublishers.ofString("{}"));
            assertThat(http.send(browser.build(), java.net.http.HttpResponse.BodyHandlers.discarding()).statusCode()).isEqualTo(403);
            var process = new ProcessBuilder("python3", args.get(0).asText(), args.get(1).asText(), args.get(2).asText()).start();
            try (var stdin = process.outputWriter()) {
                stdin.write("{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"initialize\"}\n");
                stdin.write("{\"jsonrpc\":\"2.0\",\"method\":\"notifications/initialized\"}\n");
                stdin.write("{\"jsonrpc\":\"2.0\",\"id\":2,\"method\":\"tools/list\"}\n");
            }
            assertThat(process.waitFor(15, java.util.concurrent.TimeUnit.SECONDS)).isTrue();
            assertThat(process.exitValue()).isZero();
            var replies = new String(process.getInputStream().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8).lines().toList();
            assertThat(replies).hasSize(2);
            assertThat(JSON.readTree(replies.get(1)).path("result").path("tools")).hasSize(4);

            var snapshot = (Map<?, ?>) service.call("studio_snapshot", JSON.createObjectNode());
            String revision = snapshot.get("revision").toString();
            var request = JSON.readTree("""
                    {"operations":[{"type":"setProperty","flow":"Transfer","component":"ReadFiles","property":"sourceDirectory","value":"/new"}]}
                    """);
            ((com.fasterxml.jackson.databind.node.ObjectNode) request).put("revision", revision);
            StudioAiService other = new StudioAiService(project);
            try {
                other.start();
                assertThatThrownBy(() -> other.call("studio_propose", request)).hasMessageContaining("Unknown or expired revision");
            } finally { other.dispose(); }

            // Restart after validation but before the EDT publishes the review.
            var dispatches = new java.util.concurrent.atomic.AtomicInteger();
            doAnswer(call -> {
                if (dispatches.incrementAndGet() == 2) {
                    service.stop();
                    service.start();
                }
                call.getArgument(0, Runnable.class).run();
                return null;
            }).when(app).invokeAndWait(any(Runnable.class), any(ModalityState.class));
            assertThatThrownBy(() -> service.call("studio_propose", request)).hasMessageContaining("stopped or restarted");
            assertThat(reviewed.get()).isNull();
            doAnswer(call -> { call.getArgument(0, Runnable.class).run(); return null; })
                    .when(app).invokeAndWait(any(Runnable.class), any(ModalityState.class));
            assertThatThrownBy(() -> service.call("studio_propose", request)).hasMessageContaining("Unknown or expired revision");
            snapshot = (Map<?, ?>) service.call("studio_snapshot", JSON.createObjectNode());
            ((com.fasterxml.jackson.databind.node.ObjectNode) request).put("revision", snapshot.get("revision").toString());
            service.call("studio_propose", request);
            assertThat(LiveModelSnapshot.capture(model)).isEqualTo(before);
            assertThat(reviewed.get().status).isEqualTo("awaiting_review");
            assertThat(dialogs.constructed()).hasSize(1);
            verify(dialogs.constructed().get(0)).show();
            assertThatThrownBy(() -> service.call("studio_propose", request)).hasMessageContaining("pending proposal");

            when(properties.dataHasChangedAndOKToProcess()).thenReturn(true);
            assertThatThrownBy(() -> service.apply(reviewed.get())).hasMessageContaining("pending property edits");
            when(properties.dataHasChangedAndOKToProcess()).thenReturn(false);
            model.getFlows().get(0).getConsumer().setPropertyValue("sourceDirectory", "/manual");
            assertThatThrownBy(() -> service.apply(reviewed.get())).hasMessageContaining("design changed");
            model.getFlows().get(0).getConsumer().setPropertyValue("sourceDirectory", "/incoming");
            if (failSave) {
                assertThatThrownBy(() -> service.apply(reviewed.get())).hasMessageContaining("Save failed");
                assertThat(LiveModelSnapshot.capture(model)).isEqualTo(before);
                verifyNoInteractions(undo);
                service.cancel(reviewed.get());
                assertThat(reviewed.get().status).isEqualTo("cancelled");
                return;
            }
            service.apply(reviewed.get());
            assertThat(reviewed.get().status).isEqualTo("generating");
            generated.complete(null);
            assertThat(reviewed.get().status).isEqualTo("applied");
            var captured = ArgumentCaptor.forClass(UndoableAction.class);
            verify(undo).undoableActionPerformed(captured.capture());
            service.stop(); // Disconnecting the AI must not disable the undo entry.
            captured.getValue().undo();
            assertThat(LiveModelSnapshot.capture(model)).isEqualTo(before);
            captured.getValue().redo();
            assertThat(model.getFlows().get(0).getConsumer().getPropertyValue("sourceDirectory")).isEqualTo("/new");
            when(context.getIkasanModule()).thenReturn(ModelProposalTest.model());
            assertThatThrownBy(() -> captured.getValue().undo()).isInstanceOf(UnexpectedUndoException.class);
            assertThatThrownBy(() -> service.call("studio_snapshot", JSON.createObjectNode())).hasMessageContaining("stopped");
        } finally { service.dispose(); }
    }
}
