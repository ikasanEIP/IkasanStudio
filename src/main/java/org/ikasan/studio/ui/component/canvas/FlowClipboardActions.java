package org.ikasan.studio.ui.component.canvas;

import com.intellij.openapi.application.ReadAction;
import com.intellij.ide.CopyProvider;
import com.intellij.ide.PasteProvider;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.command.undo.UndoManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.ide.CopyPasteManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.InputValidator;
import com.intellij.openapi.ui.Messages;
import org.ikasan.studio.core.generation.GenerationRequest;
import org.ikasan.studio.core.generator.GeneratorUtils;
import org.ikasan.studio.core.metapack.model.ComponentPropertyMeta;
import org.ikasan.studio.intellij.project.FlowSourceTransfer;
import org.ikasan.studio.core.model.ikasan.instance.Flow;
import org.ikasan.studio.core.model.ikasan.instance.Module;
import org.ikasan.studio.core.persistence.json.FlowClipboard;
import org.ikasan.studio.intellij.project.StudioProjectFiles;
import org.ikasan.studio.ui.StudioBundle;
import org.ikasan.studio.ui.StudioUIUtils;
import org.ikasan.studio.ui.UiContext;
import org.ikasan.studio.ui.actions.DeleteComponentUndoableAction;
import org.jetbrains.annotations.NotNull;

import javax.swing.JMenuItem;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.util.Objects;

/** Uses IntelliJ's Edit menu and configured shortcuts; model reconstruction runs off the EDT. */
final class FlowClipboardActions implements CopyProvider, PasteProvider {
    private static final Logger LOG = Logger.getInstance(FlowClipboardActions.class);
    private final Project project;
    private final DesignerCanvas canvas;
    private int pendingTasks;

    FlowClipboardActions(Project project, DesignerCanvas canvas) {
        this.project = project;
        this.canvas = canvas;
    }

    @Override public @NotNull ActionUpdateThread getActionUpdateThread() { return ActionUpdateThread.EDT; }
    @Override public boolean isCopyVisible(@NotNull DataContext context) { return true; }
    @Override public boolean isCopyEnabled(@NotNull DataContext context) { return ready() && selectedFlow() != null; }
    @Override public boolean isPastePossible(@NotNull DataContext context) { return ready(); }
    @Override public boolean isPasteEnabled(@NotNull DataContext context) { return canPaste(); }
    @Override public void performCopy(@NotNull DataContext context) { copy(selectedFlow()); }
    @Override public void performPaste(@NotNull DataContext context) { paste(); }

    JMenuItem copyItem(Flow flow) {
        JMenuItem item = new JMenuItem(StudioBundle.message("menu.CopyFlow"));
        item.setEnabled(ready());
        item.setToolTipText(StudioBundle.message("tooltip.FlowClipboard"));
        item.addActionListener(event -> copy(flow));
        return item;
    }

    JMenuItem pasteItem() {
        JMenuItem item = new JMenuItem(StudioBundle.message("menu.PasteFlow"));
        item.setEnabled(canPaste());
        item.setToolTipText(StudioBundle.message("tooltip.FlowClipboard"));
        item.addActionListener(event -> paste());
        return item;
    }

    private boolean ready() {
        if (pendingTasks != 0 || project.isDisposed()) return false;
        UiContext context = project.getService(UiContext.class);
        Module module = context.getIkasanModule();
        return context.getDesignerCanvas() == canvas && module != null && module.isInitialised()
                && !context.isMigrationActive() && context.getLatestGeneration().isDone();
    }

    private Flow selectedFlow() {
        return project.getService(UiContext.class).getSelectedComponent() instanceof Flow flow ? flow : null;
    }

    private String clipboardText() {
        try { return CopyPasteManager.getInstance().getContents(DataFlavor.stringFlavor); }
        catch (RuntimeException exception) { return null; }
    }

    private boolean canPaste() {
        if (!ready()) return false;
        String text = clipboardText();
        return FlowClipboard.isFlow(text);
    }

    private void copy(Flow flow) {
        if (!ready() || flow == null) return;
        Module module = project.getService(UiContext.class).getIkasanModule();
        if (!module.getFlows().contains(flow)) return;
        try {
            var snapshot = FlowClipboard.capture(flow, module.getVersion());
            String packageName = GeneratorUtils.getUserImplementedClassesPackageName(module, flow);
            runTask("menu.CopyFlow", () -> FlowClipboard.encode(snapshot,
                    ReadAction.compute(() -> FlowSourceTransfer.capture(project, packageName))), encoded ->
                    CopyPasteManager.getInstance().setContents(new StringSelection(encoded)));
        } catch (RuntimeException exception) { failed(exception); }
    }

    private void paste() {
        if (!ready()) return;
        String text = clipboardText();
        if (!FlowClipboard.isFlow(text)) return;
        Module module = project.getService(UiContext.class).getIkasanModule();
        String version = module.getVersion();
        runTask("menu.PasteFlow", () -> FlowClipboard.decodeTransfer(text, version), transfer -> {
            UiContext context = project.getService(UiContext.class);
            if (context.getDesignerCanvas() == canvas && context.getIkasanModule() == module
                    && Objects.equals(version, module.getVersion())) insert(module, transfer.flow(), transfer.sources());
        });
    }

