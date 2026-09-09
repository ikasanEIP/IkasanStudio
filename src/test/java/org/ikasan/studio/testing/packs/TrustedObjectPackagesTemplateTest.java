package org.ikasan.studio.testing.packs;

import org.ikasan.studio.core.TestFixtures;
import org.ikasan.studio.core.metapack.ComponentLibrary;
import org.ikasan.studio.core.model.ikasan.instance.FlowElement;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.ArrayList;
import static org.assertj.core.api.Assertions.*;

@org.junit.jupiter.api.Tag("packs")
class TrustedObjectPackagesTemplateTest extends AbstractGeneratorTestFixtures {
    static FlowElement jms(String pack, String role) throws Exception {
        var component = FlowElement.flowElementBuilder()
                .componentMeta(ComponentLibrary.getIkasanComponentByKeyMandatory(pack, "Spring JMS " + role))
                .componentName("Orders").build();
        component.setPropertyValue("connectionFactoryJndiPropertyFactoryInitial", "org.apache.activemq.jndi.ActiveMQInitialContextFactory");
        component.setPropertyValue("connectionFactoryJndiPropertyProviderUrl", "vm://embedded-broker?broker.persistent=false");
        component.setPropertyValue("connectionFactoryName", "ConnectionFactory");
        component.setPropertyValue("destinationJndiName", "orders");
        return component;
    }

    private String render(String pack, FlowElement component) throws Exception {
        return generateFlowsComponentFactoryTemplateString(pack,
                TestFixtures.getMyFirstModuleIkasanModule(pack, new ArrayList<>()), component);
    }

    @ParameterizedTest
    @MethodSource("org.ikasan.studio.testing.packs.PackExpectations#metaPacksToTest")
    void trustIsOptInAndPreservesTheJndiBuilderSettings(String pack) throws Exception {
        for (String role : new String[]{"Consumer", "Producer"}) {
            var component = jms(pack, role);
            assertThat(render(pack, component)).doesNotContain("TrustedOrdersContextFactory");
            component.setPropertyValue("trustedObjectPackages", "org.example.cat.domain, org.example.shared");
            String generated = render(pack, component);
            assertThat(generated).contains("TrustedOrdersContextFactory.initialFactoryName(",
                    "super.createConnectionFactory(environment)", "activeMq.getTrustedPackages()",
                    "setConnectionFactoryJndiPropertyProviderUrl(", "setDestinationJndiName(",
                    "org.example.cat.domain, org.example.shared").doesNotContain("setTrustAllPackages", "setTrustedObjectPackages");
        }
    }

    @ParameterizedTest
    @MethodSource("org.ikasan.studio.testing.packs.PackExpectations#metaPacksToTest")
    void explicitlySuppliedFactoryIsConfiguredWithoutReplacingJndiSettings(String pack) throws Exception {
        var component = jms(pack, "Consumer");
        component.setPropertyValue("trustedObjectPackages", "org.example.cat.domain");
        component.setPropertyValue("connectionFactory", "ordersFactory");
        assertThat(render(pack, component)).contains(".setConnectionFactory(TrustedOrdersContextFactory.configure(ordersFactory))")
                .doesNotContain(".setConnectionFactoryJndiPropertyFactoryInitial(TrustedOrdersContextFactory.initialFactoryName(");
    }

    @ParameterizedTest
    @MethodSource("org.ikasan.studio.testing.packs.PackExpectations#metaPacksToTest")
    void invalidTrustSettingsCannotBeGeneratedFromAnImportedModel(String pack) throws Exception {
        var component = jms(pack, "Consumer");
        component.setPropertyValue("trustedObjectPackages", "*");
        assertThatThrownBy(() -> render(pack, component)).hasStackTraceContaining("without wildcards");
        component.setPropertyValue("trustedObjectPackages", "org.example.cat.domain");
        component.setPropertyValue("connectionFactoryJndiProperties", "customJndiSettings");
        assertThatThrownBy(() -> render(pack, component)).hasStackTraceContaining("custom JNDI properties map");
    }
}
