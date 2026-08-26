package org.ikasan.studio.ui.actions;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import org.ikasan.studio.core.model.ikasan.instance.BasicElement;
import org.ikasan.studio.core.model.ikasan.instance.FlowElement;
import org.ikasan.studio.core.model.ikasan.instance.Module;
import org.ikasan.studio.ui.StudioBundle;
import org.ikasan.studio.ui.StudioUIUtils;
import org.ikasan.studio.ui.UiContext;
import org.ikasan.studio.ui.intellij.IkasanDebugSessionService;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.http.HttpResponse;

/**
 * Fires a time-event (Quartz-scheduled) Consumer's flow immediately, via the same /studio/inject/{flowName}
 * endpoint SendTestMessageAction uses, but without prompting for a payload. Unlike a file/message-based
 * Consumer, a plain Scheduled Consumer's real MessageProvider hands back a Quartz JobExecutionContext, not
 * text - there's no meaningful "payload" to simulate, so this just triggers the flow now rather than waiting
 * for the next cron fire. See ComponentMeta#isTimeEventConsumer().
 */
public class TriggerScheduledConsumerAction implements ActionListener {
    // No real payload applies to a time-based trigger - see class javadoc.
    private static final String NO_PAYLOAD = "";

    private final Project project;
    private final BasicElement ikasanBasicElement;

    public TriggerScheduledConsumerAction(Project project, BasicElement ikasanBasicElement) {
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

        ProgressManager.getInstance().run(new Task.Backgroundable(project, StudioBundle.message("message.TriggeringScheduledConsumer")) {
            // Deliberately not @NotNull-annotated: this project avoids @NotNull (see CLAUDE.md) because
            // the IntelliJ Gradle plugin instruments it with a runtime assertion that would surface as an
            // uncaught plugin exception rather than failing gracefully.
            @SuppressWarnings("NullableProblems")
            @Override
            public void run(ProgressIndicator indicator) {
                try {
                    HttpResponse<String> response = StudioInjectClient.postPayload(module, flowName, NO_PAYLOAD);

                    if (response.statusCode() == 200) {
                        JsonNode responseBody = new ObjectMapper().readTree(response.body());
                        String identifier = responseBody.path("identifier").asText("");
                        ApplicationManager.getApplication().invokeLater(() ->
                                StudioUIUtils.displayIdeaInfoMessage(project, StudioBundle.message("message.ScheduledConsumerTriggered", identifier)));
                    } else if (response.statusCode() == 401) {
                        ApplicationManager.getApplication().invokeLater(() ->
                                StudioUIUtils.displayIdeaWarnMessage(project, StudioBundle.message("message.TestMessageAuthenticationFailed")));
                    } else {
                        String errorDetail = response.statusCode() + ": " + response.body();
                        ApplicationManager.getApplication().invokeLater(() ->
                                StudioUIUtils.displayIdeaWarnMessage(project, StudioBundle.message("message.CouldNotSendTestMessage", errorDetail)));
                    }
                } catch (Exception e) {
                    ApplicationManager.getApplication().invokeLater(() ->
                            StudioUIUtils.displayIdeaWarnMessage(project, StudioBundle.message("message.CouldNotSendTestMessage", e.getMessage())));
                }
            }
        });
    }
}
