package org.ikasan.studio.ui.actions;

import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTextArea;
import org.ikasan.studio.ui.StudioBundle;
import org.ikasan.studio.ui.StudioUIUtils;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Lets the user provide the test message payload either by typing it directly, or by loading a file's
 * contents into the text area (via "Load from File..."). Either way, whatever is in the text area when
 * the dialog is confirmed becomes the payload, so a loaded file can still be tweaked before sending.
 */
public class SendTestMessagePayloadDialog extends DialogWrapper {
    private final Project project;
    private final JBTextArea payloadTextArea = new JBTextArea(10, 60);

    public SendTestMessagePayloadDialog(Project project) {
        super(project, true);
        this.project = project;
        init();
        setTitle(StudioBundle.message("dialog.SendTestMessage"));
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.add(new JBLabel(StudioBundle.message("message.EnterTheTestMessagePayload")), BorderLayout.NORTH);

        payloadTextArea.setLineWrap(true);
        panel.add(new JBScrollPane(payloadTextArea), BorderLayout.CENTER);

        JButton loadFromFileButton = new JButton(StudioBundle.message("button.LoadPayloadFromFile"));
        loadFromFileButton.addActionListener(e -> loadFromFile());
        JPanel southPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        southPanel.add(loadFromFileButton);
        panel.add(southPanel, BorderLayout.SOUTH);

        panel.setPreferredSize(new Dimension(500, 300));
        return panel;
    }

    private void loadFromFile() {
        VirtualFile file = FileChooser.chooseFile(
                FileChooserDescriptorFactory.createSingleFileDescriptor(), project, null);
        if (file == null) {
            return;
        }
        try {
            // Read via the VirtualFile's own API (not java.nio.file) so this also works when the file
            // lives on a remote/WSL/Docker dev environment (IntelliJ's Eel abstraction), not just locally.
            payloadTextArea.setText(new String(file.contentsToByteArray(), StandardCharsets.UTF_8));
        } catch (IOException ex) {
            StudioUIUtils.displayIdeaWarnMessage(project,
                    StudioBundle.message("message.CouldNotReadPayloadFile", ex.getMessage()));
        }
    }

    public String getPayload() {
        return payloadTextArea.getText();
    }
}
