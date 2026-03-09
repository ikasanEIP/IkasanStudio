package org.ikasan.studio.ui.harness;

import org.ikasan.studio.ui.component.palette.PaletteTabPanel;
import org.junit.Test;

public class PaletteTabPanelHarness extends ComponentTestHarness {

    /**
     * This is a test method, not a standard application entry point.
     * To run this harness, right-click this method in your IDE and select "Run 'test_display_paletteTabPanel()'".
     * This executes the test within the IntelliJ Platform environment, which is required for UI components.
     */
    @Test
    public void test_display_paletteTabPanel() {
        // It's important to create and manipulate UI components on the EDT.
        // BasePlatformTestCase and our showInFrame method handle this for you.
        PaletteTabPanel paletteTabPanel = new PaletteTabPanel(getProject());
        showInFrame(paletteTabPanel, "PaletteTabPanel Harness");
    }

    /**
     * This main method is for informational purposes only.
     * You cannot run Swing UI tests that depend on the IntelliJ Platform
     * via a standard main() method. Please run the @Test method above.
     */
    public static void main(String[] args) {
        System.out.println("-----------------------------------------------------------------");
        System.out.println("ERROR: You cannot run this harness using a standard main() method.");
        System.out.println("This is because it needs the IntelliJ Platform to be initialized.");
        System.out.println("\nTo run this harness:");
        System.out.println("1. Open this file in your IntelliJ IDE.");
        System.out.println("2. Right-click on the 'test_display_paletteTabPanel' method.");
        System.out.println("3. Select \"Run 'test_display_paletteTabPanel()'\" from the context menu.");
        System.out.println("-----------------------------------------------------------------");
    }
}
