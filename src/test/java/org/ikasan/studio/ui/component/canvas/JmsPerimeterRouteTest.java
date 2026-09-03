package org.ikasan.studio.ui.component.canvas;

import org.junit.jupiter.api.Test;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class JmsPerimeterRouteTest {
    @Test
    void routesBeyondEverySideOfTheReservedArtworkBounds() {
        Rectangle artwork = new Rectangle(150, 100, 700, 600);
        Point producer = new Point(700, 250);
        Point consumer = new Point(200, 500);

        List<Point> route = DesignerCanvas.jmsPerimeterRoute(artwork, producer, consumer, 0);

        assertThat(route).hasSize(6);
        assertThat(route.get(1).x).isGreaterThan(artwork.x + artwork.width);
        assertThat(route.get(2).y).isGreaterThan(artwork.y + artwork.height);
        assertThat(route.get(3).x).isLessThan(artwork.x);
        assertThat(route.get(4).y).isEqualTo(consumer.y);
        assertThat(route.get(5)).isEqualTo(consumer);
    }

    @Test
    void allocatesLaterLinksToDistinctOuterLanes() {
        Rectangle artwork = new Rectangle(150, 100, 700, 600);
        Point producer = new Point(700, 250);
        Point consumer = new Point(200, 500);

        List<Point> first = DesignerCanvas.jmsPerimeterRoute(artwork, producer, consumer, 0);
        List<Point> second = DesignerCanvas.jmsPerimeterRoute(artwork, producer, consumer, 1);

        assertThat(second.get(1).x).isGreaterThan(first.get(1).x);
        assertThat(second.get(2).y).isGreaterThan(first.get(2).y);
        assertThat(second.get(3).x).isLessThan(first.get(3).x);
    }


    @Test
    void routesAdjacentFlowsThroughTheGapBetweenThem() {
        Rectangle producerFlow = new Rectangle(290, 200, 420, 140);
        Rectangle consumerFlow = new Rectangle(290, 390, 420, 140);
        Point producer = new Point(820, 270);
        Point consumer = new Point(180, 460);

        List<Point> route = DesignerCanvas.jmsAdjacentFlowRoute(producerFlow, consumerFlow, producer, consumer);

        int corridorY = route.get(2).y;
        assertThat(corridorY).isGreaterThan(producerFlow.y + producerFlow.height);
        assertThat(corridorY).isLessThan(consumerFlow.y);
        assertThat(route.get(3).y).isEqualTo(corridorY);
        assertThat(route.get(5)).isEqualTo(consumer);
    }
}
