package org.ikasan.studio.core.generator;

import org.ikasan.studio.core.StudioBuildException;
import org.ikasan.studio.core.TestFixtures;
import org.ikasan.studio.core.model.ikasan.instance.FlowElement;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class FlowsUserImplementedComponentTemplateTest extends AbstractGeneratorTestFixtures {

    @BeforeEach
    public void setUp() throws StudioBuildException {
        module = TestFixtures.getMyFirstModuleIkasanModule(new ArrayList<>());
    }

    //  ------------------------------- BROKER ----------------------------------
    /**
     * See also resources/studio/templates/org/ikasan/studio/generator/Converter/MyBroker.java
     * @throws IOException if the template cant be generated
     */
    @Test
    public void testCreateFlowWith_brokerComponent() throws IOException, StudioBuildException, StudioGeneratorException {
        FlowElement flowElement = TestFixtures.getBroker();
        String templateString = generateUserImplementedComponentTemplate(flowElement);
        assertNotNull(templateString);
        assertEquals(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(flowElement, "MyBroker.java"), templateString);
    }

    //  ------------------------------- CONVERTER ----------------------------------
    /**
     * See also resources/studio/templates/org/ikasan/studio/generator/Converter/MyConverter.java
     * @throws IOException if the template cant be generated
     */
    @Test
    public void testCreateFlowWith_customConverterComponent() throws IOException, StudioBuildException, StudioGeneratorException {
        FlowElement flowElement = TestFixtures.getCustomConverter();
        String templateString = generateUserImplementedComponentTemplate(flowElement);
        assertNotNull(templateString);
        assertEquals(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(flowElement, "MyConverter.java"), templateString);
    }
    //  ------------------------------- DEBUG ----------------------------------
    /**
     * See also resources/studio/templates/org/ikasan/studio/generator/Converter/MyConverter.java
     * @throws IOException if the template cant be generated
     */
    @Test
    public void testCreateFlowWith_debugTransitionComponent() throws IOException, StudioBuildException, StudioGeneratorException {
        FlowElement flowElement = TestFixtures.getDebugTransition();
        String templateString = generateUserImplementedComponentTemplate(flowElement);
        assertNotNull(templateString);
        assertEquals(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(flowElement, "MyDebugTransition.java"), templateString);
    }
    //  ------------------------------- FILTERS ----------------------------------
    /**
     * See also resources/studio/templates/org/ikasan/studio/generator/Filter/MessageFilter.java
     * @throws IOException if the template cant be generated
     */
    @Test
    public void testCreateFlowWith_messageFilterComponent() throws IOException, StudioBuildException, StudioGeneratorException {
        FlowElement flowElement = TestFixtures.getMessageFilter();
        String templateString = generateUserImplementedComponentTemplate(flowElement);
        assertNotNull(templateString);
        assertEquals(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile( flowElement,"MessageFilter.java"), templateString);
    }

    /**
     * See also resources/studio/templates/org/ikasan/studio/generator/Filter/MessageFilter.java
     * @throws IOException if the template cant be generated
     */
    @Test
    public void testCreateFlowWith_defaultMessageFilterComponent() throws IOException, StudioBuildException, StudioGeneratorException {
        FlowElement flowElement = TestFixtures.getDefaultMessageFilter();
        String templateString = generateUserImplementedComponentTemplate(flowElement);
        assertNotNull(templateString);
        assertEquals(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile( flowElement,"DefaultMessageFilter.java"), templateString);
    }

    //  ------------------------------- ROUTER ----------------------------------
    /**
     * See also resources/studio/templates/org/ikasan/studio/generator/Router/MultiRecipientRouter.java
     * @throws IOException if the template cant be generated
     */
    @Test
    public void testCreateFlowWith_messageMultiRecipientRouterComponent() throws IOException, StudioBuildException, StudioGeneratorException {
        FlowElement flowElement = TestFixtures.getMultiRecipientRouter();
        String templateString = generateUserImplementedComponentTemplate(flowElement);
        assertNotNull(templateString);
        assertEquals(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile( flowElement,"MultiRecipientRouter.java"), templateString);
    }

    /**
     * See also resources/studio/templates/org/ikasan/studio/generator/Router/MultiRecipientRouter.java
     * @throws IOException if the template cant be generated
     */
    @Test
    public void testCreateFlowWith_messageSingleRecipientRouterComponent() throws IOException, StudioBuildException, StudioGeneratorException {
        FlowElement flowElement = TestFixtures.getSingleRecipientRouter();
        String templateString = generateUserImplementedComponentTemplate(flowElement);
        assertNotNull(templateString);
        assertEquals(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile( flowElement,"SingleRecipientRouter.java"), templateString);
    }

    //  ------------------------------- SPLITTER ----------------------------------
    /**
     * See also resources/studio/templates/org/ikasan/studio/generator/Splitter/MySplitter.java
     * @throws IOException if the template cant be generated
     */
    @Test
    public void testCreateFlowWith_customSplitterComponent() throws IOException, StudioBuildException, StudioGeneratorException {
        FlowElement flowElement = TestFixtures.getCustomSplitter();
        String templateString = generateUserImplementedComponentTemplate(flowElement);
        assertNotNull(templateString);
        assertEquals(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(flowElement, "MyCustomSplitter.java"), templateString);
    }
}