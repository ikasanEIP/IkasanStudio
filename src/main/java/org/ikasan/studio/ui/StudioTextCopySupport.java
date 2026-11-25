package org.ikasan.studio.ui;

import com.intellij.openapi.ide.CopyPasteManager;
import com.intellij.openapi.util.SystemInfo;

import javax.swing.*;
import javax.swing.plaf.basic.BasicHTML;
import javax.swing.text.JTextComponent;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.HTML;
import javax.swing.text.html.parser.ParserDelegator;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.*;
import java.io.StringReader;
import java.awt.KeyboardFocusManager;
import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

/** Scoped to Studio component trees: no polling, global event hooks or document listeners. EDT only. */
public final class StudioTextCopySupport {
    private static final String INSTALLED = StudioTextCopySupport.class.getName() + ".installed";
    private static final String TRAVERSAL = StudioTextCopySupport.class.getName() + ".traversal";
    private record Traversal(java.util.Set<AWTKeyStroke> forward, java.util.Set<AWTKeyStroke> backward) { }
    private static final ContainerListener CHILDREN = new ContainerAdapter() {
        @Override public void componentAdded(ContainerEvent event) { install(event.getChild()); }
    };
    private static final MouseListener POPUP = new MouseAdapter() {
        @Override public void mousePressed(MouseEvent event) { popup(event); }
        @Override public void mouseReleased(MouseEvent event) { popup(event); }
    };

    private StudioTextCopySupport() { }

