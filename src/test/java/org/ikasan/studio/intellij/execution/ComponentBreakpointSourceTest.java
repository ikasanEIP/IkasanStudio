package org.ikasan.studio.intellij.execution;

import org.ikasan.studio.core.metapack.model.ComponentMeta;
import org.ikasan.studio.core.model.ikasan.instance.Flow;
import org.ikasan.studio.core.model.ikasan.instance.FlowElement;
import org.ikasan.studio.core.model.ikasan.instance.Module;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class ComponentBreakpointSourceTest {
    private final Module module = mock(Module.class);
    private final Flow flow = mock(Flow.class);
    private final FlowElement element = mock(FlowElement.class);
    private final ComponentMeta meta = mock(ComponentMeta.class);

    ComponentBreakpointSourceTest() {
        when(module.getApplicationPackageName()).thenReturn("org.example");
        when(flow.getJavaPackageName()).thenReturn("flow");
        when(element.getContainingFlow()).thenReturn(flow);
        when(element.getComponentMeta()).thenReturn(meta);
    }

    @Test void matchesOrdinaryUserComponentWithoutDebugMetadata() {
        when(element.getPropertyValue("userImplementedClassName")).thenReturn("MessageGenerator");
        assertThat(matches("/project/user/src/main/java/org/example/flow/MessageGenerator.java")).isTrue();
        assertThat(matches("/other/user/src/main/java/org/example/flow/MessageGenerator.java")).isFalse();
        assertThat(matches("/project/user/src/main/java/other/MessageGenerator.java")).isFalse();
    }

    @Test void qualifiedUserClassIsNotPrefixedWithFlowPackage() {
        when(element.getPropertyValue("userImplementedClassName")).thenReturn("org.custom.Splitter");
        assertThat(matches("/project/user/src/main/java/org/custom/Splitter.java")).isTrue();
    }

    @Test void librarySourcesMatchFullImplementationPackageIncludingNestedClasses() {
        when(meta.getImplementingClass()).thenReturn("org.ikasan.component.Splitter$Nested");
        assertThat(matches("/cache/sources.jar!/org/ikasan/component/Splitter.java")).isTrue();
        assertThat(matches("/cache/sources.jar!/unrelated/Splitter.java")).isFalse();
        assertThat(matches(null)).isFalse();
    }

    @Test void userFilterIsEligibleForCanvasRetentionButAmbiguousAndUnrelatedSourcesAreNot() {
        when(element.getPropertyValue("userImplementedClassName")).thenReturn("DefaultFilterRemoval");
        when(module.getFlows()).thenReturn(java.util.List.of(flow));
        when(flow.getFlowElementsNoExternalEndPoints()).thenReturn(java.util.List.of(element));
        String source = "/project/user/src/main/java/org/example/flow/DefaultFilterRemoval.java";
        assertThat(ComponentBreakpointSource.uniqueMatch("/project", module, source)).isSameAs(element);
        assertThat(ComponentBreakpointSource.uniqueMatch("/project", module, source.replace("DefaultFilterRemoval", "Helper"))).isNull();
        when(flow.getFlowElementsNoExternalEndPoints()).thenReturn(java.util.List.of(element, element));
        assertThat(ComponentBreakpointSource.uniqueMatch("/project", module, source)).isNull();
    }

    private boolean matches(String source) {
        return ComponentBreakpointSource.matches("/project", module, element, source);
    }
}
