package org.ikasan.studio.intellij.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.intellij.openapi.application.*;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.command.undo.UndoManager;
import com.intellij.openapi.project.Project;
import org.ikasan.studio.core.ai.*;
import org.ikasan.studio.core.io.ComponentIO;
import org.ikasan.studio.core.generation.GenerationRequest;
import org.ikasan.studio.intellij.project.*;
import org.ikasan.studio.ui.UiContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class StudioAiFileProposalTest {
    @TempDir Path directory;

    @Test void importReviewCancelAndApplyWorkWithoutStartingBridge() throws Exception {
        var project = mock(Project.class);
        var context = mock(UiContext.class);
        var live = ModelProposalTest.model();
        when(project.getBasePath()).thenReturn(directory.toString());
        when(project.getService(UiContext.class)).thenReturn(context);
        when(context.getIkasanModule()).thenReturn(live);
        when(context.getPipsiIkasanModel()).thenReturn(mock(GeneratedProjectSynchronizer.class));
        when(context.getLatestGeneration()).thenReturn(CompletableFuture.completedFuture(null));
        Path modelFile = directory.resolve("generated/src/main/model/model.json");
        Files.createDirectories(modelFile.getParent());
        Files.writeString(modelFile, ComponentIO.toValidatedModuleJson(live));
        var json = new ObjectMapper();
        String proposal = json.writeValueAsString(Map.of("formatVersion", 1,
                "baseModelSha256", OfflineModelProposal.sha256(Files.readAllBytes(modelFile)),
                "operations", json.readTree("""
                [{"type":"renameComponent","flow":"Transfer","component":"ReadFiles","name":"ReadEvents"}]
                """)));
        var before = LiveModelSnapshot.capture(live);
        var reviewed = new AtomicReference<StudioAiService.Proposal>();
        var service = new StudioAiService(project);
        try (var settings = mockStatic(org.ikasan.studio.intellij.settings.IkasanStudioSettings.class);
             var applications = mockStatic(ApplicationManager.class);
             var modalities = mockStatic(ModalityState.class);
             var commands = mockStatic(CommandProcessor.class);
             var undos = mockStatic(UndoManager.class);
             var files = mockStatic(StudioProjectFiles.class);
             var dialogs = mockConstruction(StudioAiProposalDialog.class, (dialog, args) ->
                     reviewed.set((StudioAiService.Proposal) args.arguments().get(2)))) {
            settings.when(org.ikasan.studio.intellij.settings.IkasanStudioSettings::isAlwaysAskAiApproval).thenReturn(true);
            var app = mock(Application.class);
            applications.when(ApplicationManager::getApplication).thenReturn(app);
            modalities.when(ModalityState::any).thenReturn(mock(ModalityState.class));
            doAnswer(call -> { call.getArgument(0, Runnable.class).run(); return null; })
                    .when(app).invokeAndWait(any(Runnable.class), any(ModalityState.class));
            var command = mock(CommandProcessor.class);
            commands.when(CommandProcessor::getInstance).thenReturn(command);
            doAnswer(call -> { call.getArgument(1, Runnable.class).run(); return null; })
                    .when(command).executeCommand(eq(project), any(Runnable.class), anyString(), isNull());
            undos.when(() -> UndoManager.getInstance(project)).thenReturn(mock(UndoManager.class));
            var generated = new CompletableFuture<Void>();
            files.when(() -> StudioProjectFiles.refreshCodeFromModel(project, GenerationRequest.full())).thenReturn(generated);
            service.importProposal(proposal);
            assertThat(service.isRunning()).isFalse();
            assertThat(reviewed.get().fileBased).isTrue();
            assertThat(LiveModelSnapshot.capture(live)).isEqualTo(before);
            assertThatThrownBy(() -> service.importProposal(proposal)).hasMessageContaining("pending proposal");
            service.cancel(reviewed.get());
            assertThat(LiveModelSnapshot.capture(live)).isEqualTo(before);
            service.importProposal(proposal);
            live.getFlows().get(0).getConsumer().setName("ManualEdit");
            assertThatThrownBy(() -> service.apply(reviewed.get())).hasMessageContaining("design changed");
            live.getFlows().get(0).getConsumer().setName("ReadFiles");
            service.stop(); // Stopping an unrelated bridge must not cancel an imported proposal.
            assertThat(service.apply(reviewed.get())).isSameAs(generated);
            assertThat(service.isRunning()).isFalse();
            assertThat(live.getFlows().get(0).getConsumer().getIdentity()).isEqualTo("ReadEvents");
            assertThat(reviewed.get().status).isEqualTo("generating");
            generated.complete(null);
            assertThat(reviewed.get().status).isEqualTo("applied");
            Files.writeString(modelFile, ComponentIO.toValidatedModuleJson(live));
            String addFlow = json.writeValueAsString(Map.of("formatVersion", 1,
                    "baseModelSha256", OfflineModelProposal.sha256(Files.readAllBytes(modelFile)),
                    "operations", json.readTree("[{\"type\":\"addFlow\",\"flow\":\"bob\"}]")));
            int reviewsBefore = dialogs.constructed().size();
            {
                settings.when(org.ikasan.studio.intellij.settings.IkasanStudioSettings::isAlwaysAskAiApproval).thenReturn(true);
                assertThat(service.tryAutoImport(addFlow)).isFalse();
                assertThat(live.getFlows()).hasSize(1);
                service.importProposal(addFlow);
                assertThat(dialogs.constructed()).hasSize(reviewsBefore + 1);
                assertThat(live.getFlows()).hasSize(1);
                service.cancel(reviewed.get());
                settings.when(org.ikasan.studio.intellij.settings.IkasanStudioSettings::isAlwaysAskAiApproval).thenReturn(false);
                assertThat(service.tryAutoImport(addFlow)).isTrue();
                assertThat(dialogs.constructed()).hasSize(reviewsBefore + 1);
                assertThat(live.getFlows()).hasSize(2);
                assertThat(live.getFlows().get(1).getIdentity()).isEqualTo("bob");
                assertThatThrownBy(() -> service.tryAutoImport(addFlow)).isInstanceOf(Exception.class);
                assertThat(live.getFlows()).hasSize(2);
                Files.writeString(modelFile, ComponentIO.toValidatedModuleJson(live));
                String rename = json.writeValueAsString(Map.of("formatVersion", 1,
                        "baseModelSha256", OfflineModelProposal.sha256(Files.readAllBytes(modelFile)),
                        "operations", json.readTree("""
                        [{"type":"renameComponent","flow":"Transfer","component":"ReadEvents","name":"ReadAgain"},
                         {"type":"setProperty","flow":"Transfer","component":"ReadAgain","property":"sourceDirectory","value":"/auto"}]
                        """)));
                assertThat(service.tryAutoImport(rename)).isTrue();
                assertThat(live.getFlows().get(0).getConsumer().getIdentity()).isEqualTo("ReadAgain");
                assertThat(live.getFlows().get(0).getConsumer().getPropertyValue("sourceDirectory")).isEqualTo("/auto");
                assertThat(dialogs.constructed()).hasSize(reviewsBefore + 1);
                Files.writeString(modelFile, ComponentIO.toValidatedModuleJson(live));
                String replace = json.writeValueAsString(Map.of("formatVersion", 1,
                        "baseModelSha256", OfflineModelProposal.sha256(Files.readAllBytes(modelFile)),
                        "operations", json.readTree("""
                        [{"type":"replaceComponent","flow":"Transfer","component":"ReadAgain",
                          "key":"Spring JMS Consumer","name":"Receive from Tom",
                          "properties":{"destinationJndiName":"tom.to.toby"}},
                         {"type":"replaceComponent","flow":"Transfer","component":"WriteFiles",
                          "key":"Dev Null Producer","name":"Discard"}]
                        """)));
                assertThat(service.tryAutoImport(replace)).isTrue();
                assertThat(live.getFlows().get(0).getConsumer().getIdentity()).isEqualTo("Receive from Tom");
                assertThat(dialogs.constructed()).hasSize(reviewsBefore + 1);
                files.verify(() -> StudioProjectFiles.deleteUserImplementedClassFile(any(), any()), never());
                var risky = ModelProposal.prepare(LiveModelSnapshot.capture(
                        org.ikasan.studio.core.TestFixtures.getMyFirstModuleIkasanModule("V4.1.6", java.util.List.of())),
                        json.readTree("""
                        [{"type":"addFlow","flow":"existing"},
                         {"type":"addComponent","flow":"existing","key":"Event Generating Consumer","name":"Input",
                          "properties":{"endpointEventProvider":"MinuteEventProvider"}}]
                        """)).draft();
                when(context.getIkasanModule()).thenReturn(risky);
                var property = risky.getFlows().get(0).getConsumer().getProperty("endpointEventProvider");
                property.setOverwriteEnabled(true);
                Files.writeString(modelFile, ComponentIO.toValidatedModuleJson(risky));
                String unrelatedAddition = json.writeValueAsString(Map.of("formatVersion", 1,
                        "baseModelSha256", OfflineModelProposal.sha256(Files.readAllBytes(modelFile)),
                        "operations", json.readTree("[{\"type\":\"addFlow\",\"flow\":\"unrelated\"}]")));
                assertThat(service.tryAutoImport(unrelatedAddition)).isFalse();
                assertThat(risky.getFlows()).hasSize(1);
                service.importProposal(unrelatedAddition);
                assertThat(reviewed.get().userCodeReviewRequired).isTrue();
                assertThat(risky.getFlows()).hasSize(1);
                service.cancel(reviewed.get());
                property.setOverwriteEnabled(false);
                assertThat(service.tryAutoImport(unrelatedAddition)).isTrue();
                assertThat(risky.getFlows()).hasSize(2);

            }

        } finally { service.dispose(); }
    }
}
