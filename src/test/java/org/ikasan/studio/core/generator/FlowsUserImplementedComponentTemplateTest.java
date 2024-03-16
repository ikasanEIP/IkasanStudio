package org.ikasan.studio.core.generator;

import org.ikasan.studio.core.TestFixtures;
import org.ikasan.studio.core.model.ikasan.instance.FlowElement;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class FlowsUserImplementedComponentTemplateTest extends GeneratorTests {

    @BeforeEach
    public void setUp() {
        module = TestFixtures.getMyFirstModuleIkasanModule(new ArrayList<>());
    }


    //  ------------------------------- CONSUMERS ----------------------------------
    /**
     * See also resources/studio/templates/org/ikasan/studio/generator/Converter/MyConverter.java
     * @throws IOException if the template cant be generated
     */
    @Test
    public void testCreateFlowWith_customConverterComponent() throws IOException {
        FlowElement flowElement = TestFixtures.getCustomConverter();
        String templateString = FlowsUserImplementedComponentTemplate.generateContents(TestFixtures.DEFAULT_PACKAGE, flowElement);
        assertNotNull(templateString);
        assertEquals(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(flowElement, "MyConverter.java"), templateString);
    }
    //  ------------------------------- FILTERS ----------------------------------
    /**
     * See also resources/studio/templates/org/ikasan/studio/generator/Filter/MessageFilter.java
     * @throws IOException if the template cant be generated
     */
    @Test
    public void testCreateFlowWith_messageFilterComponent() throws IOException {
        FlowElement flowElement = TestFixtures.getMessageFilter();
        String templateString = FlowsUserImplementedComponentTemplate.generateContents(TestFixtures.DEFAULT_PACKAGE, flowElement);
        assertNotNull(templateString);
        assertEquals(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile( flowElement,"MessageFilter.java"), templateString);
    }
}