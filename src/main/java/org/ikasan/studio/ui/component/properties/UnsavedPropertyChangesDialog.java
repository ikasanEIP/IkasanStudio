package org.ikasan.studio.ui.component.properties;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Messages;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.ui.JBUI;
import org.ikasan.studio.ui.StudioBundle;
import org.ikasan.studio.ui.StudioUIUtils;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.util.List;

/**
 * The "Unsaved Property Changes" dialog shown before a selection change or a launch discards or applies
 * pending edits - see ComponentPropertiesPanel#confirmSelectionChangeWithPendingEdits /
 * #preparePendingChangesForLaunch. Beyond the one-line summary (already naming the component and its changed
 * property labels), a details section always lists each changed property's old and new value, so a developer
 * isn't left guessing what actually changed before choosing to Apply or Discard - or, for a change they don't
 * recognise making at all, seeing exactly what it was helps track down why it happened. The third button
 * (previously a plain "Cancel") is "Jump to Properties": it declines to apply or discard anything, exactly as
 * Cancel always did, but also opens/focuses the Studio editor and switches to the Properties tab so the
 * affected component's editor is immediately in view rather than wherever the developer happened to be
 * looking when the dialog appeared.
 */
public class UnsavedPropertyChangesDialog extends DialogWrapper {

    public record PropertyChangeDetail(String label, String oldValueDisplay, String newValueDisplay) {}

    public enum Choice { APPLY, DISCARD, JUMP_TO_PROPERTIES }

    private final String message;
    private final String componentName;
    private final List<PropertyChangeDetail> changes;
    // Chosen by whichever button closes the dialog; a window-close (X) or Escape leaves this at its default,
    // matching Cancel's old behaviour of declining to apply or discard anything.
    private Choice choice = Choice.JUMP_TO_PROPERTIES;

    public UnsavedPropertyChangesDialog(Project project, String title, String message, String componentName,
                                         List<PropertyChangeDetail> changes) {
        super(project, false);
        this.message = message;
        this.componentName = componentName;
        this.changes = changes;
        setTitle(title);
        init();
    }

    public Choice getChoice() {
        return choice;
    }

    @Override
    protected JComponent createCenterPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(JBUI.Borders.empty(4));

        JPanel messageRow = new JPanel(new BorderLayout(12, 0));
        JLabel iconLabel = new JLabel(Messages.getWarningIcon());
        iconLabel.setVerticalAlignment(SwingConstants.TOP);
        messageRow.add(iconLabel, BorderLayout.WEST);
        JBLabel messageLabel = new JBLabel("<html>" + StudioUIUtils.escapeHtml(message) + "</html>");
        messageRow.add(messageLabel, BorderLayout.CENTER);
        panel.add(messageRow);

        if (!changes.isEmpty()) {
            panel.add(buildDetailsPanel());
        }

        JPanel sized = new JPanel(new BorderLayout());
        sized.add(panel, BorderLayout.NORTH);
        sized.setPreferredSize(JBUI.size(480, panel.getPreferredSize().height));
        return sized;
    }

    private JPanel buildDetailsPanel() {
        JPanel table = new JPanel(new GridBagLayout());
        table.setBorder(BorderFactory.createTitledBorder(
                StudioBundle.message("label.ChangedPropertiesFor", componentName)));
        GridBagConstraints labelConstraints = new GridBagConstraints();
        labelConstraints.gridx = 0;
        labelConstraints.anchor = GridBagConstraints.WEST;
        labelConstraints.insets = JBUI.insets(2, 0, 2, 8);
        GridBagConstraints oldValueConstraints = new GridBagConstraints();
        oldValueConstraints.gridx = 1;
        oldValueConstraints.anchor = GridBagConstraints.WEST;
        oldValueConstraints.insets = JBUI.insets(2, 0, 2, 6);
        GridBagConstraints arrowConstraints = new GridBagConstraints();
        arrowConstraints.gridx = 2;
        arrowConstraints.anchor = GridBagConstraints.WEST;
        arrowConstraints.insets = JBUI.insets(2, 0, 2, 6);
        GridBagConstraints newValueConstraints = new GridBagConstraints();
        newValueConstraints.gridx = 3;
        newValueConstraints.anchor = GridBagConstraints.WEST;
        newValueConstraints.insets = JBUI.insets(2, 0);

        int row = 0;
        for (PropertyChangeDetail change : changes) {
            labelConstraints.gridy = row;
            oldValueConstraints.gridy = row;
            arrowConstraints.gridy = row;
            newValueConstraints.gridy = row;
            table.add(new JBLabel("<html><b>" + StudioUIUtils.escapeHtml(change.label()) + "</b></html>"), labelConstraints);
            table.add(new JBLabel(StudioUIUtils.escapeHtml(change.oldValueDisplay())), oldValueConstraints);
            table.add(new JBLabel("→"), arrowConstraints);
            table.add(new JBLabel(StudioUIUtils.escapeHtml(change.newValueDisplay())), newValueConstraints);
            row++;
        }
        JBScrollPane scrollPane = new JBScrollPane(table);
        scrollPane.setBorder(JBUI.Borders.empty());
        scrollPane.setPreferredSize(JBUI.size(450, Math.min(200, table.getPreferredSize().height + 10)));
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.add(scrollPane, BorderLayout.CENTER);
        return wrapper;
    }

    // Deliberately not @NotNull-annotated - see CLAUDE.md: this project avoids @NotNull since the IntelliJ
    // Gradle plugin instruments it with a runtime assertion that would surface as an uncaught plugin
    // exception rather than failing gracefully. DialogWrapper#createActions() itself is @NotNull.
    @SuppressWarnings("NullableProblems")
    @Override
    protected Action[] createActions() {
        Action apply = new AbstractAction(StudioBundle.message("button.ApplyChanges")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                choice = Choice.APPLY;
                close(OK_EXIT_CODE);
            }
        };
        apply.putValue(DEFAULT_ACTION, Boolean.TRUE);
        Action discard = new AbstractAction(StudioBundle.message("button.DiscardChanges")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                choice = Choice.DISCARD;
                close(NEXT_USER_EXIT_CODE);
            }
        };
        Action jumpToProperties = new AbstractAction(StudioBundle.message("button.JumpToProperties")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                choice = Choice.JUMP_TO_PROPERTIES;
                close(CANCEL_EXIT_CODE);
            }
        };
        return new Action[]{apply, discard, jumpToProperties};
    }
}
