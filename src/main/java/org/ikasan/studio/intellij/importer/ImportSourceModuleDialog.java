package org.ikasan.studio.intellij.importer;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.ui.components.*;
import com.intellij.ui.table.JBTable;
import com.intellij.util.ui.JBUI;
import org.ikasan.studio.core.importer.SourceModuleImport;
import org.ikasan.studio.core.importer.SourceCheckoutIdentity;
import org.ikasan.studio.intellij.configuration.ImportEnvironmentPropertiesDialog;
import org.ikasan.studio.intellij.project.ImportDialogWrapper;
import org.ikasan.studio.intellij.project.StudioProjectFiles;
import org.ikasan.studio.ui.StudioBundle;
import org.ikasan.studio.ui.UiContext;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/** Second stage of the module wizard. Analysis is read-only; apply transfers only explicitly reviewed items. */
public final class ImportSourceModuleDialog extends ImportDialogWrapper {
    private final Project project;
    private final JBTextField source = new JBTextField();
    private final JBCheckBox replace = new JBCheckBox(StudioBundle.message("sourceImport.replace"));
    private final JBCheckBox properties = new JBCheckBox(StudioBundle.message("sourceImport.properties"), true);
    private final JBCheckBox acceptIdentity = new JBCheckBox(StudioBundle.message("sourceImport.identity.override"));
    private final JBTextArea identitySummary = new JBTextArea();
    private final JBTextArea report = new JBTextArea(18, 85);
    private final Choices choices = new Choices();
    private final JBTable table = new JBTable(choices);
    private final JTabbedPane reviewTabs = new JBTabbedPane();
    private final SupportChoices supportChoices = new SupportChoices();
    private final JBTable supportTable = new JBTable(supportChoices);
    private final JPanel supportPanel = new JPanel(new BorderLayout(0, JBUI.scale(6)));
    private final JBTextArea supportNotice = new JBTextArea();
    private SourceModuleImport.Analysis analysis;
    private boolean analysing;
    private String lastAttempt = "";
    private final Map<String, String> stagedJava = new LinkedHashMap<>();
    private final Map<String, com.intellij.diff.contents.DocumentContent> mergeDocuments = new HashMap<>();
    private final JBLabel stagedSummary = new JBLabel();
    private final javax.swing.Timer mergeRefresh = new javax.swing.Timer(120, event -> {
        if (!isDisposed() && analysis != null) { choices.fireTableDataChanged(); refreshReport(); }
    });
    private Path analysedSource;
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
        com.intellij.openapi.util.Disposer.register(getDisposable(), mergeRefresh::stop);
        setOKActionEnabled(false);
    }

    @Override protected @org.jetbrains.annotations.NotNull JComponent createCenterPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, JBUI.scale(8)));
        JPanel top = new JPanel(new BorderLayout(0, JBUI.scale(6)));
        var help = new JBTextArea(StudioBundle.message("sourceImport.help"));
        help.setEditable(false); help.setLineWrap(true); help.setWrapStyleWord(true); help.setOpaque(false); help.setFocusable(false);
        top.add(help, BorderLayout.NORTH);
        JPanel path = new JPanel(new BorderLayout(JBUI.scale(6), 0));
        var label = new JBLabel(StudioBundle.message("sourceImport.checkout")); label.setLabelFor(source);
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
        source.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override public void focusLost(java.awt.event.FocusEvent event) {
                if (event.isTemporary() || event.getOppositeComponent() == browse) return;
                if (event.getOppositeComponent() instanceof AbstractButton button
                        && (button.getAction() == getCancelAction() || com.intellij.CommonBundle.getCancelButtonText().equals(button.getText()))) return;
                if (!source.getText().trim().equals(lastAttempt)) analyse();
            }
        });
        source.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            private void changed() { if (!analysing) { lastAttempt = ""; clearAnalysis(); } }
            @Override public void insertUpdate(javax.swing.event.DocumentEvent event) { changed(); }
            @Override public void removeUpdate(javax.swing.event.DocumentEvent event) { changed(); }
            @Override public void changedUpdate(javax.swing.event.DocumentEvent event) { changed(); }
        });
        var comparisonHint = new JBTextArea(StudioBundle.message("sourceImport.comparisonHint"));
        comparisonHint.setEditable(false); comparisonHint.setLineWrap(true); comparisonHint.setWrapStyleWord(true);
        comparisonHint.setOpaque(false); comparisonHint.setFocusable(false);
        comparisonHint.setFont(com.intellij.util.ui.UIUtil.getLabelFont());
        identitySummary.setEditable(false); identitySummary.setLineWrap(true); identitySummary.setWrapStyleWord(true);
        identitySummary.setOpaque(false); identitySummary.setFocusable(false);
        identitySummary.setFont(com.intellij.util.ui.UIUtil.getLabelFont()); identitySummary.setVisible(false);
        acceptIdentity.setVisible(false);
        acceptIdentity.addActionListener(event -> {
            if (analysis != null && analysis.identity().requiresApproval()) {
                selectedFiles.clear(); selectedDependencies.clear();
                if (!acceptIdentity.isSelected()) discardMerges();
                if (acceptIdentity.isSelected()) selectDefaults();
                setOKActionEnabled(acceptIdentity.isSelected());
                choices.fireTableDataChanged(); refreshReport();
            }
        });
        JPanel steps = new JPanel(); steps.setLayout(new BoxLayout(steps, BoxLayout.Y_AXIS));
        comparisonHint.setAlignmentX(Component.LEFT_ALIGNMENT); identitySummary.setAlignmentX(Component.LEFT_ALIGNMENT);
        acceptIdentity.setAlignmentX(Component.LEFT_ALIGNMENT);
        steps.add(identitySummary); steps.add(acceptIdentity); steps.add(comparisonHint);
        top.add(steps, BorderLayout.SOUTH); panel.add(top, BorderLayout.NORTH);
        JTabbedPane tabs = reviewTabs;
        var files = new JBScrollPane(table); files.setMinimumSize(new Dimension(0, 0));
        table.setAutoCreateRowSorter(true);
        registerComparison(table);
        table.getColumnModel().getColumn(0).setMaxWidth(JBUI.scale(100));
        table.moveColumn(0, 1);
        table.setDefaultRenderer(Boolean.class, (view, value, selected, focused, row, column) -> {
            var button = new JButton(StudioBundle.message(Boolean.TRUE.equals(value) ? "sourceImport.revert" : "sourceImport.copyAll"));
            button.setEnabled(view.getModel().isCellEditable(view.convertRowIndexToModel(row), view.convertColumnIndexToModel(column)));
            return button;
        });
        table.setDefaultEditor(Boolean.class, new CopyButtonEditor());
        table.getAccessibleContext().setAccessibleName(StudioBundle.message("sourceImport.files"));
        tabs.addTab(StudioBundle.message("sourceImport.files"), files);
        report.setEditable(false); report.setLineWrap(true); report.setWrapStyleWord(true);
        report.getAccessibleContext().setAccessibleName(StudioBundle.message("sourceImport.review"));
        var scroll = new JBScrollPane(report); scroll.setMinimumSize(new Dimension(0, 0));
        tabs.addTab(StudioBundle.message("sourceImport.review"), scroll);
        var supportHelp = new JBTextArea(StudioBundle.message("sourceImport.support.help"));
        supportHelp.setEditable(false); supportHelp.setLineWrap(true); supportHelp.setWrapStyleWord(true);
        supportHelp.setOpaque(false); supportHelp.setFocusable(false);
        supportPanel.add(supportHelp, BorderLayout.NORTH);
        supportTable.setAutoCreateRowSorter(true);
        registerComparison(supportTable);
        supportTable.getColumnModel().getColumn(1).setMaxWidth(JBUI.scale(100));
        supportTable.setDefaultRenderer(Boolean.class, table.getDefaultRenderer(Boolean.class));
        supportTable.setDefaultEditor(Boolean.class, new CopyButtonEditor());
        supportTable.getAccessibleContext().setAccessibleName(StudioBundle.message("sourceImport.support.title"));
        var supportScroll = new JBScrollPane(supportTable); supportScroll.setMinimumSize(new Dimension(0, 0));
        supportPanel.add(supportScroll, BorderLayout.CENTER);
        supportNotice.setEditable(false); supportNotice.setLineWrap(true); supportNotice.setWrapStyleWord(true);
        supportNotice.setOpaque(false); supportNotice.setFocusable(false); supportNotice.setVisible(false);
        JPanel content = new JPanel(new BorderLayout(0, JBUI.scale(6)));
        content.add(supportNotice, BorderLayout.NORTH); content.add(tabs, BorderLayout.CENTER);
        stagedSummary.setVisible(false); content.add(stagedSummary, BorderLayout.SOUTH);
        panel.add(content, BorderLayout.CENTER);
        JPanel options = new JPanel(new GridLayout(0, 1)); options.add(replace); options.add(properties);
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
            StudioProjectFiles.saveConfigurationDocuments(project, List.of("pom.xml", org.ikasan.studio.core.generator.ProjectArtifactRenderer.MODEL, "src/main/model/model.json"));
            clearAnalysis();
            var destination = Path.of(java.util.Objects.requireNonNull(project.getBasePath(), "Import requires a project directory"));
            var inspection = work(() -> SourceCheckoutIdentity.inspect(selected, destination, ProgressManager::checkCanceled), true);
            if (inspection == null || project.isDisposed() || isDisposed()) return;
            var candidate = inspection.automaticCandidate();
            String directory;
            if (candidate == null) {
                var chooser = new SourceModuleSelectionDialog(project, getContentPane(), inspection);
                if (!chooser.showAndGet() || project.isDisposed() || isDisposed()) return;
                directory = chooser.selectedDirectory();
            } else directory = candidate.directory();
            var result = work(() -> SourceModuleImport.analyse(selected, destination, directory, ProgressManager::checkCanceled), true);
            if (result == null || project.isDisposed() || isDisposed()) return;
            acceptAnalysis(result, selected);
        } finally { analysing = false; }
    }

    private void clearAnalysis() {
        analysis = null; analysedSource = null; selectedFiles.clear(); selectedDependencies.clear();
        discardMerges();
        identitySummary.setVisible(false); acceptIdentity.setVisible(false); acceptIdentity.setSelected(false);
        supportNotice.setVisible(false);
        if (reviewTabs.indexOfComponent(supportPanel) >= 0) reviewTabs.remove(supportPanel);
        setOKActionEnabled(false);
        choices.fireTableDataChanged(); supportChoices.fireTableDataChanged(); report.setText("");
    }

    private void showAnalysisProblem(String message) {
        identitySummary.setForeground(com.intellij.ui.JBColor.namedColor("Label.errorForeground", com.intellij.ui.JBColor.RED));
        identitySummary.setText(StudioBundle.message("sourceImport.analysisProblem", message));
        identitySummary.setVisible(true); setOKActionEnabled(false);
    }

    void acceptAnalysis(SourceModuleImport.Analysis result, Path selected) {
        source.setText(selected.toString());
        analysis = result; analysedSource = selected; lastAttempt = source.getText().trim();
        discardMerges();
        selectedFiles.clear(); selectedDependencies.clear(); replace.setSelected(false);
        acceptIdentity.setSelected(false); acceptIdentity.setVisible(result.identity().requiresApproval());
        var check = result.identity();
        String expected = check.expected().isBlank() ? StudioBundle.message("sourceImport.identity.unknownName") : check.expected();
        String names = check.names().isEmpty() ? StudioBundle.message("sourceImport.identity.unknownName") : String.join(", ", check.names());
        String scope = check.candidate() == null || check.candidate().directory().isEmpty() ? "." : check.candidate().directory();
        identitySummary.setForeground(check.requiresApproval()
                ? com.intellij.ui.JBColor.namedColor("Label.errorForeground", com.intellij.ui.JBColor.RED)
                : com.intellij.util.ui.UIUtil.getLabelForeground());
        identitySummary.setText(StudioBundle.message("sourceImport.identity." + check.status().name(), expected, names, scope)
                + "\n" + StudioBundle.message("sourceImport.identity.references", check.matchedImplementations().size()));
        identitySummary.setVisible(check.requiresApproval());
        if (!check.requiresApproval()) selectDefaults();
        setOKActionEnabled(!check.requiresApproval());
        choices.fireTableDataChanged(); refreshReport();
    }

    private void selectDefaults() {
        analysis.dependencies().stream().filter(SourceModuleImport.DependencyChoice::importable)
                .forEach(dependency -> selectedDependencies.add(dependency.id()));
    }

    private void refreshReport() {
        if (analysis == null) return;
        boolean hasSupport = !analysis.managedFiles().isEmpty();
        supportNotice.setVisible(hasSupport);
        if (hasSupport) {
            supportNotice.setText(StudioBundle.message("sourceImport.support.notice", analysis.managedFiles().size()));
            if (reviewTabs.indexOfComponent(supportPanel) < 0)
                reviewTabs.insertTab(StudioBundle.message("sourceImport.support.title"), null, supportPanel, null, 1);
        } else if (reviewTabs.indexOfComponent(supportPanel) >= 0) reviewTabs.remove(supportPanel);
        supportChoices.fireTableDataChanged();
        stagedSummary.setVisible(!stagedJava.isEmpty());
        stagedSummary.setText(StudioBundle.message("sourceImport.stagedSummary", stagedJava.size()));
        report.setText(SourceModuleImport.report(analysis, Set.copyOf(selectedFiles), Set.copyOf(selectedDependencies)));
        report.setCaretPosition(0);
    }

    @Override protected @Nullable ValidationInfo doValidate() {
        if (analysis == null || !source.getText().trim().equals(analysedSource.toString()))
            return new ValidationInfo(StudioBundle.message("sourceImport.analyseFirst"), source);
        if (analysis.identity().requiresApproval() && !acceptIdentity.isSelected())
            return new ValidationInfo(StudioBundle.message("sourceImport.identity.approvalRequired"), acceptIdentity);
        if (!replace.isSelected() && SourceModuleImport.importableFiles(analysis).stream().anyMatch(f -> selectedFiles.contains(f.source())
                && f.previous() != null && !f.previous().equals(plannedBytes(f))))
            return new ValidationInfo(StudioBundle.message("sourceImport.replaceRequired"), replace);
        return null;
    }

    @Override protected void doOKAction() {
        if (table.isEditing()) table.getCellEditor().stopCellEditing();
        if (supportTable.isEditing()) supportTable.getCellEditor().stopCellEditing();
        var validation = doValidate(); if (validation != null) { error(validation.message); return; }
        var context = project.getService(UiContext.class);
        if (!context.tryBeginMigration()) { error(StudioBundle.message("message.WaitForTheCurrentGenerationOrMigrationToFinish")); return; }
        Path savedReport;
        try {
            List<String> targets = new ArrayList<>(SourceModuleImport.importableFiles(analysis).stream().filter(f -> selectedFiles.contains(f.source())).map(SourceModuleImport.FileChoice::destination).toList());
            targets.add(SourceModuleImport.SUPPORT_CUSTOMISATIONS); targets.add("pom.xml"); StudioProjectFiles.saveConfigurationDocuments(project, targets);
            var files = Set.copyOf(selectedFiles); var dependencies = Set.copyOf(selectedDependencies); boolean replaceFiles = replace.isSelected();
            boolean identityApproved = acceptIdentity.isSelected();
            var merges = stagedJava.entrySet().stream().filter(entry -> files.contains(entry.getKey()))
                    .collect(java.util.stream.Collectors.toUnmodifiableMap(Map.Entry::getKey, Map.Entry::getValue));
            savedReport = work(() -> SourceModuleImport.apply(analysis, files, dependencies, replaceFiles, identityApproved, merges), false);
        } finally { context.endMigration(); }
        if (savedReport == null) return;
        StudioProjectFiles.refreshConfigurationDirectory(project);
        boolean importProperties = properties.isSelected();
        var discovered = analysis.properties(); close(OK_EXIT_CODE);
        ApplicationManager.getApplication().invokeLater(() -> {
            if (project.isDisposed()) return;
            Messages.showInfoMessage(project, StudioBundle.message("sourceImport.complete", savedReport.toString()), StudioBundle.message("sourceImport.title"));
            if (importProperties) new ImportEnvironmentPropertiesDialog(project, discovered).show();
        }, ModalityState.nonModal());
    }

    private void registerComparison(JBTable target) {
        target.setToolTipText(StudioBundle.message("sourceImport.compareGesture"));
        target.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseClicked(java.awt.event.MouseEvent event) {
                if (event.getClickCount() != 2 || !SwingUtilities.isLeftMouseButton(event)) return;
                int row = target.rowAtPoint(event.getPoint()), column = target.columnAtPoint(event.getPoint());
                if (row < 0 || column < 0 || (target == table && target.convertColumnIndexToModel(column) == 0
                        || target == supportTable && target.convertColumnIndexToModel(column) == 1)) return;
                target.setRowSelectionInterval(row, row);
                compare(target);
            }
        });
        target.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_ENTER, 0), "studio.compareSource");
        target.getActionMap().put("studio.compareSource", new AbstractAction() {
            @Override public void actionPerformed(java.awt.event.ActionEvent event) { compare(target); }
        });
    }

    private void compare(JTable selectedTable) {
        var request = comparison(selectedTable);
        if (request == null) return;
        if (request.getContents().get(1) instanceof com.intellij.diff.contents.DocumentContent destination
                && destination.getDocument().isWritable())
            new SourceComparisonDialog(project, getContentPane(), request).show();
        else com.intellij.diff.DiffManager.getInstance().showDiff(project, request);
    }

    /** Build a read-only diff from the reviewed snapshots, preserving sorted-table row mapping. */
    @Nullable com.intellij.diff.requests.SimpleDiffRequest comparison(JTable selectedTable) {
        if (analysis == null || selectedTable.getSelectedRow() < 0) return null;
        int row = selectedTable.convertRowIndexToModel(selectedTable.getSelectedRow());
        if (selectedTable == supportTable) {
            var support = analysis.managedFiles().get(row);
            if (!analysis.identity().requiresApproval() || acceptIdentity.isSelected())
                return editableComparison(supportChoice(support));
            return comparison(support.destination(), utf8(support.bytes()), support.current() == null ? "" : utf8(support.current()),
                    StudioBundle.message("sourceImport.support.originalVersion"), StudioBundle.message("sourceImport.support.destinationVersion"));
        }
        if (row < analysis.files().size()) {
            var file = analysis.files().get(row);
            if (file.source().endsWith(".java") && (!analysis.identity().requiresApproval() || acceptIdentity.isSelected()))
                return editableComparison(file);
            return comparison(file.destination(), utf8(file.bytes()), file.previous() == null ? "" : utf8(file.previous()),
                    StudioBundle.message("sourceImport.originalVersion"), StudioBundle.message("sourceImport.destinationVersion"));
        }
        var dependency = analysis.dependencies().get(row - analysis.files().size());
        var values = work(() -> SourceModuleImport.compareDependency(analysis, dependency.id()), false);
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
    private com.intellij.diff.requests.SimpleDiffRequest editableComparison(SourceModuleImport.FileChoice file) {
        var factory = com.intellij.diff.DiffContentFactory.getInstance();
        var left = factory.create(utf8(file.bytes()), com.intellij.ide.highlighter.JavaFileType.INSTANCE);
        left.getDocument().setReadOnly(true);
        var right = mergeDocuments.computeIfAbsent(file.source(), key -> {
            var content = factory.create(selectedFiles.contains(file.source()) ? utf8(plannedBytes(file)) : file.previous() == null ? "" : utf8(file.previous()), com.intellij.ide.highlighter.JavaFileType.INSTANCE);
            var document = content.getDocument(); document.setReadOnly(false);
            String baseline = file.previous() == null ? "" : utf8(file.previous()).replace("\r\n", "\n").replace("\r", "\n");
            String originalText = file.previous() == null ? utf8(file.bytes()) : utf8(file.previous());
            int newline = originalText.indexOf('\n');
            String separator = newline > 0 && originalText.charAt(newline - 1) == '\r' ? "\r\n"
                    : newline < 0 && originalText.indexOf('\r') >= 0 ? "\r" : "\n";
            var reviewed = analysis;
            document.addDocumentListener(new com.intellij.openapi.editor.event.DocumentListener() {
                @Override public void documentChanged(@org.jetbrains.annotations.NotNull com.intellij.openapi.editor.event.DocumentEvent event) {
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
        @org.jetbrains.annotations.Nls String title = file.destination();
        return new com.intellij.diff.requests.SimpleDiffRequest(title, left, right,
                StudioBundle.message("sourceImport.originalVersion"), StudioBundle.message("sourceImport.mergeDestination"));
    }

    private com.intellij.diff.requests.SimpleDiffRequest comparison(@org.jetbrains.annotations.Nls String title,
            String before, String after, @org.jetbrains.annotations.Nls String beforeTitle, @org.jetbrains.annotations.Nls String afterTitle) {
        var factory = com.intellij.diff.DiffContentFactory.getInstance();
        var left = factory.create(before); var right = factory.create(after);
        left.getDocument().setReadOnly(true); right.getDocument().setReadOnly(true);
        var request = new com.intellij.diff.requests.SimpleDiffRequest(title, left, right, beforeTitle, afterTitle);
        request.putUserData(com.intellij.diff.util.DiffUserDataKeys.FORCE_READ_ONLY, true);
        return request;
    }
    private static String utf8(String encoded) { return new String(Base64.getDecoder().decode(encoded), StandardCharsets.UTF_8); }
    private interface Work<T> { T run() throws Exception; }
    private <T> T work(Work<T> work, boolean cancellable) {
        var result = new AtomicReference<T>(); var failure = new AtomicReference<Exception>();
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

    private void copyFile(SourceModuleImport.FileChoice file, boolean copy) {
        String id = file.source();
        stagedJava.remove(id);
        var content = mergeDocuments.get(id);
        if (content != null) {
            String text = copy ? utf8(file.bytes()) : file.previous() == null ? "" : utf8(file.previous());
            com.intellij.openapi.application.WriteAction.run(() -> content.getDocument().setText(text.replace("\r\n", "\n").replace("\r", "\n")));
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
            var file = analysis.managedFiles().get(row);
            return switch (column) {
                case 0 -> file.source();
                case 1 -> selectedFiles.contains(file.source());
                default -> file.destination() + " — " + StudioBundle.message("sourceImport.support.status." + file.status().name());
            };
        }
        @Override public void setValueAt(Object value, int row, int column) {
            copyFile(supportChoice(analysis.managedFiles().get(row)), Boolean.TRUE.equals(value));
            fireTableRowsUpdated(row, row); refreshReport();
        }
    }

    private static final class CopyButtonEditor extends AbstractCellEditor implements javax.swing.table.TableCellEditor {
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
        @Override public int getRowCount() { return analysis == null ? 0 : analysis.files().size() + analysis.dependencies().size(); }
        @Override public int getColumnCount() { return 3; }
        @Override public String getColumnName(int column) { return StudioBundle.message(new String[]{"sourceImport.copyAction", "sourceImport.evidence", "sourceImport.destination"}[column]); }
        @Override public Class<?> getColumnClass(int column) { return column == 0 ? Boolean.class : String.class; }
        @Override public boolean isCellEditable(int row, int column) { return column == 0 && analysis != null && (!analysis.identity().requiresApproval() || acceptIdentity.isSelected())
                && (row < analysis.files().size() || analysis.dependencies().get(row - analysis.files().size()).importable()); }
        @Override public Object getValueAt(int row, int column) {
            if (row < analysis.files().size()) {
                var file = analysis.files().get(row);
                return switch (column) {
                    case 0 -> selectedFiles.contains(file.source());
                    case 2 -> file.destination();
                    default -> file.source() + (stagedJava.containsKey(file.source()) ? " " + StudioBundle.message("sourceImport.stagedMarker") : "") + " — " + file.reason() + (file.previous() != null && !file.previous().equals(file.bytes()) ? " " + StudioBundle.message("sourceImport.conflict") : "");
                };
            }
            var dependency = analysis.dependencies().get(row - analysis.files().size());
            return switch (column) {
                case 0 -> selectedDependencies.contains(dependency.id());
                case 2 -> StudioBundle.message(dependency.importable() ? "sourceImport.dependency.addToPom" : "sourceImport.dependency.manual");
                default -> dependency.id() + " — " + dependency.reason();
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
            fireTableRowsUpdated(row, row); refreshReport();
        }
    }
}
