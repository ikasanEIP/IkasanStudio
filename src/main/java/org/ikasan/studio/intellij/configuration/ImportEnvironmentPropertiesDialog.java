package org.ikasan.studio.intellij.configuration;

import com.intellij.diff.DiffContentFactory;
import com.intellij.diff.contents.DocumentContent;
import com.intellij.diff.requests.SimpleDiffRequest;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.ui.components.*;
import com.intellij.ui.table.JBTable;
import com.intellij.util.ui.JBUI;
import org.ikasan.studio.core.configuration.EnvironmentProperties;
import org.ikasan.studio.intellij.execution.IkasanRunConfigurationService;
import org.ikasan.studio.intellij.importer.SourceComparisonDialog;
import org.ikasan.studio.intellij.project.StudioProjectFiles;
import org.ikasan.studio.ui.StudioBundle;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/** Original/destination comparison with explicit Copy all or Save preview approval before any write. */
public final class ImportEnvironmentPropertiesDialog extends org.ikasan.studio.intellij.project.ImportDialogWrapper {
    private final Project project;
    private final Path root;
    private final List<org.ikasan.studio.core.importer.SourceModuleImport.PropertyFile> discovered;
    private final JBTextArea contents = new JBTextArea(12, 75);
    private final FilesModel filesModel = new FilesModel();
    private final JBTable files = new JBTable(filesModel);
    private String sourceName = StudioBundle.message("environment.pasted");
    private EnvironmentProperties.Preview snapshot;
    private String pending;
    private DocumentContent destinationContent;

    public ImportEnvironmentPropertiesDialog(Project project) {
        this(project, Path.of(java.util.Objects.requireNonNull(project.getBasePath(), "Import requires a project directory")));
    }

    public ImportEnvironmentPropertiesDialog(Project project, List<org.ikasan.studio.core.importer.SourceModuleImport.PropertyFile> discovered) {
        this(project, Path.of(java.util.Objects.requireNonNull(project.getBasePath(), "Import requires a project directory")), discovered);
    }

    ImportEnvironmentPropertiesDialog(Project project, Path root) { this(project, root, List.of()); }

