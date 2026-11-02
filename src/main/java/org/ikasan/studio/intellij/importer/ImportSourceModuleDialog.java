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
    private final JBTextArea report = new JBTextArea(18, 85);
    private final Choices choices = new Choices();
    private final JBTable table = new JBTable(choices);
    private SourceModuleImport.Analysis analysis;
    private Path analysedSource;
    private final Set<String> selectedFiles = new LinkedHashSet<>(), selectedDependencies = new LinkedHashSet<>();

    public ImportSourceModuleDialog(Project project) {
        super(project, project.getService(UiContext.class).getDesignerCanvas());
        this.project = project;
        setTitle(StudioBundle.message("sourceImport.title"));
        setOKButtonText(StudioBundle.message("sourceImport.apply"));
        init();
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
                    if (!isDisposed() && !project.isDisposed() && paths != null && !paths.isEmpty()) source.setText(paths.get(0));
                }));
        path.add(browse, BorderLayout.EAST); top.add(path, BorderLayout.CENTER);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        JButton analyse = new JButton(StudioBundle.message("sourceImport.analyse")); analyse.addActionListener(event -> analyse());
        actions.add(analyse);
        actions.add(Box.createHorizontalStrut(JBUI.scale(8)));
        JButton compare = new JButton(StudioBundle.message("sourceImport.compare")); compare.addActionListener(event -> compare()); actions.add(compare);
        top.add(actions, BorderLayout.SOUTH); panel.add(top, BorderLayout.NORTH);
        JTabbedPane tabs = new JBTabbedPane();
        var files = new JBScrollPane(table); files.setMinimumSize(new Dimension(0, 0));
        table.setAutoCreateRowSorter(true);
        table.getColumnModel().getColumn(0).setMaxWidth(JBUI.scale(65));
        table.getAccessibleContext().setAccessibleName(StudioBundle.message("sourceImport.files"));
        tabs.addTab(StudioBundle.message("sourceImport.files"), files);
        report.setEditable(false); report.setLineWrap(true); report.setWrapStyleWord(true);
        report.getAccessibleContext().setAccessibleName(StudioBundle.message("sourceImport.review"));
        var scroll = new JBScrollPane(report); scroll.setMinimumSize(new Dimension(0, 0));
        tabs.addTab(StudioBundle.message("sourceImport.review"), scroll);
        panel.add(tabs, BorderLayout.CENTER);
        JPanel options = new JPanel(new GridLayout(0, 1)); options.add(replace); options.add(properties);
        panel.add(options, BorderLayout.SOUTH); panel.setPreferredSize(JBUI.size(920, 650));
        return panel;
    }

    private void analyse() {
        Path selected;
        try { selected = Path.of(source.getText().trim()).toAbsolutePath().normalize(); }
        catch (RuntimeException failure) { error(StudioBundle.message("sourceImport.chooseCheckout")); return; }
        if (source.getText().isBlank()) { error(StudioBundle.message("sourceImport.chooseCheckout")); return; }
        StudioProjectFiles.saveConfigurationDocuments(project, List.of("pom.xml"));
        var result = work(() -> SourceModuleImport.analyse(selected, Path.of(java.util.Objects.requireNonNull(project.getBasePath(), "Import requires a project directory")), ProgressManager::checkCanceled), true);
        if (result == null || project.isDisposed() || isDisposed()) return;
        source.setText(selected.toString());
        analysis = result; analysedSource = selected; selectedFiles.clear(); selectedDependencies.clear(); replace.setSelected(false);
        result.files().stream().filter(f -> f.recommended() && (f.previous() == null || f.previous().equals(f.bytes())))
                .forEach(f -> selectedFiles.add(f.source()));
        choices.fireTableDataChanged(); refreshReport();
    }

    private void refreshReport() {
        if (analysis == null) return;
        report.setText(SourceModuleImport.report(analysis, Set.copyOf(selectedFiles), Set.copyOf(selectedDependencies)));
        report.setCaretPosition(0);
    }

    @Override protected @Nullable ValidationInfo doValidate() {
        if (analysis == null || !source.getText().trim().equals(analysedSource.toString()))
            return new ValidationInfo(StudioBundle.message("sourceImport.analyseFirst"), source);
        if (!replace.isSelected() && analysis.files().stream().anyMatch(f -> selectedFiles.contains(f.source())
                && f.previous() != null && !f.previous().equals(f.bytes())))
            return new ValidationInfo(StudioBundle.message("sourceImport.replaceRequired"), replace);
        return null;
    }

    @Override protected void doOKAction() {
        if (table.isEditing()) table.getCellEditor().stopCellEditing();
        var validation = doValidate(); if (validation != null) { error(validation.message); return; }
        var context = project.getService(UiContext.class);
        if (!context.tryBeginMigration()) { error(StudioBundle.message("message.WaitForTheCurrentGenerationOrMigrationToFinish")); return; }
        Path savedReport;
        try {
            List<String> targets = new ArrayList<>(analysis.files().stream().filter(f -> selectedFiles.contains(f.source())).map(SourceModuleImport.FileChoice::destination).toList());
            targets.add("pom.xml"); StudioProjectFiles.saveConfigurationDocuments(project, targets);
            var files = Set.copyOf(selectedFiles); var dependencies = Set.copyOf(selectedDependencies); boolean replaceFiles = replace.isSelected();
            savedReport = work(() -> SourceModuleImport.apply(analysis, files, dependencies, replaceFiles), false);
        } finally { context.endMigration(); }
        if (savedReport == null) return;
        StudioProjectFiles.refreshConfigurationDirectory(project);
        boolean importProperties = properties.isSelected() && !analysis.properties().isEmpty();
        var discovered = analysis.properties(); close(OK_EXIT_CODE);
        ApplicationManager.getApplication().invokeLater(() -> {
            if (project.isDisposed()) return;
            Messages.showInfoMessage(project, StudioBundle.message("sourceImport.complete", savedReport.toString()), StudioBundle.message("sourceImport.title"));
            if (importProperties) new ImportEnvironmentPropertiesDialog(project, discovered).show();
        }, ModalityState.nonModal());
    }

    private void compare() {
        if (analysis == null) return;
        int row = table.getSelectedRow(); if (row < 0) return;
        row = table.convertRowIndexToModel(row);
        String before, after;
        @com.intellij.openapi.util.NlsSafe
        String title;
        if (row < analysis.files().size()) {
            var file = analysis.files().get(row); title = file.destination();
            before = file.previous() == null ? "" : utf8(file.previous()); after = utf8(file.bytes());
        } else {
            title = "pom.xml"; before = utf8(analysis.originalPom());
            after = work(() -> SourceModuleImport.pomAfter(analysis, Set.copyOf(selectedDependencies)), false); if (after == null) return;
        }
        var factory = com.intellij.diff.DiffContentFactory.getInstance();
        com.intellij.diff.DiffManager.getInstance().showDiff(project, new com.intellij.diff.requests.SimpleDiffRequest(title,
                factory.create(before), factory.create(after), StudioBundle.message("sourceImport.before"), StudioBundle.message("sourceImport.after")));
    }
    private static String utf8(String encoded) { return new String(Base64.getDecoder().decode(encoded), StandardCharsets.UTF_8); }
    private interface Work<T> { T run() throws Exception; }
    private <T> T work(Work<T> work, boolean cancellable) {
        var result = new AtomicReference<T>(); var failure = new AtomicReference<Exception>();
        boolean finished = ProgressManager.getInstance().runProcessWithProgressSynchronously(() -> {
            try { result.set(work.run()); } catch (ProcessCanceledException cancelled) { throw cancelled; } catch (Exception problem) { failure.set(problem); }
        }, StudioBundle.message("sourceImport.analyse"), cancellable, project);
        if (failure.get() != null) error(failure.get().getMessage());
        return finished ? result.get() : null;
    }
    private void error(String message) { Messages.showErrorDialog(getContentPane() == null ? source : getContentPane(), message, StudioBundle.message("sourceImport.title")); }

    private final class Choices extends AbstractTableModel {
        @Override public int getRowCount() { return analysis == null ? 0 : analysis.files().size() + analysis.dependencies().size(); }
        @Override public int getColumnCount() { return 3; }
        @Override public String getColumnName(int column) { return StudioBundle.message(new String[]{"sourceImport.selected", "sourceImport.destination", "sourceImport.evidence"}[column]); }
        @Override public Class<?> getColumnClass(int column) { return column == 0 ? Boolean.class : String.class; }
        @Override public boolean isCellEditable(int row, int column) { return column == 0 && (row < analysis.files().size() || analysis.dependencies().get(row - analysis.files().size()).importable()); }
        @Override public Object getValueAt(int row, int column) {
            if (row < analysis.files().size()) {
                var file = analysis.files().get(row);
                return switch (column) {
                    case 0 -> selectedFiles.contains(file.source());
                    case 1 -> file.destination();
                    default -> file.source() + " — " + file.reason() + (file.previous() != null && !file.previous().equals(file.bytes()) ? " " + StudioBundle.message("sourceImport.conflict") : "");
                };
            }
            var dependency = analysis.dependencies().get(row - analysis.files().size());
            return switch (column) { case 0 -> selectedDependencies.contains(dependency.id()); case 1 -> "pom.xml: " + dependency.id(); default -> dependency.reason(); };
        }
        @Override public void setValueAt(Object value, int row, int column) {
            Set<String> selection; String id;
            if (row < analysis.files().size()) { selection = selectedFiles; id = analysis.files().get(row).source(); }
            else { selection = selectedDependencies; id = analysis.dependencies().get(row - analysis.files().size()).id(); }
            if (Boolean.TRUE.equals(value)) selection.add(id); else selection.remove(id);
            fireTableCellUpdated(row, column); refreshReport();
        }
    }
}
