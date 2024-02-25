package org.ikasan.studio.generator;

import org.junit.jupiter.api.Test;

import java.io.IOException;

public class FlowsBespokeComponentTemplateTest {

    /**
     * @See resources/studio/templates/org/ikasan/studio/generator/MyConverter.java
     * @throws IOException if the template cant be generated
     */
    @Test
    public void testCreateWith_Converter() throws IOException {
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
}