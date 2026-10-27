package org.ikasan.studio.intellij.project;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
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
// Local-IDE dialog; remote Split Mode requires a frontend UI and RPC service boundary.
@SuppressWarnings("SplitModeApiUsage")
public class ImportModelJsonDialog extends ImportDialogWrapper {
    private final Project project;
    private org.ikasan.studio.core.diagnostics.RuntimeImportReport activeReport;
    private final JBTextArea jsonArea = new JBTextArea();
    private final JBLabel chosenFilePathLabel = new JBLabel();

    private final JBCheckBox runtimeMode = new JBCheckBox(StudioBundle.message("import.runtime.enabled"));
    private final ComboBox<String> runtimePack = new ComboBox<>();
    private final JBTextField applicationPackage = new JBTextField();
    private final JBTextArea configurationArea = new JBTextArea();

    public ImportModelJsonDialog(Project project) {
        this(project, (Component) null);
    }

    public ImportModelJsonDialog(Project project, Component parent) {
        this(project, parent, () -> ComponentLibrary.getMetapackList(
                org.ikasan.studio.intellij.settings.IkasanStudioSettings.areSnapshotMetaPacksAllowed()));
    }

    ImportModelJsonDialog(Project project, java.util.function.Supplier<java.util.List<String>> loadPacks) {
        this(project, null, loadPacks);
    }