    private <T> void runTask(String title, java.util.concurrent.Callable<T> work, java.util.function.Consumer<T> success) {
        pendingTasks++;
        new Task.Backgroundable(project, StudioBundle.message(title), true) {
            private T result;
            private Exception failure;
            @Override public void run(@NotNull ProgressIndicator indicator) {
                try { result = work.call(); }
                catch (com.intellij.openapi.progress.ProcessCanceledException canceled) { throw canceled; }
                catch (Exception exception) { failure = exception; }
                indicator.checkCanceled();
            }
            @Override public void onSuccess() {
                if (project.isDisposed()) return;
                if (failure != null) { failed(failure); return; }
                try { success.accept(result); }
                catch (RuntimeException exception) { failed(exception); }
            }
            @Override public void onThrowable(@NotNull Throwable error) { failed(error); }
            @Override public void onFinished() { pendingTasks--; }
        }.queue();
    }

    void insert(Module module, Flow flow) {
        insert(module, flow, FlowClipboard.Sources.empty());
    }

    private void insert(Module module, Flow flow, FlowClipboard.Sources sources) {
        String version = module.getVersion();
        if (!FlowClipboard.nameAvailable(module, flow.getIdentity())) {
            String name = Messages.showInputDialog(project, StudioBundle.message("message.PasteFlowName"),
                    StudioBundle.message("menu.PasteFlow"), Messages.getQuestionIcon(),
                    FlowClipboard.availableName(module, flow.getIdentity()), new InputValidator() {
                        @Override public boolean checkInput(String input) { return FlowClipboard.nameAvailable(module, input.trim()); }
                        @Override public boolean canClose(String input) { return checkInput(input); }
                    });
            if (name == null) return;
            flow.setName(name.trim());
        }
        UiContext context = project.getService(UiContext.class);
        if (project.isDisposed() || context.getIkasanModule() != module || context.getDesignerCanvas() != canvas
                || !Objects.equals(version, module.getVersion()) || !FlowClipboard.nameAvailable(module, flow.getIdentity())) return;
        if (!sources.files().isEmpty()) {
            String destinationPackage = GeneratorUtils.getUserImplementedClassesPackageName(module, flow);
            runTask("menu.PasteFlow", () -> ReadAction.compute(() ->
                    FlowSourceTransfer.relocate(project, sources, destinationPackage)), relocated -> {
                if (project.isDisposed() || context.getIkasanModule() != module || context.getDesignerCanvas() != canvas
                        || !Objects.equals(version, module.getVersion())
                        || !Objects.equals(destinationPackage, GeneratorUtils.getUserImplementedClassesPackageName(module, flow))
                        || !FlowClipboard.nameAvailable(module, flow.getIdentity())) return;
                retargetCopiedClasses(flow, sources, destinationPackage);
                completeInsertion(module, flow, relocated);
            });
        } else {
            completeInsertion(module, flow, sources);
        }
    }

    private static void retargetCopiedClasses(Flow flow, FlowClipboard.Sources sources, String destinationPackage) {
        for (var component : flow.getFlowElementsNoExternalEndPoints()) {
            Object className = component.getPropertyValue(ComponentPropertyMeta.USER_IMPLEMENTED_CLASS_NAME);
            if (Boolean.FALSE.equals(component.getPropertyValue(ComponentPropertyMeta.REQUIRES_STUB))
                    && className instanceof String name && name.startsWith(sources.packageName() + ".")) {
                component.setPropertyValue(ComponentPropertyMeta.USER_IMPLEMENTED_CLASS_NAME,
                        destinationPackage + name.substring(sources.packageName().length()));
            }
            for (var property : component.getUserSuppliedClassProperties()) {
                if (property.getMeta().isProtectFromOverwrite()) property.setOverwriteEnabled(false);
            }
        }
    }

    void completeInsertion(Module module, Flow flow, FlowClipboard.Sources sources) {
        UiContext context = project.getService(UiContext.class);
        GenerationRequest request = GenerationRequest.moduleStructure(flow);
        boolean[] inserted = {false};
        CommandProcessor.getInstance().executeCommand(project, () -> {
            if (!sources.files().isEmpty()) {
                if (!context.tryBeginMigration()) {
                    StudioUIUtils.displayIdeaWarnMessage(project, StudioBundle.message("message.FlowClipboardBusy"));
                    return;
                }
                try { FlowSourceTransfer.install(project, sources); }
                catch (Exception error) { failed(error); return; }
                finally { context.endMigration(); }
            }
            int index = module.getFlows().size();
            module.getFlows().add(flow);
            inserted[0] = true;
            UndoManager.getInstance(project).undoableActionPerformed(new DeleteComponentUndoableAction(project,
                    () -> { module.getFlows().remove(flow); context.resetSelectionAfterDeletion(); },
                    () -> module.getFlows().add(Math.min(index, module.getFlows().size()), flow), request));
            StudioProjectFiles.refreshCodeFromModelAndCauseRedraw(project, request);
        }, StudioBundle.message("menu.PasteFlow"), null);
        if (inserted[0]) StudioUIUtils.displayIdeaInfoMessage(project,
                StudioBundle.message("message.FlowPasted", flow.getIdentity()));
    }

    private void failed(Throwable error) {
        if (project.isDisposed()) return;
        if (error instanceof FlowSourceTransfer.DestinationExists conflict) {
            StudioUIUtils.displayIdeaWarnMessage(project,
                    StudioBundle.message("message.FlowClipboardSourceConflict", conflict.packageName));
        } else if (error instanceof FlowClipboard.VersionMismatch mismatch) {
            StudioUIUtils.displayIdeaWarnMessage(project,
                    StudioBundle.message("message.FlowClipboardVersionMismatch", mismatch.source, mismatch.destination));
        } else {
            // Configured credentials may be present; never log clipboard contents or parser messages.
            LOG.warn("STUDIO: Flow clipboard operation failed (" + error.getClass().getSimpleName() + ")");
            StudioUIUtils.displayIdeaWarnMessage(project, StudioBundle.message("message.FlowClipboardFailed"));
        }
    }
}
