package org.ikasan.studio.intellij.importer;

import com.intellij.CommonBundle;
import com.intellij.diff.DiffContentFactory;
import com.intellij.diff.DiffManager;
import com.intellij.diff.contents.DocumentContent;
import com.intellij.diff.requests.SimpleDiffRequest;
import com.intellij.diff.util.DiffUserDataKeys;
import com.intellij.icons.AllIcons.General;
import com.intellij.ide.highlighter.JavaFileType;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.openapi.util.Disposer;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.*;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTextArea;
import com.intellij.ui.table.JBTable;
import com.intellij.util.concurrency.AppExecutorUtil;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.*;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import javax.swing.*;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.RowFilter;
import javax.swing.Timer;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import org.ikasan.studio.core.generator.ProjectArtifactRenderer;
import org.ikasan.studio.core.importer.SourceBuilderSettingsRecovery.Plan;
import org.ikasan.studio.core.importer.SourceBuilderSettingsRecovery;
import org.ikasan.studio.core.importer.SourceCheckoutIdentity.Candidate;
import org.ikasan.studio.core.importer.SourceCheckoutIdentity.Check;
import org.ikasan.studio.core.importer.SourceCheckoutIdentity.Inspection;
import org.ikasan.studio.core.importer.SourceCheckoutIdentity;
import org.ikasan.studio.core.importer.SourceModuleImport.Analysis;
import org.ikasan.studio.core.importer.SourceModuleImport.DependencyChoice;
import org.ikasan.studio.core.importer.SourceModuleImport.DependencyComparison;
import org.ikasan.studio.core.importer.SourceModuleImport.FileChoice;
import org.ikasan.studio.core.importer.SourceModuleImport.ManagedFile;
import org.ikasan.studio.core.importer.SourceModuleImport.PropertyFile;
import org.ikasan.studio.core.importer.SourceModuleImport.ReviewAction;
import org.ikasan.studio.core.importer.SourceModuleImport.SpringRegistration;
import org.ikasan.studio.core.importer.SourceModuleImport;
import org.ikasan.studio.core.io.ComponentIO;
import org.ikasan.studio.core.model.ikasan.instance.Module;
import org.ikasan.studio.intellij.configuration.ImportEnvironmentPropertiesDialog;
import org.ikasan.studio.intellij.project.ImportDialogWrapper;
import org.ikasan.studio.intellij.project.StudioProjectFiles;
import org.ikasan.studio.ui.StudioBundle;
import org.ikasan.studio.ui.StudioDialogWrapper;
import org.ikasan.studio.ui.StudioUIUtils;
import org.ikasan.studio.ui.UiContext;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** Second stage of the module wizard. Analysis is read-only; apply transfers only explicitly reviewed items. */
public final class ImportSourceModuleDialog extends ImportDialogWrapper {
    private final Project project;
    private final JBTextField source = new JBTextField();
    private final JBCheckBox properties = new JBCheckBox(StudioBundle.message("sourceImport.properties"), true);
    private final JBCheckBox acceptIdentity = new JBCheckBox(StudioBundle.message("sourceImport.identity.override"));
    private final JBTextArea identitySummary = new JBTextArea();
    private final JBTextArea fileDetails = new JBTextArea();
    private final JBTextArea supportDetails = new JBTextArea();
    private final JBScrollPane fileDetailsPane = new JBScrollPane(fileDetails);
    private final JBScrollPane supportDetailsPane = new JBScrollPane(supportDetails);
    private Map<String, List<SourceModuleImport.ReviewAction>> inlineActions = Map.of();
    private List<SourceModuleImport.ReviewAction> standaloneIssues = List.of();
    private final Choices choices = new Choices();
    private final JBTable table = new JBTable(choices);
    private final JButton copyAllNewButton = new JButton(StudioBundle.message("sourceImport.files.copyAllNew"));
    private final JBCheckBox showIdenticalFiles = new JBCheckBox(StudioBundle.message("sourceImport.files.showIdentical"));
    private final JTabbedPane reviewTabs = new JBTabbedPane();
    private final SupportChoices supportChoices = new SupportChoices();
    private final JBTable supportTable = new JBTable(supportChoices);
    private final JPanel supportPanel = new JPanel(new BorderLayout(0, JBUI.scale(6)));
    private final JBTextArea supportNotice = new JBTextArea();
    private final JBCheckBox showIdenticalSupport = new JBCheckBox(StudioBundle.message("sourceImport.support.showIdentical"));
    private SourceModuleImport.Analysis analysis;
    private boolean analysing;
    private String lastAttempt = "";
    private final Map<String, String> stagedJava = new LinkedHashMap<>();
    private final Map<String, DocumentContent> mergeDocuments = new HashMap<>();
    private final JBLabel stagedSummary = new JBLabel();
    private final JPanel registrationPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, JBUI.scale(8), 0));
    private final JButton registrationButton = new JButton(StudioBundle.message("sourceImport.registration.review"));
    private final JBLabel registrationSummary = new JBLabel();
    private SourceModuleImport.SpringRegistration registrationCandidate;
    private SourceModuleImport.SpringRegistration approvedRegistration;
    private final JPanel builderSettingsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, JBUI.scale(8), 0));
    private final JButton builderSettingsButton = new JButton(StudioBundle.message("sourceImport.builder.review"));
    private final JBLabel builderSettingsSummary = new JBLabel();
    private SourceBuilderSettingsRecovery.Plan builderCandidate, approvedBuilderSettings;
    private record ReviewState(List<SourceModuleImport.ReviewAction> actions, SourceModuleImport.SpringRegistration registration,
                               SourceBuilderSettingsRecovery.Plan builderSettings) { }

    private final Timer mergeRefresh = new Timer(120, event -> {
        if (!isDisposed() && analysis != null) { table.repaint(); refreshReview(); }
    });
    private Path analysedSource;
    private final AtomicLong reviewSequence = new AtomicLong();
    private final ExecutorService reviewWorker = AppExecutorUtil
            .createBoundedApplicationPoolExecutor("Ikasan source review", 1);
    private final Set<String> selectedFiles = new LinkedHashSet<>(), selectedDependencies = new LinkedHashSet<>();

    public ImportSourceModuleDialog(Project project) {
        this(project, true);
    }

    public ImportSourceModuleDialog(Project project, boolean importPropertiesAfterwards) {
        super(project, project.getService(UiContext.class).getDesignerCanvas());
        this.project = project;
        properties.setSelected(importPropertiesAfterwards);
        setTitle(StudioBundle.message("sourceImport.title"));
        setOKButtonText(StudioBundle.message("sourceImport.apply"));
        init();
        mergeRefresh.setRepeats(false);
        Disposer.register(getDisposable(), () -> StudioUIUtils.setAttentionPulse(registrationButton, false));
        Disposer.register(getDisposable(), mergeRefresh::stop);
        Disposer.register(getDisposable(), () -> StudioUIUtils.setAttentionPulse(builderSettingsButton, false));
        Disposer.register(getDisposable(), () -> { reviewSequence.incrementAndGet(); reviewWorker.shutdownNow(); });
        setOKActionEnabled(false);
    }

    @Override protected @NotNull JComponent createStudioContent() {
        JPanel panel = new JPanel(new BorderLayout(0, JBUI.scale(8)));
        JPanel top = new JPanel(new BorderLayout(0, JBUI.scale(6)));
        JBTextArea help = new JBTextArea(StudioBundle.message("sourceImport.help"));
        help.setEditable(false); help.setLineWrap(true); help.setWrapStyleWord(true); help.setOpaque(false); help.setFocusable(false);
        top.add(help, BorderLayout.NORTH);
        JPanel path = new JPanel(new BorderLayout(JBUI.scale(6), 0));
        JBLabel label = new JBLabel(StudioBundle.message("sourceImport.checkout")); label.setLabelFor(source);
        path.add(label, BorderLayout.WEST); path.add(source, BorderLayout.CENTER);
        JButton browse = new JButton(StudioBundle.message("sourceImport.browse"));
        browse.addActionListener(event -> StudioProjectFiles.chooseFilePaths(project,
                FileChooserDescriptorFactory.createSingleFolderDescriptor(), paths -> {
                    if (!isDisposed() && !project.isDisposed() && paths != null && !paths.isEmpty()) {
                        source.setText(paths.get(0));
                        SwingUtilities.invokeLater(this::analyse);
                    }
                }));
        path.add(browse, BorderLayout.EAST); top.add(path, BorderLayout.CENTER);
        source.setToolTipText(StudioBundle.message("sourceImport.pathHint"));
        source.addActionListener(event -> analyse());
        source.addFocusListener(new FocusAdapter() {
            @Override public void focusLost(FocusEvent event) {
                if (event.isTemporary() || event.getOppositeComponent() == browse) return;
                if (event.getOppositeComponent() instanceof AbstractButton button
                        && (button.getAction() == getCancelAction() || CommonBundle.getCancelButtonText().equals(button.getText()))) return;
                if (!source.getText().trim().equals(lastAttempt)) analyse();
            }
        });
        source.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            private void changed() { if (!analysing) { lastAttempt = ""; clearAnalysis(); } }
            @Override public void insertUpdate(javax.swing.event.DocumentEvent event) { changed(); }
            @Override public void removeUpdate(javax.swing.event.DocumentEvent event) { changed(); }
            @Override public void changedUpdate(javax.swing.event.DocumentEvent event) { changed(); }
        });
        JBTextArea comparisonHint = new JBTextArea(StudioBundle.message("sourceImport.comparisonHint"));
        comparisonHint.setEditable(false); comparisonHint.setLineWrap(true); comparisonHint.setWrapStyleWord(true);
        comparisonHint.setOpaque(false); comparisonHint.setFocusable(false);
        comparisonHint.setFont(UIUtil.getLabelFont());
        identitySummary.setEditable(false); identitySummary.setLineWrap(true); identitySummary.setWrapStyleWord(true);
        identitySummary.setOpaque(false); identitySummary.setFocusable(false);
        identitySummary.setFont(UIUtil.getLabelFont()); identitySummary.setVisible(false);
        acceptIdentity.setVisible(false);
        acceptIdentity.addActionListener(event -> {
            if (analysis != null && analysis.identity().requiresApproval()) {
                selectedFiles.clear(); selectedDependencies.clear();
                if (!acceptIdentity.isSelected()) discardMerges();
                if (acceptIdentity.isSelected()) selectDefaults();
                setOKActionEnabled(acceptIdentity.isSelected());
                choices.fireTableDataChanged(); refreshReview();
            }
        });
        JPanel steps = new JPanel(); steps.setLayout(new BoxLayout(steps, BoxLayout.Y_AXIS));
        comparisonHint.setAlignmentX(Component.LEFT_ALIGNMENT); identitySummary.setAlignmentX(Component.LEFT_ALIGNMENT);
        acceptIdentity.setAlignmentX(Component.LEFT_ALIGNMENT);
        steps.add(identitySummary); steps.add(acceptIdentity); steps.add(comparisonHint);
        top.add(steps, BorderLayout.SOUTH); panel.add(top, BorderLayout.NORTH);
        JTabbedPane tabs = reviewTabs;
        JBScrollPane files = new JBScrollPane(table); files.setMinimumSize(new Dimension(0, 0));
        table.setAutoCreateRowSorter(true);
        table.getEmptyText().setText(StudioBundle.message("sourceImport.files.allIdentical"));
        ((TableRowSorter<?>) table.getRowSorter()).setRowFilter(new RowFilter<TableModel, Integer>() {
            @Override public boolean include(Entry<? extends TableModel, ? extends Integer> entry) {
                if (analysis == null) return false;
                int row = entry.getIdentifier();
                if (row >= analysis.files().size()) return true;
                FileChoice file = analysis.files().get(row);
                return showIdenticalFiles.isSelected() || selectedFiles.contains(file.source())
                        || !file.identicalToDestination() || !rowActions(table, row).isEmpty();
            }
        });
        ((TableRowSorter<?>) table.getRowSorter()).setSortsOnUpdates(true);
        showIdenticalFiles.addActionListener(event -> {
            if (table.isEditing()) table.getCellEditor().stopCellEditing();
            refreshFileVisibility();
        });
        registerComparison(table);
        table.getColumnModel().getColumn(0).setMaxWidth(JBUI.scale(100));
        table.moveColumn(0, 1);
        table.setDefaultRenderer(Boolean.class, (view, value, selected, focused, row, column) -> {
            if (view == table && analysis != null && view.convertRowIndexToModel(row) >= analysis.files().size() + analysis.dependencies().size())
                return new JBLabel();
            JButton button = new JButton(StudioBundle.message(Boolean.TRUE.equals(value) ? "sourceImport.revert" : "sourceImport.copyAll"));
            button.setEnabled(view.getModel().isCellEditable(view.convertRowIndexToModel(row), view.convertColumnIndexToModel(column)));
            return button;
        });
        table.setDefaultEditor(Boolean.class, new CopyButtonEditor());
        table.getAccessibleContext().setAccessibleName(StudioBundle.message("sourceImport.files"));
        JPanel filePanel = new JPanel(new BorderLayout(0, JBUI.scale(6)));
        JPanel fileActions = new JPanel(new FlowLayout(FlowLayout.LEFT, JBUI.scale(8), 0));
        copyAllNewButton.setToolTipText(StudioBundle.message("sourceImport.files.copyAllNewHint"));
        copyAllNewButton.setEnabled(false);
        copyAllNewButton.addActionListener(event -> copyAllNewFiles());
        fileActions.add(showIdenticalFiles); fileActions.add(copyAllNewButton);
        filePanel.add(fileActions, BorderLayout.NORTH);
        filePanel.add(files, BorderLayout.CENTER);
        configureDetails(table, fileDetails, fileDetailsPane);
        filePanel.add(fileDetailsPane, BorderLayout.SOUTH);
        tabs.addTab(StudioBundle.message("sourceImport.files"), filePanel);
        configureInlineRenderer(table);
        JBTextArea supportHelp = new JBTextArea(StudioBundle.message("sourceImport.support.help"));
        supportHelp.setEditable(false); supportHelp.setLineWrap(true); supportHelp.setWrapStyleWord(true);
        supportHelp.setOpaque(false); supportHelp.setFocusable(false);
        JPanel supportHeader = new JPanel(new BorderLayout(0, JBUI.scale(6)));
        supportHeader.add(supportHelp, BorderLayout.NORTH);
        supportHeader.add(showIdenticalSupport, BorderLayout.SOUTH);
        supportPanel.add(supportHeader, BorderLayout.NORTH);
        showIdenticalSupport.addActionListener(event -> {
            if (supportTable.isEditing()) supportTable.getCellEditor().stopCellEditing();
            refreshSupportVisibility();
        });
        supportTable.setAutoCreateRowSorter(true);
        supportTable.getEmptyText().setText(StudioBundle.message("sourceImport.support.allIdentical"));
        ((TableRowSorter<?>) supportTable.getRowSorter()).setRowFilter(new RowFilter<TableModel, Integer>() {
            @Override public boolean include(Entry<? extends TableModel, ? extends Integer> entry) {
                if (analysis == null) return false;
                ManagedFile file = analysis.managedFiles().get(entry.getIdentifier());
                return showIdenticalSupport.isSelected() || selectedFiles.contains(file.source())
                        || !file.identicalToDestination() || file.status() == SourceModuleImport.SupportStatus.UNVERIFIED
                        || file.status() == SourceModuleImport.SupportStatus.DESTINATION_MISSING;
            }
        });
        ((TableRowSorter<?>) supportTable.getRowSorter()).setSortsOnUpdates(true);
        registerComparison(supportTable);
        supportTable.getColumnModel().getColumn(1).setMaxWidth(JBUI.scale(100));
        supportTable.setDefaultRenderer(Boolean.class, table.getDefaultRenderer(Boolean.class));
        supportTable.setDefaultEditor(Boolean.class, new CopyButtonEditor());
        supportTable.getAccessibleContext().setAccessibleName(StudioBundle.message("sourceImport.support.title"));
        JBScrollPane supportScroll = new JBScrollPane(supportTable); supportScroll.setMinimumSize(new Dimension(0, 0));
        supportPanel.add(supportScroll, BorderLayout.CENTER);
        configureDetails(supportTable, supportDetails, supportDetailsPane);
        supportPanel.add(supportDetailsPane, BorderLayout.SOUTH);
        configureInlineRenderer(supportTable);
        supportNotice.setEditable(false); supportNotice.setLineWrap(true); supportNotice.setWrapStyleWord(true);
        supportNotice.setOpaque(false); supportNotice.setFocusable(false); supportNotice.setVisible(false);
        JPanel content = new JPanel(new BorderLayout(0, JBUI.scale(6)));
        content.add(supportNotice, BorderLayout.NORTH); content.add(tabs, BorderLayout.CENTER);
        stagedSummary.setVisible(false); content.add(stagedSummary, BorderLayout.SOUTH);
        panel.add(content, BorderLayout.CENTER);
        JPanel options = new JPanel(new GridLayout(0, 1));
        registrationPanel.add(registrationButton); registrationPanel.add(registrationSummary);
        registrationPanel.setVisible(false); StudioUIUtils.setAttentionPulse(registrationButton, false);
        registrationButton.addActionListener(event -> reviewSpringRegistration());
        builderSettingsPanel.add(builderSettingsButton); builderSettingsPanel.add(builderSettingsSummary);
        builderSettingsPanel.setVisible(false);
        builderSettingsButton.addActionListener(event -> reviewBuilderSettings());
        options.add(registrationPanel); options.add(builderSettingsPanel); options.add(properties);
        panel.add(options, BorderLayout.SOUTH); panel.setPreferredSize(JBUI.size(920, 650));
        return panel;
    }

    private void analyse() {
        if (analysing || isDisposed() || project.isDisposed() || source.getText().isBlank()) return;
        analysing = true; lastAttempt = source.getText().trim();
        try {
            Path selected;
            try { selected = Path.of(source.getText().trim()).toAbsolutePath().normalize(); }
            catch (RuntimeException failure) { showAnalysisProblem(StudioBundle.message("sourceImport.chooseCheckout")); return; }
            if (source.getText().isBlank()) { error(StudioBundle.message("sourceImport.chooseCheckout")); return; }
            StudioProjectFiles.saveConfigurationDocuments(project, List.of("pom.xml", ProjectArtifactRenderer.MODEL, "src/main/model/model.json"));
            clearAnalysis();
            Path destination = Path.of(Objects.requireNonNull(StudioProjectFiles.getApplicationBasePath(project), "Import requires a project directory"));
            Inspection inspection = work(() -> SourceCheckoutIdentity.inspect(selected, destination, ProgressManager::checkCanceled), true);
            if (inspection == null || project.isDisposed() || isDisposed()) return;
            Candidate candidate = inspection.automaticCandidate();
            String directory;
            if (candidate == null) {
                SourceModuleSelectionDialog chooser = new SourceModuleSelectionDialog(project, getContentPane(), inspection);
                if (!chooser.showAndGet() || project.isDisposed() || isDisposed()) return;
                directory = chooser.selectedDirectory();
            } else directory = candidate.directory();
            Analysis result = work(() -> SourceModuleImport.analyse(selected, destination, directory, ProgressManager::checkCanceled), true);
            if (result == null || project.isDisposed() || isDisposed()) return;
            acceptAnalysis(result, selected);
        } finally { analysing = false; }
    }

    private void clearAnalysis() {
        reviewSequence.incrementAndGet();
        analysis = null; analysedSource = null; selectedFiles.clear(); selectedDependencies.clear();
        copyAllNewButton.setEnabled(false);
        registrationCandidate = null; approvedRegistration = null; registrationPanel.setVisible(false); StudioUIUtils.setAttentionPulse(registrationButton, false);
        resetBuilderSettings();
        discardMerges();
        identitySummary.setVisible(false); acceptIdentity.setVisible(false); acceptIdentity.setSelected(false);
        supportNotice.setVisible(false);
        if (reviewTabs.indexOfComponent(supportPanel) >= 0) reviewTabs.remove(supportPanel);
        setOKActionEnabled(false);
        inlineActions = Map.of(); standaloneIssues = List.of();
        choices.fireTableDataChanged(); supportChoices.fireTableDataChanged();
        updateDetails(table); updateDetails(supportTable);
    }

    private void showAnalysisProblem(String message) {
        identitySummary.setForeground(JBColor.namedColor("Label.errorForeground", JBColor.RED));
        identitySummary.setText(StudioBundle.message("sourceImport.analysisProblem", message));
        identitySummary.setVisible(true); setOKActionEnabled(false);
    }

    void acceptAnalysis(SourceModuleImport.Analysis result, Path selected) {
        source.setText(selected.toString());
        analysis = result; analysedSource = selected; lastAttempt = source.getText().trim();
        registrationCandidate = null; approvedRegistration = null; registrationPanel.setVisible(false); StudioUIUtils.setAttentionPulse(registrationButton, false);
        resetBuilderSettings();
        inlineActions = Map.of(); standaloneIssues = List.of();
        discardMerges();
        selectedFiles.clear(); selectedDependencies.clear();
        acceptIdentity.setSelected(false); acceptIdentity.setVisible(result.identity().requiresApproval());
        Check check = result.identity();
        String expected = check.expected().isBlank() ? StudioBundle.message("sourceImport.identity.unknownName") : check.expected();
        String names = check.names().isEmpty() ? StudioBundle.message("sourceImport.identity.unknownName") : String.join(", ", check.names());
        String scope = check.candidate() == null || check.candidate().directory().isEmpty() ? "." : check.candidate().directory();
        identitySummary.setForeground(check.requiresApproval()
                ? JBColor.namedColor("Label.errorForeground", JBColor.RED)
                : UIUtil.getLabelForeground());
        identitySummary.setText(StudioBundle.message("sourceImport.identity." + check.status().name(), expected, names, scope)
                + "\n" + StudioBundle.message("sourceImport.identity.references", check.matchedImplementations().size()));
        identitySummary.setVisible(check.requiresApproval());
        if (!check.requiresApproval()) selectDefaults();
        setOKActionEnabled(!check.requiresApproval());
        choices.fireTableDataChanged(); supportChoices.fireTableDataChanged(); refreshReview();
    }

    private void selectDefaults() {
        analysis.dependencies().stream().filter(SourceModuleImport.DependencyChoice::importable)
                .forEach(dependency -> selectedDependencies.add(dependency.id()));
    }

    private void refreshReview() {
        if (analysis == null) return;
        copyAllNewButton.setEnabled((!analysis.identity().requiresApproval() || acceptIdentity.isSelected())
                && analysis.files().stream().anyMatch(file -> file.previous() == null && !selectedFiles.contains(file.source())));
        boolean hasSupport = !analysis.managedFiles().isEmpty();
        supportNotice.setVisible(hasSupport);
        if (hasSupport) {
            if (reviewTabs.indexOfComponent(supportPanel) < 0)
                reviewTabs.insertTab(StudioBundle.message("sourceImport.support.title"), null, supportPanel, null, 1);
        } else if (reviewTabs.indexOfComponent(supportPanel) >= 0) reviewTabs.remove(supportPanel);
        refreshFileVisibility(); refreshSupportVisibility();
        stagedSummary.setVisible(!stagedJava.isEmpty());
        stagedSummary.setText(StudioBundle.message("sourceImport.stagedSummary", stagedJava.size()));
        long sequence = reviewSequence.incrementAndGet();
        Analysis reviewed = analysis;
        Set<String> files = Set.copyOf(selectedFiles); Set<String> dependencies = Set.copyOf(selectedDependencies);
        Map<String,String> merges = Map.copyOf(stagedJava);
        CompletableFuture.supplyAsync(() -> {
                    if (sequence != reviewSequence.get()) return new ReviewState(List.of(), null, null);
                    try {
                        return new ReviewState(SourceModuleImport.reviewActions(reviewed, files, dependencies, merges),
                                SourceModuleImport.previewSpringRegistration(reviewed, files, merges), SourceBuilderSettingsRecovery.preview(reviewed));
                    } catch (Exception failure) { throw new CompletionException(failure); }
                }, reviewWorker)
                .whenComplete((actions, failure) -> ApplicationManager.getApplication().invokeLater(() -> {
                    if (isDisposed() || project.isDisposed() || analysis != reviewed || sequence != reviewSequence.get()) return;
                    if (failure != null) {
                        registrationCandidate = null; approvedRegistration = null; registrationPanel.setVisible(false); StudioUIUtils.setAttentionPulse(registrationButton, false);
                        resetBuilderSettings();
                        Logger.getInstance(ImportSourceModuleDialog.class).warn("Could not prepare source review actions", failure);
                        attachActions(List.of(new SourceModuleImport.ReviewAction(SourceModuleImport.ReviewKind.SCAN,
                                StudioBundle.message("sourceImport.reviewAction.failed"), List.of())));
                        return;
                    }
                    builderCandidate = actions.builderSettings();
                    if (!Objects.equals(builderCandidate, approvedBuilderSettings)) approvedBuilderSettings = null;
                    builderSettingsPanel.setVisible(builderCandidate != null);
                    builderSettingsButton.setText(StudioBundle.message(approvedBuilderSettings == null ? "sourceImport.builder.review" : "sourceImport.builder.revert"));
                    builderSettingsSummary.setText(builderCandidate == null ? "" : StudioBundle.message(
                            approvedBuilderSettings == null ? "sourceImport.builder.available" : "sourceImport.builder.staged", builderCandidate.settings().size()));
                    StudioUIUtils.setAttentionPulse(builderSettingsButton, builderCandidate != null && approvedBuilderSettings == null);
                    registrationCandidate = actions.registration();
                    if (!Objects.equals(approvedRegistration, registrationCandidate)) approvedRegistration = null;
                    registrationPanel.setVisible(registrationCandidate != null);
                    StudioUIUtils.setAttentionPulse(registrationButton, registrationCandidate != null && approvedRegistration == null);
                    registrationButton.setText(StudioBundle.message(approvedRegistration == null
                            ? "sourceImport.registration.review" : "sourceImport.registration.revert"));
                    registrationSummary.setText(registrationCandidate == null ? "" : StudioBundle.message(
                            approvedRegistration == null ? "sourceImport.registration.available" : "sourceImport.registration.staged",
                            registrationCandidate.components().size()));
                    registrationSummary.setToolTipText(registrationCandidate == null ? null : registrationCandidate.destination());
                    attachActions(actions.actions().stream().filter(action ->
                            SourceModuleImport.requiresReviewAfterRegistration(approvedRegistration, action)).toList());
                }, ModalityState.any()));
    }

    private void refreshFileVisibility() {
        if (analysis == null) return;
        ((TableRowSorter<?>) table.getRowSorter()).sort();
        updateDetails(table);
    }

    private void refreshSupportVisibility() {
        if (analysis == null) return;
        ((TableRowSorter<?>) supportTable.getRowSorter()).sort();
        int shown = supportTable.getRowCount();
        supportNotice.setText(StudioBundle.message("sourceImport.support.notice", shown, analysis.managedFiles().size() - shown));
        updateDetails(supportTable);
    }

    private static String actionMessage(SourceModuleImport.ReviewAction action) {
        return StudioBundle.message("sourceImport.reviewAction." + action.kind().name(), action.subject());
    }

    private void attachActions(List<SourceModuleImport.ReviewAction> actions) {
        int fileRow = selectedModelRow(table), supportRow = selectedModelRow(supportTable);
        SourceModuleImport.ReviewAction selectedIssue = fileRow >= analysis.files().size() + analysis.dependencies().size()
                && fileRow - analysis.files().size() - analysis.dependencies().size() < standaloneIssues.size()
                ? standaloneIssues.get(fileRow - analysis.files().size() - analysis.dependencies().size()) : null;
        Map<String, List<SourceModuleImport.ReviewAction>> attached = new LinkedHashMap<>();
        List<SourceModuleImport.ReviewAction> unmatched = new ArrayList<>();
        for (ReviewAction action : actions) {
            if (action.kind() == SourceModuleImport.ReviewKind.SOURCE || action.kind() == SourceModuleImport.ReviewKind.RESOURCE
                    || action.kind() == SourceModuleImport.ReviewKind.BEAN) continue;
            String key;
            if (action.kind() == SourceModuleImport.ReviewKind.SCAN) {
                key = analysis.files().stream().filter(file -> action.subject().contains(file.source())).map(SourceModuleImport.FileChoice::destination).findFirst().orElse(null);
            } else key = action.subject();
            if (key == null) unmatched.add(action);
            else attached.computeIfAbsent(key, ignored -> new ArrayList<>()).add(action);
        }
        inlineActions = Map.copyOf(attached); standaloneIssues = List.copyOf(unmatched);
        choices.fireTableDataChanged(); supportChoices.fireTableDataChanged();
        if (selectedIssue != null) fileRow = standaloneIssues.contains(selectedIssue)
                ? analysis.files().size() + analysis.dependencies().size() + standaloneIssues.indexOf(selectedIssue) : -1;
        restoreSelection(table, fileRow); restoreSelection(supportTable, supportRow);
        updateDetails(table); updateDetails(supportTable);
    }

    private static int selectedModelRow(JTable view) {
        return view.getSelectedRow() < 0 ? -1 : view.convertRowIndexToModel(view.getSelectedRow());
    }

    private static void restoreSelection(JTable view, int row) {
        if (row >= 0 && row < view.getModel().getRowCount()) {
            int visible = view.convertRowIndexToView(row);
            if (visible >= 0) view.setRowSelectionInterval(visible, visible);
        }
    }

    private List<SourceModuleImport.ReviewAction> rowActions(JTable view, int row) {
        if (analysis == null || row < 0) return List.of();
        String key;
        if (view == supportTable) {
            if (row >= analysis.managedFiles().size()) return List.of();
            key = analysis.managedFiles().get(row).destination();
        } else if (row < analysis.files().size()) key = analysis.files().get(row).destination();
        else if (row < analysis.files().size() + analysis.dependencies().size()) key = analysis.dependencies().get(row - analysis.files().size()).id();
        else return row - analysis.files().size() - analysis.dependencies().size() < standaloneIssues.size()
                    ? List.of(standaloneIssues.get(row - analysis.files().size() - analysis.dependencies().size())) : List.of();
        return inlineActions.getOrDefault(key, List.of());
    }

    private String actionSuffix(JTable view, int row) {
        return rowActions(view, row).isEmpty() ? "" : " — " + StudioBundle.message("sourceImport.inline.attention");
    }

    private String newFileHint(SourceModuleImport.FileChoice file) {
        return StudioBundle.message(selectedFiles.contains(file.source())
                ? "sourceImport.file.newSelected" : "sourceImport.file.newHelp");
    }

    private void configureDetails(JTable view, JBTextArea details, JBScrollPane pane) {
        details.setEditable(false); details.setLineWrap(true); details.setWrapStyleWord(true);
        details.setFont(UIUtil.getLabelFont());
        details.getAccessibleContext().setAccessibleName(StudioBundle.message("sourceImport.inline.details"));
        pane.setPreferredSize(JBUI.size(0, 105)); pane.setMinimumSize(new Dimension(0, 0)); pane.setVisible(false);
        view.getSelectionModel().addListSelectionListener(event -> { if (!event.getValueIsAdjusting()) updateDetails(view); });
    }

    private void updateDetails(JTable view) {
        JBTextArea details = view == supportTable ? supportDetails : fileDetails;
        JBScrollPane pane = view == supportTable ? supportDetailsPane : fileDetailsPane;
        int selected = view.getSelectedRow();
        List<ReviewAction> actions = selected >= 0 && selected < view.getRowCount() ? rowActions(view, view.convertRowIndexToModel(selected)) : List.of();
        StringBuilder text = new StringBuilder();
        if (analysis != null && view == table && selected >= 0 && selected < view.getRowCount()) {
            int row = view.convertRowIndexToModel(selected);
            if (row < analysis.files().size()) {
                FileChoice file = analysis.files().get(row);
                if (file.previous() == null) text.append(newFileHint(file)).append('\n').append(file.destination());
            }
        }
        for (ReviewAction action : actions) {
            if (!text.isEmpty()) text.append('\n');
            text.append(actionMessage(action));
            action.evidence().forEach(evidence -> text.append("\n").append(evidence));
        }
        if (text.isEmpty() && analysis != null && view == table && selected >= 0 && selected < view.getRowCount()) {
            int row = view.convertRowIndexToModel(selected) - analysis.files().size();
            if (row >= 0 && row < analysis.dependencies().size() && !analysis.dependencies().get(row).importable())
                text.append(analysis.dependencies().get(row).reason());
        }
        details.setText(text.toString()); details.setCaretPosition(0);
        pane.setVisible(!text.isEmpty());
        if (pane.getParent() != null) { pane.getParent().revalidate(); pane.getParent().repaint(); }
    }

    private void configureInlineRenderer(JTable view) {
        view.setDefaultRenderer(String.class, new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(JTable tableView, Object value, boolean selected, boolean focused, int row, int column) {
                super.getTableCellRendererComponent(tableView, value, selected, focused, row, column);
                List<ReviewAction> actions = rowActions(tableView, tableView.convertRowIndexToModel(row));
                boolean attention = tableView != supportTable && !actions.isEmpty() && tableView.convertColumnIndexToModel(column) == 2;
                setIcon(attention ? General.Warning : null);
                setToolTipText(actions.isEmpty() ? String.valueOf(value) : actionMessage(actions.get(0)));
                int modelRow = tableView.convertRowIndexToModel(row);
                if (analysis != null && tableView == table && modelRow < analysis.files().size()
                        && tableView.convertColumnIndexToModel(column) == 2) {
                    FileChoice file = analysis.files().get(modelRow);
                    if (file.previous() == null) setToolTipText(newFileHint(file) + " " + file.destination()
                            + (actions.isEmpty() ? "" : " — " + actionMessage(actions.get(0))));
                }
                return this;
            }
        });
    }

    @Override protected @Nullable ValidationInfo doValidate() {
        if (analysis == null || !source.getText().trim().equals(analysedSource.toString()))
            return new ValidationInfo(StudioBundle.message("sourceImport.analyseFirst"), source);
        if (analysis.identity().requiresApproval() && !acceptIdentity.isSelected())
            return new ValidationInfo(StudioBundle.message("sourceImport.identity.approvalRequired"), acceptIdentity);
        return null;
    }

    private void resetBuilderSettings() {
        builderCandidate = null; approvedBuilderSettings = null;
        builderSettingsPanel.setVisible(false); StudioUIUtils.setAttentionPulse(builderSettingsButton, false);
    }

    private void reviewBuilderSettings() {
        if (approvedBuilderSettings != null) { approvedBuilderSettings = null; refreshReview(); return; }
        Plan candidate = builderCandidate;
        if (candidate == null) return;
        String changes = candidate.settings().stream().map(setting -> setting.flow() + " → " + setting.component()
                + "\n  " + setting.property() + " = " + setting.value() + "\n  " + setting.evidence())
                .collect(Collectors.joining("\n\n"));
        StudioUIUtils.setAttentionPulse(builderSettingsButton, false);
        // Bound the preview so a large module does not create a screen-height message dialog.
        StudioDialogWrapper review = new StudioDialogWrapper(project) {
            { setTitle(StudioBundle.message("sourceImport.builder.title"));
              setOKButtonText(StudioBundle.message("sourceImport.builder.stage")); init(); }
            @Override protected JComponent createStudioContent() {
                JBTextArea text = new JBTextArea(StudioBundle.message("sourceImport.builder.explanation", changes), 20, 85);
                text.setEditable(false); text.setLineWrap(true); text.setWrapStyleWord(true);
                text.getAccessibleContext().setAccessibleName(StudioBundle.message("sourceImport.builder.title"));
                JBScrollPane scroll = new JBScrollPane(text); scroll.setMinimumSize(JBUI.size(400, 250));
                scroll.setPreferredSize(JBUI.size(850, 450)); return scroll;
            }
        };
        if (review.showAndGet()) approvedBuilderSettings = candidate;
        refreshReview();
    }

    private void reviewSpringRegistration() {
        if (approvedRegistration != null) {
            approvedRegistration = null;
            refreshReview();
            return;
        }
        SpringRegistration candidate = registrationCandidate;
        if (candidate == null) return;
        // This review runs in the local IDE, like ImportDialogWrapper.
        StudioDialogWrapper review = new StudioDialogWrapper(project) {
            { setTitle(StudioBundle.message("sourceImport.registration.title"));
              setOKButtonText(StudioBundle.message("sourceImport.registration.stage")); init(); }
            @Override protected JComponent createStudioContent() {
                JPanel panel = new JPanel(new BorderLayout(0, JBUI.scale(8)));
                JBTextArea explanation = new JBTextArea(StudioBundle.message("sourceImport.registration.explanation", candidate.destination()));
                explanation.setEditable(false); explanation.setOpaque(false); explanation.setLineWrap(true); explanation.setWrapStyleWord(true);
                // Wrapped text areas otherwise measure at zero width during DialogWrapper packing,
                // producing a minimum height that can stretch the popup to the whole screen.
                explanation.setColumns(80); explanation.setRows(4); explanation.setMinimumSize(JBUI.emptySize());
                panel.add(explanation, BorderLayout.NORTH);
                JBTextArea code = new JBTextArea(candidate.javaSource()); code.setEditable(false);
                code.setRows(16); code.setColumns(90);
                code.setFont(JBUI.Fonts.create("Monospaced", 12));
                code.getAccessibleContext().setAccessibleName(StudioBundle.message("sourceImport.registration.title"));
                JBScrollPane scroll = new JBScrollPane(code); scroll.setMinimumSize(JBUI.emptySize());
                panel.add(scroll, BorderLayout.CENTER);
                panel.setMinimumSize(JBUI.size(400, 250)); panel.setPreferredSize(JBUI.size(850, 450));
                return panel;
            }
        };
        StudioUIUtils.setAttentionPulse(registrationButton, false);
        if (review.showAndGet()) approvedRegistration = candidate;
        refreshReview();
    }

    @Override protected void doOKAction() {
        if (table.isEditing()) table.getCellEditor().stopCellEditing();
        if (supportTable.isEditing()) supportTable.getCellEditor().stopCellEditing();
        ValidationInfo validation = doValidate(); if (validation != null) { error(validation.message); return; }
        UiContext context = project.getService(UiContext.class);
        if (!context.tryBeginMigration()) { error(StudioBundle.message("message.WaitForTheCurrentGenerationOrMigrationToFinish")); return; }
        Path savedReport;
        try {
            List<String> targets = new ArrayList<>(SourceModuleImport.importableFiles(analysis).stream().filter(f -> selectedFiles.contains(f.source())).map(SourceModuleImport.FileChoice::destination).toList());
            if (approvedRegistration != null) targets.add(approvedRegistration.destination());
            if (approvedBuilderSettings != null) targets.addAll(approvedBuilderSettings.outputs().keySet());
            targets.add(SourceModuleImport.SUPPORT_CUSTOMISATIONS); targets.add("pom.xml"); StudioProjectFiles.saveConfigurationDocuments(project, targets);
            Set<String> files = Set.copyOf(selectedFiles); Set<String> dependencies = Set.copyOf(selectedDependencies);
            boolean identityApproved = acceptIdentity.isSelected();
            Map<String,String> merges = stagedJava.entrySet().stream().filter(entry -> files.contains(entry.getKey()))
                    .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, Map.Entry::getValue));
            // Copy all / Save preview explicitly authorise replacement of these selected files only.
            SpringRegistration registration = approvedRegistration;
            Plan builderSettings = approvedBuilderSettings;
            savedReport = work(() -> SourceModuleImport.apply(analysis, files, dependencies, true, identityApproved, merges, registration, builderSettings), false);
            if (savedReport != null && builderSettings != null) {
                Module restored = work(() -> ComponentIO.validatePersistedModuleJson(
                        builderSettings.outputs().get(ProjectArtifactRenderer.MODEL), "recovered builder settings", false), false);
                if (restored != null) {
                    context.setIkasanModule(restored);
                    if (context.getViewHandlerFactory() != null) context.getViewHandlerFactory().clear();
                    context.resetSelectionAfterDeletion();
                    if (context.getDesignerCanvas() != null) {
                        context.getDesignerCanvas().setInitialiseAllDimensions(true);
                        context.getDesignerCanvas().repaint();
                    }
                }
            }
        } finally { context.endMigration(); }
        if (savedReport == null) return;
        StudioProjectFiles.refreshConfigurationDirectory(project);
        boolean importProperties = properties.isSelected();
        List<PropertyFile> discovered = analysis.properties(); close(OK_EXIT_CODE);
        ApplicationManager.getApplication().invokeLater(() -> {
            if (project.isDisposed()) return;
            Messages.showInfoMessage(project, StudioBundle.message("sourceImport.complete", savedReport.toString()), StudioBundle.message("sourceImport.title"));
            if (importProperties) new ImportEnvironmentPropertiesDialog(project, discovered).show();
        }, ModalityState.nonModal());
    }

    private void registerComparison(JBTable target) {
        target.setToolTipText(StudioBundle.message("sourceImport.compareGesture"));
        target.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent event) {
                if (event.getClickCount() != 2 || !SwingUtilities.isLeftMouseButton(event)) return;
                int row = target.rowAtPoint(event.getPoint()), column = target.columnAtPoint(event.getPoint());
                if (row < 0 || column < 0 || (target == table && target.convertColumnIndexToModel(column) == 0
                        || target == supportTable && target.convertColumnIndexToModel(column) == 1)) return;
                target.setRowSelectionInterval(row, row);
                compare(target);
            }
        });
        target.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "studio.compareSource");
        target.getActionMap().put("studio.compareSource", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent event) { compare(target); }
        });
    }

    private void compare(JTable selectedTable) {
        SimpleDiffRequest request = comparison(selectedTable);
        if (request == null) return;
        if (request.getContents().get(1) instanceof DocumentContent destination
                && destination.getDocument().isWritable())
            new SourceComparisonDialog(project, getContentPane(), request).show();
        else DiffManager.getInstance().showDiff(project, request);
    }

    /** Build a read-only diff from the reviewed snapshots, preserving sorted-table row mapping. */
    @Nullable SimpleDiffRequest comparison(JTable selectedTable) {
        if (analysis == null || selectedTable.getSelectedRow() < 0) return null;
        int row = selectedTable.convertRowIndexToModel(selectedTable.getSelectedRow());
        if (selectedTable == table && row >= analysis.files().size() + analysis.dependencies().size()) return null;
        if (selectedTable == supportTable) {
            ManagedFile support = analysis.managedFiles().get(row);
            if (!analysis.identity().requiresApproval() || acceptIdentity.isSelected())
                return editableComparison(supportChoice(support));
            return comparison(support.destination(), utf8(support.bytes()), support.current() == null ? "" : utf8(support.current()),
                    StudioBundle.message("sourceImport.support.originalVersion"), StudioBundle.message("sourceImport.support.destinationVersion"));
        }
        if (row < analysis.files().size()) {
            FileChoice file = analysis.files().get(row);
            if (file.source().endsWith(".java") && (!analysis.identity().requiresApproval() || acceptIdentity.isSelected()))
                return editableComparison(file);
            return comparison(file.destination(), utf8(file.bytes()), file.previous() == null ? "" : utf8(file.previous()),
                    StudioBundle.message("sourceImport.originalVersion"), StudioBundle.message("sourceImport.destinationVersion"));
        }
        DependencyChoice dependency = analysis.dependencies().get(row - analysis.files().size());
        DependencyComparison values = work(() -> SourceModuleImport.compareDependency(analysis, dependency.id()), false);
        return values == null ? null : comparison("pom.xml: " + dependency.id(), values.original(), values.destination(),
                StudioBundle.message("sourceImport.originalDependency"), StudioBundle.message("sourceImport.destinationDependency"));
    }

    private String plannedBytes(SourceModuleImport.FileChoice file) {
        String merged = stagedJava.get(file.source());
        return merged == null ? file.bytes() : Base64.getEncoder().encodeToString(merged.getBytes(StandardCharsets.UTF_8));
    }

    private void discardMerges() {
        mergeRefresh.stop();
        mergeDocuments.values().forEach(content -> content.getDocument().setReadOnly(true));
        stagedJava.clear(); mergeDocuments.clear(); stagedSummary.setVisible(false);
    }

    // Studio runs in the local IDE; backend classification comes from transitive plugin dependencies.
    @SuppressWarnings("SplitModeApiUsage")
    private SimpleDiffRequest editableComparison(SourceModuleImport.FileChoice file) {
        DiffContentFactory factory = DiffContentFactory.getInstance();
        DocumentContent left = factory.create(utf8(file.bytes()), JavaFileType.INSTANCE);
        left.getDocument().setReadOnly(true);
        DocumentContent right = mergeDocuments.computeIfAbsent(file.source(), key -> {
            DocumentContent content = factory.create(selectedFiles.contains(file.source()) ? utf8(plannedBytes(file)) : file.previous() == null ? "" : utf8(file.previous()), JavaFileType.INSTANCE);
            Document document = content.getDocument(); document.setReadOnly(false);
            String baseline = file.previous() == null ? "" : utf8(file.previous()).replace("\r\n", "\n").replace("\r", "\n");
            String originalText = file.previous() == null ? utf8(file.bytes()) : utf8(file.previous());
            int newline = originalText.indexOf('\n');
            String separator = newline > 0 && originalText.charAt(newline - 1) == '\r' ? "\r\n"
                    : newline < 0 && originalText.indexOf('\r') >= 0 ? "\r" : "\n";
            Analysis reviewed = analysis;
            document.addDocumentListener(new com.intellij.openapi.editor.event.DocumentListener() {
                @Override public void documentChanged(@NotNull com.intellij.openapi.editor.event.DocumentEvent event) {
                    if (isDisposed() || project.isDisposed() || analysis != reviewed
                            || analysis.identity().requiresApproval() && !acceptIdentity.isSelected()) return;
                    String text = event.getDocument().getText();
                    if (text.equals(baseline)) { stagedJava.remove(key); selectedFiles.remove(key); }
                    else { stagedJava.put(key, "\n".equals(separator) ? text : text.replace("\n", separator)); selectedFiles.add(key); }
                    mergeRefresh.restart();
                }
            }, getDisposable());
            return content;
        });
        @Nls String title = file.destination();
        return new SimpleDiffRequest(title, left, right,
                StudioBundle.message("sourceImport.originalVersion"), StudioBundle.message("sourceImport.mergeDestination"));
    }

    private SimpleDiffRequest comparison(@Nls String title,
            String before, String after, @Nls String beforeTitle, @Nls String afterTitle) {
        DiffContentFactory factory = DiffContentFactory.getInstance();
        DocumentContent left = factory.create(before); DocumentContent right = factory.create(after);
        left.getDocument().setReadOnly(true); right.getDocument().setReadOnly(true);
        SimpleDiffRequest request = new SimpleDiffRequest(title, left, right, beforeTitle, afterTitle);
        request.putUserData(DiffUserDataKeys.FORCE_READ_ONLY, true);
        return request;
    }
    private static String utf8(String encoded) { return new String(Base64.getDecoder().decode(encoded), StandardCharsets.UTF_8); }
    private interface Work<T> { T run() throws Exception; }
    private <T> T work(Work<T> work, boolean cancellable) {
        AtomicReference<T> result = new AtomicReference<>(); AtomicReference<Exception> failure = new AtomicReference<>();
        boolean finished = ProgressManager.getInstance().runProcessWithProgressSynchronously(() -> {
            try { result.set(work.run()); } catch (ProcessCanceledException cancelled) { throw cancelled; } catch (Exception problem) { failure.set(problem); }
        }, StudioBundle.message("sourceImport.progress"), cancellable, project);
        if (failure.get() != null) {
            if (cancellable) showAnalysisProblem(failure.get().getMessage()); else error(failure.get().getMessage());
        }
        return finished ? result.get() : null;
    }
    private void error(String message) { Messages.showErrorDialog(getContentPane() == null ? source : getContentPane(), message, StudioBundle.message("sourceImport.title")); }

    private static SourceModuleImport.FileChoice supportChoice(SourceModuleImport.ManagedFile file) {
        return new SourceModuleImport.FileChoice(file.source(), file.destination(), file.bytes(), file.current(), false, false, "");
    }

    private void copyAllNewFiles() {
        if (analysis == null || (analysis.identity().requiresApproval() && !acceptIdentity.isSelected())) return;
        if (table.isEditing()) table.getCellEditor().stopCellEditing();
        // Use the analysed snapshots, independent of table sorting/filtering. Preserve staged edits.
        for (FileChoice file : analysis.files())
            if (file.previous() == null && !selectedFiles.contains(file.source())) copyFile(file, true);
        choices.fireTableDataChanged(); refreshReview();
        updateDetails(table);
    }

    private void copyFile(SourceModuleImport.FileChoice file, boolean copy) {
        String id = file.source();
        stagedJava.remove(id);
        DocumentContent content = mergeDocuments.get(id);
        if (content != null) {
            String text = copy ? utf8(file.bytes()) : file.previous() == null ? "" : utf8(file.previous());
            WriteAction.run(() -> content.getDocument().setText(text.replace("\r\n", "\n").replace("\r", "\n")));
            stagedJava.remove(id);
        }
        if (copy) selectedFiles.add(id); else selectedFiles.remove(id);
    }

    private final class SupportChoices extends AbstractTableModel {
        @Override public int getRowCount() { return analysis == null ? 0 : analysis.managedFiles().size(); }
        @Override public int getColumnCount() { return 3; }
        @Override public String getColumnName(int column) {
            return StudioBundle.message(new String[]{"sourceImport.support.source", "sourceImport.copyAction", "sourceImport.destination"}[column]);
        }
        @Override public Class<?> getColumnClass(int column) { return column == 1 ? Boolean.class : String.class; }
        @Override public boolean isCellEditable(int row, int column) {
            return column == 1 && analysis != null && (!analysis.identity().requiresApproval() || acceptIdentity.isSelected());
        }
        @Override public Object getValueAt(int row, int column) {
            ManagedFile file = analysis.managedFiles().get(row);
            return switch (column) {
                case 0 -> file.source();
                case 1 -> selectedFiles.contains(file.source());
                default -> file.destination() + " — " + StudioBundle.message("sourceImport.support.status." + file.status().name());
            };
        }
        @Override public void setValueAt(Object value, int row, int column) {
            copyFile(supportChoice(analysis.managedFiles().get(row)), Boolean.TRUE.equals(value));
            fireTableRowsUpdated(row, row); refreshReview();
        }
    }

    private static final class CopyButtonEditor extends AbstractCellEditor implements TableCellEditor {
        private final JButton button = new JButton();
        private boolean selected;
        CopyButtonEditor() {
            button.addActionListener(event -> { selected = !selected; fireEditingStopped(); });
        }
        @Override public Object getCellEditorValue() { return selected; }
        @Override public Component getTableCellEditorComponent(JTable view, Object value, boolean focused, int row, int column) {
            selected = Boolean.TRUE.equals(value);
            button.setText(StudioBundle.message(selected ? "sourceImport.revert" : "sourceImport.copyAll"));
            return button;
        }
    }

    private final class Choices extends AbstractTableModel {
        @Override public int getRowCount() { return analysis == null ? 0 : analysis.files().size() + analysis.dependencies().size() + standaloneIssues.size(); }
        @Override public int getColumnCount() { return 3; }
        @Override public String getColumnName(int column) { return StudioBundle.message(new String[]{"sourceImport.copyAction", "sourceImport.evidence", "sourceImport.destination"}[column]); }
        @Override public Class<?> getColumnClass(int column) { return column == 0 ? Boolean.class : String.class; }
        @Override public boolean isCellEditable(int row, int column) { return column == 0 && analysis != null && (!analysis.identity().requiresApproval() || acceptIdentity.isSelected())
                && (row < analysis.files().size() || row < analysis.files().size() + analysis.dependencies().size()
                && analysis.dependencies().get(row - analysis.files().size()).importable()); }
        @Override public Object getValueAt(int row, int column) {
            if (row >= analysis.files().size() + analysis.dependencies().size()) {
                ReviewAction issue = standaloneIssues.get(row - analysis.files().size() - analysis.dependencies().size());
                return switch (column) {
                    case 0 -> false;
                    case 1 -> issue.evidence().isEmpty() ? StudioBundle.message("sourceImport.inline.unmatched") : issue.evidence().get(0);
                    default -> actionMessage(issue);
                };
            }
            if (row < analysis.files().size()) {
                FileChoice file = analysis.files().get(row);
                return switch (column) {
                    case 0 -> selectedFiles.contains(file.source());
                    case 2 -> (file.previous() == null ? StudioBundle.message("sourceImport.file.new", file.destination())
                            : file.destination()) + actionSuffix(table, row);
                    default -> file.source() + (stagedJava.containsKey(file.source()) ? " " + StudioBundle.message("sourceImport.stagedMarker") : "");
                };
            }
            DependencyChoice dependency = analysis.dependencies().get(row - analysis.files().size());
            return switch (column) {
                case 0 -> selectedDependencies.contains(dependency.id());
                case 2 -> StudioBundle.message(dependency.importable() ? "sourceImport.dependency.addToPom" : "sourceImport.dependency.manual") + actionSuffix(table, row);
                default -> dependency.id();
            };
        }
        @Override public void setValueAt(Object value, int row, int column) {
            Set<String> selection; String id;
            if (row < analysis.files().size()) { selection = selectedFiles; id = analysis.files().get(row).source(); }
            else { selection = selectedDependencies; id = analysis.dependencies().get(row - analysis.files().size()).id(); }
            boolean copy = Boolean.TRUE.equals(value);
            if (row < analysis.files().size()) {
                copyFile(analysis.files().get(row), copy);
            }
            if (copy) selection.add(id); else selection.remove(id);
            fireTableRowsUpdated(row, row); refreshReview();
        }
    }
}
