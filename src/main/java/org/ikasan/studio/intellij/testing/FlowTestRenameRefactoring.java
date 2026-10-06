package org.ikasan.studio.intellij.testing;

import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.refactoring.rename.RenamePsiElementProcessor;
import com.intellij.refactoring.rename.RenameUtil;
import com.intellij.refactoring.rename.UnresolvableCollisionUsageInfo;
import com.intellij.refactoring.listeners.RefactoringElementListener;
import com.intellij.refactoring.listeners.RefactoringElementListenerProvider;
import com.intellij.usageView.UsageInfo;
import org.ikasan.studio.core.StudioBuildUtils;
import org.ikasan.studio.core.generator.ModelTemplate;
import org.ikasan.studio.core.io.ComponentIO;
import org.ikasan.studio.core.model.ikasan.instance.Flow;
import org.ikasan.studio.core.model.ikasan.instance.FlowElement;
import org.ikasan.studio.core.model.ikasan.instance.Module;
import org.ikasan.studio.intellij.project.StudioProjectFiles;
import org.ikasan.studio.ui.StudioBundle;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

/** Targeted refactoring of recognised test references; never regenerates business scenarios. */
public final class FlowTestRenameRefactoring {
    private FlowTestRenameRefactoring() { }
    private record Edit(SmartPsiElementPointer<PsiLiteralExpression> pointer, String before, String after) { }
    private record TextEdit(com.intellij.openapi.editor.Document document, String before, String after) { }
    private record Move(VirtualFile directory, String before, String after) { }
    private record ClassRename(SmartPsiElementPointer<PsiClass> pointer, VirtualFile file,
                               String before, String after, UsageInfo[] usages) { }
    public static final class Plan {
        private final Project project;
        private final List<Edit> edits = new ArrayList<>();
        private final List<TextEdit> textEdits = new ArrayList<>();
        private final List<ClassRename> classes = new ArrayList<>();
        private final Map<com.intellij.openapi.editor.Document, String> classDocuments = new LinkedHashMap<>();
        private final List<Move> moves = new ArrayList<>();
        private final Set<String> review = new TreeSet<>();
        private Plan(Project project) { this.project = project; }
        public Set<String> reviewFiles() { return Collections.unmodifiableSet(review); }