    private ImportEnvironmentPropertiesDialog(Project project, Path root,
            List<org.ikasan.studio.core.importer.SourceModuleImport.PropertyFile> discovered) {
        super(project, project.getService(org.ikasan.studio.ui.UiContext.class).getDesignerCanvas());
        this.project = project;
        this.root = root;
        this.discovered = List.copyOf(discovered);
        contents.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            @Override public void insertUpdate(javax.swing.event.DocumentEvent event) { changed(); }
            @Override public void removeUpdate(javax.swing.event.DocumentEvent event) { changed(); }
            @Override public void changedUpdate(javax.swing.event.DocumentEvent event) { changed(); }
            private void changed() {
                sourceName = StudioBundle.message("environment.pasted");
                discardSelection();
            }
        });
        setTitle(StudioBundle.message("environment.title"));
        setOKButtonText(StudioBundle.message("environment.import"));
        init();
        refreshRow();
    }

    @Override protected @NotNull JComponent createCenterPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, JBUI.scale(8)));
        panel.add(new JBLabel(StudioBundle.message("environment.help")), BorderLayout.NORTH);
        JPanel editor = new JPanel(new BorderLayout(0, JBUI.scale(6)));
        JPanel fields = new JPanel(new GridLayout(0, 1, 0, JBUI.scale(4)));
        JButton choose = new JButton(StudioBundle.message("environment.choose"));
        choose.addActionListener(event -> chooseFile());
        fields.add(choose);
        if (!discovered.isEmpty()) {
            var sources = new com.intellij.openapi.ui.ComboBox<>(discovered.stream()
                    .map(org.ikasan.studio.core.importer.SourceModuleImport.PropertyFile::source).toArray(String[]::new));
            sources.getAccessibleContext().setAccessibleName(StudioBundle.message("sourceImport.discoveredProperties"));
            fields.add(sources);
            JButton use = new JButton(StudioBundle.message("sourceImport.useProperties"));
            use.addActionListener(event -> {
                int index = sources.getSelectedIndex();
                if (index >= 0) {
                    var file = discovered.get(index);
                    loadSource(file.source(), file.text());
                }
            });
            fields.add(use);
        }
        JBLabel label = new JBLabel(StudioBundle.message("environment.contents"));
        label.setLabelFor(contents);
        fields.add(label);
        editor.add(fields, BorderLayout.NORTH);
        contents.getAccessibleContext().setAccessibleName(StudioBundle.message("environment.contents"));
        var contentScroll = new JBScrollPane(contents);
        contentScroll.setMinimumSize(new Dimension(0, 0));
        editor.add(contentScroll, BorderLayout.CENTER);
        panel.add(editor, BorderLayout.CENTER);

        JPanel destination = new JPanel(new BorderLayout(0, JBUI.scale(6)));
        files.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        files.setRowHeight(Math.max(files.getRowHeight(), new JButton().getPreferredSize().height + JBUI.scale(2)));
        files.getAccessibleContext().setAccessibleName(StudioBundle.message("environment.files"));
        files.getColumnModel().getColumn(1).setCellRenderer((table, value, selected, focus, row, column) ->
                new JButton(StudioBundle.message(Boolean.TRUE.equals(value) ? "sourceImport.revert" : "sourceImport.copyAll")));
        files.getColumnModel().getColumn(1).setCellEditor(new CopyButtonEditor());
        files.getColumnModel().getColumn(1).setMaxWidth(JBUI.scale(100));
        files.getColumnModel().getColumn(1).setPreferredWidth(JBUI.scale(90));
        files.getColumnModel().getColumn(0).setPreferredWidth(JBUI.scale(310));
        files.getColumnModel().getColumn(2).setPreferredWidth(JBUI.scale(380));
        files.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent event) {
                if (event.getClickCount() == 2 && files.rowAtPoint(event.getPoint()) >= 0
                        && files.columnAtPoint(event.getPoint()) != 1) compare();
            }
        });
        files.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "compareProperties");
        files.getActionMap().put("compareProperties", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent event) { compare(); }
        });
        var tableScroll = new JBScrollPane(files);
        tableScroll.setPreferredSize(JBUI.size(780, 70));
        String guidance = StudioBundle.message("environment.compareHelp");
        var instruction = new JBLabel("<html><body style='width: " + JBUI.scale(760) + "px'>"
                + com.intellij.openapi.util.text.StringUtil.escapeXmlEntities(guidance) + "</body></html>");
        files.getAccessibleContext().setAccessibleDescription(guidance);
        destination.add(instruction, BorderLayout.NORTH);
        destination.add(tableScroll, BorderLayout.CENTER);
        panel.add(destination, BorderLayout.SOUTH);
        panel.setPreferredSize(JBUI.size(800, 480));
        return panel;
    }

    private void loadSource(String name, String text) {
        contents.setText(text);
        contents.setCaretPosition(0);
        sourceName = name;
        discardSelection();
    }

    private void chooseFile() {
        StudioProjectFiles.chooseFileAndReadProperties(project,
                new FileChooserDescriptor(true, false, false, false, false, false)
                        .withFileFilter(file -> file.getName().endsWith(".properties"))
                        .withTitle(StudioBundle.message("environment.choose")), result -> {
                    if (isDisposed() || project.isDisposed()) return;
                    if (result.content() == null) { error(result.errorMessage()); return; }
                    loadSource(result.path(), result.content());
                });
    }

    private void discardSelection() {
        if (destinationContent != null) destinationContent.getDocument().setReadOnly(true);
        destinationContent = null;
        snapshot = null;
        pending = null;
        refreshRow();
    }

    private void refreshRow() {
        filesModel.fireTableDataChanged();
        if (filesModel.getRowCount() > 0) files.setRowSelectionInterval(0, 0);
        setOKActionEnabled(pending != null);
    }

    private boolean destinationUnavailable() {
        if (contents.getText().isBlank()) { error(StudioBundle.message("environment.empty")); return true; }
        if (snapshot != null) return false;
        StudioProjectFiles.saveConfigurationDocuments(project,
                List.of(EnvironmentProperties.USER_CONFIGURATION, EnvironmentProperties.APPLICATION_CONFIGURATION));
        snapshot = background(() -> EnvironmentProperties.previewDeveloperDestination(root));
        return snapshot == null;
    }

    private String validationProblem(String text) {
        String problem = background(() -> {
            try {
                EnvironmentProperties.validateImport(EnvironmentProperties.Selection.developerOwned(text));
                return "";
            } catch (Exception failure) { return failure.getMessage(); }
        });
        return problem == null ? StudioBundle.message("environment.validationFailed") : problem.isEmpty() ? null : problem;
    }

    private void copyAll() {
        if (destinationUnavailable()) return;
        String problem = validationProblem(contents.getText());
        if (problem != null) { error(problem); return; }
        pending = EnvironmentProperties.Selection.developerOwned(contents.getText()).text();
        if (destinationContent != null) destinationContent.getDocument().setReadOnly(true);
        destinationContent = null;
        refreshRow();
    }

    /** Immutable original left; isolated pending destination right. Neither document is a project file. */
    @Nullable SimpleDiffRequest comparison() {
        if (destinationUnavailable()) return null;
        var factory = DiffContentFactory.getInstance();
        var type = com.intellij.openapi.fileTypes.FileTypeManager.getInstance().getFileTypeByExtension("properties");
        var original = factory.create(EnvironmentProperties.Selection.developerOwned(contents.getText()).text(), type);
        original.getDocument().setReadOnly(true);
        if (destinationContent == null) {
            destinationContent = factory.create(pending != null ? pending : snapshot.previous() == null ? "" : snapshot.previous(), type);
            destinationContent.getDocument().setReadOnly(false);
        }
        return new SimpleDiffRequest(EnvironmentProperties.USER_CONFIGURATION, original, destinationContent,
                StudioBundle.message("environment.original", sourceName),
                StudioBundle.message("environment.destination", EnvironmentProperties.USER_CONFIGURATION));
    }

    private void compare() {
        var request = comparison();
        if (request == null) return;
        if (new SourceComparisonDialog(project, getContentPane(), request, this::validationProblem).showAndGet()) acceptComparison();
    }

    void acceptComparison() {
        String reference = snapshot.previous() == null ? contents.getText() : snapshot.previous();
        String separator = reference.contains("\r\n") ? "\r\n" : reference.contains("\r") && !reference.contains("\n") ? "\r" : "\n";
        pending = EnvironmentProperties.Selection.developerOwned(destinationContent.getDocument().getText()
                .replace("\n", separator)).text();
        refreshRow();
    }

    @Override protected @Nullable ValidationInfo doValidate() {
        return pending == null ? new ValidationInfo(StudioBundle.message("environment.selectChanges"), files) : null;
    }

    @Override protected void doOKAction() {
        if (files.isEditing()) files.getCellEditor().stopCellEditing();
        var validation = doValidate();
        if (validation != null) { error(validation.message); return; }
        StudioProjectFiles.saveConfigurationDocuments(project,
                List.of(EnvironmentProperties.USER_CONFIGURATION, EnvironmentProperties.APPLICATION_CONFIGURATION));
        var selected = new EnvironmentProperties.Preview(EnvironmentProperties.Selection.developerOwned(pending),
                snapshot.previous(), snapshot.changes(), snapshot.generatedOverrides(), snapshot.sharedOverrides(), snapshot.applicationPrevious());
        if (background(() -> { EnvironmentProperties.apply(root, List.of(selected), true); return Boolean.TRUE; }) == null) return;
        StudioProjectFiles.refreshConfigurationDirectory(project);
        super.doOKAction();
        com.intellij.openapi.application.ApplicationManager.getApplication().invokeLater(() -> {
            if (project.isDisposed()) return;
            if (selected.applicationPrevious() == null) {
                Messages.showInfoMessage(project, StudioBundle.message("environment.noApplication"),
                        StudioBundle.message("environment.title"));
                return;
            }
            project.getService(IkasanRunConfigurationService.class).prepareLocalConfiguration(success -> {
                if (!project.isDisposed()) Messages.showInfoMessage(project,
                        StudioBundle.message(success ? "environment.success" : "environment.noApplication"),
                        StudioBundle.message("environment.title"));
            });
        }, com.intellij.openapi.application.ModalityState.nonModal());
    }

    private final class FilesModel extends AbstractTableModel {
        @Override public int getRowCount() { return contents.getDocument().getLength() == 0 ? 0 : 1; }
        @Override public int getColumnCount() { return 3; }
        @Override public String getColumnName(int column) {
            return StudioBundle.message(new String[]{"environment.originalColumn", "environment.actionColumn", "environment.destinationColumn"}[column]);
        }
        @Override public Class<?> getColumnClass(int column) { return column == 1 ? Boolean.class : String.class; }
        @Override public Object getValueAt(int row, int column) {
            return switch (column) {
                case 0 -> sourceName;
                case 1 -> pending != null;
                default -> EnvironmentProperties.USER_CONFIGURATION;
            };
        }
        @Override public boolean isCellEditable(int row, int column) { return column == 1; }
        @Override public void setValueAt(Object value, int row, int column) {
            if (Boolean.TRUE.equals(value)) copyAll(); else discardSelection();
        }
    }

    private static final class CopyButtonEditor extends AbstractCellEditor implements TableCellEditor {
        private final JButton button = new JButton();
        private boolean selected;
        CopyButtonEditor() { button.addActionListener(event -> { selected = !selected; fireEditingStopped(); }); }
        @Override public Object getCellEditorValue() { return selected; }
        @Override public Component getTableCellEditorComponent(JTable table, Object value, boolean focused, int row, int column) {
            selected = Boolean.TRUE.equals(value);
            button.setText(StudioBundle.message(selected ? "sourceImport.revert" : "sourceImport.copyAll"));
            return button;
        }
    }

    private interface Work<T> { T run() throws Exception; }
    private <T> T background(Work<T> work) {
        AtomicReference<T> result = new AtomicReference<>();
        AtomicReference<Exception> failure = new AtomicReference<>();
        ProgressManager.getInstance().runProcessWithProgressSynchronously(() -> {
            try { result.set(work.run()); }
            catch (com.intellij.openapi.progress.ProcessCanceledException cancelled) { throw cancelled; }
            catch (Exception problem) { failure.set(problem); }
        }, StudioBundle.message("environment.title"), false, project);
        if (failure.get() != null) error(failure.get().getMessage());
        return result.get();
    }

    private void error(String message) {
        Messages.showErrorDialog(getContentPane(), message, StudioBundle.message("environment.title"));
    }
}
