package org.ikasan.studio.intellij.configuration;

import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.ui.components.*;
import com.intellij.util.ui.JBUI;
import org.ikasan.studio.core.configuration.EnvironmentProperties;
import org.ikasan.studio.intellij.execution.IkasanRunConfigurationService;
import org.ikasan.studio.intellij.project.StudioProjectFiles;
import org.ikasan.studio.ui.StudioBundle;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/** Stages independent files, previews key changes, and explicitly approves replacement before any write. */
public final class ImportEnvironmentPropertiesDialog extends org.ikasan.studio.intellij.project.ImportDialogWrapper {
    private final Project project;
    private final Path root;
    private final List<org.ikasan.studio.core.importer.SourceModuleImport.PropertyFile> discovered;
    private final JBTextField environment = new JBTextField();
    private final JBCheckBox local = new JBCheckBox(StudioBundle.message("environment.local"), true);
    private final JBCheckBox replace = new JBCheckBox(StudioBundle.message("environment.replace"));
    private final JBTextArea contents = new JBTextArea(8, 75);
    private final JBTextArea review = new JBTextArea(10, 75);
    private final com.intellij.openapi.ui.ComboBox<String> compareFiles = new com.intellij.openapi.ui.ComboBox<>();
    private final List<EnvironmentProperties.Preview> staged = new ArrayList<>();

    public ImportEnvironmentPropertiesDialog(Project project) {
        this(project, Path.of(java.util.Objects.requireNonNull(project.getBasePath(), "Import requires a project directory")));
    }

    public ImportEnvironmentPropertiesDialog(Project project, List<org.ikasan.studio.core.importer.SourceModuleImport.PropertyFile> discovered) {
        this(project, Path.of(java.util.Objects.requireNonNull(project.getBasePath(), "Import requires a project directory")), discovered);
    }

    ImportEnvironmentPropertiesDialog(Project project, Path root) {
        this(project, root, List.of());
    }

    private ImportEnvironmentPropertiesDialog(Project project, Path root, List<org.ikasan.studio.core.importer.SourceModuleImport.PropertyFile> discovered) {
        super(project, project.getService(org.ikasan.studio.ui.UiContext.class).getDesignerCanvas());
        this.project = project;
        this.root = root;
        this.discovered = List.copyOf(discovered);
        setTitle(StudioBundle.message("environment.title"));
        setOKButtonText(StudioBundle.message("environment.import"));
        init();
    }