        /** Called inside the rename write command. Restores test edits if model persistence fails. */
        public void apply(Runnable persistModel) {
            Map<com.intellij.openapi.editor.Document, String> originals = new LinkedHashMap<>();
            // Package refactoring may already have updated imports in these documents.
            classDocuments.keySet().forEach(document -> originals.put(document, document.getText()));
            List<Runnable> notifications = new ArrayList<>();
            List<Move> moved = new ArrayList<>();
            List<TextEdit> changedText = new ArrayList<>();
            try {
                for (Edit edit : edits) {
                    var literal = edit.pointer().getElement();
                    if (literal == null || !edit.before().equals(literal.getValue()))
                        throw new IllegalStateException(StudioBundle.message("flowTest.renameChanged"));
                }
                for (Move move : moves) {
                    if (!move.directory().isValid() || !move.before().equals(move.directory().getName())
                            || move.directory().getParent().findChild(move.after()) != null)
                        throw new IllegalStateException(StudioBundle.message("flowTest.renameConflict", move.after()));
                }
                for (TextEdit edit : textEdits) {
                    if (!edit.before().equals(edit.document().getText()))
                        throw new IllegalStateException(StudioBundle.message("flowTest.renameChanged"));
                }
                for (ClassRename rename : classes) {
                    PsiClass clazz = rename.pointer().getElement();
                    if (clazz == null || !rename.before().equals(clazz.getName())
                            || rename.file().getParent().findChild(rename.after() + ".java") != null)
                        throw new IllegalStateException(StudioBundle.message("flowTest.renameConflict", rename.after()));
                }
                for (Edit edit : edits) {
                    var literal = edit.pointer().getElement();
                    if (literal == null) throw new IllegalStateException(StudioBundle.message("flowTest.renameChanged"));
                    var document = PsiDocumentManager.getInstance(project).getDocument(literal.getContainingFile());
                    if (document != null) originals.putIfAbsent(document, document.getText());
                }
                for (Edit edit : edits) replace(edit, edit.after());
                for (ClassRename rename : classes) {
                    PsiClass clazz = rename.pointer().getElement();
                    if (clazz == null) throw new IllegalStateException(StudioBundle.message("flowTest.renameChanged"));
                    List<RefactoringElementListener> listeners = new ArrayList<>();
                    for (var provider : RefactoringElementListenerProvider.EP_NAME.getExtensionList(project)) {
                        var listener = provider.getListener(clazz);
                        if (listener != null) listeners.add(listener);
                    }
                    RenamePsiElementProcessor.forElement(clazz).renameElement(clazz, rename.after(), rename.usages(),
                            new RefactoringElementListener() {
                                @Override public void elementMoved(@NotNull PsiElement element) { }
                                @Override public void elementRenamed(@NotNull PsiElement element) {
                                    notifications.add(() -> listeners.forEach(listener -> listener.elementRenamed(element)));
                                }
                            });
                    if (!rename.after().equals(clazz.getName()) || !rename.file().getName().equals(rename.after() + ".java"))
                        throw new IllegalStateException(StudioBundle.message("flowTest.renameChanged"));
                }
                for (TextEdit edit : textEdits) { edit.document().setText(edit.after()); changedText.add(edit); }
                for (Move move : moves) { move.directory().rename(this, move.after()); moved.add(move); }
                saveEditedDocuments();
                saveDocuments(originals.keySet());
                persistModel.run();
            } catch (Exception failure) {
                try {
                    Collections.reverse(moved);
                    for (Move move : moved) move.directory().rename(this, move.before());
                    for (TextEdit edit : changedText) edit.document().setText(edit.before());
                    for (ClassRename rename : classes) {
                        if (rename.file().isValid() && !rename.file().getName().equals(rename.before() + ".java"))
                            rename.file().rename(this, rename.before() + ".java");
                    }
                    originals.forEach(com.intellij.openapi.editor.Document::setText);
                    PsiDocumentManager.getInstance(project).commitAllDocuments();
                    saveDocuments(originals.keySet());
                    saveDocuments(textEdits.stream().map(TextEdit::document).toList());
                } catch (Exception restore) { failure.addSuppressed(restore); }
                throw failure instanceof RuntimeException runtime ? runtime : new IllegalStateException(failure);
            }
            // Notify run configurations only after the model and files were successfully saved.
            for (Runnable notification : notifications) {
                try { notification.run(); }
                catch (RuntimeException failure) {
                    com.intellij.openapi.diagnostic.Logger.getInstance(FlowTestRenameRefactoring.class)
                            .warn("A test rename listener could not update its references", failure);
                    review.addAll(classes.stream().map(rename -> rename.after() + ".java").toList());
                }
            }
        }
        private void saveDocuments(Collection<com.intellij.openapi.editor.Document> documents) {
            var psi = PsiDocumentManager.getInstance(project);
            psi.commitAllDocuments();
            for (var document : documents) {
                psi.doPostponedOperationsAndUnblockDocument(document);
                FileDocumentManager.getInstance().saveDocument(document);
                if (FileDocumentManager.getInstance().isDocumentUnsaved(document))
                    throw new IllegalStateException(StudioBundle.message("flowTest.renameSaveFailed"));
            }
        }
        private void replace(Edit edit, String value) {
            var literal = edit.pointer().getElement();
            if (literal == null) throw new IllegalStateException(StudioBundle.message("flowTest.renameChanged"));
            literal.replace(JavaPsiFacade.getElementFactory(project).createExpressionFromText(
                    org.ikasan.studio.core.persistence.json.StudioJson.newObjectMapper().valueToTree(value).toString(), literal));
        }
        private void saveEditedDocuments() {
            var psi = PsiDocumentManager.getInstance(project);
            psi.commitAllDocuments();
            for (TextEdit edit : textEdits) {
                FileDocumentManager.getInstance().saveDocument(edit.document());
                if (FileDocumentManager.getInstance().isDocumentUnsaved(edit.document()))
                    throw new IllegalStateException(StudioBundle.message("flowTest.renameSaveFailed"));
            }
            for (Edit edit : edits) {
                var literal = edit.pointer().getElement();
                if (literal == null) continue;
                var document = psi.getDocument(literal.getContainingFile());
                if (document != null) {
                    psi.doPostponedOperationsAndUnblockDocument(document);
                    FileDocumentManager.getInstance().saveDocument(document);
                    if (FileDocumentManager.getInstance().isDocumentUnsaved(document))
                        throw new IllegalStateException(StudioBundle.message("flowTest.renameSaveFailed"));
                }
            }
        }
    }

