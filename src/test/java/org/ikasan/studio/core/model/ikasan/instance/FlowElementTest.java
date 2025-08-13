package org.ikasan.studio.core.model.ikasan.instance;

import org.apache.maven.model.Dependency;
import org.ikasan.studio.core.StudioBuildException;
import org.ikasan.studio.core.model.ikasan.instance.decorator.DECORATOR_TYPE;
import org.ikasan.studio.core.model.ikasan.instance.decorator.Decorator;
import org.ikasan.studio.core.model.ikasan.meta.ComponentMeta;
import org.ikasan.studio.core.model.ikasan.meta.IkasanComponentLibrary;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.ikasan.studio.core.TestFixtures.getXProducerComponentMeta;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class FlowElementTest {

    private static final String SIMPLE_STRING_PROPERTY = "simpleStringProperty";
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
    }


    /**
     * Test the cloning of the fictitious flow element named "Test X Producer" to a new version
     * @throws StudioBuildException
     */
    @Test
    void cloneToNewerVersionFlowElement() throws StudioBuildException {
        FlowElement xProducerComponent = getXProducerComponent("v1");

        ComponentMeta v2XProducer = getXProducerComponentMeta("v2");
        try (MockedStatic<IkasanComponentLibrary> mockedStatic = mockStatic(IkasanComponentLibrary.class)) {
            mockedStatic.when(() -> IkasanComponentLibrary
                .getIkasanComponentByKey("v2", "Test X Producer"))
                .thenReturn(v2XProducer);

            FlowElement clonedXProducerComponent = xProducerComponent.cloneToVersion("v2", mockFlow2, mockFlowRoute2);

            assertNotNull(clonedXProducerComponent);
            // ignore anything thats mocked.

            // *** COMPONENT META ***
            // Should be same except for jar dependencies
            assertEquals("3.1.0", ((Dependency) xProducerComponent.getComponentMeta().getJarDependencies().toArray()[0]).getVersion());
            assertEquals("3.2.0", ((Dependency) clonedXProducerComponent.getComponentMeta().getJarDependencies().toArray()[0]).getVersion());
            // same except for the above

            assertThat(clonedXProducerComponent.getComponentMeta())
                    .usingRecursiveComparison()
                    .ignoringFields(
                            "jarDependencies",              // These are set in test fixture as different
                            "allowableProperties")          // These are different, the differences being tested in Component Properties below, all allowables are used for componentProperties.
                    .isEqualTo(xProducerComponent.getComponentMeta());

            ComponentProperty originalSimpleStringProperty = xProducerComponent.getProperty(SIMPLE_STRING_PROPERTY);
            ComponentProperty clonedSimpleStringProperty = clonedXProducerComponent.getProperty(SIMPLE_STRING_PROPERTY);
            // *** COMPONENT PROPERTIES ***
            // For the property named SIMPLE_STRING_PROPERTY the meta should be the same except for tUserImplementClassFtlTemplate, SetterMethod, Validatio
            assertEquals("org/ikasan/spec/component/endpoint/Producer.ftl", originalSimpleStringProperty.getMeta().getUserImplementClassFtlTemplate());
            assertEquals("org/ikasan/spec/component/endpoint/ProducerV2.ftl", clonedSimpleStringProperty.getMeta().getUserImplementClassFtlTemplate());
            assertEquals("setCronExpression", originalSimpleStringProperty.getMeta().getSetterMethod());
            assertEquals("setCronExpression2", clonedSimpleStringProperty.getMeta().getSetterMethod());
            assertEquals("^v1[A-Z_$][a-zA-Z\\d_$£]*$", originalSimpleStringProperty.getMeta().getValidation());
            assertEquals("^v2[A-Z_$][a-zA-Z\\d_$£]*$", clonedSimpleStringProperty.getMeta().getValidation());
            // same except for the above
            assertThat(clonedSimpleStringProperty.getMeta())
                .usingRecursiveComparison()
                .ignoringFields("cronExpression", "validation", "validationPattern", "userImplementClassFtlTemplate", "setterMethod")
                .isEqualTo(originalSimpleStringProperty.getMeta());
            // expected to be the same

            assertEquals(xProducerComponent.getComponentProperties().entrySet().toArray()[0], clonedXProducerComponent.getComponentProperties().entrySet().toArray()[0]);

            assertThat(clonedXProducerComponent.getComponentProperties())
                .usingRecursiveComparison()
                .ignoringFields(SIMPLE_STRING_PROPERTY)
                .withComparatorForType(
                Comparator.nullsFirst(
                    Comparator.comparing(Pattern::pattern)), Pattern.class)
                .isEqualTo(xProducerComponent.getComponentProperties());
        }
    }

    /**
     * Test the cloning of the fictitious flow element named "Test X Producer" to a new version
     * @throws StudioBuildException
     */
    @Test
    void cloneWithNullMetaPackRaisesError() throws StudioBuildException {
        Logger mockLogger = Mockito.mock(Logger.class);

        FlowElement xProducerComponent = getXProducerComponent("v1");
        xProducerComponent.resetLogger(mockLogger);

        FlowElement clonedXProducerComponent = xProducerComponent.cloneToVersion(null, mockFlow2, mockFlowRoute2);
        assertNull(clonedXProducerComponent);
        verify(mockLogger).error("STUDIO: SERIOUS ERROR - to cloneToVersion but metapackVersion was null or blank");
    }
    /**
     * Test the cloning of the fictitious flow element named "Test X Producer" to a new version
     * @throws StudioBuildException
     */
    @Test
    void cloneWithEmptyMetaPackRaisesError() throws StudioBuildException {
        Logger mockLogger = Mockito.mock(Logger.class);

        FlowElement xProducerComponent = getXProducerComponent("v1");
        xProducerComponent.resetLogger(mockLogger);

        FlowElement clonedXProducerComponent = xProducerComponent.cloneToVersion("", mockFlow2, mockFlowRoute2);
        assertNull(clonedXProducerComponent);
        verify(mockLogger).error("STUDIO: SERIOUS ERROR - to cloneToVersion but metapackVersion was null or blank");
    }

    @Test
    void cloneWhereNewMetaDoesNotExistThrowsException() throws StudioBuildException {
        FlowElement xProducerComponent = getXProducerComponent("v2");
        try (MockedStatic<IkasanComponentLibrary> mockedStatic = mockStatic(IkasanComponentLibrary.class)) {
            mockedStatic.when(() -> IkasanComponentLibrary
                            .getIkasanComponentByKey("v1", "Test X Producer"))
                    .thenReturn(null);

            StudioBuildException exception = assertThrows(
                    StudioBuildException.class,
                    () -> xProducerComponent.cloneToVersion("v1", mockFlow2, mockFlowRoute2)
            );
            assertEquals("Component [Test X Producer] not found in metapack version [v1]", exception.getMessage());
        }
    }

    /**
     * Test the cloning of the fictitious flow element named "Test X Producer" to a new version
     * @throws StudioBuildException
     */
    @Test
    void cloneToOlderVersionCFlowElement() throws StudioBuildException {
        FlowElement xProducerComponent = getXProducerComponent("v2");

        ComponentMeta v1XProducer = getXProducerComponentMeta("v1");
        try (MockedStatic<IkasanComponentLibrary> mockedStatic = mockStatic(IkasanComponentLibrary.class)) {
            mockedStatic.when(() -> IkasanComponentLibrary
                            .getIkasanComponentByKey("v1", "Test X Producer"))
                    .thenReturn(v1XProducer);

            FlowElement clonedXProducerComponent = xProducerComponent.cloneToVersion("v1", mockFlow2, mockFlowRoute2);

            assertNotNull(clonedXProducerComponent);
            // ignore anything thats mocked.

            // *** COMPONENT META ***
            // Should be same except for jar dependencies
            assertEquals("3.2.0", ((Dependency) xProducerComponent.getComponentMeta().getJarDependencies().toArray()[0]).getVersion());
            assertEquals("3.1.0", ((Dependency) clonedXProducerComponent.getComponentMeta().getJarDependencies().toArray()[0]).getVersion());
            // same except for the above

            assertThat(clonedXProducerComponent.getComponentMeta())
                    .usingRecursiveComparison()
                    .ignoringFields(
                            "jarDependencies",              // These are set in test fixture as different
                            "allowableProperties")          // These are different, the differences being tested in Component Properties below, all allowables are used for componentProperties.
                    .isEqualTo(xProducerComponent.getComponentMeta());

            ComponentProperty originalSimpleStringProperty = xProducerComponent.getProperty(SIMPLE_STRING_PROPERTY);
            ComponentProperty clonedSimpleStringProperty = clonedXProducerComponent.getProperty(SIMPLE_STRING_PROPERTY);
            // *** COMPONENT PROPERTIES ***
            // For the property named SIMPLE_STRING_PROPERTY the meta should be the same except for tUserImplementClassFtlTemplate, SetterMethod, Validatio
            assertEquals("org/ikasan/spec/component/endpoint/ProducerV2.ftl", originalSimpleStringProperty.getMeta().getUserImplementClassFtlTemplate());
            assertEquals("org/ikasan/spec/component/endpoint/Producer.ftl", clonedSimpleStringProperty.getMeta().getUserImplementClassFtlTemplate());
            assertEquals("setCronExpression2", originalSimpleStringProperty.getMeta().getSetterMethod());
            assertEquals("setCronExpression", clonedSimpleStringProperty.getMeta().getSetterMethod());
            assertEquals("^v2[A-Z_$][a-zA-Z\\d_$£]*$", originalSimpleStringProperty.getMeta().getValidation());
            assertEquals("^v1[A-Z_$][a-zA-Z\\d_$£]*$", clonedSimpleStringProperty.getMeta().getValidation());
            // same except for the above
            assertThat(clonedSimpleStringProperty.getMeta())
                    .usingRecursiveComparison()
                    .ignoringFields("cronExpression", "validation", "validationPattern", "userImplementClassFtlTemplate", "setterMethod")
                    .isEqualTo(originalSimpleStringProperty.getMeta());
            // expected to be the same

            assertEquals(xProducerComponent.getComponentProperties().entrySet().toArray()[0], clonedXProducerComponent.getComponentProperties().entrySet().toArray()[0]);

            assertThat(clonedXProducerComponent.getComponentProperties())
                    .usingRecursiveComparison()
                    .ignoringFields(SIMPLE_STRING_PROPERTY)
                    .withComparatorForType(
                            Comparator.nullsFirst(
                                    Comparator.comparing(Pattern::pattern)), Pattern.class)
                    .isEqualTo(xProducerComponent.getComponentProperties());
        }
    }


    @Test
    void addBeforeWiretapDecorator_shouldAddValidDecorator() {
        Decorator beforeWiretapDecorator = getBeforeWiretapDecorator();

        FlowElement flowElement = FlowElement.flowElementBuilder()
                .componentMeta(getXProducerComponentMeta("v1"))
                .containingFlow(mockFlow1)
                .containingFlowRoute(mockFlowRoute1)
                .componentName("Test X Producer")
                .decorators(new ArrayList<>())
                .build();

        flowElement.addDecorator(beforeWiretapDecorator);

        assertThat(flowElement.hasDecorators()).isTrue();
        assertThat(flowElement.hasBeforeDecorators()).isTrue();
        assertThat(flowElement.hasAfterDecorators()).isFalse();
        assertThat(flowElement.getBeforeDecorators()).contains(beforeWiretapDecorator);
        assertThat(flowElement.getAfterDecorators()).isEmpty();
        assertThat(flowElement.hasWiretap()).isTrue();
        assertThat(flowElement.hasLogWiretap()).isFalse();
        assertThat(flowElement.getWiretaps()).contains(beforeWiretapDecorator);
        assertThat(flowElement.getLogWiretaps()).isEmpty();
        assertThat(flowElement.getDecorators()).contains(beforeWiretapDecorator);
    }

    @Test
    void addAfterLogWiretapDecorator_shouldAddValidDecorator() {
        Decorator afterLoWiretapDecorator = getAfterLoWiretapDecorator();

        FlowElement flowElement = FlowElement.flowElementBuilder()
                .componentMeta(getXProducerComponentMeta("v1"))
                .containingFlow(mockFlow1)
                .containingFlowRoute(mockFlowRoute1)
                .componentName("Test X Producer")
                .decorators(new ArrayList<>())
                .build();

        flowElement.addDecorator(afterLoWiretapDecorator);

        assertThat(flowElement.hasDecorators()).isTrue();
        assertThat(flowElement.hasBeforeDecorators()).isFalse();
        assertThat(flowElement.hasAfterDecorators()).isTrue();
        assertThat(flowElement.getBeforeDecorators()).isEmpty();
        assertThat(flowElement.getAfterDecorators()).contains(afterLoWiretapDecorator);
        assertThat(flowElement.hasWiretap()).isFalse();
        assertThat(flowElement.hasLogWiretap()).isTrue();
        assertThat(flowElement.getWiretaps()).isEmpty();
        assertThat(flowElement.getLogWiretaps()).contains(afterLoWiretapDecorator);
        assertThat(flowElement.getDecorators()).contains(afterLoWiretapDecorator);
    }

    @Test
    void addDecorator_shouldAddValidDecoratorWhenDecoratorsEmpty() {
        Decorator validDecorator = getBeforeWiretapDecorator();

        FlowElement flowElement = FlowElement.flowElementBuilder()
                .componentMeta(getXProducerComponentMeta("v1"))
                .decorators(null)
                .build();
        assertThat(flowElement.hasDecorators()).isFalse();
        assertThat(flowElement.hasBeforeDecorators()).isFalse();
        assertThat(flowElement.hasAfterDecorators()).isFalse();
        assertThat(flowElement.hasLogWiretap()).isFalse();
        assertThat(flowElement.hasWiretap()).isFalse();
        flowElement.addDecorator(validDecorator);

        assertThat(flowElement.getDecorators()).contains(validDecorator);
    }

    @Test
    void addDecorator_shouldNotAddInvalidDecorator() {
        Decorator invalidDecorator = getInvalidDecorator();

        FlowElement flowElement = FlowElement.flowElementBuilder()
                .componentMeta(getXProducerComponentMeta("v1"))
                .decorators(new ArrayList<>())
                .build();

        flowElement.addDecorator(invalidDecorator);

        assertThat(flowElement.getDecorators()).doesNotContain(invalidDecorator);
    }

    @Test
    void addDecorator_shouldNotAddDuplicateDecorator() {
        Decorator validDecorator = getBeforeWiretapDecorator();

        FlowElement flowElement = FlowElement.flowElementBuilder()
                .componentMeta(getXProducerComponentMeta("v1"))
                .decorators(new ArrayList<>())
                .build();

        flowElement.addDecorator(validDecorator);
        flowElement.addDecorator(validDecorator);

        assertThat(flowElement.getDecorators()).containsExactly(validDecorator);
    }

    @Test
    void addDecorator_shouldAddMultipleIfNotDuplicateDecorator() {
        Decorator beforeWiretapDecorator = getBeforeWiretapDecorator();
        Decorator afterLoWiretapDecorator = getAfterLoWiretapDecorator();

        FlowElement flowElement = FlowElement.flowElementBuilder()
                .componentMeta(getXProducerComponentMeta("v1"))
                .decorators(new ArrayList<>())
                .build();

        flowElement.addDecorator(beforeWiretapDecorator);
        flowElement.addDecorator(afterLoWiretapDecorator);

        assertThat(flowElement.getDecorators()).contains(beforeWiretapDecorator);
        assertThat(flowElement.getDecorators()).contains(afterLoWiretapDecorator);
        assertThat(flowElement.getDecorators().size()).isEqualTo(2);
    }

    @Test
    void removeDecorator() {
        Decorator beforeWiretapDecorator = getBeforeWiretapDecorator();
        Decorator afterLoWiretapDecorator = getAfterLoWiretapDecorator();

        FlowElement flowElement = FlowElement.flowElementBuilder()
                .componentMeta(getXProducerComponentMeta("v1"))
                .decorators(new ArrayList<>())
                .build();

        flowElement.addDecorator(beforeWiretapDecorator);
        flowElement.addDecorator(afterLoWiretapDecorator);

        flowElement.removeDecorator(beforeWiretapDecorator.getType(), beforeWiretapDecorator.getPosition());

        assertThat(flowElement.getDecorators()).containsExactly(afterLoWiretapDecorator);
    }

    private Decorator getBeforeWiretapDecorator() {
        return Decorator.decoratorBuilder()
                .type(DECORATOR_TYPE.Wiretap.name())
                .name("BEFORE myXProducerInFlow1")
                .configurationId("myXProducerInFlow1")
                .configurable(true)
                .build();
    }

    private Decorator getAfterLoWiretapDecorator() {
        return Decorator.decoratorBuilder()
                .type(DECORATOR_TYPE.LogWiretap.name())
                .name("AFTER myXProducerInFlow1")
                .configurationId("myXProducerInFlow1")
                .configurable(true)
                .build();
    }

    private Decorator getInvalidDecorator() {
        return Decorator.decoratorBuilder()
                .type(DECORATOR_TYPE.Unknown.name())
                .build();
    }

    private FlowElement getXProducerComponent(String metaPackVersion) {
        FlowElement newXProducerComponent =  FlowElement.flowElementBuilder()
                .componentMeta(getXProducerComponentMeta(metaPackVersion))
                .decorators(new ArrayList<>())
                .build();
        // The properties reflect the state of an instance used in a specific route.
        newXProducerComponent.setDescription("myXProducerInFlow1");
        newXProducerComponent.setPropertyValue(SIMPLE_STRING_PROPERTY, "myXProducerInFlow1testStringValue");
        newXProducerComponent.setPropertyValue("simpleIntegerProperty", 10);
        return newXProducerComponent;
    }
}