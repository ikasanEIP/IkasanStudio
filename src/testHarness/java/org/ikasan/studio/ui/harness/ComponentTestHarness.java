package org.ikasan.studio.ui.harness;

import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import com.intellij.util.ui.UIUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.concurrent.CountDownLatch;

/**
 * Base class for UI component test harnesses.
 * Extends BasePlatformTestCase to provide a valid IntelliJ Platform environment.
 *
 * This class is tagged "harness", allowing these slow, UI-based tests to be
 * excluded from the main test suite.
 */
@Tag("harness")
public abstract class ComponentTestHarness extends BasePlatformTestCase {

    /**
     * This method bridges the JUnit 5 test runner with the JUnit 4-style setup
     * method in BasePlatformTestCase. It ensures that the test fixture (myFixture)
     * is properly initialized before each test.
     *
     * @throws Exception if the base setup fails.
     */
    @BeforeEach
    public void setUpHarness() throws Exception {
        super.setUp();
    }

    /**
     * This method bridges the JUnit 5 test runner with the JUnit 4-style teardown
     * method in BasePlatformTestCase. It ensures that the test fixture is properly
     * cleaned up after each test.
     *
     * @throws Exception if the base teardown fails.
     */
    @AfterEach
    public void tearDownHarness() throws Exception {
        super.tearDown();
    }

    /**
     * Displays a JComponent in a JFrame for visual inspection.
     * This method blocks the test thread until the frame is closed, allowing for interaction.
     *
     * @param component The component to display.
     * @param title     The title of the frame.
     */
    protected void showInFrame(JComponent component, String title) {
        final CountDownLatch latch = new CountDownLatch(1);

        UIUtil.invokeAndWaitIfNeeded(() -> {
            JFrame frame = new JFrame(title);
            frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            frame.getContentPane().add(component, BorderLayout.CENTER);

            frame.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosed(WindowEvent e) {
                    latch.countDown();
                }
            });

            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });

        try {
            // Block the test thread until the frame is closed by the user.
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Test harness was interrupted.", e);
        }
    }
}
