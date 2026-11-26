package org.ikasan.studio.intellij.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.intellij.openapi.application.*;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.command.undo.*;
import com.intellij.openapi.command.undo.UndoManager;
import com.intellij.openapi.command.undo.UndoableAction;
import com.intellij.openapi.project.Project;
import java.io.BufferedWriter;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpRequest.Builder;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.ikasan.studio.core.ai.LiveModelSnapshot;
import org.ikasan.studio.core.ai.ModelProposalTest;
import org.ikasan.studio.core.generation.GenerationRequest;
import org.ikasan.studio.core.model.ikasan.instance.Module;
import org.ikasan.studio.intellij.project.GeneratedProjectSynchronizer;
import org.ikasan.studio.intellij.project.StudioProjectFiles;
import org.ikasan.studio.intellij.settings.IkasanStudioSettings;
import org.ikasan.studio.ui.StudioBundle;
import org.ikasan.studio.ui.UiContext;
import org.ikasan.studio.ui.component.properties.ComponentPropertiesPanel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;

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
        Module model = ModelProposalTest.model();
        when(project.getService(UiContext.class)).thenReturn(context);
        when(project.getName()).thenReturn("test-project");
        when(project.getBasePath()).thenReturn(directory.resolve("project").toString());
        when(context.getIkasanModule()).thenReturn(model);
        when(context.getPipsiIkasanModel()).thenReturn(mock(GeneratedProjectSynchronizer.class));
        when(context.getLatestGeneration()).thenReturn(CompletableFuture.completedFuture(null));
        ComponentPropertiesPanel properties = mock(ComponentPropertiesPanel.class);
        when(context.getPropertiesPanel()).thenReturn(properties);
        Map<String,Object> before = LiveModelSnapshot.capture(model);
        StudioAiService service = new StudioAiService(project);
        when(project.getService(StudioAiService.class)).thenReturn(service);
        AtomicReference<StudioAiService.Proposal> reviewed = new AtomicReference<>();
        try (MockedStatic<IkasanStudioSettings> settings = mockStatic(IkasanStudioSettings.class);
             MockedStatic<ApplicationManager> applications = mockStatic(ApplicationManager.class);
             MockedStatic<PathManager> paths = mockStatic(PathManager.class);
             MockedStatic<ModalityState> modalities = mockStatic(ModalityState.class);
             MockedStatic<CommandProcessor> commands = mockStatic(CommandProcessor.class);
             MockedStatic<UndoManager> undos = mockStatic(UndoManager.class);
             MockedStatic<StudioProjectFiles> files = mockStatic(StudioProjectFiles.class);
             MockedConstruction<StudioAiProposalDialog> dialogs = mockConstruction(StudioAiProposalDialog.class, (dialog, construction) ->
                     reviewed.set((StudioAiService.Proposal) construction.arguments().get(2)))) {
            files.when(() -> StudioProjectFiles.getApplicationBasePath(project)).thenAnswer(call -> project.getBasePath());
            settings.when(IkasanStudioSettings::isAlwaysAskAiApproval).thenReturn(true);
            Application app = mock(Application.class);
            ModalityState writeSafeModality = mock(ModalityState.class);
            modalities.when(ModalityState::nonModal).thenReturn(writeSafeModality);
            // Mockito records this call for stubbing; its return value is intentionally ignored.
            //noinspection ResultOfMethodCallIgnored
            applications.when(ApplicationManager::getApplication).thenReturn(app);
            doAnswer(call -> {
                assertThat(call.getArgument(1, ModalityState.class)).isSameAs(writeSafeModality);
                call.getArgument(0, Runnable.class).run();
                return null;
            }).when(app).invokeAndWait(any(Runnable.class), nullable(ModalityState.class));
            doAnswer(call -> { call.getArgument(0, Runnable.class).run(); return null; }).when(app).invokeLater(any(Runnable.class));
            paths.when(PathManager::getConfigPath).thenReturn(directory.toString());
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
            assertThat(config).contains("studio-mcp-adapter.jar", "-jar", "--connection").doesNotContain("Bearer", "python");
            // Exercise the actual HTTP/stdio transport against this server, without any IDE model request.
            JsonNode args = JSON.readTree(config).path("mcpServers").path("ikasan-studio").path("args");
            // This connection file belongs to the local JUnit temporary directory.
            //noinspection UseOptimizedEelFunctions
            JsonNode connection = JSON.readTree(Files.readString(Path.of(args.get(3).asText())));
            HttpClient http = HttpClient.newHttpClient();
            URI endpoint = URI.create(connection.path("url").asText());
            Builder unauthenticated = HttpRequest.newBuilder(endpoint)
                    .POST(BodyPublishers.ofString("{}"));
            assertThat(http.send(unauthenticated.build(), BodyHandlers.discarding()).statusCode()).isEqualTo(403);
            Builder browser = HttpRequest.newBuilder(endpoint)
                    .header("Authorization", "Bearer " + connection.path("token").asText())
                    .header("Origin", "https://example.com")
                    .POST(BodyPublishers.ofString("{}"));
            assertThat(http.send(browser.build(), BodyHandlers.discarding()).statusCode()).isEqualTo(403);
            JsonNode serverConfig = JSON.readTree(config).path("mcpServers").path("ikasan-studio");
            assertThat(Path.of(serverConfig.path("command").asText())).isAbsolute().exists();
            ArrayList<String> launch = new ArrayList<>();
            launch.add(serverConfig.path("command").asText());
            args.forEach(argument -> launch.add(argument.asText()));
            Process process = new ProcessBuilder(launch).start();
            try (BufferedWriter stdin = process.outputWriter()) {
                stdin.write("{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"initialize\"}\n");
                stdin.write("{\"jsonrpc\":\"2.0\",\"method\":\"notifications/initialized\"}\n");
                stdin.write("{\"jsonrpc\":\"2.0\",\"id\":2,\"method\":\"tools/list\"}\n");
            }
            assertThat(process.waitFor(15, TimeUnit.SECONDS)).isTrue();
            assertThat(process.exitValue()).isZero();
            List<String> replies = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8).lines().toList();
            assertThat(replies).hasSize(2);
            assertThat(JSON.readTree(replies.get(1)).path("result").path("tools")).hasSize(4);

            assertThat(service.getLastAccessTransport()).isNull();
            when(project.getBasePath()).thenReturn(null);
            assertThatThrownBy(() -> service.call("studio_snapshot", JSON.createObjectNode()))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage(StudioBundle.message("ai.NotReady"));
            when(project.getBasePath()).thenReturn(directory.resolve("project").toString());
            StudioAiService.nativeCall(project, "studio_snapshot", "{}");
            assertThat(service.getLastAccessTransport()).isEqualTo("native");
            Map<?,?> snapshot = (Map<?, ?>) service.call("studio_snapshot", JSON.createObjectNode());
            String revision = snapshot.get("revision").toString();
            JsonNode request = JSON.readTree("""
                    {"operations":[{"type":"setProperty","flow":"Transfer","component":"ReadFiles","property":"sourceDirectory","value":"/new"}]}
                    """);
            ((ObjectNode) request).put("revision", revision);
            StudioAiService other = new StudioAiService(project);
            try {
                when(project.getBasePath()).thenReturn(directory.resolve("other-project").toString());
                other.start();
                when(project.getBasePath()).thenReturn(directory.resolve("project").toString());
                assertThatThrownBy(() -> other.call("studio_propose", request)).hasMessageContaining("Unknown or expired revision");
            } finally { other.dispose(); }

            // Restart after validation but before the EDT publishes the review.
            AtomicInteger dispatches = new AtomicInteger();
            doAnswer(call -> {
                if (dispatches.incrementAndGet() == 2) {
                    service.stop();
                    service.start();
                }
                call.getArgument(0, Runnable.class).run();
                return null;
            }).when(app).invokeAndWait(any(Runnable.class), same(writeSafeModality));
            assertThatThrownBy(() -> service.call("studio_propose", request)).hasMessageContaining("stopped or restarted");
            assertThat(reviewed.get()).isNull();
            doAnswer(call -> {
                assertThat(call.getArgument(1, ModalityState.class)).isSameAs(writeSafeModality);
                call.getArgument(0, Runnable.class).run();
                return null;
            }).when(app).invokeAndWait(any(Runnable.class), nullable(ModalityState.class));
            assertThatThrownBy(() -> service.call("studio_propose", request)).hasMessageContaining("Unknown or expired revision");
            assertThat(service.start()).isEqualTo(config);
            assertThat(service.getLastAccessTransport()).isNull();
            snapshot = (Map<?, ?>) service.call("studio_snapshot", JSON.createObjectNode());
            ((ObjectNode) request).put("revision", snapshot.get("revision").toString());
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
            assertThat(service.apply(reviewed.get())).isSameAs(generated);
            assertThat(reviewed.get().status).isEqualTo("generating");
            generated.complete(null);
            assertThat(reviewed.get().status).isEqualTo("applied");
            ArgumentCaptor<UndoableAction> captured = ArgumentCaptor.forClass(UndoableAction.class);
            verify(undo).undoableActionPerformed(captured.capture());
            service.stop(); // Disconnecting the AI must not disable the undo entry.
            captured.getValue().undo();
            assertThat(LiveModelSnapshot.capture(model)).isEqualTo(before);
            captured.getValue().redo();
            assertThat(model.getFlows().get(0).getConsumer().getPropertyValue("sourceDirectory")).isEqualTo("/new");
            service.start();
            settings.when(IkasanStudioSettings::isAlwaysAskAiApproval).thenReturn(false);
            // Completion notifications are not part of this transport/Undo test.
            doNothing().when(app).invokeLater(any(Runnable.class));
            snapshot = (Map<?, ?>) service.call("studio_snapshot", JSON.createObjectNode());
            ((ObjectNode) request).put("revision", snapshot.get("revision").toString());
            ((ObjectNode) request.path("operations").get(0)).put("value", "/automatic");
            Map<?,?> result = (Map<?, ?>) service.call("studio_propose", request);
            assertThat(result.get("status")).isEqualTo("applied");
            assertThat(model.getFlows().get(0).getConsumer().getPropertyValue("sourceDirectory")).isEqualTo("/automatic");
            assertThat(dialogs.constructed()).hasSize(1);
            service.stop();
            when(context.getIkasanModule()).thenReturn(ModelProposalTest.model());
            assertThatThrownBy(() -> captured.getValue().undo()).isInstanceOf(UnexpectedUndoException.class);
            assertThatThrownBy(() -> service.call("studio_snapshot", JSON.createObjectNode())).hasMessageContaining("stopped");
        } finally { service.dispose(); }
    }
}
