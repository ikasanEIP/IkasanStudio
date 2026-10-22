package org.ikasan.studio.intellij.metapack;

import org.jetbrains.annotations.NotNull;

import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Messages;
import com.intellij.ui.components.*;
import com.intellij.util.ui.JBUI;
import org.ikasan.studio.intellij.project.StudioProjectFiles;
import org.ikasan.studio.core.metapack.loading.MetaPackStore;
import org.ikasan.studio.ui.StudioBundle;
import javax.swing.*;
import java.awt.*;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** External-pack lifecycle UI; bundled packs continue to be managed by the plugin distribution. */
final class MetaPackManagerDialog extends DialogWrapper {
    private final Project project;
    private final Path directory = new StudioMetaPackDirectoryProvider().directory();
    private final DefaultListModel<MetaPackStore.Entry> model = new DefaultListModel<>();
    private final JBList<MetaPackStore.Entry> packs = new JBList<>(model);
    private final JButton update = button("metaPack.Update", this::chooseUpdate);
    private final JButton remove = button("metaPack.Remove", this::remove);
    private final JButton restore = button("metaPack.Restore", this::restore);
    private final JButton cancelPending = button("metaPack.CancelPending", this::cancelPending);

    MetaPackManagerDialog(Project project) {
        super(project, true);
        this.project = project;
        setTitle(StudioBundle.message("metaPack.Manage"));
        setOKButtonText(StudioBundle.message("metaPack.Close"));
        packs.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        packs.setCellRenderer((list, entry, index, selected, focus) -> {
            var renderer = new DefaultListCellRenderer();
            String text = entry.id() + " (" + entry.revision() + ")";
            if (entry.pending() != null) text += " — " + StudioBundle.message("metaPack.Pending." + entry.pending());
            if (entry.problem() != null) text += " — " + entry.problem();
            return renderer.getListCellRendererComponent(list, text, index, selected, focus);
        });
        packs.addListSelectionListener(event -> updateButtons());
        init();
        updateButtons();
    }

