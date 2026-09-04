package org.ikasan.studio.ui.component;

import com.intellij.icons.AllIcons;
import com.intellij.ui.AnimatedIcon;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBTextArea;
import com.intellij.ui.components.labels.LinkLabel;
import com.intellij.util.ui.JBFont;
import com.intellij.util.ui.JBUI;
import org.ikasan.studio.ui.StudioBundle;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.function.IntConsumer;

/**
 * Empty-state content shown while Ikasan Studio prepares the current project.
 */
public final class StudioInitialisationPanel extends JBPanel<StudioInitialisationPanel> {
    private final JBLabel statusIcon = new JBLabel(new AnimatedIcon.Default());
    private final JBLabel statusLabel = new JBLabel();
    private final JBTextArea detailText = new JBTextArea();
    private final LinkLabel<Object> retryLink = new LinkLabel<>(StudioBundle.message("label.Retry"), null);
    private final JPanel recoveryLinks = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));

    public StudioInitialisationPanel(Runnable retryAction, IntConsumer restoreBackupAction) {
        super(new GridBagLayout());
        setBorder(JBUI.Borders.empty(24));

        JBLabel heading = new JBLabel(StudioBundle.message("label.LoadingIkasanStudio"));
        heading.setFont(JBFont.h2());

        JBTextArea waitingText = new JBTextArea(
                StudioBundle.message("message.PleaseWaitWhileIkasanStudioLoadsTheModule"));
        configureDescription(waitingText);
        detailText.setEditable(false);
        detailText.setFocusable(false);
        detailText.setLineWrap(true);
        detailText.setWrapStyleWord(true);
        detailText.setOpaque(false);
        detailText.setBorder(JBUI.Borders.empty());
        detailText.setColumns(48);

        retryLink.setListener((source, data) -> retryAction.run(), null);
        retryLink.setVisible(false);
        recoveryLinks.setOpaque(false);
        recoveryLinks.setVisible(false);
        for (int index = 1; index <= 3; index++) {
            int backupIndex = index;
            LinkLabel<Object> link = new LinkLabel<>(StudioBundle.message("label.RestoreModelBackup", index), null);
            link.setName("restoreModelBackup" + index);
            link.setListener((source, data) -> restoreBackupAction.accept(backupIndex), null);
            recoveryLinks.add(link);
        }

        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.gridwidth = 2;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.insets = JBUI.insetsBottom(16);
        add(heading, constraints);

        constraints.gridy++;
        constraints.gridwidth = 2;
        constraints.weightx = 1;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.insets = JBUI.insetsBottom(16);
        add(waitingText, constraints);

        constraints.gridy++;
        constraints.gridwidth = 1;
        constraints.weightx = 0;
        constraints.fill = GridBagConstraints.NONE;
        constraints.insets = JBUI.insetsRight(8);
        add(statusIcon, constraints);

        constraints.gridx = 1;
        constraints.weightx = 1;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.insets = JBUI.emptyInsets();
        add(statusLabel, constraints);

        constraints.gridx = 0;
        constraints.gridy++;
        constraints.gridwidth = 2;
        constraints.insets = JBUI.insetsTop(8);
        add(detailText, constraints);

        constraints.gridy++;
        constraints.fill = GridBagConstraints.NONE;
        constraints.insets = JBUI.insetsTop(12);
        add(retryLink, constraints);

        constraints.gridy++;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        add(recoveryLinks, constraints);

        constraints.gridy++;
        constraints.weighty = 1;
        add(Box.createVerticalGlue(), constraints);

        showWaitingForIndexes();
    }

    private static void configureDescription(JBTextArea textArea) {
        textArea.setEditable(false);
        textArea.setFocusable(false);
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        textArea.setOpaque(false);
        textArea.setBorder(JBUI.Borders.empty());
        textArea.setColumns(48);
    }

    public void showWaitingForIndexes() {
        showProgress(StudioBundle.message("label.WaitingForIntellijToFinishIndexing"),
                StudioBundle.message("message.YouCanContinueWorkingWhileIntellijPreparesTheProject"));
    }

    public void showWaitingForProjectImport() {
        showProgress(StudioBundle.message("label.ImportingTheMavenProject"),
                StudioBundle.message("message.IkasanStudioWillContinueAutomaticallyWhenIntellijHasImported"));
    }

    public void showReadingProject() {
        showProgress(StudioBundle.message("label.ReadingTheIkasanProject"),
                StudioBundle.message("message.LoadingTheMavenConfigurationAndIkasanModuleModel"));
    }

    public void showLoadingComponents() {
        showProgress(StudioBundle.message("label.LoadingIkasanComponents"),
                StudioBundle.message("message.PreparingTheComponentLibraryAndDesignerPalette"));
    }

    public void showFailure(String detail, List<Integer> backupIndexes) {
        statusIcon.setIcon(AllIcons.General.Warning);
        statusLabel.setText(StudioBundle.message("message.IkasanStudioCouldNotPrepareThisProject"));
        detailText.setText(detail);
        retryLink.setVisible(true);
        for (Component component : recoveryLinks.getComponents()) {
            int index = Integer.parseInt(component.getName().replace("restoreModelBackup", ""));
            component.setVisible(backupIndexes.contains(index));
        }
        recoveryLinks.setVisible(!backupIndexes.isEmpty());
        revalidate();
        repaint();
    }

    private void showProgress(String status, String detail) {
        statusIcon.setIcon(new AnimatedIcon.Default());
        statusLabel.setText(status);
        detailText.setText(detail);
        retryLink.setVisible(false);
        recoveryLinks.setVisible(false);
        revalidate();
        repaint();
    }
}
