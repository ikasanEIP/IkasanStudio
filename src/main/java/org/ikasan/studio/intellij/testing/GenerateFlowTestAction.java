package org.ikasan.studio.intellij.testing;

import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.LocalFileSystem;
import org.ikasan.studio.core.generator.FlowTestScaffold;
import org.ikasan.studio.core.io.ComponentIO;
import org.ikasan.studio.core.model.ikasan.instance.Flow;
import org.ikasan.studio.core.migration.MigrationArtifacts;
import org.ikasan.studio.core.persistence.json.StudioJson;
import org.ikasan.studio.ui.StudioBundle;
import org.ikasan.studio.ui.UiContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.idea.maven.project.MavenProjectsManager;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicReference;
import java.util.List;
import java.util.ArrayList;

public final class GenerateFlowTestAction extends DumbAwareAction {
    private static final Logger LOG = Logger.getInstance(GenerateFlowTestAction.class);
    @Override public @NotNull ActionUpdateThread getActionUpdateThread() { return ActionUpdateThread.BGT; }
    @Override public void update(AnActionEvent event) {
        var project = event.getProject();
        var module = project == null ? null : project.getService(UiContext.class).getIkasanModule();
        event.getPresentation().setEnabled(module != null && module.isInitialised());
    }
    @Override public void actionPerformed(AnActionEvent event) {
        if (event.getProject() != null) open(event.getProject(), null);
    }
    public static void open(Project project, String selectedFlow) {
        open(project, selectedFlow, false);
    }
    public static void openAll(Project project) { open(project, null, true); }
    private static void open(Project project, String selectedFlow, boolean multiple) {
        String title = StudioBundle.message(multiple ? "flowTest.batchTitle" : "flowTest.title");
        UiContext context = project.getService(UiContext.class);
        boolean acquired = false;
        try {
            var live = context.getIkasanModule();
            if (project.getBasePath() == null || live == null || !live.isInitialised() || context.isModelPersistenceBlocked()) {
                throw new IllegalStateException(StudioBundle.message("message.ConfigureAndSaveTheModuleFirst"));
            }
            if (context.getPropertiesPanel() != null && context.getPropertiesPanel().dataHasChangedAndOKToProcess()) {
                throw new IllegalStateException(StudioBundle.message("message.ApplyOrDiscardThePendingPropertyEditsBeforeMigrating"));
            }
            String[] flows = live.getFlows().stream().map(Flow::getIdentity).toArray(String[]::new);
            if (flows.length == 0) throw new IllegalStateException(StudioBundle.message("flowTest.noFlows"));
            List<String> choices;
            boolean regenerateSupport;
            if (multiple) {
                FlowTestsDialog dialog = new FlowTestsDialog(project, flows);
                if (!dialog.showAndGet()) return;
                choices = dialog.selectedFlows();
                regenerateSupport = dialog.regenerateSupport();
            } else {
                FlowTestDialog dialog = new FlowTestDialog(project, flows, selectedFlow);
                if (!dialog.showAndGet()) return;
                choices = List.of(dialog.selectedFlow());
                regenerateSupport = dialog.regenerateSupport();
            }
            if (choices.isEmpty()) return;
            boolean batchWrite = multiple || regenerateSupport;
            // Block canvas generation/migration while modal background work snapshots the saved model and writes files.
            if (!context.tryBeginMigration()) throw new IllegalStateException(StudioBundle.message("message.WaitForTheCurrentGenerationOrMigrationToFinish"));
            acquired = true;
            for (var document : FileDocumentManager.getInstance().getUnsavedDocuments()) {
                var file = FileDocumentManager.getInstance().getFile(document);
                if (file != null && file.getPath().startsWith(project.getBasePath() + "/")) {
                    throw new IllegalStateException(StudioBundle.message("flowTest.saveFiles"));
                }
            }
            Path root = Path.of(project.getBasePath());
            AtomicReference<com.intellij.openapi.vfs.VirtualFile> result = new AtomicReference<>();
            AtomicReference<Exception> failure = new AtomicReference<>();
            AtomicReference<List<Path>> created = new AtomicReference<>(List.of());
            AtomicReference<FlowTestFiles.ExistingTestException> archiveApproval = new AtomicReference<>();
            AtomicReference<List<FlowTestFiles.ExistingTestException>> batchApproval = new AtomicReference<>();
            java.util.concurrent.atomic.AtomicBoolean batchReviewed = new java.util.concurrent.atomic.AtomicBoolean();
            while (true) {
                failure.set(null);
                ProgressManager.getInstance().runProcessWithProgressSynchronously(() -> {
                    try {
                        String source = Files.readString(root.resolve(MigrationArtifacts.MODEL));
                        var module = ComponentIO.validatePersistedModuleJson(source, "flow test generation", false);
                        var mapper = StudioJson.newObjectMapper();
                        if (!mapper.readTree(ComponentIO.toJson(module)).equals(mapper.readTree(ComponentIO.toJson(live)))) {
                            throw new IllegalStateException(StudioBundle.message("message.TheCanvasAndSavedModelDiffer"));
                        }
                        String pom = Files.readString(root.resolve("pom.xml"));
                        String applicationPom = Files.readString(root.resolve("generated/pom.xml"));
                        List<FlowTestScaffold.Scaffold> scaffolds = new ArrayList<>();
                        for (String choice : choices) {
                            var flow = module.getFlows().stream().filter(f -> choice.equals(f.getIdentity())).findFirst()
                                    .orElseThrow(() -> new IllegalArgumentException(StudioBundle.message("flowTest.noFlows")));
                            try { scaffolds.add(FlowTestScaffold.render(module, flow, pom, applicationPom)); }
                            catch (Exception ex) { throw new IllegalArgumentException(choice + ": " + ex.getMessage(), ex); }
                        }
                        if (regenerateSupport) {
                            var first = scaffolds.get(0);
                            scaffolds.add(new FlowTestScaffold.Scaffold(first.rootPom(), FlowTestScaffold.SUPPORT_PATH,
                                    java.util.Map.of(FlowTestScaffold.SUPPORT_PATH, first.files().get(FlowTestScaffold.SUPPORT_PATH),
                                            FlowTestScaffold.TEST_PROPERTIES_PATH, first.files().get(FlowTestScaffold.TEST_PROPERTIES_PATH))));
                        }
                        if (!Files.readString(root.resolve(MigrationArtifacts.MODEL)).equals(source)) {
                            throw new IllegalStateException(StudioBundle.message("message.TheModelChangedWhilePreparingMigration"));
                        }
                        if (batchWrite && !batchReviewed.get()) FlowTestFiles.checkExisting(root, scaffolds);
                        List<Path> tests = batchWrite ? (batchApproval.get() == null
                                ? FlowTestFiles.writeAll(root, pom, scaffolds)
                                : FlowTestFiles.archiveAndWriteAll(root, pom, scaffolds, batchApproval.get()))
                                : List.of(archiveApproval.get() == null
                                        ? FlowTestFiles.write(root, pom, scaffolds.get(0))
                                        : FlowTestFiles.archiveAndWrite(root, pom, scaffolds.get(0), archiveApproval.get()));
                        created.set(tests);
                        if (tests.isEmpty()) return;
                        context.setIkasanPomModel(null);
                        var base = LocalFileSystem.getInstance().refreshAndFindFileByNioFile(root);
                        if (base != null) base.refresh(false, true);
                        result.set(LocalFileSystem.getInstance().refreshAndFindFileByNioFile(tests.get(0)));
                    } catch (Exception ex) { failure.set(ex); }
                }, title, false, project);
                if (batchWrite && failure.get() instanceof FlowTestFiles.ExistingTestsException existing) {
                    String names = String.join("\n", existing.tests().stream().map(t -> t.path().getFileName().toString()).toList());
                    int choice = Messages.showDialog(project, StudioBundle.message("flowTest.batchArchiveQuestion", names), title,
                            new String[]{StudioBundle.message("flowTest.skipExisting"), StudioBundle.message("flowTest.archiveGenerate"),
                                    StudioBundle.message("flowTest.cancel")}, 0, Messages.getQuestionIcon());
                    if (choice != 0 && choice != 1) return;
                    batchReviewed.set(true);
                    if (choice == 1) batchApproval.set(existing.tests());
                } else if (!batchWrite && failure.get() instanceof FlowTestFiles.ExistingTestException existing) {
                    if (Messages.showYesNoDialog(project,
                            StudioBundle.message("flowTest.archiveQuestion", existing.path().getFileName()), title,
                            StudioBundle.message("flowTest.archiveGenerate"), StudioBundle.message("flowTest.keepExisting"),
                            Messages.getQuestionIcon()) != Messages.YES) return;
                    // The worker revalidates both the model and these exact existing bytes after confirmation.
                    archiveApproval.set(existing);
                } else break;
            }
            if (failure.get() != null) throw failure.get();
            if (project.isDisposed()) return;
            if (!created.get().isEmpty()) MavenProjectsManager.getInstance(project).forceUpdateAllProjectsOrFindAllAvailablePomFiles();
            if (result.get() != null) FileEditorManager.getInstance(project).openFile(result.get(), true);
            Messages.showInfoMessage(project, batchWrite
                    ? (batchApproval.get() == null ? "" : StudioBundle.message("flowTest.batchArchived", batchApproval.get().size()) + "\n\n")
                            + StudioBundle.message("flowTest.batchCreated", created.get().stream().filter(p -> !p.getFileName().toString().equals("ModuleFlowTestSupport.java")).count(), choices.size() - created.get().stream().filter(p -> !p.getFileName().toString().equals("ModuleFlowTestSupport.java")).count())
                    : (archiveApproval.get() == null ? "" : StudioBundle.message("flowTest.archived") + "\n\n")
                            + StudioBundle.message("flowTest.created"), title);
        } catch (Exception ex) {
            LOG.warn("Could not generate flow test", ex);
            if (!project.isDisposed()) Messages.showWarningDialog(project, ex.getMessage() == null ? ex.toString() : ex.getMessage(), title);
        } finally {
            if (acquired) context.endMigration();
        }
    }
}
