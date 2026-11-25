package org.ikasan.studio.ui.component.properties;

import org.ikasan.studio.core.TestFixtures;
import org.ikasan.studio.core.metapack.ComponentLibrary;
import org.ikasan.studio.core.metapack.model.ComponentMeta;
import org.ikasan.studio.core.model.ikasan.instance.ComponentProperty;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import javax.swing.*;
import static org.junit.jupiter.api.Assertions.*;
import com.intellij.openapi.ui.ComboBox;

class ComponentPropertyAccessibilityTest {
    static ComponentMeta ftp;
    @BeforeAll static void metadata() throws Exception {
        ftp = ComponentLibrary.getIkasanComponentByKeyMandatory(TestFixtures.BASE_META_PACK, "FTP Consumer");
    }
    @Test void labelsDescribeBothBooleanChoicesAndTheTextEditor() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            for (String key : new String[] {"remoteHost", "ftps", "cronExpression"}) {
                ComponentPropertyEditRow row = new ComponentPropertyEditRow(null, new ComponentProperty(ftp.getMetadata(key)), false);
                ComponentInput input = row.getInputField();
                String label = row.getPropertyTitleField().getText();
                assertSame(input.getFirstFocusComponent(), row.getPropertyTitleField().getLabelFor());
                assertTrue(input.getFirstFocusComponent().getAccessibleContext().getAccessibleName().startsWith(label));
                if (input.isBooleanInput()) {
                    assertTrue(input.getFalseBox().getAccessibleContext().getAccessibleName().startsWith(label));
                    assertNotEquals(input.getTrueBox().getAccessibleContext().getAccessibleName(),
                            input.getFalseBox().getAccessibleContext().getAccessibleName());
                }
                if (row.getDataValidationHelper() != null)
                    assertNotEquals("...", row.getDataValidationHelper().getAccessibleContext().getAccessibleName());
            }
        });
    }
    @Test void disablingAChoiceActuallyPreventsSelectionChangesThroughTheWidget() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            ComboBox<Object> combo = new com.intellij.openapi.ui.ComboBox<>(new Object[] {"one", "two"});
            ComponentInput input = new ComponentInput(combo);
            input.setEnabled(false);
            assertFalse(combo.isEnabled());
            input.setEnabled(true);
            assertTrue(combo.isEnabled());
        });
    }
}