    /** Prepare PSI and directory discovery off EDT, before changing the model. */
    // Swing callbacks in newer IDEs do not necessarily hold write-intent access.
    @SuppressWarnings("UnstableApiUsage")
    public static Plan prepare(Project project, Module module, Flow flow, FlowElement component, String newName) {
        AtomicReference<Plan> result = new AtomicReference<>();
        AtomicReference<RuntimeException> failure = new AtomicReference<>();
        com.intellij.openapi.application.WriteIntentReadAction.run((Runnable) () ->
                ProgressManager.getInstance().runProcessWithProgressSynchronously(() -> {
                    try { result.set(ReadAction.compute(() -> inspect(project, module, flow, component, newName))); }
                    catch (RuntimeException ex) { failure.set(ex); }
                }, StudioBundle.message("flowTest.renamePreparing"), false, project));
        if (failure.get() != null) throw failure.get();
        return result.get();
    }

    static Plan inspect(Project project, Module module, Flow flow, FlowElement component, String newName) {
        Plan plan = new Plan(project);
        VirtualFile base = StudioProjectFiles.getProjectBaseDir(project);
        if (base == null) return plan;
        String oldName = component == null ? flow.getIdentity() : component.getIdentity();
        String oldFlow = flow.getIdentity();
        String resourceBefore = oldFlow.replace(' ', '_');
        String resourceAfter = component == null ? newName.replace(' ', '_') : resourceBefore;
        if (component != null) {
            resourceBefore += "/" + oldName.replace(' ', '_');
            resourceAfter += "/" + newName.replace(' ', '_');
        }
        for (String segment : List.of(oldName, newName, oldFlow)) {
            if (segment.contains("/") || segment.contains("\\") || segment.equals(".") || segment.equals(".."))
                throw new IllegalStateException(StudioBundle.message("flowTest.renameConflict", segment));
        }
        VirtualFile resources = base.findFileByRelativePath("user-flow-tests/src/test/resources");
        VirtualFile source = resources == null ? null : resources.findFileByRelativePath(resourceBefore);
        if (source != null && !resourceBefore.equals(resourceAfter)) {
            String leaf = newName.replace(' ', '_');
            if (!source.isDirectory() || source.getParent().findChild(leaf) != null)
                throw new IllegalStateException(StudioBundle.message("flowTest.renameConflict", resourceAfter));
            // Two logical names can map to one fixture directory; never move shared fixtures silently.
            long aliases = component == null ? module.getFlows().stream()
                    .filter(f -> f.getIdentity().replace(' ', '_').equals(oldFlow.replace(' ', '_'))).count()
                    : flow.getFlowElementsNoExternalEndPoints().stream()
                    .filter(e -> e.getIdentity().replace(' ', '_').equals(oldName.replace(' ', '_'))).count();
            if (aliases > 1) throw new IllegalStateException(StudioBundle.message("flowTest.renameConflict", resourceBefore));
            plan.moves.add(new Move(source, source.getName(), leaf));
        }
        Map<String, String> keys = propertyRenames(module, flow, component, newName);
        VirtualFile properties = resources == null ? null : resources.findChild("module-test.properties");
        if (properties != null) {
            var manager = FileDocumentManager.getInstance();
            var document = manager.getDocument(properties);
            if (document != null) {
                String before = document.getText();
                String after = before;
                for (var entry : keys.entrySet()) {
                    String oldKey = entry.getKey(), newKey = entry.getValue();
                    String oldPattern = "(?m)^(\\s*)" + java.util.regex.Pattern.quote(oldKey) + "(?=\\s*[=:])";
                    if (java.util.regex.Pattern.compile(oldPattern).matcher(after).find()
                            && java.util.regex.Pattern.compile("(?m)^\\s*" + java.util.regex.Pattern.quote(newKey) + "\\s*[=:]").matcher(after).find())
                        throw new IllegalStateException(StudioBundle.message("flowTest.renamePropertyConflict", newKey));
                    after = after.replaceAll(oldPattern, "$1" + java.util.regex.Matcher.quoteReplacement(newKey));
                    after = after.replace("${" + oldKey + "}", "${" + newKey + "}")
                            .replace("${" + oldKey + ":", "${" + newKey + ":");
                }
                if (!before.equals(after)) {
                    if (manager.isDocumentUnsaved(document))
                        throw new IllegalStateException(StudioBundle.message("flowTest.renameUnsaved", properties.getName()));
                    plan.textEdits.add(new TextEdit(document, before, after));
                }
            }
        }
        VirtualFile tests = base.findFileByRelativePath("user-flow-tests/src/test/java");
        if (tests != null) inspectFiles(plan, tests, oldFlow, oldName, newName, component == null,
                "/" + resourceBefore + "/", "/" + resourceAfter + "/", keys);
        return plan;
    }

