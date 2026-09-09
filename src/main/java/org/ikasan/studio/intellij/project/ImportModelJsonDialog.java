package org.ikasan.studio.intellij.project;

import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Messages;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTextArea;
import com.intellij.util.ui.JBUI;
import org.ikasan.studio.core.StudioBuildException;
import org.ikasan.studio.ui.StudioBundle;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;

/**
 * Collects a model.json for import either by pasting it directly or by choosing a file (which fills the same text
 * area, so the chosen content stays reviewable before Import is pressed). Import itself is delegated to
 * {@link ModelImporter#importFromText} - a failed validation keeps the dialog open for correction.
 */
public class ImportModelJsonDialog extends DialogWrapper {
    private final Project project;
    private final JBTextArea jsonArea = new JBTextArea();
    private final JBLabel chosenFilePathLabel = new JBLabel();

    public ImportModelJsonDialog(Project project) {
        super(project, true);
        this.project = project;
        setTitle(StudioBundle.message("dialog.ImportModelJson"));
        setOKButtonText(StudioBundle.message("button.ImportModelJson"));
        init();
        // Import needs something to import: keep the OK action disabled until a file has been chosen or model
        // JSON has been pasted into the text area.
        setOKActionEnabled(false);
        jsonArea.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent event) {
                updateOkAction();
            }

            @Override
            public void removeUpdate(DocumentEvent event) {
                updateOkAction();
            }

            @Override
            public void changedUpdate(DocumentEvent event) {
                updateOkAction();
            }
        });
    }

    private void updateOkAction() {
        String text = jsonArea.getText();
        setOKActionEnabled(text != null && !text.isBlank());
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, JBUI.scale(8)));

        JBLabel explanation = new JBLabel(StudioBundle.message("message.ImportModelJsonExplanation"));
        explanation.setBorder(JBUI.Borders.emptyBottom(4));
        panel.add(explanation, BorderLayout.NORTH);

        jsonArea.setRows(16);
        jsonArea.setLineWrap(false);
        jsonArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        panel.add(new JBScrollPane(jsonArea), BorderLayout.CENTER);

        JPanel chooseRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        JButton chooseFileButton = new JButton(StudioBundle.message("button.ChooseModelJsonFile"));
        chooseFileButton.addActionListener(event -> chooseFile());
        chooseRow.add(chooseFileButton);
        chooseRow.add(Box.createHorizontalStrut(JBUI.scale(8)));
        chosenFilePathLabel.setForeground(com.intellij.util.ui.UIUtil.getLabelForeground());
        chooseRow.add(chosenFilePathLabel);
        panel.add(chooseRow, BorderLayout.SOUTH);

        return panel;
    }

    @Override
    public @Nullable JComponent getPreferredFocusedComponent() {
        return jsonArea;
    }

    private void chooseFile() {
        FileChooserDescriptor descriptor = FileChooserDescriptorFactory.createSingleFileDescriptor()
                .withTitle(StudioBundle.message("dialog.ChooseModelJsonFile"))
                .withDescription(StudioBundle.message("message.ChooseModelJsonFileDescription"));
        StudioProjectFiles.chooseFileAndReadText(project, descriptor, result -> {
            if (isDisposed() || project.isDisposed()) return;
            if (result.content() != null) {
                jsonArea.setText(result.content());
                chosenFilePathLabel.setText(result.path());
                chosenFilePathLabel.setToolTipText(result.path());
                jsonArea.requestFocusInWindow();
            } else {
                chosenFilePathLabel.setText("");
                Messages.showErrorDialog(project,
                        StudioBundle.message("message.ImportModelFileReadFailed", result.errorMessage()),
                        StudioBundle.message("dialog.ImportModelJson"));
            }
        });
    }

    @Override
    protected void doOKAction() {
        String json = jsonArea.getText();
        if (json == null || json.isBlank()) {
            Messages.showErrorDialog(project,
                    StudioBundle.message("message.ImportModelNoContent"),
                    StudioBundle.message("dialog.ImportModelJson"));
            return;
        }
        try {
            ModelImporter.importFromText(project, json, chosenFilePathLabel.getText().isBlank()
                    ? StudioBundle.message("label.PastedModelJson") : chosenFilePathLabel.getText());
        } catch (com.intellij.openapi.progress.ProcessCanceledException cancelled) {
            throw cancelled;
        } catch (StudioBuildException | RuntimeException e) {
            Messages.showErrorDialog(project,
                    StudioBundle.message("message.ImportModelFailed", e.getMessage()),
                    StudioBundle.message("dialog.ImportModelJson"));
            // Keep the dialog open so the developer can correct the JSON and retry.
            return;
        }
        close(OK_EXIT_CODE);
    }
}
