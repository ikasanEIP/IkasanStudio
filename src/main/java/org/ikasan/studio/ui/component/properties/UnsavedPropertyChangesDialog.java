package org.ikasan.studio.ui.component.properties;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.util.ui.JBUI;
import org.ikasan.studio.ui.StudioBundle;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JPanel;
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
        this.changes = List.copyOf(changes);
        setTitle(title);
        init();
    }

    public Choice getChoice() {
        return choice;
    }

    @Override
    protected JComponent createCenterPanel() {
        return buildContent(message, componentName, changes);
    }

    static JComponent buildContent(String message, String componentName, List<PropertyChangeDetail> changes) {
        JPanel details = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.anchor = GridBagConstraints.NORTHWEST;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 2;
        c.weightx = 1;
        c.insets = JBUI.insetsBottom(12);
        details.add(PropertyDialogLayout.wrappedText(message, 520), c);
        if (!changes.isEmpty()) {
            c.gridy++;
            details.add(PropertyDialogLayout.wrappedText(
                    StudioBundle.message("label.ChangedPropertiesFor", componentName), 520), c);
            for (PropertyChangeDetail change : changes) {
                c.gridy++;
                c.gridx = 0;
                c.gridwidth = 2;
                c.weightx = 1;
                c.insets = JBUI.insets(12, 0, 3, 0);
                var heading = PropertyDialogLayout.wrappedText(change.label(), 520);
                heading.setFont(heading.getFont().deriveFont(java.awt.Font.BOLD));
                details.add(heading, c);
                c.gridwidth = 1;
                PropertyDialogLayout.addRow(details, c, StudioBundle.message("label.SavedPropertyValue"), change.oldValueDisplay());
                PropertyDialogLayout.addRow(details, c, StudioBundle.message("label.NewPropertyValue"), change.newValueDisplay());
            }
        }
        return PropertyDialogLayout.wrap(details, null);
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