    private static void inspectFiles(Plan plan, VirtualFile directory, String flow, String oldName,
                                     String newName, boolean flowRename, String oldResource, String newResource,
                                     Map<String, String> keys) {
        Deque<VirtualFile> pending = new ArrayDeque<>();
        pending.add(directory);
        while (!pending.isEmpty()) {
            VirtualFile file = pending.removeFirst();
            if (file.isDirectory()) {
                if (file == directory || !file.getName().equals("support"))
                    Collections.addAll(pending, file.getChildren());
                continue;
            }
            if (!file.getName().endsWith(".java")) continue;
            PsiFile psi = PsiManager.getInstance(plan.project).findFile(file);
            if (!(psi instanceof PsiJavaFile)) continue;
            for (PsiClass clazz : PsiTreeUtil.findChildrenOfType(psi, PsiClass.class)) {
                boolean belongs = false;
                for (PsiLiteralExpression literal : PsiTreeUtil.findChildrenOfType(clazz, PsiLiteralExpression.class)) {
                    if (PsiTreeUtil.getParentOfType(literal, PsiClass.class) == clazz
                            && flow.equals(literal.getValue()) && isFlowIdentity(literal)) belongs = true;
                }
                if (!belongs) {
                    // Computed/inherited identities cannot be attributed safely; make likely leftovers visible.
                    for (PsiLiteralExpression literal : PsiTreeUtil.findChildrenOfType(clazz, PsiLiteralExpression.class)) {
                        if (literal.getValue() instanceof String value
                                && (value.equals(flow) || value.startsWith(oldResource) || keys.containsKey(value)))
                            plan.review.add(file.getName());
                    }
                    continue;
                }
                var doc = FileDocumentManager.getInstance().getCachedDocument(file);
                if (doc != null && FileDocumentManager.getInstance().isDocumentUnsaved(doc))
                    throw new IllegalStateException(StudioBundle.message("flowTest.renameUnsaved", file.getName()));
                if (flowRename) prepareClassRename(plan, clazz, flow, newName);
                for (PsiLiteralExpression literal : PsiTreeUtil.findChildrenOfType(clazz, PsiLiteralExpression.class)) {
                    if (PsiTreeUtil.getParentOfType(literal, PsiClass.class) != clazz
                            || !(literal.getValue() instanceof String value)) continue;
                    String replacement = null;
                    PsiField field = PsiTreeUtil.getParentOfType(literal, PsiField.class);
                    PsiMethod method = PsiTreeUtil.getParentOfType(literal, PsiMethod.class);
                    PsiMethodCallExpression call = PsiTreeUtil.getParentOfType(literal, PsiMethodCallExpression.class);
                    String callName = call == null ? "" : Objects.toString(call.getMethodExpression().getReferenceName(), "");
                    if (flowRename && oldName.equals(value) && isFlowIdentity(literal)) replacement = newName;
                    if (!flowRename && oldName.equals(value)) {
                        if (field != null && field.getInitializer() == literal
                                && Set.of("CONSUMER_NAME", "PRODUCER_NAME").contains(field.getName())) replacement = newName;
                        if (method != null && "defineExpectedPath".equals(method.getName()) && Set.of("consumer", "scheduledConsumer",
                                "converter", "translator", "broker", "filter", "splitter", "router", "multiRecipientRouter",
                                "sequencer", "producer").contains(callName)) replacement = newName;
                        if (call != null && Set.of("runObservationTest", "runTest").contains(callName)) {
                            var args = call.getArgumentList().getExpressions();
                            if (args.length > 1 && args[1] == literal) replacement = newName;
                        }
                    }
                    if (keys.containsKey(value) && Set.of("getRequiredProperty", "getProperty").contains(callName))
                        replacement = keys.get(value);
                    if (value.startsWith(oldResource) && field != null && field.getInitializer() == literal
                            && (field.getName().endsWith("_RESOURCE") || field.getName().endsWith("_FILENAME")))
                        replacement = newResource + value.substring(oldResource.length());
                    if (replacement != null && !replacement.equals(value))
                        plan.edits.add(new Edit(SmartPointerManager.createPointer(literal), value, replacement));
                    else if (value.equals(oldName) || value.startsWith(oldResource) || keys.containsKey(value))
                        plan.review.add(file.getName());
                }
            }
        }
    }
    private static void prepareClassRename(Plan plan, PsiClass clazz, String oldFlow, String newFlow) {
        String before = StudioBuildUtils.toJavaClassName(oldFlow) + "FlowTest";
        String after = StudioBuildUtils.toJavaClassName(newFlow) + "FlowTest";
        VirtualFile file = clazz.getContainingFile().getVirtualFile();
        if (before.equals(after) || !before.equals(clazz.getName()) || clazz.getContainingClass() != null
                || !file.getName().equals(before + ".java")) return;
        if (!PsiNameHelper.getInstance(plan.project).isIdentifier(after)
                || file.getParent().findChild(after + ".java") != null)
            throw new IllegalStateException(StudioBundle.message("flowTest.renameConflict", after));
        UsageInfo[] usages = RenameUtil.findUsages(clazz, after, false, false, Map.of(clazz, after));
        for (UsageInfo usage : usages) {
            if (usage instanceof UnresolvableCollisionUsageInfo)
                throw new IllegalStateException(StudioBundle.message("flowTest.renameConflict", after));
            if (usage.getFile() != null) rememberClassDocument(plan, usage.getFile());
        }
        rememberClassDocument(plan, clazz.getContainingFile());
        plan.classes.add(new ClassRename(SmartPointerManager.createPointer(clazz), file, before, after, usages));
    }

