package org.ikasan.studio.ui.component;

import javax.swing.*;
import java.awt.*;

/**
 * This class implements the Scrollable interface so that we can use a JPanel containing a gridPanel can be used
 * within a scrollpane in a similar way to JTable, JList and friends.
 *
 * It is recommended to use a border layout to ensure normal scrollbar functionality.
 *
 * Once the gridbag JPanel is created and populated, simply add it to this panel.
 *
 * It is assumed this panel will be added to a scrollpane e.g.
 *
 * JScrollPane scroll = new JScrollPane(new ScrollableGridbagPanel(gridBagJPanel));
 */
public class ScrollableGridbagPanel extends JPanel implements Scrollable {

    public ScrollableGridbagPanel(JPanel panelContainingGridbag) {
        super(new BorderLayout());
        this.add(panelContainingGridbag);
    }

    @Override
    public Dimension getPreferredSize() {
        return getPreferredScrollableViewportSize();
    }

    /**
     * How small must the displayed area be before the scrollbars appear.
     *
     * @return a dimension containing the size of the viewport needed
     *          to display {@code visibleRowCount} rows
     * @see #getPreferredScrollableViewportSize
     */
    public Dimension getPreferredScrollableViewportSize() {
        int height = 0;
        int width = 0;
        if (this.getComponentCount() > 0) {
            for (Component component : this.getComponents()) {
                Dimension dimension = component.getPreferredSize();

                height = Math.max(height, dimension.height);
                width += Math.max(width, dimension.width);
            }
        }
        return new Dimension(width, height);
    }

    /**
     * Returns the distance to scroll to expose the next or previous block.
     * e.g. pg up / down
     * <p>
     * For vertical scrolling, the following rules are used:
     * <ul>
     * <li>if scrolling down, returns the distance to scroll so that the last
     * visible element becomes the first completely visible element
     * <li>if scrolling up, returns the distance to scroll so that the first
     * visible element becomes the last completely visible element
     * <li>returns {@code visibleRect.height} if the list is empty
     * </ul>
     * <p>
     * For horizontal scrolling, when the layout orientation is either
     * {@code VERTICAL_WRAP} or {@code HORIZONTAL_WRAP}:
     * <ul>
     * <li>if scrolling right, returns the distance to scroll so that the
     * last visible element becomes
     * the first completely visible element
     * <li>if scrolling left, returns the distance to scroll so that the first
     * visible element becomes the last completely visible element
     * <li>returns {@code visibleRect.width} if the list is empty
     * </ul>
     * <p>
     * For horizontal scrolling and {@code VERTICAL} orientation,
     * returns {@code visibleRect.width}.
     * <p>
     * Note that the value of {@code visibleRect} must be the equal to
     * {@code this.getVisibleRect()}.
     *
     * @param visibleRect the view area visible within the viewport
     * @param orientation {@code SwingConstants.HORIZONTAL} or
     *                    {@code SwingConstants.VERTICAL}
     * @param direction less or equal to zero to scroll up/back,
     *                  greater than zero for down/forward
     * @return the "block" increment for scrolling in the specified direction;
     *         always positive
     * @see #getScrollableUnitIncrement
     * @see Scrollable#getScrollableBlockIncrement
     * @throws IllegalArgumentException if {@code visibleRect} is {@code null}, or
     *         {@code orientation} isn't one of {@code SwingConstants.VERTICAL} or
     *         {@code SwingConstants.HORIZONTAL}
     */
    public int getScrollableBlockIncrement(Rectangle visibleRect,
                                           int orientation, int direction) {
        return 50;
    }

    /**
     * Returns the distance to scroll to expose the next or previous
     * row (for vertical scrolling) or column (for horizontal scrolling) e.g.  arrow up / down
     * <p>
     * For horizontal scrolling, if the layout orientation is {@code VERTICAL},
     * then the list's font size is returned (or {@code 1} if the font is
     * {@code null}).
     *
     * @param visibleRect the view area visible within the viewport
     * @param orientation {@code SwingConstants.HORIZONTAL} or
     *                    {@code SwingConstants.VERTICAL}
     * @param direction less or equal to zero to scroll up/back,
     *                  greater than zero for down/forward
     * @return the "unit" increment for scrolling in the specified direction;
     *         always positive
     * @see #getScrollableBlockIncrement
     * @see Scrollable#getScrollableUnitIncrement
     * @throws IllegalArgumentException if {@code visibleRect} is {@code null}, or
     *         {@code orientation} isn't one of {@code SwingConstants.VERTICAL} or
     *         {@code SwingConstants.HORIZONTAL}
     */
    public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation,
                                          int direction) {
        return 10;
    }

    /**
     * Do we care if the viewport is higher than the contained panel e.g.
     * Do we want the contained panel to stretch
     *
     * Returns {@code true} if this {@code JPanel} is displayed in a
     * {@code JViewport} and the viewport is taller than the list's
     * preferred height, or if the layout orientation is {@code VERTICAL_WRAP}
     * and {@code visibleRowCount <= 0}; otherwise returns {@code false}.
     * <p>
     * If {@code false}, then don't track the viewport's height. This allows
     * vertical scrolling if the {@code JViewport} is itself embedded in a
     * {@code JScrollPane}.
     *
     * @return whether or not an enclosing viewport should force the list's
     *         height to match its own
     * @see Scrollable#getScrollableTracksViewportHeight
     */
    public boolean getScrollableTracksViewportHeight() {
        return false;
    }

    /**
     * Do we care if the viewport is wider than the contained panel e.g.
     * Do we want the contained panel to stretch
     *
     * Returns {@code true} if this {@code JPanel} is displayed in a
     * {@code JViewport} and the viewport is wider than the JPanel's
     * preferred width (i.e. we may want the panel within to stretch so we must inform the
     * scroll pane that it needs to take note), or if the layout orientation is {@code HORIZONTAL_WRAP}
     * and {@code visibleRowCount <= 0}; otherwise returns {@code false}.
     * <p>
     * If {@code false}, then don't track the viewport's width. This allows
     * horizontal scrolling if the {@code JViewport} is itself embedded in a
     * {@code JScrollPane}.
     *
     * @return whether or not an enclosing viewport should force the enclosed JPanel's
     *         width to match its own
     * @see Scrollable#getScrollableTracksViewportWidth
     */
    public boolean getScrollableTracksViewportWidth() {
        return getParent() != null ? getParent().getSize().width > getPreferredSize().width
                : true;
    }
}
