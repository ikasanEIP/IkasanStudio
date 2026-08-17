package org.ikasan.studio.core.generator;

import org.ikasan.studio.core.StudioBuildException;
import org.ikasan.studio.core.TestFixtures;
import org.ikasan.studio.core.model.ikasan.instance.FlowElement;
import org.ikasan.studio.core.model.ikasan.instance.Module;
import org.ikasan.studio.core.model.ikasan.instance.decorator.Decorator;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Wiretap/LogWiretap decorators are persisted in model.json but, as of writing, are not read by
 * FlowTemplate, FlowsComponentFactoryTemplate or PropertiesTemplate - this is why
 * {@code DecoratorComponentAction} can use {@code GenerationRequest.modelOnly()} and skip regenerating
 * source entirely when a decorator is added/removed (see IkasanFlowRouteViewHandler/PIPSIIkasanModel
 * performance work). If a template is ever changed to render decorator state into generated code, that
 * shortcut becomes wrong (stale generated code, not just a stale cache) and these tests will fail,
 * flagging that DecoratorComponentAction needs to move off modelOnly.
 */
public class DecoratorCodeGenerationInvarianceTest extends AbstractGeneratorTestFixtures {

    private Decorator wiretap(String position, String componentName) {
        return Decorator.decoratorBuilder()
                .type("Wiretap")
                .name(position + " " + componentName)
                .configurationId("wiretap-test")
                .configurable(true)
                .build();
    }

    private Decorator logWiretap(String position, String componentName) {
        return Decorator.decoratorBuilder()
                .type("LogWiretap")
                .name(position + " " + componentName)
                .configurationId("log-wiretap-test")
                .configurable(true)
                .build();
    }

    @ParameterizedTest
    @MethodSource("org.ikasan.studio.core.TestFixtures#metaPacksToTest")
    public void decoratorsDoNotChangeFlowTemplateOutput(String metaPackVersion) throws StudioBuildException, StudioGeneratorException {
        Module undecoratedModule = TestFixtures.getMyFirstModuleIkasanModule(metaPackVersion, new ArrayList<>());
        FlowElement undecorated = TestFixtures.getBroker(metaPackVersion);
        String withoutDecorators = generateFlowTemplateString(metaPackVersion, undecoratedModule, undecorated);

        Module decoratedModule = TestFixtures.getMyFirstModuleIkasanModule(metaPackVersion, new ArrayList<>());
        FlowElement decorated = TestFixtures.getBroker(metaPackVersion);
        decorated.addDecorator(wiretap("BEFORE", decorated.getComponentName()));
        decorated.addDecorator(wiretap("AFTER", decorated.getComponentName()));
        decorated.addDecorator(logWiretap("BEFORE", decorated.getComponentName()));
        decorated.addDecorator(logWiretap("AFTER", decorated.getComponentName()));
        String withDecorators = generateFlowTemplateString(metaPackVersion, decoratedModule, decorated);

        assertEquals(withoutDecorators, withDecorators,
                "FlowTemplate output changed when decorators were added - DecoratorComponentAction's " +
                        "GenerationRequest.modelOnly() optimisation is no longer safe, it must trigger a " +
                        "flow regeneration instead.");
    }

    @ParameterizedTest
    @MethodSource("org.ikasan.studio.core.TestFixtures#metaPacksToTest")
    public void decoratorsDoNotChangeFlowsComponentFactoryTemplateOutput(String metaPackVersion) throws StudioBuildException, StudioGeneratorException {
        Module undecoratedModule = TestFixtures.getMyFirstModuleIkasanModule(metaPackVersion, new ArrayList<>());
        FlowElement undecorated = TestFixtures.getBroker(metaPackVersion);
        String withoutDecorators = generateFlowsComponentFactoryTemplateString(metaPackVersion, undecoratedModule, undecorated);

        Module decoratedModule = TestFixtures.getMyFirstModuleIkasanModule(metaPackVersion, new ArrayList<>());
        FlowElement decorated = TestFixtures.getBroker(metaPackVersion);
        decorated.addDecorator(wiretap("BEFORE", decorated.getComponentName()));
        decorated.addDecorator(wiretap("AFTER", decorated.getComponentName()));
        decorated.addDecorator(logWiretap("BEFORE", decorated.getComponentName()));
        decorated.addDecorator(logWiretap("AFTER", decorated.getComponentName()));
        String withDecorators = generateFlowsComponentFactoryTemplateString(metaPackVersion, decoratedModule, decorated);

        assertEquals(withoutDecorators, withDecorators,
                "FlowsComponentFactoryTemplate output changed when decorators were added - DecoratorComponentAction's " +
                        "GenerationRequest.modelOnly() optimisation is no longer safe, it must trigger a " +
                        "flow regeneration instead.");
    }

    @ParameterizedTest
    @MethodSource("org.ikasan.studio.core.TestFixtures#metaPacksToTest")
    public void wiretapsAreGeneratedAsIndexedStartupProperties(String metaPackVersion) throws StudioBuildException, StudioGeneratorException {
        Module undecoratedModule = TestFixtures.getMyFirstModuleIkasanModule(metaPackVersion, new ArrayList<>());
        FlowElement undecorated = TestFixtures.getBroker(metaPackVersion);
        String withoutDecorators = generatePropertiesTemplateString(metaPackVersion, undecoratedModule, undecorated);

        Module decoratedModule = TestFixtures.getMyFirstModuleIkasanModule(metaPackVersion, new ArrayList<>());
        FlowElement decorated = TestFixtures.getBroker(metaPackVersion);
        decorated.addDecorator(wiretap("BEFORE", decorated.getComponentName()));
        decorated.addDecorator(wiretap("AFTER", decorated.getComponentName()));
        decorated.addDecorator(logWiretap("BEFORE", decorated.getComponentName()));
        decorated.addDecorator(logWiretap("AFTER", decorated.getComponentName()));
        String withDecorators = generatePropertiesTemplateString(metaPackVersion, decoratedModule, decorated);

        assertNotEquals(withoutDecorators, withDecorators);
        assertTrue(withDecorators.contains("ikasan.module.activator.wiretap.deleteAllTriggers=true"));
        assertTrue(withDecorators.contains("ikasan.module.activator.wiretap.triggers[0]="));
        assertTrue(withDecorators.contains(",before," + decorated.getComponentName() + ",300"));
        assertTrue(withDecorators.contains("ikasan.module.activator.wiretap.triggers[1]="));
        assertTrue(withDecorators.contains(",after," + decorated.getComponentName() + ",300"));
        assertFalse(withDecorators.contains("LogWiretap"));

        String moduleConfig = ModuleConfigTemplate.create(decoratedModule);
        assertTrue(moduleConfig.contains("@org.springframework.context.annotation.Profile(\"debug\")"));
        assertTrue(moduleConfig.contains("\"loggingJob\""));
        assertTrue(moduleConfig.contains("\"BEFORE\""));
        assertTrue(moduleConfig.contains("\"AFTER\""));
        assertTrue(moduleConfig.contains("\"" + decorated.getComponentName() + "\""));
    }
}
