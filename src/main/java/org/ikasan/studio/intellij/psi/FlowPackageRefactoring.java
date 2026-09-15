package org.ikasan.studio.intellij.psi;

import com.intellij.ide.highlighter.JavaFileType;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.psi.*;
import com.intellij.psi.search.FileTypeIndex;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.refactoring.rename.RenameProcessor;
import com.intellij.usageView.UsageInfo;
import org.ikasan.studio.core.StudioBuildUtils;
import org.ikasan.studio.core.generator.GeneratorUtils;
import org.ikasan.studio.core.model.ikasan.instance.Flow;
import org.ikasan.studio.core.model.ikasan.instance.Module;
import org.ikasan.studio.intellij.project.StudioProjectFiles;
import org.ikasan.studio.ui.StudioBundle;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

/** Refactors the flow's implementation package before its name is committed to the model. */
public final class FlowPackageRefactoring {
    private FlowPackageRefactoring() {}

    public record Plan(Project project, String oldPackage, String newPackage, PsiPackage source,
                       List<PsiDirectory> directories, List<String> files) {}

    /** Index and filesystem discovery runs under modal background progress, before any mutation. */
    public static Plan prepare(Project project, Module module, Flow flow, String newName) {
        if (DumbService.isDumb(project)) throw new IllegalStateException(StudioBundle.message("message.FlowRenameIndexing"));
        AtomicReference<Plan> plan = new AtomicReference<>();
        AtomicReference<RuntimeException> failure = new AtomicReference<>();
        boolean completed = ProgressManager.getInstance().runProcessWithProgressSynchronously(() -> {
            try {
                plan.set(ReadAction.compute(() -> inspect(project, module, flow, newName)));
            } catch (RuntimeException e) { failure.set(e); }
        }, StudioBundle.message("dialog.FlowPackageChange"), true, project);
        if (!completed) return null;
        if (failure.get() != null) throw failure.get();
        return plan.get();
    }

    static Plan inspect(Project project, Module module, Flow flow, String newName) {
        String oldPackage = GeneratorUtils.getUserImplementedClassesPackageName(module, flow);
        String newPackage = module.getApplicationPackageName() + "." + StudioBuildUtils.toJavaPackageName(newName);
        if (oldPackage.equals(newPackage)) return new Plan(project, oldPackage, newPackage, null, List.of(), List.of());
        VirtualFile base = StudioProjectFiles.getProjectBaseDir(project);
        if (base == null) throw new IllegalStateException(StudioBundle.message("message.FlowRenameNoProject"));
        for (Flow sibling : module.getFlows()) {
            if (sibling != flow && newPackage.equals(GeneratorUtils.getUserImplementedClassesPackageName(module, sibling))) {
                throw new IllegalStateException(StudioBundle.message("message.FlowRenameConflict", newPackage));
            }
        }
        // Existing-class references are explicitly outside Studio's ownership. Do not move their package.
        for (Flow candidate : module.getFlows()) {
            for (var element : candidate.getFlowElementsNoExternalEndPoints()) {
                Object name = element.getPropertyValue("userImplementedClassName");
                if (Boolean.FALSE.equals(element.getPropertyValue("requiresStub")) && name instanceof String className
                        && className.startsWith(oldPackage + ".")) {
                    throw new IllegalStateException(StudioBundle.message("message.FlowRenameExternalClass", className));
                }
            }
        }
        JavaPsiFacade facade = JavaPsiFacade.getInstance(project);
        PsiPackage source = facade.findPackage(oldPackage);
        List<PsiDirectory> directories = new ArrayList<>();
        if (source != null) {
            for (PsiDirectory directory : source.getDirectories(GlobalSearchScope.projectScope(project))) {
                VirtualFile file = directory.getVirtualFile();
                String relative = com.intellij.openapi.vfs.VfsUtilCore.getRelativePath(file, base);
                if (relative == null || !(relative.startsWith("user/src/") || relative.startsWith("generated/src/"))) {
                    throw new IllegalStateException(StudioBundle.message("message.FlowRenameOutsideSources", file.getPath()));
                }
                directories.add(directory);
            }
            // Package rename affects all directories. Refuse roots outside the inspected project scope.
            if (source.getDirectories().length != directories.size()) {
                throw new IllegalStateException(StudioBundle.message("message.FlowRenameOutsideSources", oldPackage));
            }
        }
        String leaf = newPackage.substring(newPackage.lastIndexOf('.') + 1);
        for (PsiDirectory directory : directories) {
            if (directory.getParentDirectory() == null || directory.getParentDirectory().findSubdirectory(leaf) != null) {
                throw new IllegalStateException(StudioBundle.message("message.FlowRenameConflict", newPackage));
            }
        }
        // Also catch an unindexed destination or source (Maven import has not registered its source root).
        for (String root : List.of("user/src/main/java/", "user/src/test/java/", "generated/src/main/java/")) {
            if (base.findFileByRelativePath(root + newPackage.replace('.', '/')) != null) {
                throw new IllegalStateException(StudioBundle.message("message.FlowRenameConflict", newPackage));
            }
            VirtualFile old = base.findFileByRelativePath(root + oldPackage.replace('.', '/'));
            if (old != null && directories.stream().noneMatch(dir -> dir.getVirtualFile().equals(old))) {
                throw new IllegalStateException(StudioBundle.message("message.FlowRenameIndexing"));
            }
        }
        List<String> files = new ArrayList<>();
        for (PsiDirectory directory : directories) collectFiles(directory.getVirtualFile(), file -> files.add(file.getPath()));
        return new Plan(project, oldPackage, newPackage, directories.isEmpty() ? null : source,
                List.copyOf(directories), List.copyOf(files));
    }

