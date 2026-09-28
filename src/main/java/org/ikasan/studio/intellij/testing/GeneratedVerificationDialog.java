package org.ikasan.studio.intellij.testing;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBLabel;
import com.intellij.util.ui.JBUI;
import org.ikasan.studio.ui.StudioBundle;
import javax.swing.JComponent;
import javax.swing.JPanel;
import java.awt.BorderLayout;

/** Confirmation with an explicit, default-off archive option for an existing baseline. */
// Local-IDE dialog; remote Split Mode requires a frontend UI and RPC service boundary.
@SuppressWarnings("SplitModeApiUsage")
final class GeneratedVerificationDialog extends DialogWrapper {
    private final boolean existingTests;
    private final JBCheckBox archive = new JBCheckBox(StudioBundle.message("verification.archive"), false);

    GeneratedVerificationDialog(Project project, boolean existingTests) {
        super(project);
        this.existingTests = existingTests;
        setTitle(StudioBundle.message("verification.title"));
        setOKButtonText(StudioBundle.message("flowTest.generate"));
        archive.setToolTipText(StudioBundle.message("verification.archive.help"));
        init();
    }

    boolean archivePrevious() { return existingTests && archive.isSelected(); }

    @Override protected JComponent createCenterPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, JBUI.scale(12)));
        String prompt = StudioBundle.message(existingTests ? "verification.replace" : "verification.create");
        panel.add(new JBLabel("<html><body style='width: 420px'>"
                + StringUtil.escapeXmlEntities(prompt) + "</body></html>"), BorderLayout.CENTER);
        if (existingTests) panel.add(archive, BorderLayout.SOUTH);
        return panel;
    }
}
