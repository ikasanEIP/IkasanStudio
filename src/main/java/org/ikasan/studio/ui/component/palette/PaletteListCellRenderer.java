package org.ikasan.studio.ui.component.palette;

// Display an icon and a string for each object in the list.

import org.ikasan.studio.ui.model.PaletteItemIkasanComponent;
import org.ikasan.studio.ui.model.PaletteItemSeparator;

import javax.swing.*;
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
            PaletteItemIkasanComponent paletteItem = (PaletteItemIkasanComponent) value;
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
        } else {
            PaletteItemSeparator paletteItemSeparator = (PaletteItemSeparator) value;
            setText(paletteItemSeparator.getIkasanFlowUIComponent().getTitle());
            setIcon(null);
            this.setFocusable(false);
        }
        return this;
    }
}
