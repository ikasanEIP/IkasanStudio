package org.ikasan.studio.ui.component.palette;

// Display an icon and a string for each object in the list.

import org.ikasan.studio.ui.StudioUIUtils;
import org.ikasan.studio.ui.model.PaletteItem;
import org.ikasan.studio.ui.model.PaletteItemIkasanComponent;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import java.awt.*;

public class PaletteListCellRenderer extends JLabel implements ListCellRenderer<Object> {
    // This is the only method defined by ListCellRenderer.
    // We just reconfigure the JLabel each time we're called.
    public Component getListCellRendererComponent(
            JList<?> list,           // the list
            Object value,            // value to display
            int index,               // cell index
            boolean isSelected,      // is the cell selected
            boolean cellHasFocus)    // does the cell have focus
    {
        if (value instanceof PaletteItemIkasanComponent) {
            PaletteItem paletteItem = (PaletteItem) value;
            setText(paletteItem.getIkasanFlowElementViewHandler().getText());
            setIcon(paletteItem.getIkasanFlowElementViewHandler().getDisplayIcon());
            setBorder(BorderFactory.createEmptyBorder(2, 0, 0, 0));

            if (isSelected) {
                setBackground(list.getSelectionBackground());
                setForeground(list.getSelectionForeground());
            } else {
                setBackground(list.getBackground());
                setForeground(list.getForeground());
            }
            setEnabled(list.isEnabled());
            setFont(list.getFont());
            setOpaque(true);
            setBorder(new EmptyBorder(0, 0, 2, 0));
        } else {
            if (index > 0) {
                setBorder(new MatteBorder(1, 0, 0, 0, StudioUIUtils.IKASAN_GREY));
            }
            PaletteItem paletteItemSeparator = (PaletteItem) value;
            setText(paletteItemSeparator.getIkasanComponentMeta().getName());
            Font labelFont = getFont();
            Font boldLabelFond = new Font(labelFont.getFontName(), Font.BOLD, labelFont.getSize());
            setFont(boldLabelFond);
            setForeground(StudioUIUtils.IKASAN_ORANGE);

            setIcon(null);
            this.setFocusable(false);
        }
        return this;
    }
}
