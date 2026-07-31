package org.ikasan.studio.ui.component;

import com.intellij.icons.AllIcons;
import com.intellij.ui.AnimatedIcon;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBTextArea;
import com.intellij.ui.components.labels.LinkLabel;
import com.intellij.util.ui.JBFont;
import com.intellij.util.ui.JBUI;

import javax.swing.*;
import java.awt.*;

/**
 * Empty-state content shown while Ikasan Studio prepares the current project.
 */
public final class StudioInitialisationPanel extends JBPanel<StudioInitialisationPanel> {
    private final JBLabel statusIcon = new JBLabel(new AnimatedIcon.Default());
    private final JBLabel statusLabel = new JBLabel();
    private final JBTextArea detailText = new JBTextArea();
    private final LinkLabel<Object> retryLink = new LinkLabel<>("Retry", null);

    public StudioInitialisationPanel(Runnable retryAction) {
        super(new GridBagLayout());
        setBorder(JBUI.Borders.empty(24));

        JBLabel heading = new JBLabel("Ikasan Studio");
        heading.setFont(JBFont.h2());

        detailText.setEditable(false);
        detailText.setFocusable(false);
        detailText.setLineWrap(true);
        detailText.setWrapStyleWord(true);
        detailText.setOpaque(false);
        detailText.setBorder(JBUI.Borders.empty());
        detailText.setColumns(48);

        retryLink.setListener((source, data) -> retryAction.run(), null);
        retryLink.setVisible(false);

        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.gridwidth = 2;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.insets = JBUI.insetsBottom(16);
        add(heading, constraints);

        constraints.gridy++;
        constraints.gridwidth = 1;
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
        constraints.weighty = 1;
        add(Box.createVerticalGlue(), constraints);

        showWaitingForIndexes();
    }

    public void showWaitingForIndexes() {
        showProgress("Waiting for IntelliJ to finish indexing…",
                "You can continue working while IntelliJ prepares the project.");
    }

    public void showReadingProject() {
        showProgress("Reading the Ikasan project…",
                "Loading the Maven configuration and Ikasan module model.");
    }

    public void showLoadingComponents() {
        showProgress("Loading Ikasan components…",
                "Preparing the component library and designer palette.");
    }

    public void showFailure(String detail) {
        statusIcon.setIcon(AllIcons.General.Warning);
        statusLabel.setText("Ikasan Studio could not prepare this project");
        detailText.setText(detail);
        retryLink.setVisible(true);
        revalidate();
        repaint();
    }

    private void showProgress(String status, String detail) {
        statusIcon.setIcon(new AnimatedIcon.Default());
        statusLabel.setText(status);
        detailText.setText(detail);
        retryLink.setVisible(false);
        revalidate();
        repaint();
    }
}
