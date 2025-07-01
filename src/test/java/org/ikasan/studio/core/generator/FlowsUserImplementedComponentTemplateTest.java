package org.ikasan.studio.core.generator;

import org.ikasan.studio.core.StudioBuildException;
import org.ikasan.studio.core.TestFixtures;
import org.ikasan.studio.core.model.ikasan.instance.FlowElement;
import org.ikasan.studio.core.model.ikasan.instance.Module;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class FlowsUserImplementedComponentTemplateTest extends AbstractGeneratorTestFixtures {
    //  ------------------------------- BROKER ----------------------------------
    /**
     * See also resources/studio/templates/org/ikasan/studio/generator/Converter/MyBroker.java
     * @throws IOException if the template cant be generated
     */
    @ParameterizedTest
    @MethodSource("org.ikasan.studio.core.TestFixtures#metaPacksToTest")
    public void testCreateFlowWith_brokerComponent(String metaPackVersion) throws IOException, StudioBuildException, StudioGeneratorException {
        Module module = TestFixtures.getMyFirstModuleIkasanModule(metaPackVersion, new ArrayList<>());
        FlowElement flowElement = TestFixtures.getBroker(metaPackVersion);
        String templateString = generateUserImplementedComponentTemplate(metaPackVersion, module, flowElement);
        assertNotNull(templateString);
        assertEquals(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(metaPackVersion, flowElement, "MyBroker.java"), templateString);
    }

    //  ------------------------------- CONVERTER ----------------------------------
    /**
     * See also resources/studio/templates/org/ikasan/studio/generator/Converter/MyConverter.java
     * @throws IOException if the template cant be generated
     */
    @ParameterizedTest
    @MethodSource("org.ikasan.studio.core.TestFixtures#metaPacksToTest")
    public void testCreateFlowWith_customConverterComponent(String metaPackVersion) throws IOException, StudioBuildException, StudioGeneratorException {
        Module module = TestFixtures.getMyFirstModuleIkasanModule(metaPackVersion, new ArrayList<>());
        FlowElement flowElement = TestFixtures.getCustomConverter(metaPackVersion);
        String templateString = generateUserImplementedComponentTemplate(metaPackVersion, module, flowElement);
        assertNotNull(templateString);
        assertEquals(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(metaPackVersion, flowElement, "MyConverter.java"), templateString);
    }

    //  ------------------------------- CONVERTER ----------------------------------
    /**
     * See also resources/studio/templates/org/ikasan/studio/generator/Converter/MyTranslator.java
     * @throws IOException if the template cant be generated
     */
    @ParameterizedTest
    @MethodSource("org.ikasan.studio.core.TestFixtures#metaPacksToTest")
    public void testCreateFlowWith_customTranslatorComponent(String metaPackVersion) throws IOException, StudioBuildException, StudioGeneratorException {
        Module module = TestFixtures.getMyFirstModuleIkasanModule(metaPackVersion, new ArrayList<>());
        FlowElement flowElement = TestFixtures.getCustomTranslator(metaPackVersion);
        String templateString = generateUserImplementedComponentTemplate(metaPackVersion, module, flowElement);
        assertNotNull(templateString);
        assertEquals(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(metaPackVersion, flowElement, "MyTranslator.java"), templateString);
    }

    //  ------------------------------- DEBUG ----------------------------------
    /**
     * See also resources/studio/templates/org/ikasan/studio/generator/Converter/MyConverter.java
     * @throws IOException if the template cant be generated
     */
    @ParameterizedTest
    @MethodSource("org.ikasan.studio.core.TestFixtures#metaPacksToTest")
    public void testCreateFlowWith_debugTransitionComponent(String metaPackVersion) throws IOException, StudioBuildException, StudioGeneratorException {
        Module module = TestFixtures.getMyFirstModuleIkasanModule(metaPackVersion, new ArrayList<>());
        FlowElement flowElement = TestFixtures.getDebugTransition(metaPackVersion);
        String templateString = generateUserImplementedComponentTemplate(metaPackVersion, module, flowElement);
        assertNotNull(templateString);
        assertEquals(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(metaPackVersion, flowElement, "MyDebugTransition.java"), templateString);
    }
    //  ------------------------------- FILTERS ----------------------------------
    /**
     * See also resources/studio/templates/org/ikasan/studio/generator/Filter/MessageFilter.java
     * @throws IOException if the template cant be generated
     */
    @ParameterizedTest
    @MethodSource("org.ikasan.studio.core.TestFixtures#metaPacksToTest")
    public void testCreateFlowWith_messageFilterComponent(String metaPackVersion) throws IOException, StudioBuildException, StudioGeneratorException {
        Module module = TestFixtures.getMyFirstModuleIkasanModule(metaPackVersion, new ArrayList<>());
        FlowElement flowElement = TestFixtures.getMessageFilter(metaPackVersion);
        String templateString = generateUserImplementedComponentTemplate(metaPackVersion, module, flowElement);
        assertNotNull(templateString);
        assertEquals(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(metaPackVersion, flowElement,"MessageFilter.java"), templateString);
    }

    /**
     * See also resources/studio/templates/org/ikasan/studio/generator/Filter/MessageFilter.java
     * @throws IOException if the template cant be generated
     */
    @ParameterizedTest
    @MethodSource("org.ikasan.studio.core.TestFixtures#metaPacksToTest")
    public void testCreateFlowWith_defaultMessageFilterComponent(String metaPackVersion) throws IOException, StudioBuildException, StudioGeneratorException {
        Module module = TestFixtures.getMyFirstModuleIkasanModule(metaPackVersion, new ArrayList<>());
        FlowElement flowElement = TestFixtures.getDefaultMessageFilter(metaPackVersion);
        String templateString = generateUserImplementedComponentTemplate(metaPackVersion, module, flowElement);
        assertNotNull(templateString);
        assertEquals(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(metaPackVersion, flowElement,"DefaultMessageFilter.java"), templateString);
    }

    //  ------------------------------- ROUTER ----------------------------------
    /**
     * See also resources/studio/templates/org/ikasan/studio/generator/Router/MultiRecipientRouter.java
     * @throws IOException if the template cant be generated
     */
    @ParameterizedTest
    @MethodSource("org.ikasan.studio.core.TestFixtures#metaPacksToTest")
    public void testCreateFlowWith_messageMultiRecipientRouterComponent(String metaPackVersion) throws IOException, StudioBuildException, StudioGeneratorException {
        Module module = TestFixtures.getMyFirstModuleIkasanModule(metaPackVersion, new ArrayList<>());
        FlowElement flowElement = TestFixtures.getMultiRecipientRouter(metaPackVersion);
        String templateString = generateUserImplementedComponentTemplate(metaPackVersion, module, flowElement);
        assertNotNull(templateString);
        assertEquals(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(metaPackVersion, flowElement,"MultiRecipientRouter.java"), templateString);
    }

    /**
     * See also resources/studio/templates/org/ikasan/studio/generator/Router/MultiRecipientRouter.java
     * @throws IOException if the template cant be generated
     */
    @ParameterizedTest
    @MethodSource("org.ikasan.studio.core.TestFixtures#metaPacksToTest")
    public void testCreateFlowWith_messageSingleRecipientRouterComponent(String metaPackVersion) throws IOException, StudioBuildException, StudioGeneratorException {
        Module module = TestFixtures.getMyFirstModuleIkasanModule(metaPackVersion, new ArrayList<>());
        FlowElement flowElement = TestFixtures.getSingleRecipientRouter(metaPackVersion);
        String templateString = generateUserImplementedComponentTemplate(metaPackVersion, module, flowElement);
        assertNotNull(templateString);
        assertEquals(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(metaPackVersion, flowElement,"SingleRecipientRouter.java"), templateString);
    }

    //  ------------------------------- SPLITTER ----------------------------------
    /**
     * See also resources/studio/templates/org/ikasan/studio/generator/Splitter/MySplitter.java
     * @throws IOException if the template cant be generated
     */
    @ParameterizedTest
    @MethodSource("org.ikasan.studio.core.TestFixtures#metaPacksToTest")
    public void testCreateFlowWith_customSplitterComponent(String metaPackVersion) throws IOException, StudioBuildException, StudioGeneratorException {
        Module module = TestFixtures.getMyFirstModuleIkasanModule(metaPackVersion, new ArrayList<>());
        FlowElement flowElement = TestFixtures.getCustomSplitter(metaPackVersion);
        String templateString = generateUserImplementedComponentTemplate(metaPackVersion, module, flowElement);
        assertNotNull(templateString);
        assertEquals(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(metaPackVersion, flowElement, "MyCustomSplitter.java"), templateString);
    }
}