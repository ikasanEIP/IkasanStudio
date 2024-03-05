package org.ikasan.studio.generator;

import org.ikasan.studio.TestFixtures;
import org.ikasan.studio.model.ikasan.instance.FlowElement;
import org.ikasan.studio.model.ikasan.instance.Module;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class FlowsBespokeComponentTemplateTest {

    /**
     * @See resources/studio/templates/org/ikasan/studio/generator/MyConverter.java
     * @throws IOException if the template cant be generated
     */
    @Test
    public void testCreateWith_Converter() {
        String clazzName = "MyConverter";

        // Wait till refactor
//        Module ikasanModule = TestFixtures.getMyFirstModuleIkasanModule();
//        ikasanModule.addFlow(new Flow());
//        Flow newFlow = ikasanModule.getFlows().get(0);
//        FlowElement ikasanFlowComponent = FlowElement.createFlowElement(IkasanComponentMetax.CUSTOM_CONVERTER, newFlow);
//        ikasanFlowComponent.updatePropertyValue(ComponentPropertyMeta.NAME, "bespokeVarName");
//        ikasanFlowComponent.updatePropertyValue(ComponentPropertyMeta.DESCRIPTION, "Beskpoke conversion");
//        ikasanFlowComponent.updatePropertyValue(ComponentPropertyMeta.BESPOKE_CLASS_NAME, clazzName);
//        ikasanFlowComponent.updatePropertyValue(ComponentPropertyMeta.FROM_TYPE, "java.lang.String");
//        ikasanFlowComponent.updatePropertyValue(ComponentPropertyMeta.TO_TYPE, "java.lang.Integer");
//
//        newFlow.addFlowComponent(ikasanFlowComponent);
//        String templateString = FlowsBespokeComponentTemplate.generateContents(TestFixtures.DEFAULT_PACKAGE, ikasanFlowComponent);
//        assertThat(templateString, is(notNullValue()));
//        assertThat(templateString, is(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(clazzName + ".java")));
    }
    Module module;
    //    Flow ikasanFlow = new Flow();

    @BeforeEach
    public void setUp() {
        module = TestFixtures.getMyFirstModuleIkasanModule(new ArrayList<>());
    }


    //  ------------------------------- CONSUMERS ----------------------------------
    /**
     * See also resources/studio/templates/org/ikasan/studio/generator/ComponentFactoryFullyPopulatedEventGeneratingConsumerComponent.java
     * @throws IOException if the template cant be generated
     */
    @Test
    public void testCreateFlowWith_customConverterComponent() throws IOException {
        FlowElement flowElement = TestFixtures.getCustomConverter();
        String templateString = FlowsBespokeComponentTemplate.generateContents(TestFixtures.DEFAULT_PACKAGE, flowElement);
        assertNotNull(templateString);
        assertEquals(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile( "MyConverter.java"), templateString);
    }
}