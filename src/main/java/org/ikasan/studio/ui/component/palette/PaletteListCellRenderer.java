package org.ikasan.studio.ui.component.palette;

// Display an icon and a string for each object in the list.

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.util.ui.JBUI;
import org.ikasan.studio.ui.model.PaletteItem;

import javax.swing.*;
import java.awt.*;

public class PaletteListCellRenderer extends DefaultListCellRenderer {
    public static final Logger LOG = Logger.getInstance("PaletteListCellRenderer");
    // This is the only method defined by ListCellRenderer.
    // We just reconfigure the JLabel each time we're called.
    public Component getListCellRendererComponent(
            JList<?> list,           // the list
            Object value,            // value to display
            int index,               // cell index
            boolean isSelected,      // is the cell selected
            boolean cellHasFocus)    // does the cell have focus
    {
        super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
        if (value instanceof PaletteItem paletteItem) {
            var handler = paletteItem.getIkasanPaletteElementViewHandler();
            setText(paletteItem.isCategory() ? handler.getComponentMeta().getName() : handler.getText());
            setIcon(paletteItem.isCategory() ? null : handler.getDisplayIcon());
            setFont(paletteItem.isCategory() ? list.getFont().deriveFont(Font.BOLD) : list.getFont());
            // Keep the look-and-feel's selection colours and focus border, including high contrast.
            setBorder(BorderFactory.createCompoundBorder(getBorder(),
                    paletteItem.isCategory() ? JBUI.Borders.empty(5, 3, 4, 3) : JBUI.Borders.emptyBottom(4)));
            setToolTipText(getText());
            getAccessibleContext().setAccessibleName(getText());
        } else {
            LOG.warn("STUDIO: The PaletteListCellRenderer should contain a PaletteItem but did contain " + value);
        }
        return this;
    }
}