    /** Persist the new identity within the refactoring command, before generation is allowed to run. */
    public static boolean renameAndSave(Plan plan, Module module, Flow flow, String newName) {
        var context = plan.project().getService(org.ikasan.studio.ui.UiContext.class);
        if (context.isModelPersistenceBlocked()) throw new IllegalStateException(context.getModelPersistenceBlockReason());
        String oldName = flow.getIdentity();
        Map<Flow, String> oldOwners = new LinkedHashMap<>();
        var links = org.ikasan.studio.core.model.analysis.TestJmsHarnessLinks.findLinks(module).stream()
                .filter(link -> link.ownerProducer().getContainingFlow() == flow).toList();
        links.forEach(link -> oldOwners.put(link.harnessFlow(), (String) link.harnessFlow().getPropertyValue("testHarnessOwner")));
        Runnable restoreModel = () -> {
            flow.setName(oldName);
            oldOwners.forEach((harness, owner) -> harness.setPropertyValue("testHarnessOwner", owner));
        };
        Runnable applyModel = () -> {
            flow.setName(newName);
            links.forEach(link -> link.harnessFlow().setPropertyValue("testHarnessOwner",
                    org.ikasan.studio.core.model.analysis.TestJmsHarnessLinks.ownerKeyFor(link.ownerProducer())));
        };
        return execute(plan, () -> {
            try {
                VirtualFile base = StudioProjectFiles.getProjectBaseDir(plan.project());
                if (base == null) throw new IllegalStateException(StudioBundle.message("message.FlowRenameNoProject"));
                applyModel.run();
                StudioProjectFiles.replaceJsonModelFileSafely(plan.project(),
                        org.ikasan.studio.core.generator.ModelTemplate.create(module));
                // Protected atomic model writes and subsequent asynchronous generation are not one IDE
                // undo transaction. Block a partial file-only undo; reverse through Rename flow instead.
                VirtualFile modelFile = base.findFileByRelativePath("generated/src/main/model/model.json");
                if (modelFile != null) {
                    com.intellij.openapi.command.undo.UndoManager.getInstance(plan.project()).nonundoableActionPerformed(
                            com.intellij.openapi.command.undo.DocumentReferenceManager.getInstance().create(modelFile), true);
                }
            } catch (RuntimeException failure) {
                restoreModel.run();
                throw failure;
            }
        });
    }

