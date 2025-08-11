package org.ikasan.studio.core.model.ikasan.instance;

import org.apache.maven.model.Dependency;
import org.ikasan.studio.ObjectComparator;
import org.ikasan.studio.core.StudioBuildException;
import org.ikasan.studio.core.model.ikasan.meta.ComponentMeta;
import org.ikasan.studio.core.model.ikasan.meta.IkasanComponentLibrary;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.ikasan.studio.core.TestFixtures.getXProducerComponentMeta;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

class FlowElementTest {

    private FlowElement xProducerComponent;
    private Flow mockFlow1;
    private Flow mockFlow2;
    private FlowRoute mockFlowRoute1;
    private FlowRoute mockFlowRoute2;
    @BeforeEach
    void setUp() {
        mockFlow1 = mock(Flow.class);
        mockFlow2 = mock(Flow.class);
        mockFlowRoute1 = mock(FlowRoute.class);
        mockFlowRoute2 = mock(FlowRoute.class);
        xProducerComponent = FlowElement.flowElementBuilder()
                .componentMeta(getXProducerComponentMeta("v1"))
                .containingFlow(mockFlow1)
                .containingFlowRoute(mockFlowRoute1)
                .componentName("Test X Producer")
                .decorators(new ArrayList<>())
                .build();

        // The properties reflect the state of an instance used in a specific route.
        xProducerComponent.setDescription("myXProducerInFlow1");
        xProducerComponent.setPropertyValue("simpleStringProperty", "myXProducerInFlow1testStringValue");
        xProducerComponent.setPropertyValue("simpleIntegerProperty", 10);
    }


    /**
     * Test the cloning of the fictitious flow element named "Test X Producer" to a new version
     * @throws StudioBuildException
     */
    @Test
    void cloneToVersionClonesFlowElementWithNewVersion() throws StudioBuildException {
        ComponentMeta v2XProducer = getXProducerComponentMeta("v2");
        try (MockedStatic<IkasanComponentLibrary> mockedStatic = mockStatic(IkasanComponentLibrary.class)) {
            mockedStatic.when(() -> IkasanComponentLibrary
                .getIkasanComponentByKey("v2", "Test X Producer"))
                .thenReturn(v2XProducer);

            FlowElement clonedXProducerComponent = xProducerComponent.cloneToVersion("v2", mockFlow2, mockFlowRoute2);

            assertNotNull(clonedXProducerComponent);
            // ignore anything thats mocked.
            // Expected difference
            assertEquals("3.1.0", ((Dependency) xProducerComponent.getComponentMeta().getJarDependencies().toArray()[0]).getVersion());
            assertEquals("3.2.0", ((Dependency) clonedXProducerComponent.getComponentMeta().getJarDependencies().toArray()[0]).getVersion());

            assertEquals("org/ikasan/spec/component/endpoint/Producer.ftl", xProducerComponent.getProperty("simpleStringProperty").getMeta().getUserImplementClassFtlTemplate());
            assertEquals("org/ikasan/spec/component/endpoint/ProducerV2.ftl", clonedXProducerComponent.getProperty("simpleStringProperty").getMeta().getUserImplementClassFtlTemplate());
            assertEquals("setCronExpression", xProducerComponent.getProperty("simpleStringProperty").getMeta().getSetterMethod());
            assertEquals("setCronExpression2", clonedXProducerComponent.getProperty("simpleStringProperty").getMeta().getSetterMethod());
            assertEquals("^v1[A-Z_$][a-zA-Z\\d_$£]*$", xProducerComponent.getProperty("simpleStringProperty").getMeta().getValidation());
            assertEquals("^v2[A-Z_$][a-zA-Z\\d_$£]*$", clonedXProducerComponent.getProperty("simpleStringProperty").getMeta().getValidation());

            assertEquals(xProducerComponent.getComponentProperties().entrySet().toArray()[0], clonedXProducerComponent.getComponentProperties().entrySet().toArray()[0]);
            assertEquals(Collections.emptyList(), ObjectComparator.compareAttributesExcept(
                    xProducerComponent.getProperty("simpleStringProperty").getMeta(),
                    clonedXProducerComponent.getProperty("simpleStringProperty").getMeta(),
                    List.of("cronExpression", "validation", "userImplementClassFtlTemplate", "setterMethod")));

            //            assertThat(clonedXProducerComponent.getComponentProperties()).usingRecursiveComparison()
//                .withComparatorForType(
//                    Comparator.nullsFirst(
//                        Comparator.comparing(Pattern::pattern)), Pattern.class)
//                .isEqualTo(xProducerComponent.getComponentProperties());
        }
    }

}