package org.ikasan.studio.core.generator;

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

public class ModuleConfigTemplateTest extends AbstractGeneratorTestFixtures {

    /**
     * @see resources/studio/templates/org/ikasan/studio/generator/ModuleConfigEmptyIkasanModel.java
     * @throws IOException if the template cant be generated
     */
    @ParameterizedTest
    @MethodSource("org.ikasan.studio.core.TestFixtures#metaPacksToTest")
    public void testCreateModuleWith_emptyIkasanModel(String metaPackVersion) throws IOException, StudioBuildException, StudioGeneratorException {
        Module module = TestFixtures.getMyFirstModuleIkasanModule(metaPackVersion, new ArrayList<>());

        String templateString = ModuleConfigTemplate.generateContents(module);
        assertNotNull(templateString);
        assertEquals(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(metaPackVersion, module, ModuleConfigTemplate.MODULE_CLASS_NAME + "EmptyIkasanModel.java"), templateString);
    }

    /**
     * @see resources/studio/templates/org/ikasan/studio/generator/ModuleConfigOneFlow.java
     * @throws IOException if the template cant be generated
     */
    @ParameterizedTest
    @MethodSource("org.ikasan.studio.core.TestFixtures#metaPacksToTest")
    public void testCreateModuleWith_oneFlow(String metaPackVersion) throws IOException, StudioBuildException, StudioGeneratorException {
        Flow flow1 = TestFixtures.getUnbuiltFlow(metaPackVersion).build();
        Module module = TestFixtures.getMyFirstModuleIkasanModule(metaPackVersion, Collections.singletonList(flow1));

        String templateString = ModuleConfigTemplate.generateContents(module);
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

        String templateString = ModuleConfigTemplate.generateContents(module);
        assertNotNull(templateString);
        assertTrue(templateString.contains("\"classpath:filetransfer-service-conf.xml\""),
                "ModuleConfig for a module containing an FtpConsumer must @ImportResource filetransfer-service-conf.xml so BaseFileTransferDao is available");
    }

    @Test
    public void testCreateModuleWith_ftpConsumer_v4_0_x_addsImportConfigurationClass() throws StudioBuildException, StudioGeneratorException {
        Module module = buildModuleWithFtpConsumer(TestFixtures.META_IKASAN_PACK_4_1_6);

        String templateString = ModuleConfigTemplate.generateContents(module);
        assertNotNull(templateString);
        assertTrue(templateString.contains("org.ikasan.connector.basefiletransfer.BaseFileTransferAutoConfiguration.class"),
                "ModuleConfig for a module containing an FtpConsumer must @Import BaseFileTransferAutoConfiguration so BaseFileTransferDao is available");
    }
}