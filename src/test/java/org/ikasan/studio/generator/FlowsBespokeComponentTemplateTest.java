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