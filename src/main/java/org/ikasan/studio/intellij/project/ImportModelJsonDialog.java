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
public class ImportModelJsonDialog extends ImportDialogWrapper {
    private final Project project;
    private org.ikasan.studio.core.diagnostics.RuntimeImportReport activeReport;
    private final JBTextArea jsonArea = new JBTextArea();
    private final JBLabel chosenFilePathLabel = new JBLabel();

    private final JBCheckBox runtimeMode = new JBCheckBox(StudioBundle.message("import.runtime.enabled"));
    private final ComboBox<String> runtimePack = new ComboBox<>();
    private final JBTextField applicationPackage = new JBTextField();
    private final JBTextArea configurationArea = new JBTextArea();
    private final JBCheckBox importSource = new JBCheckBox(StudioBundle.message("sourceImport.afterModel"));
    private final JBCheckBox importProperties = new JBCheckBox(StudioBundle.message("environment.afterImport"));
    private enum Acquisition { NONE, MANUAL, FETCHED }
    private Acquisition acquisition = Acquisition.NONE;
    private final JPanel acquisitionRow = new JPanel(new FlowLayout(FlowLayout.LEFT, JBUI.scale(6), 0));
    private final JPanel runtimeOptions = new JPanel(new GridBagLayout());
    private final JPanel documentPanel = new JPanel(new BorderLayout(0, JBUI.scale(6)));
    private final JPanel optionalSteps = new JPanel();
    private final JPanel fileRow = new JPanel(new FlowLayout(FlowLayout.LEFT, JBUI.scale(6), 0));
    private final JTabbedPane documents = new com.intellij.ui.components.JBTabbedPane();
    private final JButton pasteMetadata = new JButton(StudioBundle.message("import.journey.pasteMetadata"));
    private final JButton pasteConfiguration = new JButton(StudioBundle.message("import.journey.pasteConfiguration"));
    private final JButton chooseFileButton = new JButton();
    private final JButton chooseConfigurationButton = new JButton();
    private final JBLabel studioInputOr = new JBLabel(StudioBundle.message("import.journey.or"));
    private final JBLabel studioPastePrompt = new JBLabel(StudioBundle.message("import.journey.studioPaste"));
    private final JButton diagnostics = new JButton(StudioBundle.message("import.diagnostics.title"));
    private final JBTextArea journeyHelp = importHelp("import.journey.studioHelp");
    private final JBTextArea sourceHelp = importHelp("import.journey.chooseSource");
    private final JBTextArea runtimeDefaultsHelp = importHelp("import.runtime.defaultsHelp");
    private final JBTextArea runtimePlaceholder = importHelp("import.journey.chooseSource");


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
        setTitle(StudioBundle.message("import.journey.title"));
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
                    selectFetchedRuntimeVersion();
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

    /** Runtime acquisition is stage one; source review starts only after successful generation. */
    public void enableSourceWizard() {
        setTitle(StudioBundle.message("sourceImport.wizardTitle"));
        if (!runtimeMode.isSelected()) runtimeMode.doClick();
        importSource.setSelected(true);
    }

