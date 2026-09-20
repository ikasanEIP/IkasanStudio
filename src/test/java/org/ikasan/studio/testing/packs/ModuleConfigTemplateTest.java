package org.ikasan.studio.testing.packs;

import org.ikasan.studio.core.generator.*;

import org.ikasan.studio.core.StudioBuildException;
import org.ikasan.studio.core.TestFixtures;
import org.ikasan.studio.core.model.ikasan.instance.Flow;
import org.ikasan.studio.core.model.ikasan.instance.FlowElement;
import org.ikasan.studio.core.model.ikasan.instance.FlowRoute;
import org.ikasan.studio.core.model.ikasan.instance.Module;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@org.junit.jupiter.api.Tag("packs")
public class ModuleConfigTemplateTest extends AbstractGeneratorTestFixtures {

    /**
     * Descriptions and names are free text. A quote or backslash in one (e.g. a description of Handles "urgent"
     * orders) must be escaped in the Java string literal it becomes, or the generated project does not compile.
     */
    @ParameterizedTest
    @MethodSource("org.ikasan.studio.testing.packs.PackExpectations#metaPacksToTest")
    public void descriptionsAndNamesWithQuotesAndBackslashesAreEscapedInGeneratedJava(String metaPackVersion) throws Exception {
        Module module = TestFixtures.getMyFirstModuleIkasanModule(metaPackVersion, new ArrayList<>());
        module.setPropertyValue("description", "Handles \"urgent\" orders in C:\\data");
        Flow flow = TestFixtures.getUnbuiltFlow(metaPackVersion).build();
        flow.setName("Order \"A\"");
        flow.setPropertyValue("description", "Flow \"desc\" C:\\in");
        module.addFlow(flow);
        FlowElement consumer = org.ikasan.studio.core.model.ikasan.instance.FlowElementFactory.createFlowElement(metaPackVersion,
                org.ikasan.studio.core.metapack.ComponentLibrary.getIkasanComponentByKeyMandatory(metaPackVersion, "Event Generating Consumer"),
                flow, flow.getFlowRoute(), "Read \"in\"");
        consumer.defaultUnsetMandatoryProperties();
        flow.setConsumer(consumer);
        FlowElement producer = org.ikasan.studio.core.model.ikasan.instance.FlowElementFactory.createFlowElement(metaPackVersion,
                org.ikasan.studio.core.metapack.ComponentLibrary.getIkasanComponentByKeyMandatory(metaPackVersion, "Dev Null Producer"),
                flow, flow.getFlowRoute(), "Sink \\ end");
        producer.defaultUnsetMandatoryProperties();
        flow.getFlowRoute().insertFlowElement(0, producer);

        String moduleConfig = ModuleConfigTemplate.create(module);
        assertTrue(moduleConfig.contains(".withDescription(\"Handles \\\"urgent\\\" orders in C:\\\\data\")"), moduleConfig);
        String flowSource = FlowTemplate.create(TestFixtures.DEFAULT_PACKAGE, module, flow);
        assertTrue(flowSource.contains(".withDescription(\"Flow \\\"desc\\\" C:\\\\in\")"), flowSource);
        assertTrue(flowSource.contains("getFlowBuilder(\"Order \\\"A\\\"\")"), flowSource);
        assertTrue(flowSource.contains("\"Read \\\"in\\\"\""), flowSource);
        assertTrue(flowSource.contains("\"Sink \\\\ end\""), flowSource);
    }

    /**
     * A flow called Default or Import is plausible, but the generated variable and package must not be the Java
     * keyword itself or the project does not compile.
     */
    @ParameterizedTest
    @MethodSource("org.ikasan.studio.testing.packs.PackExpectations#metaPacksToTest")
    public void flowsNamedAfterJavaKeywordsGenerateLegalVariablesAndPackages(String metaPackVersion) throws Exception {
        Module module = TestFixtures.getMyFirstModuleIkasanModule(metaPackVersion, new ArrayList<>());
        Flow flow = TestFixtures.getUnbuiltFlow(metaPackVersion).build();
        flow.setName("Import");
        module.addFlow(flow);
        FlowElement consumer = org.ikasan.studio.core.model.ikasan.instance.FlowElementFactory.createFlowElement(metaPackVersion,
                org.ikasan.studio.core.metapack.ComponentLibrary.getIkasanComponentByKeyMandatory(metaPackVersion, "Event Generating Consumer"),
                flow, flow.getFlowRoute(), "Default");
        consumer.defaultUnsetMandatoryProperties();
        flow.setConsumer(consumer);

        String moduleConfig = ModuleConfigTemplate.create(module);
        assertTrue(moduleConfig.contains("flow.import_.Import import_;"), moduleConfig);
        assertTrue(moduleConfig.contains(".addFlow(import_.getImport())"), moduleConfig);
        String flowSource = FlowTemplate.create(TestFixtures.DEFAULT_PACKAGE, module, flow);
        assertTrue(flowSource.contains("Flow import_ = flowBuilder"), flowSource);
        String factory = FlowsComponentFactoryTemplate.create(TestFixtures.DEFAULT_PACKAGE, module, flow);
        assertTrue(!factory.matches("(?s).*\\b(default|import)\\s*;.*"), factory);
    }

