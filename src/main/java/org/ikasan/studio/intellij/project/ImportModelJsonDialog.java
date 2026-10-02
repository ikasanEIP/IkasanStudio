package org.ikasan.studio.intellij.project;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBTextField;
import org.ikasan.studio.core.importer.IkasanRuntimeImport;
import org.ikasan.studio.core.metapack.ComponentLibrary;
import org.ikasan.studio.ui.UiContext;
import java.util.concurrent.atomic.AtomicReference;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.colors.EditorFontType;
import com.intellij.util.ui.JBFont;

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

    private final JBCheckBox runtimeMode = new JBCheckBox(StudioBundle.message("import.runtime.enabled"));
    private final ComboBox<String> runtimePack = new ComboBox<>();
    private final JBTextField applicationPackage = new JBTextField();
    private final JBTextArea configurationArea = new JBTextArea();

    public ImportModelJsonDialog(Project project) {
        super(project, true);
        this.project = project;
        setTitle(StudioBundle.message("dialog.ImportModelJson"));
        setOKButtonText(StudioBundle.message("button.ImportModelJson"));
        applicationPackage.setText(project.getService(UiContext.class).getOptions().getPackageName());
        init();
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                var packs = ComponentLibrary.getMetapackList();
                ApplicationManager.getApplication().invokeLater(() -> {
                    if (isDisposed() || project.isDisposed()) return;
                    packs.forEach(runtimePack::addItem);
                    updateOkAction();
                });
            } catch (RuntimeException failure) {
                ApplicationManager.getApplication().invokeLater(() -> {
                    if (!isDisposed() && !project.isDisposed()) Messages.showErrorDialog(project,
                            StudioBundle.message("import.runtime.packsUnavailable"), StudioBundle.message("dialog.ImportModelJson"));
                });
            }
        });
        runtimePack.addActionListener(event -> updateOkAction());
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
        setOKActionEnabled(text != null && !text.isBlank()
                && (!runtimeMode.isSelected() || runtimePack.getSelectedItem() != null));
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, JBUI.scale(8)));

        JBLabel explanation = new JBLabel(StudioBundle.message("message.ImportModelJsonExplanation"));
        explanation.setBorder(JBUI.Borders.emptyBottom(4));
        JPanel options = new JPanel(new GridLayout(0, 1, 0, JBUI.scale(4)));
        options.add(explanation);
        options.add(runtimeMode);
        JPanel runtimeOptions = new JPanel(new GridLayout(0, 2, JBUI.scale(8), JBUI.scale(4)));
        JBLabel packLabel = new JBLabel(StudioBundle.message("import.runtime.pack"));
        packLabel.setLabelFor(runtimePack);
        runtimeOptions.add(packLabel);
        runtimeOptions.add(runtimePack);
        JBLabel packageLabel = new JBLabel(StudioBundle.message("import.runtime.package"));
        packageLabel.setLabelFor(applicationPackage);
        runtimeOptions.add(packageLabel);
        runtimeOptions.add(applicationPackage);
        runtimeOptions.setVisible(false);
        options.add(runtimeOptions);
        panel.add(options, BorderLayout.NORTH);

        jsonArea.setRows(16);
        jsonArea.setLineWrap(false);
        jsonArea.setFont(JBFont.create(EditorColorsManager.getInstance().getGlobalScheme().getFont(EditorFontType.PLAIN), false));
        JTabbedPane documents = new com.intellij.ui.components.JBTabbedPane();
        documents.addTab(StudioBundle.message("import.runtime.moduleTab"), new JBScrollPane(jsonArea));
        configurationArea.setRows(16);
        configurationArea.setFont(jsonArea.getFont());
        JPanel configurationPanel = new JPanel(new BorderLayout(0, JBUI.scale(4)));
        configurationPanel.add(new JBScrollPane(configurationArea), BorderLayout.CENTER);
        JButton chooseConfiguration = new JButton(StudioBundle.message("import.runtime.chooseConfiguration"));
        chooseConfiguration.addActionListener(event -> chooseConfiguration());
        configurationPanel.add(chooseConfiguration, BorderLayout.SOUTH);
        documents.addTab(StudioBundle.message("import.runtime.configurationTab"), configurationPanel);
        documents.setEnabledAt(1, false);
        runtimeMode.addActionListener(event -> {
            runtimeOptions.setVisible(runtimeMode.isSelected());
            documents.setSelectedIndex(0);
            documents.setEnabledAt(1, runtimeMode.isSelected());
            updateOkAction();
            panel.revalidate();
            panel.repaint();
        });
        panel.add(documents, BorderLayout.CENTER);

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

    private void chooseConfiguration() {
        StudioProjectFiles.chooseFileAndReadText(project,
                FileChooserDescriptorFactory.createSingleFileDescriptor()
                        .withTitle(StudioBundle.message("import.runtime.chooseConfiguration")), result -> {
                    if (isDisposed() || project.isDisposed()) return;
                    if (result.content() != null) configurationArea.setText(result.content());
                    else Messages.showErrorDialog(project,
                            StudioBundle.message("message.ImportModelFileReadFailed", result.errorMessage()),
                            StudioBundle.message("dialog.ImportModelJson"));
                });
    }

    private IkasanRuntimeImport.Result convertRuntime(String json) throws StudioBuildException {
        String config = configurationArea.getText();
        String pack = (String) runtimePack.getSelectedItem();
        String packageName = applicationPackage.getText().trim();
        AtomicReference<java.util.List<IkasanRuntimeImport.MappingChoice>> choices = new AtomicReference<>();
        AtomicReference<StudioBuildException> inspectionFailure = new AtomicReference<>();
        boolean inspected = ProgressManager.getInstance().runProcessWithProgressSynchronously(() -> {
            try { choices.set(IkasanRuntimeImport.mappingChoices(json, pack)); }
            catch (StudioBuildException problem) { inspectionFailure.set(problem); }
        }, StudioBundle.message("import.runtime.reviewTitle"), true, project);
        if (!inspected || project.isDisposed()) throw new com.intellij.openapi.progress.ProcessCanceledException();
        if (inspectionFailure.get() != null) throw inspectionFailure.get();
        var mappings = chooseMappings(choices.get());
        AtomicReference<IkasanRuntimeImport.Result> converted = new AtomicReference<>();
        AtomicReference<StudioBuildException> failure = new AtomicReference<>();
        boolean completed = ProgressManager.getInstance().runProcessWithProgressSynchronously(() -> {
            try { converted.set(IkasanRuntimeImport.convert(json, config, pack, packageName, mappings)); }
            catch (StudioBuildException problem) { failure.set(problem); }
        }, StudioBundle.message("import.runtime.reviewTitle"), true, project);
        if (!completed || project.isDisposed()) throw new com.intellij.openapi.progress.ProcessCanceledException();
        if (failure.get() != null) throw failure.get();
        return converted.get();
    }

    private java.util.Map<IkasanRuntimeImport.ComponentRef, String> chooseMappings(
            java.util.List<IkasanRuntimeImport.MappingChoice> choices) {
        java.util.Map<IkasanRuntimeImport.ComponentRef, String> result = new java.util.LinkedHashMap<>();
        if (choices.isEmpty()) return result;
        java.util.Map<IkasanRuntimeImport.ComponentRef, ComboBox<String>> selectors = new java.util.LinkedHashMap<>();
        DialogWrapper dialog = new DialogWrapper(project, true) {
            { setTitle(StudioBundle.message("import.runtime.mappingTitle")); init(); }
            @Override protected JComponent createCenterPanel() {
                JPanel panel = new JPanel(new BorderLayout(0, JBUI.scale(8)));
                panel.add(new JBLabel(StudioBundle.message("import.runtime.mappingHelp")), BorderLayout.NORTH);
                JPanel rows = new JPanel(new GridLayout(0, 2, JBUI.scale(8), JBUI.scale(4)));
                for (var choice : choices) {
                    ComboBox<String> selector = new ComboBox<>(choice.variants().toArray(String[]::new));
                    selector.setSelectedItem(null);
                    selectors.put(choice.component(), selector);
                    JBLabel label = new JBLabel(choice.component().flow() + " → " + choice.component().component());
                    label.setLabelFor(selector);
                    rows.add(label);
                    rows.add(selector);
                }
                panel.add(new JBScrollPane(rows), BorderLayout.CENTER);
                return panel;
            }
            @Override protected com.intellij.openapi.ui.ValidationInfo doValidate() {
                for (var selector : selectors.values()) if (selector.getSelectedItem() == null)
                    return new com.intellij.openapi.ui.ValidationInfo(StudioBundle.message("import.runtime.mappingRequired"), selector);
                return null;
            }
        };
        if (!dialog.showAndGet()) throw new com.intellij.openapi.progress.ProcessCanceledException();
        selectors.forEach((ref, selector) -> result.put(ref, (String) selector.getSelectedItem()));
        return result;
    }

    private boolean reviewRuntimeImport(IkasanRuntimeImport.Result result) {
        DialogWrapper review = new DialogWrapper(project, true) {
            { setTitle(StudioBundle.message("import.runtime.reviewTitle"));
              setOKButtonText(StudioBundle.message("import.runtime.accept")); init(); }
            @Override protected JComponent createCenterPanel() {
                JBTextArea text = new JBTextArea(String.join("\n\n", result.review()), 20, 85);
                text.setEditable(false);
                text.setLineWrap(true);
                text.setWrapStyleWord(true);
                text.setCaretPosition(0);
                return new JBScrollPane(text);
            }
        };
        return review.showAndGet();
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
            if (runtimeMode.isSelected()) {
                IkasanRuntimeImport.Result converted = convertRuntime(json);
                if (!reviewRuntimeImport(converted)) return;
                json = converted.studioJson();
            }
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