    private void updateOkAction() {
        boolean runtime = runtimeMode.isSelected();
        boolean hasMetadata = !jsonArea.getText().isBlank();
        boolean hasSource = !runtime || acquisition != Acquisition.NONE;
        acquisitionRow.setVisible(runtime);
        runtimeOptions.setVisible(runtime && hasSource);
        runtimeDefaultsHelp.setVisible(runtime && hasSource && hasMetadata);
        documentPanel.setVisible(hasSource);
        runtimePlaceholder.setVisible(runtime && !hasSource);
        optionalSteps.setVisible(hasSource && hasMetadata);
        diagnostics.setVisible(runtime);
        fileRow.setVisible(!runtime || acquisition == Acquisition.MANUAL);
        studioInputOr.setVisible(!runtime);
        studioPastePrompt.setVisible(!runtime);
        chosenFilePathLabel.setVisible(fileRow.isVisible());
        jsonArea.getAccessibleContext().setAccessibleName(StudioBundle.message(runtime
                ? "import.runtime.moduleTab" : "import.journey.studioPaste"));
        jsonArea.setEditable(!runtime || acquisition == Acquisition.MANUAL);
        configurationArea.setEditable(acquisition == Acquisition.MANUAL);
        chooseConfigurationButton.setText(StudioBundle.message(acquisition == Acquisition.FETCHED
                ? "import.runtime.overrideConfiguration" : "import.runtime.chooseConfiguration"));
        pasteConfiguration.setEnabled(runtime && hasSource && hasMetadata);
        if (documents.getTabCount() > 1) {
            documents.setEnabledAt(1, hasMetadata && hasSource);
            if (!hasMetadata && documents.getSelectedIndex() == 1) documents.setSelectedIndex(0);
        }
        chooseFileButton.setText(StudioBundle.message(runtime ? "import.journey.chooseMetadata" : "import.journey.chooseStudio"));
        journeyHelp.setText(StudioBundle.message(runtime ? "import.journey.runtimeHelp" : "import.journey.studioHelp"));
        sourceHelp.setVisible(runtime);
        sourceHelp.setText(StudioBundle.message(acquisition == Acquisition.FETCHED
                ? "import.journey.fetchedHelp" : "import.journey.manualHelp"));
        setOKButtonText(StudioBundle.message(runtime ? "import.journey.reviewImport" : "import.journey.importStudio"));
        setOKActionEnabled(hasSource && hasMetadata && (!runtime || runtimePack.getSelectedItem() != null));
        if (getContentPane() != null) { getContentPane().revalidate(); getContentPane().repaint(); }
    }