    private static void rememberClassDocument(Plan plan, PsiFile file) {
        var document = PsiDocumentManager.getInstance(plan.project).getDocument(file);
        if (document == null || !file.isWritable())
            throw new IllegalStateException(StudioBundle.message("flowTest.renameConflict", file.getName()));
        if (FileDocumentManager.getInstance().isDocumentUnsaved(document))
            throw new IllegalStateException(StudioBundle.message("flowTest.renameUnsaved", file.getName()));
        plan.classDocuments.putIfAbsent(document, document.getText());
    }

    private static boolean isFlowIdentity(PsiLiteralExpression literal) {
        PsiField field = PsiTreeUtil.getParentOfType(literal, PsiField.class);
        PsiMethod method = PsiTreeUtil.getParentOfType(literal, PsiMethod.class);
        if (field != null) return "FLOW_NAME".equals(field.getName()) && field.getInitializer() == literal;
        if (method == null || !"getFlowName".equals(method.getName()) || method.getBody() == null) return false;
        var statements = method.getBody().getStatements();
        return statements.length == 1 && statements[0] instanceof PsiReturnStatement returned
                && returned.getReturnValue() == literal;
    }
    private static Map<String, String> propertyRenames(Module module, Flow flow, FlowElement component, String name) {
        Module copy;
        try { copy = ComponentIO.validatePersistedModuleJson(ModelTemplate.create(module), "test rename", false); }
        catch (org.ikasan.studio.core.StudioBuildException failure) { throw new IllegalStateException(failure); }
        Flow copyFlow = copy.getFlows().get(module.getFlows().indexOf(flow));
        var originalElements = flow.getFlowElementsNoExternalEndPoints();
        var copiedElements = copyFlow.getFlowElementsNoExternalEndPoints();
        if (component == null) copyFlow.setName(name);
        else {
            int index = originalElements.indexOf(component);
            if (index < 0) return Map.of(); // External canvas endpoints do not own test component mappings.
            copiedElements.get(index).setName(name);
        }
        Map<String, String> mappings = new LinkedHashMap<>();
        for (int i = 0; i < originalElements.size(); i++) {
            var before = originalElements.get(i);
            var after = copiedElements.get(i);
            for (var property : before.getComponentPropertyList()) {
                String label = property.getMeta().getPropertyConfigFileLabel();
                if (label == null || label.isBlank()) continue;
                String oldKey = StudioBuildUtils.substitutePlaceholderInLowerCase(module, flow, before, label);
                String newKey = StudioBuildUtils.substitutePlaceholderInLowerCase(copy, copyFlow, after, label);
                if (!oldKey.equals(newKey)) mappings.put(oldKey, newKey);
            }
        }
        // Explicit shared property labels may still belong to another endpoint after this rename.
        // Such references are ambiguous and must retain their existing meaning.
        Set<String> retainedKeys = new HashSet<>();
        for (Flow remainingFlow : copy.getFlows()) for (var element : remainingFlow.getFlowElementsNoExternalEndPoints()) {
            for (var property : element.getComponentPropertyList()) {
                String label = property.getMeta().getPropertyConfigFileLabel();
                if (label != null && !label.isBlank()) retainedKeys.add(
                        StudioBuildUtils.substitutePlaceholderInLowerCase(copy, remainingFlow, element, label));
            }
        }
        mappings.keySet().removeAll(retainedKeys);
        return mappings;
    }

