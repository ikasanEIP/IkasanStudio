package org.ikasan.studio.ui.harness;

import org.ikasan.studio.core.TestFixtures;
import org.ikasan.studio.ui.component.properties.ComponentPropertiesPanel;
import org.junit.jupiter.api.Test;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Regression guard for a real layout bug (confirmed via this test before the fix): the "Recipe description"
 * JTextArea's own preferred height was correctly computed, but the JBScrollPane wrapping it lived inside a
 * GridBagLayout section where every row - including this one - has weighty 0. Whenever that section ended up
 * even slightly shorter than the sum of its rows' preferred heights (an ordinary state, since the panel's
 * preferred height varies with whichever component/recipe is selected), GridBagLayout took the whole shortfall
 * out of this one row - a JScrollPane's own minimumSize collapses to almost nothing, unlike the text fields
 * around it, so it was always the row picked to absorb the deficit, crushed to a few pixels regardless of how
 * many rows it asked for as its preferred size. The fix (ComponentPropertiesPanel) gives it an explicit
 * minimumSize floor instead, so any real shortfall propagates up to the panel's own outer JBScrollPane, which
 * already scrolls correctly.
 */
public class RecipeDescriptionSizingHarnessTest extends ComponentTestHarness {

    @Test
    void recipeDescriptionNeverShrinksBelowItsMinimumHeight() throws Exception {
        var converter = TestFixtures.getCustomConverter("V3.3.9");
        converter.setPropertyValue("fromType", "org.ikasan.filetransfer.Payload");
        converter.setPropertyValue("toType", "org.ikasan.component.endpoint.email.producer.EmailPayload");
        converter.setPropertyValue("conversionRecipeId", "file-transfer-payload-to-email-attachment");

        com.intellij.util.ui.UIUtil.invokeAndWaitIfNeeded(() -> {
            ComponentPropertiesPanel panel = new ComponentPropertiesPanel(getProject(), false);
            panel.updateTargetComponent(converter);

            JFrame frame = new JFrame("sizing probe");
            frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            frame.getContentPane().add(panel, BorderLayout.CENTER);
            // Deliberately shorter than this panel's natural preferred height, to force the same squeeze the
            // real properties panel is under whenever there isn't room to show every row at full size.
            frame.setSize(480, 500);
            frame.setVisible(true);
            frame.validate();

            JTextArea recipeHelp = findByAccessibleName(panel, "Conversion details");
            // Fully qualified, not statically imported - BasePlatformTestCase's JUnit 3 ancestry (junit.framework.
            // TestCase/Assert) provides instance methods of the same name but with message-first parameter order,
            // which silently shadows a static import of the JUnit 5 Assertions of the same name (see
            // FlowsUserImplementedComponentTemplateTest/ConversionRecipeEditorTest for the same workaround).
            org.junit.jupiter.api.Assertions.assertNotNull(recipeHelp, "Recipe description text area was not found in the properties panel");
            Container scroll = recipeHelp.getParent().getParent(); // JViewport -> JScrollPane
            org.junit.jupiter.api.Assertions.assertInstanceOf(JScrollPane.class, scroll, "Expected the text area's grandparent to be its JScrollPane");
            int minimumHeight = scroll.getMinimumSize().height;
            org.junit.jupiter.api.Assertions.assertTrue(minimumHeight >= 40, "Recipe description scrollpane has no meaningful minimum height guard: " + minimumHeight);
            org.junit.jupiter.api.Assertions.assertTrue(scroll.getSize().height >= minimumHeight,
                    "Recipe description was squeezed below its own declared minimum height: actual=" + scroll.getSize().height + " minimum=" + minimumHeight);

            frame.dispose();
        });
    }

    private JTextArea findByAccessibleName(Container root, String name) {
        Deque<Component> stack = new ArrayDeque<>();
        stack.push(root);
        while (!stack.isEmpty()) {
            Component c = stack.pop();
            if (c instanceof JTextArea ta && ta.getAccessibleContext() != null
                    && name.equals(ta.getAccessibleContext().getAccessibleName())) {
                return ta;
            }
            if (c instanceof Container container) {
                for (Component child : container.getComponents()) stack.push(child);
            }
        }
        return null;
    }
}
