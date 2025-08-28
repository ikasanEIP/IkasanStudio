package org.ikasan.studio.core;

import javax.swing.*;

public class StudioComparitors {
    public static boolean imageIconsEqual(ImageIcon i1, ImageIcon i2) {
        System.out.println("Comparing icons: " + i1 + " vs " + i2);
        if (i1 == i2) return true;
        if (i1 == null || i2 == null) return false;
        System.out.println("Heights: " + i1.getIconHeight() + " vs " + i2.getIconHeight());
        return i1.getIconHeight() == i2.getIconHeight();
    }
    public static int compareImageIcons(ImageIcon i1, ImageIcon i2) {
        if (i1 == i2) return 0;
        if (i1 == null) return -1;
        if (i2 == null) return 1;
        return Integer.compare(i1.getIconHeight(), i2.getIconHeight());
    }
}