    /** A flow named entirely in non-Latin script must not generate an empty package segment ("flow..."). */
    @ParameterizedTest
    @MethodSource("org.ikasan.studio.testing.packs.PackExpectations#metaPacksToTest")
    public void flowNamedEntirelyInNonLatinScriptGeneratesALegalPackage(String metaPackVersion) throws Exception {
        Module module = TestFixtures.getMyFirstModuleIkasanModule(metaPackVersion, new ArrayList<>());
        Flow flow = TestFixtures.getUnbuiltFlow(metaPackVersion).build();
        flow.setName("\u65e5\u672c\u8a9e");
        module.addFlow(flow);

        String moduleConfig = ModuleConfigTemplate.create(module);
        assertTrue(!moduleConfig.contains("flow.."), moduleConfig);
        assertTrue(moduleConfig.matches("(?s).*flow\\._[0-9a-f]+\\.\\S+ \\S+;.*"), moduleConfig);
    }

    /**
     * Expected output: src/test/resources/studio/templates/org/ikasan/studio/generator/&lt;version&gt;/Module/ModuleConfigEmptyIkasanModel.java
     * @throws IOException if the template cant be generated
     */
    @ParameterizedTest
    @MethodSource("org.ikasan.studio.testing.packs.PackExpectations#metaPacksToTest")
    public void testCreateModuleWith_emptyIkasanModel(String metaPackVersion) throws IOException, StudioBuildException, StudioGeneratorException {
        Module module = TestFixtures.getMyFirstModuleIkasanModule(metaPackVersion, new ArrayList<>());

        String templateString = ModuleConfigTemplate.create(module);
        assertNotNull(templateString);
        assertEquals(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(metaPackVersion, module, ModuleConfigTemplate.MODULE_CLASS_NAME + "EmptyIkasanModel.java"), templateString);
    }

    /**
     * Expected output: src/test/resources/studio/templates/org/ikasan/studio/generator/&lt;version&gt;/Module/ModuleConfigOneFlow.java
     * @throws IOException if the template cant be generated
     */
    @ParameterizedTest
    @MethodSource("org.ikasan.studio.testing.packs.PackExpectations#metaPacksToTest")
    public void testCreateModuleWith_oneFlow(String metaPackVersion) throws IOException, StudioBuildException, StudioGeneratorException {
        Flow flow1 = TestFixtures.getUnbuiltFlow(metaPackVersion).build();
        Module module = TestFixtures.getMyFirstModuleIkasanModule(metaPackVersion, Collections.singletonList(flow1));

        String templateString = ModuleConfigTemplate.create(module);
        assertNotNull(templateString);
        assertEquals(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(metaPackVersion, module, ModuleConfigTemplate.MODULE_CLASS_NAME + "OneFlow.java"), templateString);
    }

    /**
     * FTP/SFTP components need Spring beans (e.g. BaseFileTransferDao) that aren't discoverable via
     * component-scan - the generated ModuleConfig must pull them in via @ImportResource (V3.3.9, XML-based)
     * or @Import (V4.1.6, Java @Configuration-class-based). See ComponentMeta#importResources /
     * #importConfigurationClasses and Module#getAllUniqueSortedImportResources /
     * #getAllUniqueSortedImportConfigurationClasses.
     */
    private Module buildModuleWithFtpConsumer(String metaPackVersion) throws StudioBuildException {
        Module module = TestFixtures.getMyFirstModuleIkasanModule(metaPackVersion, new ArrayList<>());
        Flow flow = TestFixtures.getUnbuiltFlow(metaPackVersion).metapackVersion(metaPackVersion).build();
        module.addFlow(flow);
        FlowElement flowElement = TestFixtures.getFtpConsumer(metaPackVersion);
        flowElement.setContainingFlowRoute(flow.getFlowRoute());
        flow.setFlowRoute(FlowRoute.flowRouteBuilder().flowElements(Collections.singletonList(flowElement)).flow(flow).build());
        return module;
    }

    @Test
    public void testCreateModuleWith_ftpConsumer_v3_3_8_addsImportResource() throws StudioBuildException, StudioGeneratorException {
        Module module = buildModuleWithFtpConsumer(TestFixtures.META_IKASAN_PACK_3_3_9);

        String templateString = ModuleConfigTemplate.create(module);
        assertNotNull(templateString);
        assertTrue(templateString.contains("\"classpath:filetransfer-service-conf.xml\""),
                "ModuleConfig for a module containing an FtpConsumer must @ImportResource filetransfer-service-conf.xml so BaseFileTransferDao is available");
    }

    @Test
    public void testCreateModuleWith_ftpConsumer_v4_0_x_addsImportConfigurationClass() throws StudioBuildException, StudioGeneratorException {
        Module module = buildModuleWithFtpConsumer(TestFixtures.META_IKASAN_PACK_4_1_6);

        String templateString = ModuleConfigTemplate.create(module);
        assertNotNull(templateString);
        assertTrue(templateString.contains("org.ikasan.connector.basefiletransfer.BaseFileTransferAutoConfiguration.class"),
                "ModuleConfig for a module containing an FtpConsumer must @Import BaseFileTransferAutoConfiguration so BaseFileTransferDao is available");
    }
}