package org.ikasan.studio.ui.actions;

import com.fasterxml.jackson.databind.JsonNode;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import org.ikasan.studio.core.model.ikasan.instance.BasicElement;
import org.ikasan.studio.core.model.ikasan.instance.FlowElement;
import org.ikasan.studio.core.model.ikasan.instance.Module;
import org.ikasan.studio.integration.ikasan.StudioInjectClient;
import org.ikasan.studio.ui.StudioBundle;
import org.ikasan.studio.ui.StudioUIUtils;
import org.ikasan.studio.ui.UiContext;
import org.ikasan.studio.intellij.execution.IkasanDebugSessionService;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Fires a time-event (Quartz-scheduled) Consumer's flow immediately, via the same /rest/studio/inject/{flowName}
 * endpoint SendTestMessageAction uses, but without prompting for a payload. Unlike a file/message-based
 * Consumer, a plain Scheduled Consumer's real MessageProvider hands back a Quartz JobExecutionContext, not
 * text - there's no meaningful "payload" to simulate. Server-side, StudioInjectController recognises any
 * org.ikasan.scheduler.ScheduledComponent consumer and calls Scheduler.triggerJob() on its real JobDetail
 * instead of building a synthetic event, so this genuinely fires the flow now (same production code path)
 * rather than waiting for the next cron fire. See ComponentMeta#isTimeEventConsumer().
 */
public class TriggerScheduledConsumerAction implements ActionListener {
    private static final Logger LOG = Logger.getInstance("#TriggerScheduledConsumerAction");
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

        TestInjectionTask.run(project, StudioBundle.message("message.TriggeringScheduledConsumer"), flowName,
                () -> StudioInjectClient.postPayload(module, flowName, NO_PAYLOAD, null),
                responseBody -> {
                    String identifier = responseBody.path("identifier").asText("");
                    boolean fileConsumer = flowElement.getComponentMeta().isFileBasedConsumer();
                    String criteriaText = fileConsumer ? formatCriteria(responseBody.path("criteria")) : "";
                    LOG.info("STUDIO: Triggered scheduled consumer for flow " + flowName
                            + " identifier " + identifier + " criteria [" + criteriaText + "]");
                    return TestInjectionTask.Notice.info(fileConsumer
                            ? StudioBundle.message("message.ScheduledConsumerTriggeredWithCriteria", identifier, criteriaText)
                            : StudioBundle.message("message.ScheduledConsumerTriggered", identifier));
                });
    }

    /**
     * Flattens the module-reported scan criteria (a small JSON object of configured consumer values such as
     * minimumAgeSeconds, filterDuplicates and filenamePattern) into a single display/log line. Older generated
     * modules that predate criteria reporting return the unavailable placeholder.
     */
    private static String formatCriteria(JsonNode criteriaNode) {
        if (criteriaNode == null || !criteriaNode.isObject() || criteriaNode.isEmpty()) {
            return StudioBundle.message("message.ScanCriteriaUnavailable");
        }
        java.util.List<String> entries = new java.util.ArrayList<>();
        criteriaNode.fields().forEachRemaining(entry ->
                entries.add(entry.getKey() + "=" + nodeToText(entry.getValue())));
        return String.join(", ", entries);
    }

    private static String nodeToText(JsonNode node) {
        if (node.isArray()) {
            java.util.List<String> items = new java.util.ArrayList<>();
            node.forEach(item -> items.add(item.asText()));
            return String.join(", ", items);
        }
        return node.asText();
    }
}
