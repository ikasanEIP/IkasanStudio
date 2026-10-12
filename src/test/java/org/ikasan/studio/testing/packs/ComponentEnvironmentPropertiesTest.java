package org.ikasan.studio.testing.packs;

import org.ikasan.studio.core.TestFixtures;
import org.ikasan.studio.core.generator.FlowsComponentFactoryTemplate;
import org.ikasan.studio.core.generator.ModelTemplate;
import org.ikasan.studio.core.generator.PropertiesTemplate;
import org.ikasan.studio.core.io.ComponentIO;
import org.ikasan.studio.core.metapack.ComponentLibrary;
import org.ikasan.studio.core.model.ikasan.instance.FlowElement;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.StringReader;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@org.junit.jupiter.api.Tag("packs")
class ComponentEnvironmentPropertiesTest {
    @ParameterizedTest
    @MethodSource("org.ikasan.studio.testing.packs.PackExpectations#metaPacksToTest")
    void builtInValueSettingsHavePropertyBindingsWhileJavaObjectsRemainReferences(String pack) throws Exception {
        for (var component : ComponentLibrary.getIkasanComponents(pack).values()) {
            for (var property : component.getAllowableProperties().values()) {
                if (!property.isSetterProperty()) continue;
                String context = component.getName() + "." + property.getPropertyName();
                if (property.isUserSuppliedClass() || "classLiteral".equals(property.getUsageDataType())
                        || Set.of("configurationId", "configuredResourceId").contains(property.getPropertyName())) {
                    assertTrue(property.getPropertyConfigFileLabel() == null
                            || property.getPropertyConfigFileLabel().isBlank(), context);
                } else {
                    assertNotNull(property.getPropertyConfigFileLabel(), context);
                    assertFalse(property.getPropertyConfigFileLabel().isBlank(), context);
                }
            }
        }
    }

    @ParameterizedTest
    @MethodSource("org.ikasan.studio.testing.packs.PackExpectations#metaPacksToTest")
    void jmsMapsAndZeroFalseValuesSurviveModelReloadAndReachPropertyBackedSetters(String pack) throws Exception {
        var consumer = FlowElement.flowElementBuilder()
                .componentMeta(ComponentLibrary.getIkasanComponentByKeyMandatory(pack, "Spring JMS Consumer"))
                .componentName("Receive").build();
        Map<String, String> jndi = Map.of("java.naming.provider.url", "tcp://broker.example:61616");
        consumer.setPropertyValue("connectionFactoryJndiProperties", jndi);
        consumer.setPropertyValue("receiveTimeout", 0L);
        consumer.setPropertyValue("pubSubDomain", false);
        var flow = TestFixtures.getUnbuiltFlow(pack).consumer(consumer).build();
        consumer.setContainingFlowRoute(flow.getFlowRoute());
        var module = TestFixtures.getMyFirstModuleIkasanModule(pack, List.of(flow));
        var restored = ComponentIO.validatePersistedModuleJson(ModelTemplate.create(module), "JMS environment settings", false);
        var restoredConsumer = restored.getFlows().get(0).getConsumer();
        assertEquals(jndi, restoredConsumer.getPropertyValue("connectionFactoryJndiProperties"));
        String factory = FlowsComponentFactoryTemplate.create("example", restored, restored.getFlows().get(0));
        Properties properties = new Properties();
        properties.load(new StringReader(PropertiesTemplate.create(restored)));
        assertEquals("0", properties.getProperty("myflow1.receive.jms.consumer.receiveTimeout"));
        assertEquals("false", properties.getProperty("myflow1.receive.jms.consumer.pubSubDomain"));
        assertEquals(jndi, org.ikasan.studio.core.persistence.json.StudioJson.newObjectMapper().readValue(
                properties.getProperty("myflow1.receive.jms.consumer.connectionFactoryJndiProperties"), Map.class));
        assertTrue(factory.contains(".setConnectionFactoryJndiProperties(studioStringMap("), factory);
        assertTrue(factory.contains(".setReceiveTimeout(myFlow1ReceiveJmsConsumerReceiveTimeout)"), factory);
        assertTrue(factory.contains(".setPubSubDomain(myFlow1ReceiveJmsConsumerPubSubDomain)"), factory);
    }
}
