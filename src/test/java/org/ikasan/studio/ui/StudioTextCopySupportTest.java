package org.ikasan.studio.ui;

import org.junit.jupiter.api.Test;
import javax.swing.*;
import java.awt.KeyboardFocusManager;
import java.util.ArrayList;
import static org.junit.jupiter.api.Assertions.*;
import java.awt.AWTKeyStroke;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.CellRendererPane;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JPopupMenu;
import javax.swing.JTextArea;
import javax.swing.text.Document;

class StudioTextCopySupportTest {
    @Test void explanatoryTextCanBeSelectedAndCopiedWithoutBecomingEditable() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            JTextArea text = new JTextArea("First detail\nSecond detail");
            text.setEditable(false); text.setFocusable(false);
            StudioTextCopySupport.install(text);
            assertTrue(text.isFocusable()); assertFalse(text.isEditable());
            text.select(0, 5);
            ArrayList<String> copied = new ArrayList<>();
            JPopupMenu menu = StudioTextCopySupport.popupFor(text, copied::add);
            assertNotNull(menu);
            ((JMenuItem) menu.getComponent(0)).doClick();
            assertEquals(java.util.List.of("First"), copied);
            ((JMenuItem) menu.getComponent(1)).doClick();
            assertEquals(text.getText(), text.getSelectedText());
            assertTrue(text.getFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS).contains(
                    KeyStroke.getKeyStroke("TAB")));
        });
    }

    @Test void labelsAndHeadingsCopyVisibleTextAndDecodeHtmlWithoutMarkup() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            JLabel label = new JLabel("<html><b>Version &amp; status</b><br>4.1.3 &#8594; 4.1.6</html>");
            StudioTextCopySupport.install(label);
            ArrayList<String> copied = new ArrayList<>();
            JPopupMenu menu = StudioTextCopySupport.popupFor(label, copied::add);
            assertNotNull(menu);
            ((JMenuItem) menu.getComponent(0)).doClick();
            assertEquals(1, copied.size());
            assertTrue(copied.get(0).contains("Version & status"));
            assertTrue(copied.get(0).contains("4.1.3 → 4.1.6"));
            assertFalse(copied.get(0).contains("<html>"));
            label.setText("Updated status");
            ((JMenuItem) menu.getComponent(0)).doClick();
            assertEquals("Updated status", copied.get(1));
        });
    }

    @Test void dynamicChildrenAreCoveredAndRepeatedInstallationDoesNotAddListeners() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            JPanel root = new JPanel();
            StudioTextCopySupport.install(root);
            int listeners = root.getContainerListeners().length;
            StudioTextCopySupport.install(root);
            assertEquals(listeners, root.getContainerListeners().length);
            JTextArea text = new JTextArea("New diagnostic");
            text.setEditable(false); text.setFocusable(false);
            root.add(text);
            assertTrue(text.isFocusable());
            int mouseListeners = text.getMouseListeners().length;
            root.remove(text); root.add(text); StudioTextCopySupport.install(root);
            assertEquals(mouseListeners, text.getMouseListeners().length);
        });
    }

    @Test void editableControlsCustomMenusAndButtonActionsArePreserved() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            JTextArea text = new JTextArea("Editable JSON");
            Set<AWTKeyStroke> traversal = text.getFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS);
            JPopupMenu originalMenu = new JPopupMenu(); originalMenu.add("Existing action");
            text.setComponentPopupMenu(originalMenu);
            StudioTextCopySupport.install(text);
            assertNull(StudioTextCopySupport.popupFor(text));
            assertSame(originalMenu, text.getComponentPopupMenu());
            text.setEditable(false); text.setEditable(true);
            assertEquals(traversal, text.getFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS));
            JButton button = new JButton("Review and import");
            AtomicInteger clicks = new java.util.concurrent.atomic.AtomicInteger();
            button.addActionListener(event -> clicks.incrementAndGet());
            StudioTextCopySupport.install(button); button.doClick();
            assertEquals(1, clicks.get());
            assertEquals("Review and import", StudioTextCopySupport.copyText(button));
        });
    }

    @Test void passwordFieldsAndRendererTreesAreUntouched() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            JPasswordField password = new JPasswordField("secret");
            password.setEditable(false); password.setFocusable(false);
            int listeners = password.getMouseListeners().length;
            StudioTextCopySupport.install(password);
            assertFalse(password.isFocusable());
            assertEquals(listeners, password.getMouseListeners().length);
            assertNull(StudioTextCopySupport.popupFor(password));
            assertNull(StudioTextCopySupport.copyText(password));
            CellRendererPane renderer = new CellRendererPane(); JLabel label = new JLabel("Rendered cell");
            renderer.add(label);
            int before = label.getMouseListeners().length;
            StudioTextCopySupport.install(renderer);
            assertEquals(before, label.getMouseListeners().length);
        });
    }

    @Test void openingCopyMenuDoesNotReadTheWholeDocument() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            JTextArea text = new JTextArea("Large read-only document");
            text.setEditable(false);
            Document model = org.mockito.Mockito.spy(text.getDocument());
            text.setDocument(model);
            org.mockito.Mockito.clearInvocations(model);
            assertNotNull(StudioTextCopySupport.popupFor(text));
            assertDoesNotThrow(() -> {
                org.mockito.Mockito.verify(model, org.mockito.Mockito.never()).getText(
                        org.mockito.ArgumentMatchers.anyInt(), org.mockito.ArgumentMatchers.anyInt());
            });
        });
    }
}
