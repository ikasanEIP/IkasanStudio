package org.ikasan.studio.ui.component.palette;

// Display an icon and a string for each object in the list.

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.util.ui.JBUI;
import org.ikasan.studio.ui.StudioUIUtils;
import org.ikasan.studio.ui.model.PaletteItem;

import javax.swing.*;
import javax.swing.border.MatteBorder;
import java.awt.*;

public class PaletteListCellRenderer extends JLabel implements ListCellRenderer<Object> {
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
        // This is only sed for PaletteItems
        if (value instanceof PaletteItem paletteItem) {
            if (paletteItem.isCategory()) {
                // Separator
                if (index > 0) {
                    setBorder(new MatteBorder(1, 0, 0, 0, StudioUIUtils.IKASAN_GREY));
                }

                setText(paletteItem.getIkasanComponentMeta().getName());
                Font labelFont = getFont();
                Font boldLabelFond = new Font(labelFont.getFontName(), Font.BOLD, labelFont.getSize());
                setFont(boldLabelFond);
                setForeground(StudioUIUtils.IKASAN_ORANGE);

                setIcon(null);
                this.setFocusable(false);

            } else {
                setText(paletteItem.getIkasanPaletteElementViewHandler().getText());
                setIcon(paletteItem.getIkasanPaletteElementViewHandler().getDisplayIcon());
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
                setBorder(JBUI.Borders.emptyBottom(2));
            }
        } else {
            LOG.warn("The PaletteListCellRenderer should contain a PaletteItem but did contain " + value);
        }
        return this;
    }
}
