package org.ikasan.studio.core.generator;

import org.ikasan.studio.core.model.ikasan.instance.Module;
import org.ikasan.studio.core.model.ikasan.instance.Flow;
import java.util.Map;
import java.util.LinkedHashMap;

/** Studio-owned test connection mappings. Contains property keys, never connection credentials. */
public final class ModuleTestWiring {
    public static final String PATH = "generated/src/main/resources/studio-flow-test-wiring.json";
    public static final int SCHEMA_VERSION = 1;
    private ModuleTestWiring() { }

    public static String create(Module module) throws StudioGeneratorException {
        try {
            Map<String, Object> data = new LinkedHashMap<>(values(module));
            data.put("schemaVersion", SCHEMA_VERSION);
            data.put("modelFingerprint", FlowTestScaffold.supportFingerprint(module));
            data.put("modelFields", FlowTestScaffold.supportModelFields(module));
            return org.ikasan.studio.core.persistence.json.StudioJson.newObjectMapper()
                    .writerWithDefaultPrettyPrinter().writeValueAsString(data);
        } catch (Exception failure) { throw new StudioGeneratorException("Cannot generate flow-test wiring", failure); }
    }

    static Map<String, Object> values(Module module) {
        Map<String, Object> values = new LinkedHashMap<>();
        java.util.Set<String> propertyKeys = new java.util.TreeSet<>();
        for (var moduleFlow : module.getFlows()) for (var component : moduleFlow.getFlowElementsNoExternalEndPoints()) {
            for (var property : component.getComponentProperties().values()) {
                String label = property.getMeta().getPropertyConfigFileLabel();
                if (label != null && !label.isBlank() && !property.valueNotSet()) {
                    propertyKeys.add(org.ikasan.studio.core.StudioBuildUtils.substitutePlaceholderInLowerCase(module, moduleFlow, component, label));
                }
            }
        }
        java.util.Set<String> sampleConsumerClasses = new java.util.TreeSet<>();
        for (var moduleFlow : module.getFlows()) {
            var consumer = moduleFlow.getConsumer();
            if (consumer == null || !"sample-submission".equals(consumer.getComponentMeta().getFlowTestInputMode())) continue;
            String implementationClass = consumer.getPropertyValueAsString("userImplementedClassName");
            if (implementationClass != null && !implementationClass.isBlank()) {
                sampleConsumerClasses.add(implementationClass.contains(".") ? implementationClass
                        : GeneratorUtils.getUserImplementedClassesPackageName(module, moduleFlow) + "." + implementationClass);
            }
        }
        values.put("sampleConsumerClasses", sampleConsumerClasses);
        // The capability flag defines the connection contract; labels remain pack-owned.
        java.util.List<Map<String, String>> ftpEndpoints = new java.util.ArrayList<>();
        for (var moduleFlow : module.getFlows()) for (var component : moduleFlow.getFlowElementsNoExternalEndPoints()) {
            if (!component.getComponentMeta().supportsTestFtpServer()) continue;
            Map<String, String> endpoint = new LinkedHashMap<>();
            endpoint.put("name", moduleFlow.getIdentity() + " / " + component.getIdentity());
            endpoint.put("flow", moduleFlow.getIdentity());
            endpoint.put("component", component.getIdentity());
            endpoint.put("consumer", Boolean.toString(component.getComponentMeta().isConsumer()));
            endpoint.put("secure", component.getPropertyValueAsString("ftps"));
            for (String name : java.util.List.of("remoteHost", "remotePort", "username", "password",
                    component.getComponentMeta().isProducer() ? "outputDirectory" : "sourceDirectory")) {
                var property = component.getProperty(name);
                String label = property == null ? null : property.getMeta().getPropertyConfigFileLabel();
                endpoint.put(name.endsWith("Directory") ? "directory" : name,
                        label == null || label.isBlank() || property.valueNotSet() ? "" :
                        org.ikasan.studio.core.StudioBuildUtils.substitutePlaceholderInLowerCase(module, moduleFlow, component, label));
            }
            ftpEndpoints.add(endpoint);
        }
        values.put("ftpEndpoints", ftpEndpoints);
        java.util.List<Map<String, String>> smtpEndpoints = new java.util.ArrayList<>();
        for (var moduleFlow : module.getFlows()) for (var component : moduleFlow.getFlowElementsNoExternalEndPoints()) {
            if (component.getComponentMeta().supportsTestMailServer()) {
                smtpEndpoints.add(Map.of("flow", moduleFlow.getIdentity(), "component", component.getIdentity()));
            }
        }
        values.put("smtpEndpoints", smtpEndpoints);
        java.util.List<Map<String, String>> sftpEndpoints = new java.util.ArrayList<>();
        for (var moduleFlow : module.getFlows()) for (var component : moduleFlow.getFlowElementsNoExternalEndPoints()) {
            if (component.getComponentMeta().supportsTestSftpServer()) {
                sftpEndpoints.add(Map.of("flow", moduleFlow.getIdentity(), "component", component.getIdentity(),
                        "consumer", Boolean.toString(component.getComponentMeta().isConsumer())));
            }
        }
        values.put("sftpEndpoints", sftpEndpoints);
        values.put("modulePropertyKeys", propertyKeys);
        // Reuse a common configured ActiveMQ consumer URL via Spring, without copying connection values.
        // Ambiguous modules retain explicit configuration rather than silently choosing another broker.
        Map<String, String> jmsBrokerKeys = new LinkedHashMap<>();
        for (var moduleFlow : module.getFlows()) {
            var consumer = moduleFlow.getConsumer();
            if (consumer == null || !"org.ikasan.component.endpoint.jms.spring.consumer.JmsContainerConsumer".equals(
                    consumer.getComponentMeta().getImplementingClass())) continue;
            var provider = consumer.getProperty("connectionFactoryJndiPropertyProviderUrl");
            if (provider == null) continue;
            String label = provider.getMeta().getPropertyConfigFileLabel();
            if (!provider.valueNotSet() && label != null && !label.isBlank()) {
                jmsBrokerKeys.put(consumer.getPropertyValueAsString("connectionFactoryJndiPropertyProviderUrl"),
                        org.ikasan.studio.core.StudioBuildUtils.substitutePlaceholderInLowerCase(module, moduleFlow, consumer, label));
            }
        }
        values.put("jmsBrokerPropertyKey", jmsBrokerKeys.size() == 1 && jmsBrokerKeys.keySet().iterator().next().startsWith("vm://")
                ? jmsBrokerKeys.values().iterator().next() : "");
        // A stable test override may redirect only endpoints sharing the unambiguous consumer broker.
        java.util.List<String> commonBrokerKeys = new java.util.ArrayList<>();
        if (jmsBrokerKeys.size() == 1) {
            String common = jmsBrokerKeys.keySet().iterator().next();
            for (var moduleFlow : module.getFlows()) for (var component : moduleFlow.getFlowElementsNoExternalEndPoints()) {
                var provider = component.getProperty("connectionFactoryJndiPropertyProviderUrl");
                if (provider != null && common.equals(component.getPropertyValueAsString("connectionFactoryJndiPropertyProviderUrl"))) {
                    String label = provider.getMeta().getPropertyConfigFileLabel();
                    if (label != null && !label.isBlank()) commonBrokerKeys.add(
                            org.ikasan.studio.core.StudioBuildUtils.substitutePlaceholderInLowerCase(module, moduleFlow, component, label));
                }
            }
        }
        values.put("jmsBrokerPropertyKeys", commonBrokerKeys);
        values.put("flowNames", module.getFlows().stream().map(Flow::getIdentity).toList());
        return values;
    }
}
