package org.ikasan.studio.ui.actions;

import com.intellij.openapi.project.Project;
import org.ikasan.studio.core.StudioBuildException;
import org.ikasan.studio.core.generation.GenerationRequest;
import org.ikasan.studio.core.metapack.ComponentLibrary;
import org.ikasan.studio.core.metapack.model.ComponentMeta;
import org.ikasan.studio.core.model.ikasan.instance.ComponentProperty;
import org.ikasan.studio.core.model.ikasan.instance.Flow;
import org.ikasan.studio.core.model.ikasan.instance.FlowElement;
import org.ikasan.studio.core.model.ikasan.instance.FlowElementFactory;
import org.ikasan.studio.core.model.ikasan.instance.Module;
import org.ikasan.studio.intellij.project.StudioProjectFiles;
import org.ikasan.studio.ui.StudioBundle;
import org.ikasan.studio.ui.StudioUIUtils;
import org.ikasan.studio.ui.UiContext;
import org.ikasan.studio.ui.component.canvas.DesignerCanvas;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Locale;

/** Creates a visible, disposable JMS sink flow for exercising a producer against its real broker settings. */
public class CreateTestJmsConsumerFlowAction implements ActionListener {
    private static final String BASIC_JMS_CONSUMER = "Basic AMQ Spring JMS Consumer";
    private static final String DEV_NULL_PRODUCER = "Dev Null Producer";

    private final Project project;
    private final FlowElement producer;

    public CreateTestJmsConsumerFlowAction(Project project, FlowElement producer) {
        this.project = project;
        this.producer = producer;
    }

    /**
     * The generated module and Studio run in separate JVMs, so a Studio-side consumer cannot attach to an
     * ActiveMQ vm:// broker. Instead, this action adds the test consumer to the generated module itself.
     */
    public static boolean supports(FlowElement producer) {
        if (producer == null) return false;
        String providerUrl = stringValue(producer, "connectionFactoryJndiPropertyProviderUrl");
        String factory = stringValue(producer, "connectionFactoryJndiPropertyFactoryInitial");
        return providerUrl != null && providerUrl.startsWith("vm://")
                && factory != null && factory.toLowerCase(Locale.ROOT).contains("activemq");
    }

    @Override
    public void actionPerformed(ActionEvent event) {
        UiContext context = project.getService(UiContext.class);
        Module module = context.getIkasanModule();
        DesignerCanvas canvas = context.getDesignerCanvas();
        if (module == null || canvas == null) return;

        try {
            String version = module.getMetaVersion();
            String destination = stringValue(producer, "destinationJndiName");
            String suffix = destination == null || destination.isBlank() ? "JMS" : destination;
            Flow testFlow = new Flow(version);
            testFlow.setName(uniqueFlowName(module, "Test " + suffix));

            ComponentMeta consumerMeta = ComponentLibrary.getIkasanComponentByKeyMandatory(version, BASIC_JMS_CONSUMER);
            FlowElement consumer = FlowElementFactory.createFlowElement(version, consumerMeta, testFlow,
                    testFlow.getFlowRoute(), "Receive " + suffix);
            copyCompatibleConfiguration(producer, consumer);
            consumer.setPropertyValue("autoContentConversion", true);
            consumer.defaultUnsetMandatoryProperties();
            testFlow.setConsumer(consumer);

            ComponentMeta sinkMeta = ComponentLibrary.getIkasanComponentByKeyMandatory(version, DEV_NULL_PRODUCER);
            FlowElement sink = FlowElementFactory.createFlowElement(version, sinkMeta, testFlow,
                    testFlow.getFlowRoute(), "Discard " + suffix);
            sink.defaultUnsetMandatoryProperties();
            testFlow.getFlowRoute().getFlowElements().add(sink);
            module.addFlow(testFlow);

            // Reuse the normal insertion path so debug identity, generated class naming and code generation
            // stay identical to a developer choosing "Add Debug" manually.
            if (canvas.insertDebugComponentAfter(consumer) == null) {
                StudioProjectFiles.refreshCodeFromModel(project, GenerationRequest.moduleStructure(testFlow));
            }
            StudioUIUtils.displayIdeaInfoMessage(project,
                    StudioBundle.message("message.TestJmsConsumerFlowCreated", testFlow.getIdentity(), suffix));
        } catch (StudioBuildException | RuntimeException failure) {
            StudioUIUtils.displayIdeaWarnMessage(project,
                    StudioBundle.message("message.TestJmsConsumerFlowFailed", failure.getMessage()));
        }
    }

    private static void copyCompatibleConfiguration(FlowElement source, FlowElement target) {
        for (String propertyName : target.getComponentMeta().getPropertyKeys()) {
            if ("componentName".equals(propertyName) || "configuredResourceId".equals(propertyName)) continue;
            ComponentProperty sourceProperty = source.getProperty(propertyName);
            if (sourceProperty != null && sourceProperty.getValue() != null) {
                target.setPropertyValue(propertyName, sourceProperty.getValue());
            }
        }
    }

    private static String uniqueFlowName(Module module, String requested) {
        String candidate = requested;
        int suffix = 2;
        while (containsFlowNamed(module, candidate)) {
            candidate = requested + " " + suffix++;
        }
        return candidate;
    }

    private static boolean containsFlowNamed(Module module, String name) {
        return module.getFlows().stream().anyMatch(flow -> name.equals(flow.getIdentity()));
    }

    private static String stringValue(FlowElement element, String propertyName) {
        Object value = element.getPropertyValue(propertyName);
        return value == null ? null : value.toString();
    }
}
