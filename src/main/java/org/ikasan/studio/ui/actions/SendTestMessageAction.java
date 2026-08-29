package org.ikasan.studio.ui.actions;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import org.ikasan.studio.core.model.ikasan.instance.BasicElement;
import org.ikasan.studio.core.model.ikasan.instance.FlowElement;
import org.ikasan.studio.core.model.ikasan.instance.Module;
import org.ikasan.studio.ui.StudioBundle;
import org.ikasan.studio.ui.StudioUIUtils;
import org.ikasan.studio.ui.UiContext;
import org.ikasan.studio.ui.intellij.IkasanDebugSessionService;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.ConnectException;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;

/**
 * Prompts for a payload and POSTs it to the running module's /studio/inject/{flowName} endpoint (generated
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

        String payload;
        String payloadClassName;
        if (flowElement.getComponentMeta().producesFileListPayload()) {
            // A real file picker instead of the generic text/JSON dialog - see ComponentMeta#producesFileListPayload
            // for why this is only offered when the declared payload really is java.util.List<java.io.File>.
            List<String> filePaths = chooseTestFilePaths();
            if (filePaths == null) {
                return;
            }
            try {
                payload = new ObjectMapper().writeValueAsString(filePaths);
            } catch (Exception e) {
                LOG.warn("STUDIO: Could not build JSON payload from chosen test files", e);
                StudioUIUtils.displayIdeaWarnMessage(project, StudioBundle.message("message.CouldNotSendTestMessage", e.getMessage()));
                return;
            }
            // Matches ComponentMeta#FILE_LIST_TYPE - resolved on the generated controller side via Jackson's
            // TypeFactory#constructFromCanonical (see studioInjectControllerTemplate_en.ftl), which already
            // knows how to deserialize each JSON string element into a real java.io.File.
            payloadClassName = flowElement.getEffectiveOutputTypeDescription();
        } else {
            SendTestMessagePayloadDialog payloadDialog = new SendTestMessagePayloadDialog(project, componentKey);
            if (!payloadDialog.showAndGet()) {
                return;
            }
            payload = payloadDialog.getPayload();
            payloadClassName = payloadDialog.getPayloadClassName();
        }

        ProgressManager.getInstance().run(new Task.Backgroundable(project, StudioBundle.message("message.SendingTestMessage")) {
            // Deliberately not @NotNull-annotated: this project avoids @NotNull (see CLAUDE.md) because
            // the IntelliJ Gradle plugin instruments it with a runtime assertion that would surface as an
            // uncaught plugin exception rather than failing gracefully.
            @SuppressWarnings("NullableProblems")
            @Override
            public void run(ProgressIndicator indicator) {
                try {
                    HttpResponse<String> response = StudioInjectClient.postPayload(module, flowName, payload, payloadClassName);

                    if (response.statusCode() == 200) {
                        JsonNode responseBody = new ObjectMapper().readTree(response.body());
                        String identifier = responseBody.path("identifier").asText("");
                        ApplicationManager.getApplication().invokeLater(() ->
                                StudioUIUtils.displayIdeaInfoMessage(project, StudioBundle.message("message.TestMessageSent", identifier)));
                    } else if (response.statusCode() == 401) {
                        ApplicationManager.getApplication().invokeLater(() ->
                                StudioUIUtils.displayIdeaWarnMessage(project, StudioBundle.message("message.TestMessageAuthenticationFailed")));
                    } else {
                        String errorDetail = response.statusCode() + ": " + response.body();
                        ApplicationManager.getApplication().invokeLater(() ->
                                StudioUIUtils.displayIdeaWarnMessage(project, StudioBundle.message("message.CouldNotSendTestMessage", errorDetail)));
                    }
                } catch (ConnectException e) {
                    // The single most common real-world cause: the debug process is alive (isDebugModuleRunning()
                    // above only checks the ProcessHandler, not whether Spring Boot has finished starting) but
                    // Tomcat hasn't bound its port yet - Spring only opens it right at the end of context
                    // refresh, and a JMS-backed consumer's listener container/JNDI setup can push that out by
                    // several seconds. Give a specific, actionable message rather than the generic one below.
                    LOG.warn("STUDIO: Could not send test message to flow " + flowName + " - module not yet accepting connections", e);
                    ApplicationManager.getApplication().invokeLater(() ->
                            StudioUIUtils.displayIdeaWarnMessage(project, StudioBundle.message("message.ModuleNotYetAcceptingConnections")));
                } catch (Exception e) {
                    // warn (not error): IntelliJ's logger renders error-level stack traces directly to the
                    // user, and this is already surfaced via the popup below - see CLAUDE.md.
                    LOG.warn("STUDIO: Could not send test message to flow " + flowName, e);
                    String errorDetail = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
                    ApplicationManager.getApplication().invokeLater(() ->
                            StudioUIUtils.displayIdeaWarnMessage(project, StudioBundle.message("message.CouldNotSendTestMessage", errorDetail)));
                }
            }
        });
    }

    /**
     * Lets the user pick one or more real files from disk, in place of typing/pasting a payload - only offered
     * when the target Consumer's declared payload really is java.util.List<java.io.File> (see
     * ComponentMeta#producesFileListPayload). Synchronous/modal, matching how the rest of this codebase already
     * uses the file chooser (see SendTestMessagePayloadDialog#loadFromFile).
     * @return the chosen files' absolute paths, or null if the dialog was cancelled (nothing chosen)
     */
    private List<String> chooseTestFilePaths() {
        FileChooserDescriptor descriptor = FileChooserDescriptorFactory.createMultipleFilesNoJarsDescriptor()
                .withTitle(StudioBundle.message("dialog.ChooseTestFiles"))
                .withDescription(StudioBundle.message("message.ChooseTestFilesDescription"));
        VirtualFile[] files = FileChooser.chooseFiles(descriptor, project, null);
        if (files.length == 0) {
            return null;
        }
        List<String> paths = new ArrayList<>();
        for (VirtualFile file : files) {
            paths.add(VfsUtilCore.virtualToIoFile(file).getAbsolutePath());
        }
        return paths;
    }
}
