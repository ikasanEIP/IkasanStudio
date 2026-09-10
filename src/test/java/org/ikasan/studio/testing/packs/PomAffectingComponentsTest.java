package org.ikasan.studio.testing.packs;

import org.ikasan.studio.core.generator.*;

import org.ikasan.studio.core.StudioBuildException;
import org.ikasan.studio.core.TestFixtures;
import org.ikasan.studio.core.model.ikasan.instance.Module;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Not many components result in the need for adsitional elements in the pom.
 */
@org.junit.jupiter.api.Tag("packs")
public class PomAffectingComponentsTest extends AbstractGeneratorTestFixtures {
    @ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings = {"SFTP Consumer", "SFTP Producer"})
    void java11SftpComponentsDeclareEd25519Provider(String componentName) throws Exception {
        var component = org.ikasan.studio.core.metapack.ComponentLibrary
                .getIkasanComponentByKeyMandatory("V3.3.9", componentName);
        var providers = component.getJarDependencies().stream()
                .filter(dependency -> "org.bouncycastle".equals(dependency.getGroupId())
                        && "bcprov-jdk18on".equals(dependency.getArtifactId()))
                .toList();
        assertEquals(1, providers.size());
        assertEquals("1.85.2", providers.get(0).getVersion());
        org.junit.jupiter.api.Assertions.assertFalse(Boolean.parseBoolean(providers.get(0).getOptional()),
                "The provider must reach the generated application's runtime classpath");
    }

    @ParameterizedTest
    @MethodSource("org.ikasan.studio.testing.packs.PackExpectations#metaPacksToTest")
    void nestedRoutesContributeDependenciesAndSpringImports(String pack) throws Exception {
        var email = TestFixtures.getEmailProducer(pack);
        var nestedEmail = TestFixtures.getEmailProducer(pack);
        var support = org.ikasan.studio.core.model.ikasan.instance.FlowElement.flowElementBuilder()
                .componentMeta(org.ikasan.studio.core.metapack.model.ComponentMeta.builder().name("Nested support").componentTypeMeta(email.getComponentMeta().getComponentTypeMeta())
                        .importResources(java.util.Set.of("classpath:nested-context.xml"))
                        .importConfigurationClasses(java.util.Set.of("example.NestedConfiguration"))
                        .build()).componentName("nested support").build();
        var flow = TestFixtures.getUnbuiltFlow(pack)
                .consumer(TestFixtures.getEventGeneratingConsumer(pack)).build();
        var nested = org.ikasan.studio.core.model.ikasan.instance.FlowRoute.flowRouteBuilder()
                .flow(flow).routeName("nested")
                .flowElements(java.util.List.of(nestedEmail, support)).build();
        var branch = org.ikasan.studio.core.model.ikasan.instance.FlowRoute.flowRouteBuilder()
                .flow(flow).routeName("route2").flowElements(java.util.List.of(email))
                .childRoutes(java.util.List.of(nested)).build();
        flow.setFlowRoute(org.ikasan.studio.core.model.ikasan.instance.FlowRoute.flowRouteBuilder()
                .flow(flow).flowElements(java.util.List.of()).childRoutes(java.util.List.of(branch)).build());
        Module module = TestFixtures.getMyFirstModuleIkasanModule(pack, java.util.List.of(flow));

        assertEquals(1, module.getAllUniqueSortedJarDependencies().stream()
                .filter(d -> "org.ikasan".equals(d.getGroupId())
                        && "ikasan-email-endpoint".equals(d.getArtifactId())).count());
        org.junit.jupiter.api.Assertions.assertTrue(module.getAllUniqueSortedImportResources()
                .contains("classpath:nested-context.xml"));
        org.junit.jupiter.api.Assertions.assertTrue(module.getAllUniqueSortedImportConfigurationClasses()
                .contains("example.NestedConfiguration"));
    }

    /**
     * See also application_emptyFlow.properties
     * @throws IOException if the template cant be generated
     */
    @ParameterizedTest
    @MethodSource("org.ikasan.studio.testing.packs.PackExpectations#metaPacksToTest")
    public void testCreateProperties_emptyFlow_with_non_default_port(String metaPackVersion) throws IOException, StudioBuildException, StudioGeneratorException {
        Module module = TestFixtures.getMyFirstModuleIkasanModule(metaPackVersion, new ArrayList<>());
        String templateString = PropertiesTemplate.create(module);
        assertNotNull(templateString);
        assertEquals(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(metaPackVersion, module, PropertiesTemplate.MODULE_PROPERTIES_FILENAME + "_emptyFlow.properties"), templateString);
    }
}