    private ImportModelJsonDialog(Project project, Component parent, java.util.function.Supplier<java.util.List<String>> loadPacks) {
        super(project, parent);
        this.project = project;
        setTitle(StudioBundle.message("dialog.ImportModelJson"));
        setOKButtonText(StudioBundle.message("button.ImportModelJson"));
        applicationPackage.setText(project.getService(UiContext.class).getOptions().getPackageName());
        init();
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                var packs = loadPacks.get();
                // Pure UI publication must run inside the modal import dialog, without PSI/model writes.
                ApplicationManager.getApplication().invokeLater(() -> {
                    if (isDisposed() || project.isDisposed()) return;
                    packs.forEach(runtimePack::addItem);
                    updateOkAction();
                }, ModalityState.any());
            } catch (RuntimeException failure) {
                ApplicationManager.getApplication().invokeLater(() -> {
                    if (!isDisposed() && !project.isDisposed()) Messages.showErrorDialog(messageParent(),
                            StudioBundle.message("import.runtime.packsUnavailable"), StudioBundle.message("dialog.ImportModelJson"));
                }, ModalityState.any());
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

        JPanel options = new JPanel();
        options.setLayout(new BoxLayout(options, BoxLayout.Y_AXIS));
        options.add(importHelp("message.ImportModelJsonExplanation"));
        options.add(Box.createVerticalStrut(JBUI.scale(8)));
        runtimeMode.setAlignmentX(Component.LEFT_ALIGNMENT);
        runtimeMode.setToolTipText(StudioBundle.message("import.runtime.modeHelp"));
        options.add(runtimeMode);
        options.add(Box.createVerticalStrut(JBUI.scale(4)));
        JPanel runtimeOptions = new JPanel(new GridBagLayout());
        runtimeOptions.setAlignmentX(Component.LEFT_ALIGNMENT);
        GridBagConstraints row = new GridBagConstraints();
        row.anchor = GridBagConstraints.WEST;
        row.insets = JBUI.insets(2, 0, 2, 8);
        JBLabel packLabel = new JBLabel(StudioBundle.message("import.runtime.pack"));
        packLabel.setLabelFor(runtimePack);
        row.gridx = 0;
        row.gridy = 0;
        runtimeOptions.add(packLabel, row);
        row.gridx = 1;
        row.weightx = 1;
        row.fill = GridBagConstraints.HORIZONTAL;
        runtimeOptions.add(runtimePack, row);
        JBLabel packageLabel = new JBLabel(StudioBundle.message("import.runtime.package"));
        packageLabel.setLabelFor(applicationPackage);
        row.gridx = 0;
        row.gridy = 1;
        row.weightx = 0;
        row.fill = GridBagConstraints.NONE;
        runtimeOptions.add(packageLabel, row);
        row.gridx = 1;
        row.weightx = 1;
        row.fill = GridBagConstraints.HORIZONTAL;
        runtimeOptions.add(applicationPackage, row);
        runtimeOptions.setVisible(false);
        options.add(runtimeOptions);
        JBTextArea runtimeDefaultsHelp = importHelp("import.runtime.defaultsHelp");
        runtimeDefaultsHelp.setVisible(false);
        options.add(runtimeDefaultsHelp);
        panel.add(options, BorderLayout.NORTH);

        jsonArea.setRows(16);
        jsonArea.setColumns(80);
        jsonArea.setLineWrap(false);
        jsonArea.setFont(JBFont.create(EditorColorsManager.getInstance().getGlobalScheme().getFont(EditorFontType.PLAIN), false));
        JTabbedPane documents = new com.intellij.ui.components.JBTabbedPane();
        documents.addTab(StudioBundle.message("import.runtime.studioTab"), documentScrollPane(jsonArea));
        configurationArea.setRows(16);
        configurationArea.setColumns(80);
        configurationArea.setFont(jsonArea.getFont());
        JPanel configurationPanel = new JPanel(new BorderLayout(0, JBUI.scale(4)));
        configurationPanel.add(importHelp("import.runtime.configurationHelp"), BorderLayout.NORTH);
        configurationPanel.add(documentScrollPane(configurationArea), BorderLayout.CENTER);
        JButton chooseConfiguration = new JButton(StudioBundle.message("import.runtime.chooseConfiguration"));
        chooseConfiguration.addActionListener(event -> chooseConfiguration());
        configurationPanel.add(chooseConfiguration, BorderLayout.SOUTH);
        runtimeMode.addActionListener(event -> {
            runtimeOptions.setVisible(runtimeMode.isSelected());
            runtimeDefaultsHelp.setVisible(runtimeMode.isSelected());
            documents.setSelectedIndex(0);
            documents.setTitleAt(0, StudioBundle.message(runtimeMode.isSelected()
                    ? "import.runtime.moduleTab" : "import.runtime.studioTab"));
            if (runtimeMode.isSelected()) {
                documents.addTab(StudioBundle.message("import.runtime.configurationTab"), configurationPanel);
            } else if (documents.getTabCount() > 1) {
                documents.removeTabAt(1);
            }
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
        JButton fetch = new JButton(StudioBundle.message("import.remote.title"));
        fetch.addActionListener(event -> {
            var connection = new RuntimeImportConnectionDialog(project, getContentPane());
            if (connection.showAndGet() && !isDisposed() && !project.isDisposed()) {
                applyFetchedDocuments(connection.documents());
            }
        });
        chooseRow.add(fetch);
        JButton diagnostics = new JButton(StudioBundle.message("import.diagnostics.title"));
        diagnostics.addActionListener(event -> new RuntimeImportReportDialog(project, getContentPane()).show());
        chooseRow.add(Box.createHorizontalStrut(JBUI.scale(8)));
        chooseRow.add(diagnostics);
        chooseRow.add(Box.createHorizontalStrut(JBUI.scale(8)));
        chosenFilePathLabel.setForeground(com.intellij.util.ui.UIUtil.getLabelForeground());
        chooseRow.add(chosenFilePathLabel);
        panel.add(chooseRow, BorderLayout.SOUTH);

        panel.setPreferredSize(JBUI.size(780, 540));
        return panel;
    }

    private Component messageParent() {
        // Headless DialogWrapper peers have no content pane; the editor is still a valid message parent.
        return getContentPane() == null ? jsonArea : getContentPane();
    }

    private static JBScrollPane documentScrollPane(JBTextArea text) {
        JBScrollPane scroll = new JBScrollPane(text);
        scroll.setPreferredSize(JBUI.size(720, 300));
        scroll.setMinimumSize(new Dimension(0, 0));
        return scroll;
    }

    private static JBTextArea importHelp(String key) {
        JBTextArea help = new JBTextArea(StudioBundle.message(key));
        help.setColumns(70);
        help.setEditable(false);
        help.setFocusable(false);
        help.setLineWrap(true);
        help.setWrapStyleWord(true);
        help.setOpaque(false);
        help.setFont(com.intellij.util.ui.UIUtil.getLabelFont());
        help.setForeground(com.intellij.util.ui.UIUtil.getLabelForeground());
        help.setAlignmentX(Component.LEFT_ALIGNMENT);
        return help;
    }

    @Override
    public @Nullable JComponent getPreferredFocusedComponent() {
        return jsonArea;
    }

    /** Acquisition changes only the editable preview; Import still performs mapping and explicit review. */
    void applyFetchedDocuments(org.ikasan.studio.integration.ikasan.RuntimeImportClient.Documents documents) {
        if (documents == null || isDisposed() || project.isDisposed()) return;
        if (!runtimeMode.isSelected()) runtimeMode.doClick();
        jsonArea.setText(documents.moduleJson());
        jsonArea.setCaretPosition(0);
        configurationArea.setText(documents.configurationJson());
        configurationArea.setCaretPosition(0);
        chosenFilePathLabel.setText(StudioBundle.message("import.remote.source"));
        chosenFilePathLabel.setToolTipText(null);
        updateOkAction();
    }

    private void chooseFile() {
        FileChooserDescriptor descriptor = new FileChooserDescriptor(true, true, true, true, false, false)
                .withTitle(StudioBundle.message("dialog.ChooseModelJsonFile"))
                .withDescription(StudioBundle.message("message.ChooseModelJsonFileDescription"));
        StudioProjectFiles.chooseFileAndReadText(project, descriptor, result -> {
            if (isDisposed() || project.isDisposed()) return;
            if (result.content() != null) {
                jsonArea.setText(result.content());
                jsonArea.setCaretPosition(0);
                chosenFilePathLabel.setText(result.path());
                chosenFilePathLabel.setToolTipText(result.path());
                jsonArea.requestFocusInWindow();
            } else {
                chosenFilePathLabel.setText("");
                Messages.showErrorDialog(messageParent(),
                        StudioBundle.message("message.ImportModelFileReadFailed", result.errorMessage()),
                        StudioBundle.message("dialog.ImportModelJson"));
            }
        });
    }

    private void chooseConfiguration() {
        StudioProjectFiles.chooseFileAndReadText(project,
                new FileChooserDescriptor(true, true, true, true, false, false)
                        .withTitle(StudioBundle.message("import.runtime.chooseConfiguration")), result -> {
                    if (isDisposed() || project.isDisposed()) return;
                    if (result.content() != null) {
                        configurationArea.setText(result.content());
                        configurationArea.setCaretPosition(0);
                    }
                    else Messages.showErrorDialog(messageParent(),
                            StudioBundle.message("message.ImportModelFileReadFailed", result.errorMessage()),
                            StudioBundle.message("dialog.ImportModelJson"));
                });
    }

    String selectRuntimePack(String json) throws StudioBuildException {
        String selected = (String) runtimePack.getSelectedItem();
        var available = new java.util.ArrayList<String>();
        for (int i = 0; i < runtimePack.getItemCount(); i++) available.add(runtimePack.getItemAt(i));
        String configurationAreaText = configurationArea.getText();
        var recommendation = new AtomicReference<IkasanRuntimeImport.PackRecommendation>();
        var failure = new AtomicReference<StudioBuildException>();
        boolean completed = ProgressManager.getInstance().runProcessWithProgressSynchronously(() -> {
            try {
                if (activeReport != null) {
                    activeReport.inspect(json, configurationAreaText);
                    activeReport.pack(selected);
                    activeReport.stage(org.ikasan.studio.core.diagnostics.RuntimeImportReport.Stage.MATCHING);
                }
                // Honor a suitable explicit selection, otherwise suggest the closest available pack.
                var chosen = IkasanRuntimeImport.recommendPack(json, java.util.List.of(selected));
                recommendation.set(chosen.isPresent() ? chosen.get()
                        : IkasanRuntimeImport.recommendPack(json, available).orElse(null));
            } catch (StudioBuildException problem) { failure.set(problem); }
        }, StudioBundle.message("import.runtime.reviewTitle"), true, project);
        if (!completed || project.isDisposed()) throw new com.intellij.openapi.progress.ProcessCanceledException();
        if (failure.get() != null) throw failure.get();
        var choice = recommendation.get();
        if (choice == null) return selected; // Conversion reports the unsupported version with actionable guidance.
        if (!choice.pack().equals(selected) || !choice.exact()) {
            int answer = Messages.showYesNoDialog(messageParent(),
                    StudioBundle.message(choice.exact() ? "import.runtime.versionExact" : "import.runtime.versionPatch",
                            choice.sourceVersion(), choice.pack(), choice.targetVersion()),
                    StudioBundle.message("import.runtime.versionTitle"), Messages.getQuestionIcon());
            if (answer != Messages.YES) throw new com.intellij.openapi.progress.ProcessCanceledException();
            runtimePack.setSelectedItem(choice.pack());
        }
        return choice.pack();
    }

    private IkasanRuntimeImport.Result convertRuntime(String json) throws StudioBuildException {
        String config = configurationArea.getText();
        String pack = selectRuntimePack(json);
        String packageName = applicationPackage.getText().trim();
        activeReport.pack(pack);
        AtomicReference<java.util.List<IkasanRuntimeImport.MappingChoice>> choices = new AtomicReference<>();
        AtomicReference<StudioBuildException> inspectionFailure = new AtomicReference<>();
        boolean inspected = ProgressManager.getInstance().runProcessWithProgressSynchronously(() -> {
            try {
                IkasanRuntimeImport.diagnose(json, config, pack, activeReport);
                choices.set(IkasanRuntimeImport.mappingChoices(json, config, pack));
            }
            catch (StudioBuildException problem) { inspectionFailure.set(problem); }
        }, StudioBundle.message("import.runtime.reviewTitle"), true, project);
        if (!inspected || project.isDisposed()) throw new com.intellij.openapi.progress.ProcessCanceledException();
        if (inspectionFailure.get() != null) throw inspectionFailure.get();
        var mappings = chooseMappings(choices.get());
        AtomicReference<IkasanRuntimeImport.Result> converted = new AtomicReference<>();
        AtomicReference<StudioBuildException> failure = new AtomicReference<>();
        boolean completed = ProgressManager.getInstance().runProcessWithProgressSynchronously(() -> {
            try {
                activeReport.stage(org.ikasan.studio.core.diagnostics.RuntimeImportReport.Stage.CONVERSION);
                converted.set(IkasanRuntimeImport.convert(json, config, pack, packageName, mappings));
            }
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
        DialogWrapper dialog = new ImportDialogWrapper(project, getContentPane()) {
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
                JBScrollPane scroll = new JBScrollPane(rows);
                scroll.setPreferredSize(JBUI.size(620, Math.min(320, Math.max(80, rows.getPreferredSize().height))));
                scroll.setMinimumSize(new Dimension(0, 0));
                panel.add(scroll, BorderLayout.CENTER);
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
        DialogWrapper review = new ImportDialogWrapper(project, getContentPane()) {
            { setTitle(StudioBundle.message("import.runtime.reviewTitle"));
              setOKButtonText(StudioBundle.message("import.runtime.accept")); init(); }
            @Override protected JComponent createCenterPanel() {
                JBTextArea text = new JBTextArea(String.join("\n\n", result.review()), 20, 85);
                text.setEditable(false);
                text.setLineWrap(true);
                text.setWrapStyleWord(true);
                text.setCaretPosition(0);
                JBScrollPane scroll = new JBScrollPane(text);
                scroll.setPreferredSize(JBUI.size(720, 360));
                scroll.setMinimumSize(new Dimension(0, 0));
                return scroll;
            }
        };
        return review.showAndGet();
    }

    @Override
    protected void doOKAction() {
        String json = jsonArea.getText();
        if (json == null || json.isBlank()) {
            Messages.showErrorDialog(messageParent(),
                    StudioBundle.message("message.ImportModelNoContent"),
                    StudioBundle.message("dialog.ImportModelJson"));
            return;
        }
        activeReport = org.ikasan.studio.intellij.diagnostics.RuntimeImportDiagnostics.get(project).start();
        activeReport.stage(org.ikasan.studio.core.diagnostics.RuntimeImportReport.Stage.PARSE);
        try {
            if (runtimeMode.isSelected()) {
                IkasanRuntimeImport.Result converted = convertRuntime(json);
                if (!reviewRuntimeImport(converted)) { activeReport.cancelled(); return; }
                json = converted.studioJson();
            }
            ModelImporter.importFromText(project, json, chosenFilePathLabel.getText().isBlank()
                    ? StudioBundle.message("label.PastedModelJson") : chosenFilePathLabel.getText(), activeReport);
        } catch (com.intellij.openapi.progress.ProcessCanceledException cancelled) {
            activeReport.cancelled();
            throw cancelled;
        } catch (StudioBuildException | RuntimeException e) {
            activeReport.failure(e);
            Messages.showErrorDialog(messageParent(),
                    StudioBundle.message("message.ImportModelFailed", e.getMessage()),
                    StudioBundle.message("dialog.ImportModelJson"));
            // Keep the dialog open so the developer can correct the JSON and retry.
            return;
        }
        close(OK_EXIT_CODE);
    }
}