    private void supplyJson(int tab) {
        acquisition = Acquisition.MANUAL;
        updateOkAction();
        documents.setSelectedIndex(tab);
        (tab == 0 ? jsonArea : configurationArea).requestFocusInWindow();
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, JBUI.scale(8)));
        JPanel options = new JPanel();
        options.setLayout(new BoxLayout(options, BoxLayout.Y_AXIS));
        options.add(journeyHelp);
        options.add(Box.createVerticalStrut(JBUI.scale(8)));
        runtimeMode.setAlignmentX(Component.LEFT_ALIGNMENT);
        runtimeMode.setToolTipText(StudioBundle.message("import.runtime.modeHelp"));
        options.add(runtimeMode);
        options.add(Box.createVerticalStrut(JBUI.scale(6)));

        JButton fetch = new JButton(StudioBundle.message("import.remote.title"));
        fetch.addActionListener(event -> {
            var connection = new RuntimeImportConnectionDialog(project, getContentPane());
            if (connection.showAndGet() && !isDisposed() && !project.isDisposed()) applyFetchedDocuments(connection.documents());
        });
        acquisitionRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        acquisitionRow.add(fetch);
        acquisitionRow.add(new JBLabel(StudioBundle.message("import.journey.or")));
        pasteMetadata.addActionListener(event -> supplyJson(0));
        acquisitionRow.add(pasteMetadata);
        pasteConfiguration.addActionListener(event -> supplyJson(1));
        pasteConfiguration.setToolTipText(StudioBundle.message("import.journey.configurationPrerequisite"));
        acquisitionRow.add(pasteConfiguration);
        options.add(acquisitionRow);
        runtimePlaceholder.setAlignmentX(Component.LEFT_ALIGNMENT);
        options.add(runtimePlaceholder);
        options.add(Box.createVerticalStrut(JBUI.scale(6)));

        runtimeOptions.setAlignmentX(Component.LEFT_ALIGNMENT);
        GridBagConstraints row = new GridBagConstraints();
        row.anchor = GridBagConstraints.WEST;
        row.insets = JBUI.insets(2, 0, 2, 8);
        JBLabel packLabel = new JBLabel(StudioBundle.message("import.runtime.pack"));
        packLabel.setLabelFor(runtimePack);
        row.gridx = 0; row.gridy = 0;
        runtimeOptions.add(packLabel, row);
        row.gridx = 1; row.weightx = 1; row.fill = GridBagConstraints.HORIZONTAL;
        runtimeOptions.add(runtimePack, row);
        JBLabel packageLabel = new JBLabel(StudioBundle.message("import.runtime.package"));
        packageLabel.setLabelFor(applicationPackage);
        row.gridx = 0; row.gridy = 1; row.weightx = 0; row.fill = GridBagConstraints.NONE;
        runtimeOptions.add(packageLabel, row);
        row.gridx = 1; row.weightx = 1; row.fill = GridBagConstraints.HORIZONTAL;
        runtimeOptions.add(applicationPackage, row);
        options.add(runtimeOptions);
        options.add(runtimeDefaultsHelp);
        panel.add(options, BorderLayout.NORTH);

        jsonArea.setRows(12); jsonArea.setColumns(75); jsonArea.setLineWrap(false);
        jsonArea.setFont(JBFont.create(EditorColorsManager.getInstance().getGlobalScheme().getFont(EditorFontType.PLAIN), false));
        jsonArea.getAccessibleContext().setAccessibleName(StudioBundle.message("import.runtime.moduleTab"));
        configurationArea.setRows(12); configurationArea.setColumns(75); configurationArea.setLineWrap(false);
        configurationArea.setFont(jsonArea.getFont());
        configurationArea.getAccessibleContext().setAccessibleName(StudioBundle.message("import.runtime.configurationTab"));
        JBScrollPane moduleEditor = documentScrollPane(jsonArea);
        JPanel configurationPanel = new JPanel(new BorderLayout(0, JBUI.scale(4)));
        configurationPanel.add(importHelp("import.runtime.configurationHelp"), BorderLayout.NORTH);
        configurationPanel.add(documentScrollPane(configurationArea), BorderLayout.CENTER);
        chooseConfigurationButton.addActionListener(event -> chooseConfiguration());
        configurationPanel.add(chooseConfigurationButton, BorderLayout.SOUTH);
        runtimeMode.addActionListener(event -> {
            documentPanel.remove(runtimeMode.isSelected() ? moduleEditor : documents);
            documents.removeAll();
            if (runtimeMode.isSelected()) {
                documents.addTab(StudioBundle.message("import.runtime.moduleTab"), moduleEditor);
                documents.addTab(StudioBundle.message("import.runtime.configurationTab"), configurationPanel);
                documentPanel.add(documents, BorderLayout.CENTER);
            } else {
                documentPanel.add(moduleEditor, BorderLayout.CENTER);
            }
            updateOkAction();
        });
        JPanel documentHeader = new JPanel(new BorderLayout(0, JBUI.scale(6)));
        documentHeader.add(sourceHelp, BorderLayout.NORTH);
        documentHeader.add(fileRow, BorderLayout.SOUTH);
        documentPanel.add(documentHeader, BorderLayout.NORTH);
        documentPanel.add(moduleEditor, BorderLayout.CENTER);
        chooseFileButton.addActionListener(event -> chooseFile());
        fileRow.add(chooseFileButton);
        fileRow.add(studioInputOr);
        studioPastePrompt.setLabelFor(jsonArea);
        fileRow.add(studioPastePrompt);
        chosenFilePathLabel.setForeground(com.intellij.util.ui.UIUtil.getLabelForeground());
        documentPanel.add(chosenFilePathLabel, BorderLayout.SOUTH);
        // Fetched JSON is inspectable; editing or choosing configuration explicitly switches to manual input.
        panel.add(documentPanel, BorderLayout.CENTER);

        JPanel bottom = new JPanel(new BorderLayout(0, JBUI.scale(6)));
        optionalSteps.setLayout(new BoxLayout(optionalSteps, BoxLayout.Y_AXIS));
        optionalSteps.add(new com.intellij.ui.TitledSeparator(StudioBundle.message("import.journey.followUp")));
        optionalSteps.add(importHelp("import.journey.followUpHelp"));
        importSource.setAlignmentX(Component.LEFT_ALIGNMENT); optionalSteps.add(importSource);
        importProperties.setAlignmentX(Component.LEFT_ALIGNMENT); optionalSteps.add(importProperties);
        bottom.add(optionalSteps, BorderLayout.CENTER);
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        diagnostics.addActionListener(event -> new RuntimeImportReportDialog(project, getContentPane()).show());
        footer.add(diagnostics); bottom.add(footer, BorderLayout.SOUTH);
        panel.add(bottom, BorderLayout.SOUTH);
        panel.setPreferredSize(JBUI.size(920, 680));
        updateOkAction();
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
        return runtimeMode.isSelected() && acquisition == Acquisition.NONE ? pasteMetadata : jsonArea;
    }

    /** Acquisition changes only the editable preview; Import still performs mapping and explicit review. */
    void applyFetchedDocuments(org.ikasan.studio.integration.ikasan.RuntimeImportClient.Documents documents) {
        if (documents == null || isDisposed() || project.isDisposed()) return;
        if (!runtimeMode.isSelected()) runtimeMode.doClick();
        acquisition = Acquisition.FETCHED;
        jsonArea.setText(documents.moduleJson());
        jsonArea.setCaretPosition(0);
        configurationArea.setText(documents.configurationJson());
        configurationArea.setCaretPosition(0);
        this.documents.setSelectedIndex(0);
        chosenFilePathLabel.setText(StudioBundle.message("import.remote.source"));
        chosenFilePathLabel.setToolTipText(null);
        selectFetchedRuntimeVersion();
        updateOkAction();
    }

    /** Resolve pack metadata off the EDT, including when discovery finishes after the fetch. */
    private void selectFetchedRuntimeVersion() {
        if (acquisition != Acquisition.FETCHED || runtimePack.getItemCount() == 0) return;
        String json = jsonArea.getText();
        Object selected = runtimePack.getSelectedItem();
        var available = new java.util.ArrayList<String>();
        for (int i = 0; i < runtimePack.getItemCount(); i++) available.add(runtimePack.getItemAt(i));
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                var recommendation = IkasanRuntimeImport.recommendPack(json, available);
                // Patch substitutions still require the existing explicit review confirmation.
                if (recommendation.isEmpty() || !recommendation.get().exact()) return;
                ApplicationManager.getApplication().invokeLater(() -> {
                    if (isDisposed() || project.isDisposed() || acquisition != Acquisition.FETCHED
                            || !json.equals(jsonArea.getText())
                            || !java.util.Objects.equals(selected, runtimePack.getSelectedItem())) return;
                    runtimePack.setSelectedItem(recommendation.get().pack());
                }, ModalityState.any());
            } catch (StudioBuildException ignored) {
                // Invalid or unsupported metadata receives actionable diagnostics during review.
            }
        });
    }

    private void chooseFile() {
        FileChooserDescriptor descriptor = new FileChooserDescriptor(true, true, true, true, false, false)
                .withTitle(StudioBundle.message(runtimeMode.isSelected() ? "import.journey.chooseMetadata" : "dialog.ChooseModelJsonFile"))
                .withDescription(StudioBundle.message(runtimeMode.isSelected() ? "import.journey.manualHelp" : "message.ChooseModelJsonFileDescription"));
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
        if (runtimeMode.isSelected()) supplyJson(1);
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
                var chosen = IkasanRuntimeImport.recommendPack(json, selected == null ? java.util.List.of() : java.util.List.of(selected));
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
                JBTextArea text = new JBTextArea(RuntimeImportReviewText.format(result), 20, 85);
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

    private Runnable sourceReviewStep(boolean propertiesAfterSource) {
        return () -> new org.ikasan.studio.intellij.importer.ImportSourceModuleDialog(project, propertiesAfterSource).show();
    }

    @Override
    protected void doOKAction() {
        if (runtimeMode.isSelected() && acquisition == Acquisition.NONE) {
            setErrorText(StudioBundle.message("import.journey.chooseSource"));
            return;
        }
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
                    ? StudioBundle.message("label.PastedModelJson") : chosenFilePathLabel.getText(), activeReport,
                    importSource.isSelected() ? sourceReviewStep(importProperties.isSelected())
                            : importProperties.isSelected() ? () -> new org.ikasan.studio.intellij.configuration.ImportEnvironmentPropertiesDialog(project).show() : null);
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
