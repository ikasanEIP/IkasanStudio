package org.ikasan.studio.intellij.project;

import com.intellij.ide.highlighter.JavaFileType;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VFileProperty;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileVisitor;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import org.ikasan.studio.core.persistence.json.FlowClipboard;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Transfers detached source text: never moves or changes files in the source project. */
public final class FlowSourceTransfer {
    private static final String SOURCE_ROOT = "user/src/main/java/";
    private static final int MAX_SOURCE_LENGTH = 8 * 1024 * 1024;

    private FlowSourceTransfer() { }

    /** Call under a background read action. Includes unsaved editor text and package-local helpers. */
    public static FlowClipboard.Sources capture(Project project, String packageName) throws IOException {
        VirtualFile base = StudioProjectFiles.getProjectBaseDir(project);
        if (base == null) throw new IOException("Source project is unavailable");
        rejectSymlinks(base, SOURCE_ROOT + packageName.replace('.', '/'));
        VirtualFile directory = base.findFileByRelativePath(SOURCE_ROOT + packageName.replace('.', '/'));
        Map<String, String> files = new TreeMap<>();
        if (directory != null) collect(directory, files);
        FlowClipboard.Sources sources = new FlowClipboard.Sources(packageName, files);
        sources.validate();
        return sources;
    }

    private static void collect(VirtualFile directory, Map<String, String> files) throws IOException {
        VfsUtilCore.visitChildrenRecursively(directory, new VirtualFileVisitor<Void>() {
            private int length;

            @Override public boolean visitFile(@NotNull VirtualFile file) {
                ProgressManager.checkCanceled();
                try {
                    if (file.is(VFileProperty.SYMLINK)) throw new IOException("Linked source files cannot be copied");
                    if (!file.isDirectory() && file.getName().endsWith(".java")) {
                        if (file.getLength() > MAX_SOURCE_LENGTH) throw new IOException("Source exceeds clipboard size limit");
                        var document = FileDocumentManager.getInstance().getDocument(file);
                        if (document == null) throw new IOException("Source text is unavailable");
                        String text = document.getText();
                        length += text.length();
                        if (length > MAX_SOURCE_LENGTH) throw new IOException("Sources exceed clipboard size limit");
                        String path = VfsUtilCore.getRelativePath(file, directory, '/');
                        if (path == null) throw new IOException("Source path is unavailable");
                        files.put(path, text);
                    }
                    return true;
                } catch (IOException failure) {
                    throw new VisitorException(failure);
                }
            }
        }, IOException.class);
    }

    /** Prepare detached Java text in a background read action, before modifying the destination. */
    public static FlowClipboard.Sources relocate(Project project, FlowClipboard.Sources sources, String newPackage) throws IOException {
        sources.validate();
        Map<String, String> files = new TreeMap<>();
        for (var entry : sources.files().entrySet()) {
            ProgressManager.checkCanceled();
            String path = entry.getKey();
            String text = entry.getValue();
            PsiJavaFile javaFile = (PsiJavaFile) PsiFileFactory.getInstance(project)
                    .createFileFromText(path, JavaFileType.INSTANCE, text);
            int slash = path.lastIndexOf('/');
            String suffix = slash < 0 ? "" : "." + path.substring(0, slash).replace('/', '.');
            if (!javaFile.getPackageName().equals(sources.packageName() + suffix)) {
                throw new IOException("Source package does not match its file path");
            }
            Set<TextRange> references = new HashSet<>();
            Map<TextRange, String> replacements = new HashMap<>();
            javaFile.accept(new JavaRecursiveElementWalkingVisitor() {
                private void reference(PsiElement element) {
                    if (element.getText().equals(sources.packageName())) references.add(element.getTextRange());
                }
                @Override public void visitReferenceElement(@NotNull PsiJavaCodeReferenceElement reference) {
                    reference(reference);
                    super.visitReferenceElement(reference);
                }
                @Override public void visitReferenceExpression(@NotNull PsiReferenceExpression expression) {
                    reference(expression);
                    super.visitReferenceExpression(expression);
                }
                @Override public void visitLiteralExpression(@NotNull PsiLiteralExpression expression) {
                    if (expression.getValue() instanceof String value && value.startsWith(sources.packageName() + ".")
                            && isBeanIdentity(expression)) {
                        replacements.put(expression.getTextRange(), "\"" + StringUtil.escapeStringCharacters(newPackage
                                + value.substring(sources.packageName().length())) + "\"");
                    }
                    super.visitLiteralExpression(expression);
                }
            });
            references.forEach(range -> replacements.put(range, newPackage));
            StringBuilder result = new StringBuilder(text);
            replacements.entrySet().stream().sorted(Map.Entry.<TextRange, String>comparingByKey(
                    Comparator.comparingInt(TextRange::getStartOffset)).reversed()).forEach(replacement ->
                    result.replace(replacement.getKey().getStartOffset(), replacement.getKey().getEndOffset(), replacement.getValue()));
            files.put(path, result.toString());
        }
        FlowClipboard.Sources result = new FlowClipboard.Sources(newPackage, files);
        result.validate();
        return result;
    }

