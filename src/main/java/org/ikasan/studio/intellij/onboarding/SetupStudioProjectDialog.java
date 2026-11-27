package org.ikasan.studio.intellij.onboarding;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.ProjectJdkTable;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.JBUI;
import java.awt.*;
import java.util.Map;
import javax.lang.model.SourceVersion;
import javax.swing.*;
import org.ikasan.studio.core.project.MavenProjectBootstrap.Eligibility;
import org.ikasan.studio.core.project.MavenProjectBootstrap.Options;
import org.ikasan.studio.intellij.migration.MigrationJdk;
import org.ikasan.studio.ui.StudioBundle;
import org.ikasan.studio.ui.StudioDialogWrapper;

final class SetupStudioProjectDialog extends StudioDialogWrapper {
    private final ComboBox<String> versions;
    private final ComboBox<Sdk> jdks = new ComboBox<>();
    private final JBTextField pkg;
    private final JBTextField port = new JBTextField("8080");
    private final JBLabel java = new JBLabel();
    private final Project project;
    private final Map<String, Integer> javaVersions;
    SetupStudioProjectDialog(Project project, Eligibility eligibility, Map<String, Integer> packs) {
        super(project); this.project = project; javaVersions = Map.copyOf(packs);
        versions = new ComboBox<>(packs.keySet().toArray(String[]::new));
        pkg = new JBTextField(eligibility.packageName(), 32);
        jdks.setRenderer(new DefaultListCellRenderer() {
            @Override public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean selected, boolean focus) {
                return super.getListCellRendererComponent(list, value instanceof Sdk sdk ? sdk.getName() : "", index, selected, focus);
            }
        });
        Sdk current = ProjectRootManager.getInstance(project).getProjectSdk();
        for (String pack : packs.keySet()) if (MigrationJdk.matches(current, packs.get(pack))) versions.setSelectedItem(pack);
        versions.addActionListener(event -> selectJdks());
        selectJdks();
        setTitle(StudioBundle.message("setup.Title")); setOKButtonText(StudioBundle.message("setup.Review")); init();
    }
    private void selectJdks() {
        jdks.removeAllItems();
        for (Sdk sdk : ProjectJdkTable.getInstance().getAllJdks()) if (MigrationJdk.matches(sdk, requiredJava())) jdks.addItem(sdk);
        Sdk current = ProjectRootManager.getInstance(project).getProjectSdk();
        if (MigrationJdk.matches(current, requiredJava())) jdks.setSelectedItem(current);
        java.setText(StudioBundle.message("setup.RequiredJava", requiredJava()));
    }
    int requiredJava() { return javaVersions.get((String) versions.getSelectedItem()); }
    Sdk sdk() { return (Sdk) jdks.getSelectedItem(); }
    Options options() { return new Options((String) versions.getSelectedItem(), pkg.getText().trim(), Integer.parseInt(port.getText().trim())); }
    @Override protected ValidationInfo doValidate() {
        if (!SourceVersion.isName(pkg.getText().trim())) return new ValidationInfo(StudioBundle.message("setup.BadPackage"), pkg);
        try { int value = Integer.parseInt(port.getText().trim()); if (value < 1 || value > 65533) throw new NumberFormatException(); }
        catch (NumberFormatException invalid) { return new ValidationInfo(StudioBundle.message("setup.BadPort"), port); }
        if (!MigrationJdk.matches(sdk(), requiredJava())) return new ValidationInfo(StudioBundle.message("message.SelectAnInstalledJdk", requiredJava()), jdks);
        return null;
    }
    @Override protected JComponent createStudioContent() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints(); c.insets = JBUI.insets(5); c.anchor = GridBagConstraints.WEST;
        String[] labels = {StudioBundle.message("setup.Version"), StudioBundle.message("setup.Package"), StudioBundle.message("setup.Port"), StudioBundle.message("setup.Jdk")};
        JComponent[] controls = {versions, pkg, port, jdks};
        for (int i=0; i<labels.length; i++) {
            c.gridy=i; c.gridx=0; c.weightx=0; c.fill=GridBagConstraints.NONE;
            JBLabel label = new JBLabel(labels[i]); label.setLabelFor(controls[i]); panel.add(label,c);
            c.gridx=1; c.weightx=1; c.fill=GridBagConstraints.HORIZONTAL; panel.add(controls[i],c);
        }
        c.gridy++; c.gridx=0; c.gridwidth=2; panel.add(java,c);
        return panel;
    }
}
