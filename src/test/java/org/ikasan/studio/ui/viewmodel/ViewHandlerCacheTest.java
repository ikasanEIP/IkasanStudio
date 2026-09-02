package org.ikasan.studio.ui.viewmodel;

import com.intellij.openapi.project.Project;
import org.ikasan.studio.core.model.ikasan.instance.FlowElement;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class ViewHandlerCacheTest {

    @Test
    void ownsHandlersByDomainObjectIdentityAndReleasesThemOnClear() {
        ViewHandlerCache cache = new ViewHandlerCache(mock(Project.class));
        FlowElement first = mock(FlowElement.class);
        FlowElement second = mock(FlowElement.class);

        AbstractViewHandlerIntellij firstHandler = cache.getOrCreate(first);

        assertThat(cache.getOrCreate(first)).isSameAs(firstHandler);
        assertThat(cache.getOrCreate(second)).isNotSameAs(firstHandler);
        assertThat(cache.size()).isEqualTo(2);

        cache.clear();

        assertThat(cache.size()).isZero();
        assertThat(cache.getOrCreate(first)).isNotSameAs(firstHandler);
    }
}