    @Override protected JComponent createCenterPanel() {
        JPanel panel = new JPanel(new BorderLayout(JBUI.scale(8), JBUI.scale(8)));
        JBTextArea note = new JBTextArea(StudioBundle.message("metaPack.ManageNote"));
        note.setEditable(false); note.setLineWrap(true); note.setWrapStyleWord(true); note.setOpaque(false);
        note.setFont(UIManager.getFont("Label.font")); note.setFocusable(false);
        panel.add(note, BorderLayout.NORTH);
        var scroll = new JBScrollPane(packs); scroll.setPreferredSize(JBUI.size(640, 240));
        packs.getEmptyText().setText(StudioBundle.message("metaPack.NoExternalPacks"));
        packs.getAccessibleContext().setAccessibleName(StudioBundle.message("metaPack.ExternalPacks"));
        panel.add(scroll, BorderLayout.CENTER);
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT));
        buttons.add(update); buttons.add(remove); buttons.add(restore); buttons.add(cancelPending);
        panel.add(buttons, BorderLayout.SOUTH);
        return panel;
    }
    @Override protected Action @NotNull [] createActions() { return new Action[]{getOKAction()}; }

    void reload() {
        perform(() -> {
            var entries = MetaPackUiTasks.run(project, StudioBundle.message("metaPack.Loading"), () -> MetaPackStore.list(directory));
            String selected = packs.getSelectedValue() == null ? null : packs.getSelectedValue().id();
            model.clear(); entries.forEach(model::addElement);
            for (int i = 0; i < model.size(); i++) if (model.get(i).id().equals(selected)) packs.setSelectedIndex(i);
            if (packs.getSelectedIndex() < 0 && !model.isEmpty()) packs.setSelectedIndex(0);
            updateButtons();
        });
    }
    private void updateButtons() {
        var selected = packs.getSelectedValue();
        boolean usable = selected != null && selected.problem() == null;
        boolean pending = usable && selected.pending() != null;
        update.setEnabled(usable && !pending); remove.setEnabled(usable && !pending);
        restore.setEnabled(usable && (!pending || "removed".equals(selected.pending())));
        cancelPending.setEnabled(selected != null && selected.pending() != null && !"removed".equals(selected.pending()));
    }
    private void chooseUpdate() {
        var selected = packs.getSelectedValue();
        if (selected == null) return;
        var descriptor = FileChooserDescriptorFactory.createSingleFileDescriptor("jar");
        descriptor.setTitle(StudioBundle.message("metaPack.Update"));
        StudioProjectFiles.chooseFilePaths(project, descriptor, files -> {
            if (isDisposed() || project.isDisposed() || files == null || files.isEmpty()) return;
            perform(() -> reviewUpdate(selected, Path.of(files.get(0))));
        });
    }
    private void reviewUpdate(MetaPackStore.Entry selected, Path source) throws Exception {
        var candidate = MetaPackUiTasks.run(project, StudioBundle.message("metaPack.Progress"), () -> MetaPackStore.review(source));
        if (!selected.id().equals(candidate.manifest().id())) throw new IllegalArgumentException(StudioBundle.message("metaPack.WrongId", selected.id()));
        String review = "removed".equals(selected.pending())
                ? StudioBundle.message("metaPack.ConfirmRestore", selected.id(), candidate.manifest().packVersion())
                : StudioBundle.message("metaPack.ConfirmUpdate", selected.id(), selected.revision(), candidate.manifest().packVersion());
        if (Messages.showYesNoDialog(project, review, StudioBundle.message("metaPack.Update"), Messages.getQuestionIcon()) != Messages.YES) return;
        if ("removed".equals(selected.pending())) {
            MetaPackUiTasks.run(project, StudioBundle.message("metaPack.Progress"), () ->
                    org.ikasan.studio.core.metapack.loading.MetaPackInstaller.install(source, directory, ProgressManager::checkCanceled, candidate));
        } else {
            MetaPackUiTasks.run(project, StudioBundle.message("metaPack.Progress"), () -> MetaPackStore.stageUpdate(source, selected.file(), directory,
                    ProgressManager::checkCanceled, candidate, selected.sha256()));
        }
        reload();
    }
    private void remove() {
        var selected = packs.getSelectedValue();
        if (selected == null) return;
        if (Messages.showYesNoDialog(project, StudioBundle.message("metaPack.ConfirmRemove", selected.id(), selected.revision()),
                StudioBundle.message("metaPack.Remove"), Messages.getWarningIcon()) != Messages.YES) return;
        perform(() -> {
            MetaPackUiTasks.run(project, StudioBundle.message("metaPack.Progress"), () -> {
                MetaPackStore.stageRemoval(selected.file(), directory, selected.sha256()); return null;
            });
            reload();
        });
    }
    private void cancelPending() {
        var selected = packs.getSelectedValue();
        if (selected == null) return;
        perform(() -> {
            MetaPackUiTasks.run(project, StudioBundle.message("metaPack.Progress"), () -> { MetaPackStore.cancel(directory, selected.id()); return null; });
            reload();
        });
    }
    private record Previous(String label, Path file) { @Override public @NotNull String toString() { return label; } }
    private void restore() {
        var selected = packs.getSelectedValue();
        if (selected == null) return;
        perform(() -> {
            List<Previous> previous = MetaPackUiTasks.run(project, StudioBundle.message("metaPack.Loading"), () -> {
                var result = new ArrayList<Previous>();
                for (Path file : MetaPackStore.history(directory, selected.id())) {
                    var manifest = MetaPackStore.manifest(file);
                    if ("removed".equals(selected.pending()) || !manifest.packVersion().equals(selected.revision())) result.add(new Previous(manifest.packVersion(), file));
                }
                return result;
            });
            if (previous.isEmpty()) {
                Messages.showInfoMessage(project, StudioBundle.message("metaPack.NoHistory"), StudioBundle.message("metaPack.Restore")); return;
            }
            var choices = new JBList<>(previous); choices.setSelectedIndex(0);
            var picker = new DialogWrapper(project, true) {
                { setTitle(StudioBundle.message("metaPack.Restore")); init(); }
                @Override protected JComponent createCenterPanel() { return new JBScrollPane(choices); }
            };
            if (picker.showAndGet() && choices.getSelectedValue() != null) reviewUpdate(selected, choices.getSelectedValue().file());
        });
    }
    private static JButton button(String key, Runnable action) {
        var button = new JButton(StudioBundle.message(key)); button.addActionListener(event -> action.run()); return button;
    }
    @FunctionalInterface private interface Work { void run() throws Exception; }
    private void perform(Work work) {
        try { work.run(); }
        catch (com.intellij.openapi.progress.ProcessCanceledException cancelled) { throw cancelled; }
        catch (Exception e) { Messages.showErrorDialog(project, StudioBundle.message("metaPack.Failed", e.getMessage()), StudioBundle.message("metaPack.Manage")); }
    }
}
