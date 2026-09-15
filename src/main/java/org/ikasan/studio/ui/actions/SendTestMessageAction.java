package org.ikasan.studio.ui.actions;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.project.Project;
import com.intellij.util.Consumer;
import org.ikasan.studio.core.model.ikasan.instance.BasicElement;
import org.ikasan.studio.core.model.ikasan.instance.FlowElement;
import org.ikasan.studio.core.model.ikasan.instance.Module;
import org.ikasan.studio.integration.ikasan.StudioInjectClient;
import org.ikasan.studio.intellij.project.StudioProjectFiles;
import org.ikasan.studio.ui.StudioBundle;
import org.ikasan.studio.ui.StudioUIUtils;
import org.ikasan.studio.ui.UiContext;
import org.ikasan.studio.intellij.execution.IkasanDebugSessionService;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

/**
 * Prompts for a payload and POSTs it to the running module's /rest/studio/inject/{flowName} endpoint (generated
 * by StudioInjectControllerTemplate), which is only reachable when the module was launched via "Debug module"
 * (the studio-debug Spring profile). Bypasses the real broker entirely.
 */
public class SendTestMessageAction implements ActionListener {
    private static final Logger LOG = Logger.getInstance("#SendTestMessageAction");
    private final Project project;
    private final BasicElement ikasanBasicElement;

    public SendTestMessageAction(Project project, BasicElement ikasanBasicElement) {
        this.project = project;
        this.ikasanBasicElement = ikasanBasicElement;
    }

    @Override
    public void actionPerformed(ActionEvent actionEvent) {
        if (!(ikasanBasicElement instanceof FlowElement flowElement) || !flowElement.getComponentMeta().isConsumer()) {
            StudioUIUtils.displayIdeaWarnMessage(project, StudioBundle.message("message.SendTestMessageCanOnlyBeUsedOnConsumers"));
            return;
        }
        if (!project.getService(IkasanDebugSessionService.class).isDebugModuleRunning()) {
            StudioUIUtils.displayIdeaWarnMessage(project,
                    StudioBundle.message("message.SendTestMessageRequiresRunningDebugModule"));
            return;
        }

        Module module = project.getService(UiContext.class).getIkasanModule();
        String flowName = flowElement.getContainingFlow().getIdentity();
        // Namespaces the dialog's persisted last-used payload - componentName is only guaranteed unique within
        // its own flow (see BasicElement javadoc), so module+flow+component avoids collisions between two
        // different flows that happen to reuse the same component name.
        String componentKey = module.getIdentity() + "/" + flowName + "/" + flowElement.getIdentity();

        String testPayloadAdapter = flowElement.getComponentMeta().getTestPayloadAdapter();
        if (org.ikasan.studio.core.metapack.model.ComponentMeta.FILE_TRANSFER_TEST_PAYLOAD_ADAPTER.equals(testPayloadAdapter)) {
            chooseTestPayloadFile(chosenFile -> {
                if (chosenFile == null) {
                    return;
                }
                sendPayload(module, flowName, new PreparedPayload(
                        chosenFile.base64Content(), null, testPayloadAdapter, chosenFile.name()));
            });
            return;
        }
        if (flowElement.getComponentMeta().producesFileListPayload()) {
            // A real file picker instead of the generic text/JSON dialog - see ComponentMeta#producesFileListPayload
            // for why this is only offered when the declared payload really is java.util.List<java.io.File>.
            chooseTestFilePaths(filePaths -> {
                if (filePaths == null) {
                    return;
                }
                try {
                    String payload = StudioInjectClient.writeJson(filePaths);
                    // The generated controller deserializes this canonical List<File> type.
                    sendPayload(module, flowName, new PreparedPayload(payload, flowElement.getEffectiveOutputTypeDescription(), flowElement.getComponentMeta().isLocalFileConsumer() ? "studio-local-file-list" : null, null));
                } catch (Exception e) {
                    LOG.warn("STUDIO: Could not build JSON payload from chosen test files", e);
                    StudioUIUtils.displayIdeaWarnMessage(project, StudioBundle.message("message.CouldNotSendTestMessage", e.getMessage()));
                }
            });
            return;
        }
        SendTestMessagePayloadDialog payloadDialog = new SendTestMessagePayloadDialog(project, componentKey);
        if (!payloadDialog.showAndGet()) {
            return;
        }
        sendPayload(module, flowName, new PreparedPayload(payloadDialog.getPayload(), payloadDialog.getPayloadClassName(), null, null));
    }

    private void sendPayload(Module module, String flowName, PreparedPayload preparedPayload) {
        TestInjectionTask.run(project, StudioBundle.message("message.SendingTestMessage"), flowName,
                () -> StudioInjectClient.postPayload(module, flowName, preparedPayload.payload(),
                        preparedPayload.payloadClassName(), preparedPayload.payloadAdapter(), preparedPayload.payloadFilename()),
                responseBody -> {
                    if (("studio-local-file-list".equals(preparedPayload.payloadAdapter())
                            || org.ikasan.studio.core.metapack.model.ComponentMeta.FILE_TRANSFER_TEST_PAYLOAD_ADAPTER.equals(preparedPayload.payloadAdapter()))
                            && !"invoked".equals(responseBody.path("status").asText())) {
                        return TestInjectionTask.Notice.warning(StudioBundle.message("message.LocalFileTestNeedsRegeneration"));
                    }
                    return TestInjectionTask.Notice.info(StudioBundle.message("message.TestMessageSent",
                            responseBody.path("identifier").asText("")));
                });
    }

    /**
     * The synchronous FileChooser.chooseFile restores its last-selected file via a blocking VFS lookup, which
     * IntelliJ 2024.3+ flags as a slow operation when run directly on the EDT (as this is, from a canvas mouse
     * click) - the async overload behind StudioProjectFiles.chooseFileAndEncodeBase64 defers that work off the
     * EDT and invokes callback once a choice is made, never on cancel.
     */
    private void chooseTestPayloadFile(Consumer<StudioProjectFiles.ChosenFileContent> callback) {
        FileChooserDescriptor descriptor = FileChooserDescriptorFactory.createSingleFileNoJarsDescriptor()
                .withTitle(StudioBundle.message("dialog.ChooseFileTransferTestPayload"))
                .withDescription(StudioBundle.message("message.ChooseFileTransferTestPayloadDescription"));
        StudioProjectFiles.chooseFileAndEncodeBase64(project, descriptor, callback);
    }

    private record PreparedPayload(String payload, String payloadClassName, String payloadAdapter, String payloadFilename) {}

    /**
     * Lets the user pick one or more real files from disk, in place of typing/pasting a payload - only offered
     * when the target Consumer's declared payload really is java.util.List<java.io.File> (see
     * ComponentMeta#producesFileListPayload). Async, for the same reason as chooseTestPayloadFile above.
     * @param callback receives the chosen files' absolute paths, or null if nothing was chosen
     */
    private void chooseTestFilePaths(Consumer<List<String>> callback) {
        FileChooserDescriptor descriptor = FileChooserDescriptorFactory.createMultipleFilesNoJarsDescriptor()
                .withTitle(StudioBundle.message("dialog.ChooseTestFiles"))
                .withDescription(StudioBundle.message("message.ChooseTestFilesDescription"));
        StudioProjectFiles.chooseFilePaths(project, descriptor, callback);
    }
}
