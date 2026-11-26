package org.ikasan.studio.intellij.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.intellij.openapi.application.*;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.command.undo.UndoManager;
import com.intellij.openapi.project.Project;
import java.nio.file.*;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import org.ikasan.studio.core.TestFixtures;
import org.ikasan.studio.core.ai.*;
import org.ikasan.studio.core.generation.GenerationRequest;
import org.ikasan.studio.core.io.ComponentIO;
import org.ikasan.studio.core.model.ikasan.instance.ComponentProperty;
import org.ikasan.studio.core.model.ikasan.instance.Module;
import org.ikasan.studio.intellij.ai.StudioAiService.Proposal;
import org.ikasan.studio.intellij.project.*;
import org.ikasan.studio.intellij.project.StudioProjectFiles;
import org.ikasan.studio.intellij.settings.IkasanStudioSettings;
import org.ikasan.studio.ui.UiContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class StudioAiFileProposalTest {
    @TempDir Path directory;

    @Test void importWithoutProjectDirectoryReportsActionableFailure() {
        Project project = mock(Project.class);
        StudioAiService service = new StudioAiService(project);
        try {
            assertThatThrownBy(() -> service.importProposal("{\"operations\":[]}"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("Open a project before importing an AI proposal.");
            verify(project, never()).getService(UiContext.class);
        } finally { service.dispose(); }
    }

    @Test void importReviewCancelAndApplyWorkWithoutStartingBridge() throws Exception {
        Project project = mock(Project.class);
        UiContext context = mock(UiContext.class);
        Module live = ModelProposalTest.model();
        when(project.getBasePath()).thenReturn(directory.toString());
        when(project.getService(UiContext.class)).thenReturn(context);
        when(context.getIkasanModule()).thenReturn(live);
        when(context.getPipsiIkasanModel()).thenReturn(mock(GeneratedProjectSynchronizer.class));
        when(context.getLatestGeneration()).thenReturn(CompletableFuture.completedFuture(null));
        Path modelFile = directory.resolve("generated/src/main/model/model.json");
        Files.createDirectories(modelFile.getParent());
        Files.writeString(modelFile, ComponentIO.toValidatedModuleJson(live));
        ObjectMapper json = new ObjectMapper();
        String proposal = json.writeValueAsString(Map.of("formatVersion", 1,
                "baseModelSha256", OfflineModelProposal.sha256(Files.readAllBytes(modelFile)),
                "operations", json.readTree("""
                [{"type":"renameComponent","flow":"Transfer","component":"ReadFiles","name":"ReadEvents"}]
                """)));
        Map<String,Object> before = LiveModelSnapshot.capture(live);
        AtomicReference<Proposal> reviewed = new AtomicReference<>();
        StudioAiService service = new StudioAiService(project);
        try (MockedStatic<IkasanStudioSettings> settings = mockStatic(IkasanStudioSettings.class);
             MockedStatic<ApplicationManager> applications = mockStatic(ApplicationManager.class);
             MockedStatic<ModalityState> modalities = mockStatic(ModalityState.class);
             MockedStatic<CommandProcessor> commands = mockStatic(CommandProcessor.class);
             MockedStatic<UndoManager> undos = mockStatic(UndoManager.class);
             MockedStatic<StudioProjectFiles> files = mockStatic(StudioProjectFiles.class);
             MockedConstruction<StudioAiProposalDialog> dialogs = mockConstruction(StudioAiProposalDialog.class, (dialog, args) ->
                     reviewed.set((StudioAiService.Proposal) args.arguments().get(2)))) {
            files.when(() -> StudioProjectFiles.getApplicationBasePath(project)).thenAnswer(call -> project.getBasePath());
            settings.when(IkasanStudioSettings::isAlwaysAskAiApproval).thenReturn(true);
            Application app = mock(Application.class);
            //noinspection ResultOfMethodCallIgnored
            applications.when(ApplicationManager::getApplication).thenReturn(app);
            ModalityState writeSafeModality = mock(ModalityState.class);
            modalities.when(ModalityState::nonModal).thenReturn(writeSafeModality);
            doAnswer(call -> {
                assertThat(call.getArgument(1, ModalityState.class)).isSameAs(writeSafeModality);
                call.getArgument(0, Runnable.class).run();
                return null;
            }).when(app).invokeAndWait(any(Runnable.class), nullable(ModalityState.class));
            CommandProcessor command = mock(CommandProcessor.class);
            commands.when(CommandProcessor::getInstance).thenReturn(command);
            doAnswer(call -> { call.getArgument(1, Runnable.class).run(); return null; })
                    .when(command).executeCommand(eq(project), any(Runnable.class), anyString(), isNull());
            undos.when(() -> UndoManager.getInstance(project)).thenReturn(mock(UndoManager.class));
            CompletableFuture<Void> generated = new CompletableFuture<>();
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
            settings.when(IkasanStudioSettings::isAlwaysAskAiApproval).thenReturn(false);
            settings.when(IkasanStudioSettings::isConfirmAiDeletes).thenReturn(true);
            String deletion = json.writeValueAsString(Map.of("formatVersion", 1,
                    "baseModelSha256", OfflineModelProposal.sha256(Files.readAllBytes(modelFile)),
                    "operations", json.readTree("[{\"type\":\"deleteFlow\",\"flow\":\"Transfer\"}]")));
            assertThat(service.tryAutoImport(deletion)).isFalse();
            service.importProposal(deletion);
            assertThat(reviewed.get().deletionReviewRequired).isTrue();
            assertThat(live.getFlows()).hasSize(1);
            service.cancel(reviewed.get());
            assertThat(live.getFlows()).hasSize(1);
            settings.when(IkasanStudioSettings::isConfirmAiDeletes).thenReturn(false);
            int reviewsBefore = dialogs.constructed().size();
            {
                settings.when(IkasanStudioSettings::isAlwaysAskAiApproval).thenReturn(true);
                assertThat(service.tryAutoImport(addFlow)).isFalse();
                assertThat(live.getFlows()).hasSize(1);
                service.importProposal(addFlow);
                assertThat(dialogs.constructed()).hasSize(reviewsBefore + 1);
                assertThat(live.getFlows()).hasSize(1);
                service.cancel(reviewed.get());
                settings.when(IkasanStudioSettings::isAlwaysAskAiApproval).thenReturn(false);
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
                Files.writeString(modelFile, ComponentIO.toValidatedModuleJson(live));
                String deleteBob = json.writeValueAsString(Map.of("formatVersion", 1,
                        "baseModelSha256", OfflineModelProposal.sha256(Files.readAllBytes(modelFile)),
                        "operations", json.readTree("[{\"type\":\"deleteFlow\",\"flow\":\"bob\"}]")));
                settings.when(IkasanStudioSettings::isConfirmAiDeletes).thenReturn(true);
                service.importProposal(deleteBob);
                assertThat(reviewed.get().deletionReviewRequired).isTrue();
                assertThat(live.getFlows()).hasSize(2);
                service.apply(reviewed.get()).join();
                assertThat(live.getFlows()).hasSize(1);
                Files.writeString(modelFile, ComponentIO.toValidatedModuleJson(live));
                String deleteTransfer = json.writeValueAsString(Map.of("formatVersion", 1,
                        "baseModelSha256", OfflineModelProposal.sha256(Files.readAllBytes(modelFile)),
                        "operations", json.readTree("[{\"type\":\"deleteFlow\",\"flow\":\"Transfer\"}]")));
                settings.when(IkasanStudioSettings::isConfirmAiDeletes).thenReturn(false);
                int reviewsBeforeAutoDelete = dialogs.constructed().size();
                assertThat(service.tryAutoImport(deleteTransfer)).isTrue();
                assertThat(live.getFlows()).isEmpty();
                assertThat(dialogs.constructed()).hasSize(reviewsBeforeAutoDelete);
                files.verify(() -> StudioProjectFiles.deleteUserImplementedClassFile(any(), any()), never());
                Module risky = ModelProposal.prepare(LiveModelSnapshot.capture(
                        TestFixtures.getMyFirstModuleIkasanModule("V4.1.6", List.of())),
                        json.readTree("""
                        [{"type":"addFlow","flow":"existing"},
                         {"type":"addComponent","flow":"existing","key":"Event Generating Consumer","name":"Input",
                          "properties":{"endpointEventProvider":"MinuteEventProvider"}}]
                        """)).draft();
                when(context.getIkasanModule()).thenReturn(risky);
                ComponentProperty property = risky.getFlows().get(0).getConsumer().getProperty("endpointEventProvider");
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