    @Override protected @Nullable JComponent createCenterPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, JBUI.scale(8)));
        JBTextArea help = new JBTextArea(StudioBundle.message("environment.help"));
        help.setEditable(false);
        help.setLineWrap(true);
        help.setWrapStyleWord(true);
        help.setOpaque(false);
        help.setFocusable(false);
        panel.add(help, BorderLayout.NORTH);
        JPanel editor = new JPanel(new BorderLayout(0, JBUI.scale(6)));
        JPanel fields = new JPanel(new GridLayout(0, 1, 0, JBUI.scale(4)));
        JBLabel label = new JBLabel(StudioBundle.message("environment.name"));
        label.setLabelFor(environment);
        fields.add(label);
        fields.add(environment);
        fields.add(local);
        JButton choose = new JButton(StudioBundle.message("environment.choose"));
        choose.addActionListener(event -> chooseFile());
        fields.add(choose);
        if (!discovered.isEmpty()) {
            var sources = new com.intellij.openapi.ui.ComboBox<>(discovered.stream().map(org.ikasan.studio.core.importer.SourceModuleImport.PropertyFile::source).toArray(String[]::new));
            sources.getAccessibleContext().setAccessibleName(StudioBundle.message("sourceImport.discoveredProperties"));
            fields.add(sources);
            JButton use = new JButton(StudioBundle.message("sourceImport.useProperties"));
            use.addActionListener(event -> {
                int index = sources.getSelectedIndex();
                if (index >= 0) {
                    var file = discovered.get(index);
                    contents.setText(file.text()); contents.setCaretPosition(0); environment.setText(file.environment());
                }
            });
            fields.add(use);
        }
        editor.add(fields, BorderLayout.NORTH);
        contents.getAccessibleContext().setAccessibleName(StudioBundle.message("environment.contents"));
        var contentScroll = new JBScrollPane(contents);
        contentScroll.setMinimumSize(new Dimension(0, 0));
        editor.add(contentScroll, BorderLayout.CENTER);
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton add = new JButton(StudioBundle.message("environment.reviewFile"));
        add.addActionListener(event -> stage());
        buttons.add(add);
        JButton clear = new JButton(StudioBundle.message("environment.clear"));
        clear.addActionListener(event -> { staged.clear(); compareFiles.removeAllItems(); review.setText(""); replace.setSelected(false); });
        buttons.add(clear);
        editor.add(buttons, BorderLayout.SOUTH);
        review.setEditable(false);
        review.getAccessibleContext().setAccessibleName(StudioBundle.message("environment.preview"));
        JPanel preview = new JPanel(new BorderLayout());
        JPanel compareRow = new JPanel(new BorderLayout(JBUI.scale(6), 0));
        compareFiles.getAccessibleContext().setAccessibleName(StudioBundle.message("environment.preview"));
        compareRow.add(compareFiles, BorderLayout.CENTER);
        JButton compare = new JButton(StudioBundle.message("environment.compare"));
        compare.addActionListener(event -> compare());
        compareRow.add(compare, BorderLayout.EAST);
        preview.add(compareRow, BorderLayout.NORTH);
        var reviewScroll = new JBScrollPane(review);
        reviewScroll.setMinimumSize(new Dimension(0, 0));
        preview.add(reviewScroll, BorderLayout.CENTER);
        preview.add(replace, BorderLayout.SOUTH);
        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, editor, preview);
        split.setResizeWeight(0.55);
        panel.add(split, BorderLayout.CENTER);
        panel.setPreferredSize(JBUI.size(780, 650));
        return panel;
    }

    private void chooseFile() {
        StudioProjectFiles.chooseFileAndReadProperties(project,
                new FileChooserDescriptor(true, false, false, false, false, false)
                        .withFileFilter(file -> file.getName().endsWith(".properties"))
                        .withTitle(StudioBundle.message("environment.choose")), result -> {
                    if (isDisposed() || project.isDisposed()) return;
                    if (result.content() == null) { error(result.errorMessage()); return; }
                    contents.setText(result.content());
                    contents.setCaretPosition(0);
                    String name = Path.of(result.path()).getFileName().toString();
                    environment.setText(name.startsWith("application-")
                            ? name.substring("application-".length(), name.length() - ".properties".length()) : "");
                });
    }

    private void stage() {
        if (contents.getText().isBlank()) { error(StudioBundle.message("environment.empty")); return; }
        try {
            var selection = new EnvironmentProperties.Selection(contents.getText(), environment.getText(), local.isSelected());
            StudioProjectFiles.saveConfigurationDocuments(project, List.of(selection.relativePath(),
                    "generated/src/main/resources/application.properties", ".gitignore"));
            var result = background(() -> EnvironmentProperties.preview(root, selection));
            if (result == null) return;
            if (staged.stream().anyMatch(p -> p.selection().relativePath().equals(selection.relativePath()))) {
                error(StudioBundle.message("environment.duplicate", selection.relativePath())); return;
            }
            staged.add(result);
            compareFiles.addItem(selection.relativePath());
            compareFiles.setSelectedIndex(staged.size() - 1);
            replace.setSelected(false);
            var snapshot = List.copyOf(staged);
            String summary = background(() -> renderReview(snapshot));
            if (summary != null) review.setText(summary);
            review.setCaretPosition(0);
            contents.setText("");
        } catch (com.intellij.openapi.progress.ProcessCanceledException cancelled) { throw cancelled; }
        catch (Exception failure) { error(failure.getMessage()); }
    }

    private String renderReview(List<EnvironmentProperties.Preview> snapshots) throws java.io.IOException {
        StringBuilder text = new StringBuilder();
        for (var preview : snapshots) {
            text.append(preview.selection().relativePath()).append("\n");
            text.append(StudioBundle.message(preview.previous() == null ? "environment.newFile" : "environment.existingFile")).append("\n");
            for (var change : preview.changes()) text.append("  ").append(StudioBundle.message("environment.change." + change.kind()))
                    .append(": ").append(change.key()).append("\n");
            for (var key : preview.generatedOverrides()) text.append("  ").append(StudioBundle.message("environment.overrides"))
                    .append(": ").append(key).append("\n");
            java.util.Set<String> shared = new java.util.TreeSet<>(preview.sharedOverrides());
            for (var other : snapshots) {
                var lower = other.selection();
                var upper = preview.selection();
                boolean lowerPriority = !upper.environment().isEmpty() && lower.environment().isEmpty()
                        && (upper.localOnly() || !lower.localOnly())
                        || upper.localOnly() && !lower.localOnly() && upper.environment().equals(lower.environment());
                if (other != preview && lowerPriority) {
                    var keys = new java.util.TreeSet<>(EnvironmentProperties.parse(lower.text()).stringPropertyNames());
                    keys.retainAll(EnvironmentProperties.parse(upper.text()).stringPropertyNames());
                    shared.addAll(keys);
                }
            }
            for (String key : shared) text.append("  ").append(StudioBundle.message("environment.sharedOverride"))
                    .append(": ").append(key).append("\n");
            text.append("\n");
        }
        return text.toString();
    }

    @Override protected @Nullable ValidationInfo doValidate() {
        if (!contents.getText().isBlank()) return new ValidationInfo(StudioBundle.message("environment.pending"), contents);
        if (staged.isEmpty()) return new ValidationInfo(StudioBundle.message("environment.empty"), contents);
        if (!replace.isSelected() && staged.stream().anyMatch(p -> p.previous() != null))
            return new ValidationInfo(StudioBundle.message("environment.replaceRequired"), replace);
        return null;
    }

    @Override protected void doOKAction() {
        var validation = doValidate();
        if (validation != null) { error(validation.message); return; }
        StudioProjectFiles.saveConfigurationDocuments(project,
                java.util.stream.Stream.concat(staged.stream().map(p -> p.selection().relativePath()),
                        java.util.stream.Stream.of(".gitignore")).toList());
        boolean approved = replace.isSelected();
        if (background(() -> { EnvironmentProperties.apply(root, List.copyOf(staged), approved); return Boolean.TRUE; }) == null) return;
        StudioProjectFiles.refreshConfigurationDirectory(project);
        List<String> environments = staged.stream().map(p -> p.selection().environment()).distinct().toList();
        super.doOKAction();
        com.intellij.openapi.application.ApplicationManager.getApplication().invokeLater(() -> {
            if (project.isDisposed()) return;
            project.getService(IkasanRunConfigurationService.class).prepareEnvironments(environments, success -> {
                if (!project.isDisposed()) Messages.showInfoMessage(project,
                        StudioBundle.message(success ? "environment.success" : "environment.noApplication"),
                        StudioBundle.message("environment.title"));
            });
        }, com.intellij.openapi.application.ModalityState.nonModal());
    }

    private void compare() {
        int index = compareFiles.getSelectedIndex();
        if (index < 0) return;
        var preview = staged.get(index);
        StudioProjectFiles.saveConfigurationDocuments(project, List.of("generated/src/main/resources/application.properties"));
        String baseline = background(() -> EnvironmentProperties.comparisonBaseline(root, preview));
        if (baseline == null) return;
        var factory = com.intellij.diff.DiffContentFactory.getInstance();
        com.intellij.diff.DiffManager.getInstance().showDiff(project, new com.intellij.diff.requests.SimpleDiffRequest(
                preview.selection().relativePath(), factory.create(baseline), factory.create(preview.selection().text()),
                StudioBundle.message(preview.previous() == null ? "environment.generatedReference" : "environment.existingReference"),
                StudioBundle.message("environment.importedReference")));
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
