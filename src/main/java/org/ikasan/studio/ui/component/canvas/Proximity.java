package org.ikasan.studio.ui.component.canvas;

import org.ikasan.studio.Pair;

import java.awt.*;

/**
 * Reflects the location of one component relative to another.
 */
public enum Proximity {
    LEFT, RIGHT, CENTER, NONE;

    /**
     * Return the proximity of A relative to B i.e. is B to the LEFT of A or to the RIGHT of A
     * @param a the first point
     * @param b the point we are looking to identify as relativve
     * @param proximityDetection how far to look
     * @return the proximity of A relative to B i.e. is B to the LEFT of A or to the RIGHT of A
     */
    public static Proximity getRelativeProximity(Point a, Point b, Pair<Integer, Integer> proximityDetection) {
        Proximity returnProximity = Proximity.NONE;
        int deltaX = a.x - b.x ;
        int deltaY = a.y - b.y ;
        if (Math.abs(deltaX) < proximityDetection.getX() && Math.abs(deltaY) < proximityDetection.getY()) {
            if (deltaX > 0)  {
                returnProximity = Proximity.LEFT;
            } else if (deltaX < 0) {
                returnProximity = Proximity.RIGHT;
            } else if (deltaX == 0)
                returnProximity = Proximity.CENTER;
        }
        return returnProximity;
    }
}