    /** Returns false on a cancelled refactoring; callers must leave model edits pending in that case. */
    public static boolean execute(Plan plan) {
        return execute(plan, () -> {});
    }

    static boolean execute(Plan plan, Runnable afterPackageRename) {
        if (plan.source() == null) {
            com.intellij.openapi.command.WriteCommandAction.runWriteCommandAction(plan.project(), afterPackageRename);
            return true;
        }
        Processor processor = new Processor(plan, afterPackageRename);
        processor.setPreviewUsages(false);
        processor.run();
        return processor.completed;
    }

    private static void collectFiles(VirtualFile directory, java.util.function.Consumer<VirtualFile> consumer) {
        VfsUtilCore.iterateChildrenRecursively(directory, null, file -> {
            ProgressManager.checkCanceled();
            if (!file.isDirectory()) consumer.accept(file);
            return true;
        });
    }

    private record Original(VirtualFile file, byte[] bytes, String documentText) {}
    private record BeanEdit(SmartPsiElementPointer<PsiLiteralExpression> literal, String replacement) {}

    private static final class Processor extends RenameProcessor {
        private final Plan plan;
        private final Map<VirtualFile, Original> originals = new LinkedHashMap<>();
        private final List<BeanEdit> beanEdits = new ArrayList<>();
        private boolean completed;
        private final Runnable afterPackageRename;

        Processor(Plan plan, Runnable afterPackageRename) {
            super(plan.project(), plan.source(), plan.newPackage().substring(plan.newPackage().lastIndexOf('.') + 1), false, false);
            this.plan = plan;
            this.afterPackageRename = afterPackageRename;
        }

        @Override
        public UsageInfo @NotNull [] findUsages() {
            UsageInfo[] usages = super.findUsages();
            originals.clear();
            beanEdits.clear();
            Map<String, String> beanNames = new HashMap<>();
            for (PsiDirectory directory : plan.directories()) {
                collectFiles(directory.getVirtualFile(), file -> {
                    remember(file);
                    PsiFile psi = PsiManager.getInstance(plan.project()).findFile(file);
                    if (psi instanceof PsiJavaFile javaFile) {
                        for (PsiClass clazz : javaFile.getClasses()) {
                            String name = clazz.getQualifiedName();
                            PsiAnnotation annotation = clazz.getAnnotation("org.springframework.stereotype.Component");
                            if (name != null && annotation != null
                                    && annotation.findDeclaredAttributeValue("value") instanceof PsiLiteralExpression literal
                                    && name.equals(literal.getValue())) {
                                beanNames.put(name, plan.newPackage() + name.substring(plan.oldPackage().length()));
                            }
                        }
                    }
                });
            }
            for (UsageInfo usage : usages) {
                PsiFile file = usage.getFile();
                if (file != null && file.getVirtualFile() != null) remember(file.getVirtualFile());
            }
            // Java refactoring does not update arbitrary annotation string literals. Only change exact
            // references to Studio's default bean names, preserving deliberately customised identities.
            if (!beanNames.isEmpty()) {
                for (VirtualFile file : FileTypeIndex.getFiles(JavaFileType.INSTANCE, GlobalSearchScope.projectScope(plan.project()))) {
                    ProgressManager.checkCanceled();
                    PsiFile psi = PsiManager.getInstance(plan.project()).findFile(file);
                    if (psi == null) continue;
                    for (PsiAnnotation annotation : PsiTreeUtil.findChildrenOfType(psi, PsiAnnotation.class)) {
                        String qualified = annotation.getQualifiedName();
                        String attribute = "javax.annotation.Resource".equals(qualified) || "jakarta.annotation.Resource".equals(qualified)
                                ? "name" : "value";
                        if (!("name".equals(attribute) || "org.springframework.stereotype.Component".equals(qualified)
                                || "org.springframework.beans.factory.annotation.Qualifier".equals(qualified))) continue;
                        if (annotation.findDeclaredAttributeValue(attribute) instanceof PsiLiteralExpression literal
                                && literal.getValue() instanceof String value && beanNames.containsKey(value)) {
                            remember(file);
                            beanEdits.add(new BeanEdit(SmartPointerManager.createPointer(literal), beanNames.get(value)));
                        }
                    }
                }
            }
            return usages;
        }