    public static void install(Component component) {
        if (!(component instanceof JComponent control) || control instanceof JPasswordField
                || control instanceof JMenuItem
                || control instanceof JPopupMenu || control instanceof JMenuBar
                || Boolean.TRUE.equals(control.getClientProperty(INSTALLED))) return;
        control.putClientProperty(INSTALLED, Boolean.TRUE);
        if (control instanceof JTabbedPane tabs) {
            // Tab headers belong to the platform UI delegate. Adding mouse listeners to their
            // labels can steal the click that normally selects a tab; only visit page contents.
            for (int i = 0; i < tabs.getTabCount(); i++) install(tabs.getComponentAt(i));
            tabs.addContainerListener(new ContainerAdapter() {
                @Override public void componentAdded(ContainerEvent event) {
                    for (int i = 0; i < tabs.getTabCount(); i++) {
                        if (tabs.getComponentAt(i) == event.getChild()) install(event.getChild());
                    }
                }
            });
            return;
        }
        if (control instanceof JTextComponent text) {
            if (text instanceof JTextArea) text.putClientProperty(TRAVERSAL, new Traversal(
                    text.getFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS),
                    text.getFocusTraversalKeys(KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS)));
            configureReadOnly(text);
            text.addPropertyChangeListener("editable", event -> configureReadOnly(text));
            text.addMouseListener(POPUP);
            return;
        }
        if (control instanceof JLabel || control instanceof AbstractButton) {
            control.addMouseListener(POPUP);
            return;
        }
        // Native controls own their editors, renderers and keyboard actions.
        if (control instanceof JComboBox<?> || control instanceof JTable || control instanceof JList<?>
                || control instanceof JTree || control instanceof JSpinner || control instanceof JScrollBar
                || control instanceof JSlider) return;
        for (Component child : control.getComponents()) install(child);
        control.addContainerListener(CHILDREN);
    }

    private static void configureReadOnly(JTextComponent text) {
        if (text.isEditable()) {
            if (text.getClientProperty(TRAVERSAL) instanceof Traversal original) {
                text.setFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS, original.forward());
                text.setFocusTraversalKeys(KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS, original.backward());
            }
            return;
        }
        text.setFocusable(true);
        if (text instanceof JTextArea) {
            // Informational paragraphs should not trap Tab like an editable multiline editor.
            KeyboardFocusManager focus = KeyboardFocusManager.getCurrentKeyboardFocusManager();
            text.setFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS,
                    focus.getDefaultFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS));
            text.setFocusTraversalKeys(KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS,
                    focus.getDefaultFocusTraversalKeys(KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS));
        }
    }

    private static void popup(MouseEvent event) {
        if (!event.isPopupTrigger() || event.isConsumed()) return;
        JComponent control = (JComponent) event.getComponent();
        if (control.getComponentPopupMenu() != null) return; // Preserve existing menus and actions.
        if (control instanceof JTextComponent text && text.isEditable()) return;
        JPopupMenu menu = popupFor(control);
        if (menu == null) return;
        menu.show(control, event.getX(), event.getY());
        event.consume();
    }

    static JPopupMenu popupFor(JComponent control) {
        return popupFor(control, value -> CopyPasteManager.getInstance().setContents(new StringSelection(value)));
    }

    static JPopupMenu popupFor(JComponent control, java.util.function.Consumer<String> clipboard) {
        if (control instanceof JPasswordField || (control instanceof JTextComponent text && text.isEditable())) return null;
        // Do not read large documents or parse HTML just to open a context menu.
        boolean hasText = control instanceof JTextComponent text ? text.getDocument().getLength() > 0
                : control instanceof JLabel label ? label.getText() != null && !label.getText().isBlank()
                : control instanceof AbstractButton button && button.getText() != null && !button.getText().isBlank();
        if (!hasText) return null;
        JPopupMenu menu = new JPopupMenu();
        boolean selected = control instanceof JTextComponent text && text.getSelectedText() != null;
        JMenuItem copy = new JMenuItem(StudioBundle.message(selected ? "textCopy.selection" : "textCopy.text"));
        copy.addActionListener(event -> {
            String value = copyText(control);
            if (value != null) clipboard.accept(value);
        });
        menu.add(copy);
        if (control instanceof JTextComponent text) {
            JMenuItem select = new JMenuItem(StudioBundle.message("textCopy.selectAll"));
            select.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_A,
                    SystemInfo.isMac ? InputEvent.META_DOWN_MASK : InputEvent.CTRL_DOWN_MASK));
            select.addActionListener(event -> { text.requestFocusInWindow(); text.selectAll(); });
            menu.add(select);
        }
        return menu;
    }

    static String copyText(JComponent control) {
        if (control instanceof JPasswordField) return null;
        if (control instanceof JTextComponent text) {
            String selected = text.getSelectedText();
            if (selected != null) return selected;
            try {
                String all = text.getDocument().getText(0, text.getDocument().getLength());
                return text.getDocument() instanceof HTMLDocument ? all.strip() : all;
            } catch (javax.swing.text.BadLocationException ignored) { return null; }
        }
        String value = control instanceof JLabel label ? label.getText()
                : control instanceof AbstractButton button ? button.getText() : null;
        if (!BasicHTML.isHTMLString(value)) return value;
        // A parser callback extracts visible text without constructing views, loading CSS,
        // following links or retrieving images. This runs only after an explicit copy request.
        StringBuilder plain = new StringBuilder();
        try {
            new ParserDelegator().parse(new StringReader(value), new HTMLEditorKit.ParserCallback() {
                private int hidden;
                private boolean hiddenTag(HTML.Tag tag) {
                    return tag == HTML.Tag.HEAD || tag == HTML.Tag.SCRIPT || tag == HTML.Tag.STYLE;
                }
                private void lineBreak() {
                    if (!plain.isEmpty() && plain.charAt(plain.length() - 1) != '\n') plain.append('\n');
                }
                @Override public void handleStartTag(HTML.Tag tag, javax.swing.text.MutableAttributeSet attributes, int pos) {
                    if (hiddenTag(tag)) hidden++;
                    else if (hidden == 0 && tag.breaksFlow()) lineBreak();
                }
                @Override public void handleEndTag(HTML.Tag tag, int pos) {
                    if (hiddenTag(tag)) hidden = Math.max(0, hidden - 1);
                    else if (hidden == 0 && tag.breaksFlow()) lineBreak();
                }
                @Override public void handleSimpleTag(HTML.Tag tag, javax.swing.text.MutableAttributeSet attributes, int pos) {
                    if (hidden == 0 && tag == HTML.Tag.BR) lineBreak();
                }
                @Override public void handleText(char[] data, int pos) { if (hidden == 0) plain.append(data); }
            }, true);
            return plain.toString().strip();
        } catch (java.io.IOException ignored) { return value; }
    }
}