    private static boolean isBeanIdentity(@NotNull PsiLiteralExpression expression) {
        if (!(expression.getParent() instanceof PsiNameValuePair pair)) return false;
        PsiAnnotation annotation = PsiTreeUtil.getParentOfType(pair, PsiAnnotation.class);
        if (annotation == null || annotation.getNameReferenceElement() == null) return false;
        String name = annotation.getNameReferenceElement().getText();
        String attribute = pair.getName();
        return ((name.equals("Component") || name.equals("org.springframework.stereotype.Component")
                || name.equals("Qualifier") || name.equals("org.springframework.beans.factory.annotation.Qualifier"))
                && (attribute == null || attribute.equals("value")))
                || ((name.equals("Resource") || name.equals("javax.annotation.Resource") || name.equals("jakarta.annotation.Resource"))
                && "name".equals(attribute));
    }

    /** New files only. A collision or write failure leaves existing destination files untouched. */
    public static void install(Project project, FlowClipboard.Sources sources) throws IOException {
        install(project, sources, path -> { });
    }

    @FunctionalInterface
    interface WriteFailureInjector { void beforeWrite(String path) throws IOException; }

    static void install(Project project, FlowClipboard.Sources sources, WriteFailureInjector injector) throws IOException {
        sources.validate();
        if (sources.files().isEmpty()) return;
        VirtualFile base = StudioProjectFiles.getProjectBaseDir(project);
        if (base == null) throw new IOException("Destination project is unavailable");
        String packagePath = SOURCE_ROOT + sources.packageName().replace('.', '/');
        List<VirtualFile> created = new ArrayList<>();
        WriteAction.run(() -> {
            rejectSymlinks(base, packagePath);
            if (base.findFileByRelativePath(packagePath) != null) throw new DestinationExists(sources.packageName());
            try {
                for (var entry : new TreeMap<>(sources.files()).entrySet()) {
                    String[] segments = (packagePath + "/" + entry.getKey()).split("/");
                    VirtualFile directory = base;
                    for (int i = 0; i < segments.length - 1; i++) {
                        VirtualFile next = directory.findChild(segments[i]);
                        if (next == null) {
                            next = directory.createChildDirectory(FlowSourceTransfer.class, segments[i]);
                            created.add(next);
                        }
                        directory = next;
                    }
                    String name = segments[segments.length - 1];
                    if (directory.findChild(name) != null) throw new DestinationExists(sources.packageName());
                    injector.beforeWrite(entry.getKey());
                    VirtualFile file = directory.createChildData(FlowSourceTransfer.class, name);
                    created.add(file);
                    file.setBinaryContent(entry.getValue().getBytes(StandardCharsets.UTF_8));
                }
            } catch (IOException | RuntimeException failure) {
                for (int i = created.size() - 1; i >= 0; i--) {
                    try { if (created.get(i).isValid()) created.get(i).delete(FlowSourceTransfer.class); }
                    catch (IOException rollback) { failure.addSuppressed(rollback); }
                }
                throw failure;
            }
        });
    }

    private static void rejectSymlinks(VirtualFile base, String relativePath) throws IOException {
        VirtualFile current = base;
        for (String segment : relativePath.split("/")) {
            current = current.findChild(segment);
            if (current == null) return;
            if (current.is(VFileProperty.SYMLINK)) throw new IOException("Linked source paths are not supported");
        }
    }

    public static final class DestinationExists extends IOException {
        public final String packageName;
        public DestinationExists(String packageName) {
            super("The destination source package already exists");
            this.packageName = packageName;
        }
    }
}