        private void remember(VirtualFile file) {
            originals.computeIfAbsent(file, key -> {
                try (var stream = key.getInputStream()) {
                    var document = FileDocumentManager.getInstance().getCachedDocument(key);
                    return new Original(key, stream.readAllBytes(), document == null ? null : document.getText());
                } catch (IOException e) { throw new IllegalStateException("Cannot back up refactoring input: " + key.getPath(), e); }
            });
        }

        @Override
        protected @NotNull Collection<? extends PsiElement> getElementsToWrite(@NotNull com.intellij.usageView.UsageViewDescriptor descriptor) {
            List<PsiElement> elements = new ArrayList<>(super.getElementsToWrite(descriptor));
            for (BeanEdit edit : beanEdits) {
                PsiElement literal = edit.literal().getElement();
                if (literal != null) elements.add(literal);
            }
            return elements;
        }

        @Override
        protected boolean isPreviewUsages(UsageInfo @NotNull [] usages) {
            // The Studio confirmation lists the affected package files. Do not leave a deferred preview
            // that could execute after the property editor has switched to a different component.
            return false;
        }

        @Override
        public void performRefactoring(UsageInfo @NotNull [] usages) {
            try {
                String newLeaf = plan.newPackage().substring(plan.newPackage().lastIndexOf('.') + 1);
                for (PsiDirectory directory : plan.directories()) {
                    if (!directory.isValid() || directory.getParentDirectory() == null
                            || directory.getParentDirectory().findSubdirectory(newLeaf) != null) {
                        throw new IllegalStateException(StudioBundle.message("message.FlowRenameConflict", plan.newPackage()));
                    }
                }
                super.performRefactoring(usages);
                for (PsiDirectory directory : plan.directories()) {
                    if (!directory.isValid() || !directory.getName().equals(plan.newPackage().substring(plan.newPackage().lastIndexOf('.') + 1))) {
                        throw new IllegalStateException("Package refactoring did not complete");
                    }
                }
                for (BeanEdit edit : beanEdits) {
                    PsiLiteralExpression literal = edit.literal().getElement();
                    if (literal == null) throw new IllegalStateException("A bean reference changed during refactoring");
                    literal.replace(JavaPsiFacade.getElementFactory(plan.project()).createExpressionFromText(
                            "\"" + edit.replacement() + "\"", literal));
                }
                afterPackageRename.run();
                completed = true;
            } catch (RuntimeException failure) {
                rollback(failure);
                throw failure;
            }
        }

        private void rollback(RuntimeException failure) {
            String oldLeaf = plan.oldPackage().substring(plan.oldPackage().lastIndexOf('.') + 1);
            try {
                WriteAction.run(() -> {
                    for (PsiDirectory directory : plan.directories()) {
                        if (directory.isValid() && !oldLeaf.equals(directory.getName())) directory.setName(oldLeaf);
                    }
                    for (Original original : originals.values()) {
                        var document = FileDocumentManager.getInstance().getCachedDocument(original.file());
                        if (document != null) PsiDocumentManager.getInstance(plan.project()).doPostponedOperationsAndUnblockDocument(document);
                        original.file().setBinaryContent(original.bytes());
                        if (document != null) {
                            if (original.documentText() != null) document.setText(original.documentText());
                            else FileDocumentManager.getInstance().reloadFromDisk(document);
                        }
                    }
                    PsiDocumentManager.getInstance(plan.project()).commitAllDocuments();
                });
            } catch (Exception rollbackFailure) { failure.addSuppressed(rollbackFailure); }
        }
    }
}
