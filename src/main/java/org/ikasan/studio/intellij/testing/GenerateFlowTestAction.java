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
    /** Generates a frozen module-wide baseline; running Maven tests is a separate explicit action. */
    public static void openVerification(Project project) {
        String title = StudioBundle.message("verification.title");
        UiContext context = project.getService(UiContext.class);
        boolean acquired = false;
        try {
            String basePath = project.getBasePath();
            var live = context.getIkasanModule();
            if (basePath == null || live == null || !live.isInitialised() || context.isModelPersistenceBlocked())
                throw new IllegalStateException(StudioBundle.message("message.ConfigureAndSaveTheModuleFirst"));
            if (context.getPropertiesPanel() != null && context.getPropertiesPanel().dataHasChangedAndOKToProcess())
                throw new IllegalStateException(StudioBundle.message("message.ApplyOrDiscardThePendingPropertyEditsBeforeMigrating"));
            for (var document : FileDocumentManager.getInstance().getUnsavedDocuments()) {
                var file = FileDocumentManager.getInstance().getFile(document);
                if (file != null && file.getPath().startsWith(basePath + "/"))
                    throw new IllegalStateException(StudioBundle.message("flowTest.saveFiles"));
            }
            if (!context.tryBeginMigration()) throw new IllegalStateException(StudioBundle.message("message.WaitForTheCurrentGenerationOrMigrationToFinish"));
            acquired = true;
            Path root = Path.of(basePath);
            AtomicReference<Exception> failure = new AtomicReference<>();
            AtomicReference<org.ikasan.studio.core.generator.GeneratedVerification.Bundle> bundle = new AtomicReference<>();
            AtomicReference<String> snapshot = new AtomicReference<>();
            AtomicReference<String> parent = new AtomicReference<>();
            AtomicReference<String> model = new AtomicReference<>();
            ProgressManager.getInstance().runProcessWithProgressSynchronously(() -> {
                try {
                    model.set(Files.readString(root.resolve(MigrationArtifacts.MODEL)));
                    var saved = ComponentIO.validatePersistedModuleJson(model.get(), "generated verification", false);
                    var mapper = StudioJson.newObjectMapper();
                    if (!mapper.readTree(org.ikasan.studio.core.generator.ModelTemplate.create(saved)).equals(mapper.readTree(org.ikasan.studio.core.generator.ModelTemplate.create(live))))
                        throw new IllegalStateException(StudioBundle.message("message.TheCanvasAndSavedModelDiffer"));
                    parent.set(Files.readString(root.resolve("pom.xml")));
                    snapshot.set(GeneratedVerificationFiles.snapshot(root.resolve(org.ikasan.studio.core.generator.GeneratedVerification.DIRECTORY)));
                    bundle.set(org.ikasan.studio.core.generator.GeneratedVerification.render(saved, model.get(), parent.get(),
                            Files.readString(root.resolve("generated/pom.xml"))));
                } catch (Exception ex) { failure.set(ex); }
            }, title, false, project);
            if (failure.get() != null) throw failure.get();
            var dialog = new GeneratedVerificationDialog(project, !snapshot.get().equals("missing"));
            if (!dialog.showAndGet()) return;
            boolean archivePrevious = dialog.archivePrevious();
            ProgressManager.getInstance().runProcessWithProgressSynchronously(() -> {
                try {
                    if (!Files.readString(root.resolve(MigrationArtifacts.MODEL)).equals(model.get()))
                        throw new IllegalStateException(StudioBundle.message("message.TheModelChangedWhilePreparingMigration"));
                    GeneratedVerificationFiles.write(root, parent.get(), snapshot.get(), bundle.get(), archivePrevious);
                    var base = LocalFileSystem.getInstance().refreshAndFindFileByNioFile(root);
                    if (base != null) base.refresh(false, true);
                } catch (Exception ex) { failure.set(ex); }
            }, title, false, project);
            if (failure.get() != null) throw failure.get();
            context.setIkasanPomModel(null);
            if (!project.isDisposed()) {
                MavenProjectsManager.getInstance(project).forceUpdateAllProjectsOrFindAllAvailablePomFiles();
                Messages.showInfoMessage(project, StudioBundle.message("verification.created"), title);
            }
        } catch (Exception failure) {
            LOG.warn("Could not generate verification baseline", failure);
            if (!project.isDisposed()) Messages.showWarningDialog(project, String.valueOf(failure.getMessage()), title);
        } finally { if (acquired) context.endMigration(); }
    }

    public static void openPropertiesRefresh(Project project) { open(project, null, false, true); }

    private static void open(Project project, String selectedFlow, boolean multiple) {
        open(project, selectedFlow, multiple, false);
    }

    private static void open(Project project, String selectedFlow, boolean multiple, boolean refreshRequested) {
        String title = StudioBundle.message(multiple ? "flowTest.batchTitle" : "flowTest.title");
        UiContext context = project.getService(UiContext.class);
        boolean acquired = false;
        try {
            String basePath = project.getBasePath();
            var live = context.getIkasanModule();
            if (basePath == null || live == null || !live.isInitialised() || context.isModelPersistenceBlocked()) {
                throw new IllegalStateException(StudioBundle.message("message.ConfigureAndSaveTheModuleFirst"));
            }
            if (context.getPropertiesPanel() != null && context.getPropertiesPanel().dataHasChangedAndOKToProcess()) {
                throw new IllegalStateException(StudioBundle.message("message.ApplyOrDiscardThePendingPropertyEditsBeforeMigrating"));
            }
            String[] flows = live.getFlows().stream().map(Flow::getIdentity).toArray(String[]::new);
            if (flows.length == 0) throw new IllegalStateException(StudioBundle.message("flowTest.noFlows"));
            java.util.Set<String> ftpFlows = live.getFlows().stream()
                    .filter(flow -> flow.getFlowElementsNoExternalEndPoints().stream()
                            .anyMatch(element -> element.getComponentMeta().supportsTestFtpServer()))
                    .map(Flow::getIdentity).collect(java.util.stream.Collectors.toSet());
            java.util.Set<String> smtpFlows = live.getFlows().stream()
                    .filter(flow -> flow.getFlowElementsNoExternalEndPoints().stream()
                            .anyMatch(element -> element.getComponentMeta().supportsTestMailServer()))
                    .map(Flow::getIdentity).collect(java.util.stream.Collectors.toSet());
            java.util.Set<String> sftpFlows = live.getFlows().stream()
                    .filter(flow -> flow.getFlowElementsNoExternalEndPoints().stream()
                            .anyMatch(element -> element.getComponentMeta().supportsTestSftpServer()))
                    .map(Flow::getIdentity).collect(java.util.stream.Collectors.toSet());
            java.util.concurrent.atomic.AtomicBoolean stale = new java.util.concurrent.atomic.AtomicBoolean();
            AtomicReference<Exception> fingerprintFailure = new AtomicReference<>();
            ProgressManager.getInstance().runProcessWithProgressSynchronously(() -> {
                try {
                    Path support = Path.of(basePath).resolve(FlowTestScaffold.SUPPORT_PATH);
                    stale.set(!Files.exists(support) || FlowTestScaffold.supportNeedsRefresh(Files.readString(support), live));
                } catch (Exception ex) { fingerprintFailure.set(ex); }
            }, title, false, project);
            if (fingerprintFailure.get() != null) throw fingerprintFailure.get();
            boolean staleSupport = stale.get();
            boolean useLocalSftp;
            List<String> choices;
            boolean useLocalSmtp;
            boolean useLocalFtp;
            boolean regenerateSupport;
            boolean refreshProperties;
            if (multiple) {
                FlowTestsDialog dialog = new FlowTestsDialog(project, flows, ftpFlows, smtpFlows, sftpFlows);
                dialog.setSupportStale(staleSupport);
                dialog.setRefreshProperties(refreshRequested);
                if (!dialog.showAndGet()) return;
                choices = dialog.selectedFlows();
                regenerateSupport = dialog.regenerateSupport();
                refreshProperties = dialog.refreshProperties();
                useLocalFtp = dialog.useLocalFtp();
                useLocalSmtp = dialog.useLocalSmtp();
                useLocalSftp = dialog.useLocalSftp();
            } else {
                FlowTestDialog dialog = new FlowTestDialog(project, flows, selectedFlow, ftpFlows, smtpFlows, sftpFlows);
                dialog.setSupportStale(staleSupport);
                dialog.setRefreshProperties(refreshRequested);
                if (!dialog.showAndGet()) return;
                choices = List.of(dialog.selectedFlow());
                regenerateSupport = dialog.regenerateSupport();
                refreshProperties = dialog.refreshProperties();
                useLocalFtp = dialog.useLocalFtp();
                useLocalSmtp = dialog.useLocalSmtp();
                useLocalSftp = dialog.useLocalSftp();
            }
            if (choices.isEmpty()) return;
            boolean batchWrite = multiple || regenerateSupport || refreshProperties;
            // Block canvas generation/migration while modal background work snapshots the saved model and writes files.
            if (!context.tryBeginMigration()) throw new IllegalStateException(StudioBundle.message("message.WaitForTheCurrentGenerationOrMigrationToFinish"));
            acquired = true;
            for (var document : FileDocumentManager.getInstance().getUnsavedDocuments()) {
                var file = FileDocumentManager.getInstance().getFile(document);
                if (file != null && file.getPath().startsWith(basePath + "/")) {
                    throw new IllegalStateException(StudioBundle.message("flowTest.saveFiles"));
                }
            }
            Path root = Path.of(basePath);
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
                        if (!mapper.readTree(org.ikasan.studio.core.generator.ModelTemplate.create(module)).equals(mapper.readTree(org.ikasan.studio.core.generator.ModelTemplate.create(live)))) {
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
                            scaffolds.addAll(FlowTestFiles.supportRefreshPlans(scaffolds));
                        }
                        String freshProperties = scaffolds.get(0).files().get(FlowTestScaffold.TEST_PROPERTIES_PATH);
                        if (staleSupport || refreshProperties) {
                            // Refresh shared wiring, never select existing business scenarios for replacement.
                            scaffolds = FlowTestFiles.preserveExistingScenarios(root, scaffolds);
                        }
                        if (!Files.readString(root.resolve(MigrationArtifacts.MODEL)).equals(source)) {
                            throw new IllegalStateException(StudioBundle.message("message.TheModelChangedWhilePreparingMigration"));
                        }
                        if (batchWrite && !batchReviewed.get()) FlowTestFiles.checkExisting(root, scaffolds);
                        // Skip Existing is an explicit decision to retain developer-owned support.
                        // The writer still creates missing scenarios, helpers and resources.
                        List<Path> tests = batchWrite ? (batchApproval.get() == null
                                ? FlowTestFiles.writeAll(root, pom, scaffolds)
                                : FlowTestFiles.archiveAndWriteAll(root, pom, scaffolds, batchApproval.get()))
                                : List.of(archiveApproval.get() == null
                                        ? FlowTestFiles.write(root, pom, scaffolds.get(0))
                                        : FlowTestFiles.archiveAndWrite(root, pom, scaffolds.get(0), archiveApproval.get()));
                        if (refreshProperties) {
                            Path properties = FlowTestFiles.refreshProperties(root, freshProperties);
                            if (tests.isEmpty()) tests = List.of(properties);
                        }
                        created.set(tests);
                        if (tests.isEmpty()) return;
                        if (useLocalSftp) FlowTestFiles.enableLocalSftp(root);
                        if (useLocalFtp) FlowTestFiles.enableLocalFtp(root);
                        if (useLocalSmtp) FlowTestFiles.enableLocalSmtp(root);
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
            FlowTestPropertyWarnings.checkAfterRename(project);
            if (!created.get().isEmpty()) MavenProjectsManager.getInstance(project).forceUpdateAllProjectsOrFindAllAvailablePomFiles();
            if (result.get() != null) FileEditorManager.getInstance(project).openFile(result.get(), true);
            long scenarioCount = created.get().stream()
                    .filter(path -> path.getParent().equals(root.resolve("user-flow-tests/src/test/java/org/ikasan/studio/flowtests")))
                    .count();
            Messages.showInfoMessage(project, (refreshProperties ? StudioBundle.message("flowTest.propertiesRefreshed") + "\n\n" : "") + (staleSupport ? StudioBundle.message("flowTest.supportRefreshedReview") + "\n\n" : "") + (batchWrite
                    ? (batchApproval.get() == null ? "" : StudioBundle.message("flowTest.batchArchived", batchApproval.get().size()) + "\n\n")
                            + StudioBundle.message("flowTest.batchCreated", scenarioCount, choices.size() - scenarioCount)
                    : (archiveApproval.get() == null ? "" : StudioBundle.message("flowTest.archived") + "\n\n")
                            + StudioBundle.message("flowTest.created")), title);
        } catch (Exception ex) {
            LOG.warn("Could not generate flow test", ex);
            if (!project.isDisposed()) Messages.showWarningDialog(project, ex.getMessage() == null ? ex.toString() : ex.getMessage(), title);
        } finally {
            if (acquired) context.endMigration();
        }
    }
}
