package org.ikasan.studio.ui.component.canvas;

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
    private boolean busy;

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
        if (busy || project.isDisposed()) return false;
        UiContext context = project.getService(UiContext.class);
        Module module = context.getIkasanModule();
        return context.getDesignerCanvas() == canvas && module != null && module.isInitialised();
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
        return text != null && text.startsWith(FlowClipboard.PREFIX);
    }

    private void copy(Flow flow) {
        if (!ready() || flow == null) return;
        Module module = project.getService(UiContext.class).getIkasanModule();
        if (!module.getFlows().contains(flow)) return;
        try {
            var snapshot = FlowClipboard.capture(flow, module.getVersion());
            runTask("menu.CopyFlow", () -> FlowClipboard.encode(snapshot), encoded ->
                    CopyPasteManager.getInstance().setContents(new StringSelection(encoded)));
        } catch (RuntimeException exception) { failed(exception); }
    }

    private void paste() {
        if (!ready()) return;
        String text = clipboardText();
        if (text == null || !text.startsWith(FlowClipboard.PREFIX)) return;
        Module module = project.getService(UiContext.class).getIkasanModule();
        String version = module.getVersion();
        runTask("menu.PasteFlow", () -> FlowClipboard.decode(text, version), flow -> {
            UiContext context = project.getService(UiContext.class);
            if (context.getDesignerCanvas() == canvas && context.getIkasanModule() == module
                    && Objects.equals(version, module.getVersion())) insert(module, flow);
        });
    }

    private <T> void runTask(String title, java.util.concurrent.Callable<T> work, java.util.function.Consumer<T> success) {
        busy = true;
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
            @Override public void onFinished() { busy = false; }
        }.queue();
    }

    void insert(Module module, Flow flow) {
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
        GenerationRequest request = GenerationRequest.moduleStructure(flow);
        CommandProcessor.getInstance().executeCommand(project, () -> {
            int index = module.getFlows().size();
            module.getFlows().add(flow);
            UndoManager.getInstance(project).undoableActionPerformed(new DeleteComponentUndoableAction(project,
                    () -> { module.getFlows().remove(flow); context.resetSelectionAfterDeletion(); },
                    () -> module.getFlows().add(Math.min(index, module.getFlows().size()), flow), request));
            StudioProjectFiles.refreshCodeFromModelAndCauseRedraw(project, request);
        }, StudioBundle.message("menu.PasteFlow"), null);
        StudioUIUtils.displayIdeaInfoMessage(project, StudioBundle.message("message.FlowPasted", flow.getIdentity()));
    }

    private void failed(Throwable error) {
        if (project.isDisposed()) return;
        if (error instanceof FlowClipboard.VersionMismatch mismatch) {
            StudioUIUtils.displayIdeaWarnMessage(project,
                    StudioBundle.message("message.FlowClipboardVersionMismatch", mismatch.source, mismatch.destination));
        } else {
            // Configured credentials may be present; never log clipboard contents or parser messages.
            LOG.warn("STUDIO: Flow clipboard operation failed (" + error.getClass().getSimpleName() + ")");
            StudioUIUtils.displayIdeaWarnMessage(project, StudioBundle.message("message.FlowClipboardFailed"));
        }
    }
}
