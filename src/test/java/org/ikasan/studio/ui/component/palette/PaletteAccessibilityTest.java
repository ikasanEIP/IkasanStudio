package org.ikasan.studio.ui.component.palette;

import org.ikasan.studio.core.TestFixtures;
import org.ikasan.studio.core.metapack.ComponentLibrary;
import org.ikasan.studio.core.metapack.model.ComponentMeta;
import org.ikasan.studio.ui.model.PaletteItem;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import java.awt.Color;
import javax.swing.*;
import javax.swing.border.CompoundBorder;
import static org.junit.jupiter.api.Assertions.*;

class PaletteAccessibilityTest {
    static ComponentMeta meta;
    @BeforeAll static void metadata() throws Exception {
        meta = ComponentLibrary.getIkasanComponentByKeyMandatory(TestFixtures.BASE_META_PACK, "FTP Consumer");
    }
    @Test void categoryAndComponentReuseThemeColoursAndNativeFocusBorder() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            var list = new JList<>();
            var renderer = new PaletteListCellRenderer();
            String longName = "Long endpoint and component description ".repeat(8);
            for (Color background : new Color[] {Color.WHITE, Color.DARK_GRAY, Color.BLACK}) {
                list.setBackground(background); list.setForeground(Color.YELLOW);
                list.setSelectionBackground(Color.BLUE); list.setSelectionForeground(Color.WHITE);
                for (PaletteItem item : new PaletteItem[] {new PaletteItem(longName, meta), new PaletteItem(meta)}) {
                    var label = (JLabel) renderer.getListCellRendererComponent(list, item, 0, true, true);
                    assertEquals(list.getSelectionBackground(), label.getBackground());
                    assertEquals(list.getSelectionForeground(), label.getForeground());
                    assertNotNull(((CompoundBorder) label.getBorder()).getOutsideBorder());
                    assertEquals(label.getText(), label.getAccessibleContext().getAccessibleName());
                    assertEquals(label.getText(), label.getToolTipText());
                    renderer.getListCellRendererComponent(list, item, 0, false, false);
                    assertEquals(background, label.getBackground());
                    assertEquals(Color.YELLOW, label.getForeground());
                }
            }
        });
    }
}