    // Required before entering a write command from a Swing callback without implicit read access.
    @SuppressWarnings("UnstableApiUsage")
    public static void renameComponentAndSave(Project project, Module module, FlowElement component, String newName) {
        var context = project.getService(org.ikasan.studio.ui.UiContext.class);
        if (context.isModelPersistenceBlocked()) throw new IllegalStateException(context.getModelPersistenceBlockReason());
        Plan plan = prepare(project, module, component.getContainingFlow(), component, newName);
        String oldName = component.getIdentity();
        Map<Flow, String> owners = new LinkedHashMap<>();
        var links = org.ikasan.studio.core.model.analysis.TestJmsHarnessLinks.findLinks(module).stream()
                .filter(link -> link.ownerProducer() == component).toList();
        links.forEach(link -> owners.put(link.harnessFlow(), (String) link.harnessFlow().getPropertyValue("testHarnessOwner")));
        com.intellij.openapi.application.WriteIntentReadAction.run((Runnable) () ->
                WriteCommandAction.runWriteCommandAction(project, () -> {
                    try {
                        component.setName(newName);
                        links.forEach(link -> link.harnessFlow().setPropertyValue("testHarnessOwner",
                                org.ikasan.studio.core.model.analysis.TestJmsHarnessLinks.ownerKeyFor(component)));
                        plan.apply(() -> StudioProjectFiles.replaceJsonModelFileSafely(project, ModelTemplate.create(module)));
                        var base = StudioProjectFiles.getProjectBaseDir(project);
                        var modelFile = base == null ? null : base.findFileByRelativePath("generated/src/main/model/model.json");
                        if (modelFile != null) com.intellij.openapi.command.undo.UndoManager.getInstance(project)
                                .nonundoableActionPerformed(com.intellij.openapi.command.undo.DocumentReferenceManager
                                        .getInstance().create(modelFile), true);
                    } catch (RuntimeException failure) {
                        component.setName(oldName);
                        owners.forEach((flow, owner) -> flow.setPropertyValue("testHarnessOwner", owner));
                        throw failure;
                    }
                }));
        reportReview(plan);
    }

    public static void reportReview(Plan plan) {
        if (!plan.review.isEmpty()) com.intellij.openapi.ui.Messages.showWarningDialog(plan.project,
                StudioBundle.message("flowTest.renameReview", String.join("\n", plan.review)),
                StudioBundle.message("flowTest.renamePreparing"));
    }
}
