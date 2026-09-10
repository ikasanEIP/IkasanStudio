package org.ikasan.studio.ui.component.properties;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import org.ikasan.studio.ui.StudioBundle;

import javax.swing.JComponent;
import javax.swing.JPanel;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

/** A bounded, wrapping warning keeps even long package names readable. */
final class FlowPackageChangeDialog extends DialogWrapper {
    private final String oldPackage;
    private final String newPackage;

    FlowPackageChangeDialog(Project project, String oldPackage, String newPackage) {
        super(project, false);
        this.oldPackage = oldPackage;
        this.newPackage = newPackage;
        setTitle(StudioBundle.message("dialog.FlowPackageChange"));
        setOKButtonText(StudioBundle.message("button.RenameFlow"));
        init();
    }

    @Override
    protected JComponent createCenterPanel() {
        JPanel details = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.anchor = GridBagConstraints.NORTHWEST;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridwidth = 2;
        c.weightx = 1;
        details.add(PropertyDialogLayout.wrappedText(StudioBundle.message("message.FlowPackageChange"), 520), c);
        c.gridwidth = 1;
        c.gridy = 0;
        PropertyDialogLayout.addRow(details, c, StudioBundle.message("label.OldFlowPackage"), oldPackage);
        PropertyDialogLayout.addRow(details, c, StudioBundle.message("label.NewFlowPackage"), newPackage);
        return PropertyDialogLayout.wrap(details, null);
    }
}
