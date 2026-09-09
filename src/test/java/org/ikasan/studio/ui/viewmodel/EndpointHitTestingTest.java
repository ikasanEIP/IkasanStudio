package org.ikasan.studio.ui.viewmodel;

import com.intellij.openapi.project.Project;
import org.ikasan.studio.core.TestFixtures;
import org.ikasan.studio.core.model.ikasan.instance.Flow;
import org.ikasan.studio.core.model.ikasan.instance.FlowRoute;
import org.ikasan.studio.core.model.ikasan.instance.Module;
import org.ikasan.studio.ui.UiContext;
import org.junit.jupiter.api.Test;

import javax.swing.JPanel;
import java.awt.Font;
import java.awt.image.BufferedImage;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class EndpointHitTestingTest {
    @Test
    void repaintReplacesOldEndpointHitAreasWithoutGrowingTheProjectCache() throws Exception {
        Project project = mock(Project.class);
        UiContext context = mock(UiContext.class);
        when(project.getService(UiContext.class)).thenReturn(context);
        ViewHandlerCache cache = new ViewHandlerCache(project);
        when(context.getViewHandlerFactory()).thenReturn(cache);
        Module module = mock(Module.class);
        when(context.getIkasanModule()).thenReturn(module);
        when(module.getMetaVersion()).thenReturn(TestFixtures.BASE_META_PACK);
        Flow flow = mock(Flow.class);
        FlowRoute route = mock(FlowRoute.class);
        when(flow.getFlowRoute()).thenReturn(route);
        var producer = TestFixtures.getEmailProducer(TestFixtures.BASE_META_PACK);
        when(route.getConsumerAndFlowRouteElements()).thenReturn(List.of(producer));
        var handler = new IkasanFlowRouteViewHandler(project, flow, route);
        var flowView = ViewHandlerCache.getFlowViewHandler(project, flow);
        flowView.setLeftX(100);
        flowView.setWidth(200);
        var producerView = ViewHandlerCache.getFlowComponentViewHandler(project, producer);
        producerView.setLeftX(200);
        producerView.setTopY(40);
        producerView.setWidth(60);
        int initialCacheSize = cache.size();
        var graphics = new BufferedImage(1000, 400, BufferedImage.TYPE_INT_ARGB).createGraphics();
        graphics.setFont(new Font(Font.DIALOG, Font.PLAIN, 12));
        try {
            var panel = new JPanel();
            handler.paintRoute(panel, graphics, route, null);
            var first = handler.getEndpointViewHandlerForOwner(producer);
            int oldX = first.getLeftX() + 10;
            int y = first.getTopY() + 10;
            assertThat(handler.getOwnerForEndpointAtXY(oldX, y)).isSameAs(producer);

            flowView.setWidth(400);
            producerView.setLeftX(oldX - 5);
            handler.paintRoute(panel, graphics, route, null);
            var moved = handler.getEndpointViewHandlerForOwner(producer);
            assertThat(handler.getOwnerForEndpointAtXY(oldX, y)).isNull();
            assertThat(handler.getOwnerForEndpointAtXY(moved.getLeftX() + 10, y)).isSameAs(producer);
            assertThat(cache.size()).isEqualTo(initialCacheSize);

            when(route.getConsumerAndFlowRouteElements()).thenReturn(List.of());
            handler.paintRoute(panel, graphics, route, null);
            assertThat(handler.getEndpointViewHandlerForOwner(producer)).isNull();
            assertThat(handler.getOwnerForEndpointAtXY(moved.getLeftX() + 10, y)).isNull();
        } finally {
            graphics.dispose();
            handler.dispose();
            cache.clear();
        }
    }
}
