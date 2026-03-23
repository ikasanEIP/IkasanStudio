package org.ikasan.studio.ui.harness;

import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBPanel;
import org.ikasan.studio.core.StudioBuildException;
import org.ikasan.studio.core.TestFixtures;
import org.ikasan.studio.core.model.ikasan.instance.ExceptionResolution;
import org.ikasan.studio.ui.UiContext;
import org.ikasan.studio.ui.component.ScrollableGridbagPanel;
import org.ikasan.studio.ui.component.canvas.CanvasPanel;
import org.ikasan.studio.ui.component.palette.PaletteTabPanel;
import org.ikasan.studio.ui.component.properties.ComponentPropertiesPanel;
import org.ikasan.studio.ui.component.properties.CronPanel;
import org.ikasan.studio.ui.component.properties.ExceptionResolutionPanel;
import org.ikasan.studio.ui.component.properties.ExceptionResolverPanel;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;

/**
 * This class is used to display individual custom panels so that can be inspected and tested manually.
 * It will NOT be executed as part of the general JUnit test run.
 * -
 * The suite() method returns an empty TestSuite to prevent JUnit 3/Vintage (triggered by BasePlatformTestCase
 * extending TestCase) from raising "No tests found". Individual methods can still be run via right-click in the IDE.
 */
public class PanelHarnessTest extends ComponentTestHarness {

    /**
     * This is a test method, not a standard application entry point.
     * To run this harness, right-click this method in your IDE and select "Run 'displayCanvasPanel()'".
     * This executes the test within the IntelliJ Platform environment, which is required for UI components.
     */
    @Test
    void displayCanvasPanel() {
        // It's important to create and manipulate UI components on the EDT.
        // BasePlatformTestCase and our showInFrame method handle this for you.
        CanvasPanel canvasPanel = new CanvasPanel(getProject());
        showInFrame(canvasPanel, "CanvasPanel Harness");
    }

    /**
     * This is a test method, not a standard application entry point.
     * To run this harness, right-click this method in your IDE and select "Run 'displayComponentPropertiesPanel()'".
     * This executes the test within the IntelliJ Platform environment, which is required for UI components.
     */
    @Test
    void displayComponentPropertiesPanel() throws StudioBuildException {
        // It's important to create and manipulate UI components on the EDT.
        // BasePlatformTestCase and our showInFrame method handle this for you.
        ComponentPropertiesPanel componentPropertiesPanel = new ComponentPropertiesPanel(getProject(), false);
        componentPropertiesPanel.updateTargetComponent(TestFixtures.getGenericComponent());
        showInFrame(componentPropertiesPanel, "displayComponentPropertiesPanel Harness");
    }

    /**
     * This is a test method, not a standard application entry point.
     * To run this harness, right-click this method in your IDE and select "Run 'displayScrollableGridbagPanel()'".
     * This executes the test within the IntelliJ Platform environment, which is required for UI components.
     */
    @Test
    void displayCronPanel() {
        // It's important to create and manipulate UI components on the EDT.
        // BasePlatformTestCase and our showInFrame method handle this for you.
        CronPanel cronPanel = new CronPanel(getProject(), "* * * * *");
        showInFrame(cronPanel, "CronPanel Harness");
    }

    /**
     * This is a test method, not a standard application entry point.
     * To run this harness, right-click this method in your IDE and select "Run 'displayExceptionResolutionPanel()'".
     * This executes the test within the IntelliJ Platform environment, which is required for UI components.
     */
    @Test
    void displayExceptionResolutionPanel() throws StudioBuildException {
        // It's important to create and manipulate UI components on the EDT.
        // BasePlatformTestCase and our showInFrame method handle this for you.
        Project project = getProject();
        UiContext uiContext = project.getService(UiContext.class);
        uiContext.setIkasanModule(TestFixtures.getMyFirstModuleIkasanModule(TestFixtures.BASE_META_PACK, new ArrayList<>()));

        ExceptionResolutionPanel exceptionResolutionPanel = new ExceptionResolutionPanel(new ArrayList<>(), getProject(), true);
        ExceptionResolution newResolution = new ExceptionResolution(TestFixtures.BASE_META_PACK);
        exceptionResolutionPanel.updateTargetComponent(newResolution);
        showInFrame(exceptionResolutionPanel, "ExceptionResolutionPanel Harness");
    }

    /**
     * This is a test method, not a standard application entry point.
     * To run this harness, right-click this method in your IDE and select "Run 'displayExceptionResolverPanel()'".
     * This executes the test within the IntelliJ Platform environment, which is required for UI components.
     */
    @Test
    void displayExceptionResolverPanel() throws StudioBuildException {
        // It's important to create and manipulate UI components on the EDT.
        // BasePlatformTestCase and our showInFrame method handle this for you.
        Project project = getProject();
        UiContext uiContext = project.getService(UiContext.class);
        uiContext.setIkasanModule(TestFixtures.getMyFirstModuleIkasanModule(TestFixtures.BASE_META_PACK, new ArrayList<>()));

        ExceptionResolverPanel exceptionResolverPanel = new ExceptionResolverPanel(getProject(), true);
        showInFrame(exceptionResolverPanel, "ExceptionResolverPanel Harness");
        exceptionResolverPanel.updateTargetComponent(TestFixtures.getExceptionResolver(TestFixtures.BASE_META_PACK));
    }

    /**
     * This is a test method, not a standard application entry point.
     * To run this harness, right-click this method in your IDE and select "Run 'displayPaletteTabPanel()'".
     * This executes the test within the IntelliJ Platform environment, which is required for UI components.
     */
    @Test
    void displayPaletteTabPanel() {
        // It's important to create and manipulate UI components on the EDT.
        // BasePlatformTestCase and our showInFrame method handle this for you.
        PaletteTabPanel paletteTabPanel = new PaletteTabPanel(getProject());
        showInFrame(paletteTabPanel, "PaletteTabPanel Harness");
    }

    /**
     * This is a test method, not a standard application entry point.
     * To run this harness, right-click this method in your IDE and select "Run 'displayScrollableGridbagPanel()'".
     * This executes the test within the IntelliJ Platform environment, which is required for UI components.
     */
    @Test
    void displayScrollableGridbagPanel() {
        // It's important to create and manipulate UI components on the EDT.
        // BasePlatformTestCase and our showInFrame method handle this for you.
        @SuppressWarnings("rawtypes")
        ScrollableGridbagPanel scrollableGridbagPanel = new ScrollableGridbagPanel(new JBPanel());
        showInFrame(scrollableGridbagPanel, "ScrollableGridbagPanel Harness");
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
        System.out.println("2. Right-click on the 'displayPaletteTabPanel' method.");
        System.out.println("3. Select \"Run 'displayPaletteTabPanel()'\" from the context menu.");
        System.out.println("-----------------------------------------------------------------");
    }
}
