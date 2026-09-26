package org.ikasan.studio.intellij.project;

import com.intellij.ide.highlighter.JavaFileType;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiErrorElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiFileFactory;
import com.intellij.psi.util.PsiTreeUtil;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.ikasan.studio.StudioRuntimeException;
import org.ikasan.studio.ui.viewmodel.AbstractViewHandlerIntellij;

import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Stages, validates, commits and—on failure—rolls back one coherent source-generation update. */
final class GenerationTransactionManager {
    private static final ThreadLocal<GenerationBatch> ACTIVE = new ThreadLocal<>();

    private GenerationTransactionManager() { }

    record Summary(int created, int updated, int unchanged) { }

    static boolean isActive() {
        return ACTIVE.get() != null;
    }

    static void begin() {
        if (ACTIVE.get() != null) throw new IllegalStateException("A generation transaction is already active");
        ACTIVE.set(new GenerationBatch());
    }

    static void abort() {
        ACTIVE.remove();
    }

    /** @return true when the write was captured instead of applied. */
    static boolean stage(String relativePath, String content, AbstractViewHandlerIntellij viewHandler) {
        GenerationBatch batch = ACTIVE.get();
        if (batch == null) return false;
        batch.stage(safePath(relativePath), content, viewHandler);
        return true;
    }

    static void authoriseUserReplacement(String relativePath) {
        GenerationBatch batch = ACTIVE.get();
        if (batch == null) throw new IllegalStateException("No generation transaction is active");
        batch.authoriseUserReplacement(safePath(relativePath));
    }

    static void afterCommit(Runnable action) {
        GenerationBatch batch = ACTIVE.get();
        if (batch == null) throw new IllegalStateException("No generation transaction is active");
        batch.afterCommit(action);
    }

    @FunctionalInterface
    interface WriteFailureInjector { void beforeWrite(String path, int index) throws IOException; }

    static Summary commit(Project project) {
        return commit(project, (path, index) -> { });
    }

    static Summary commit(Project project, WriteFailureInjector failureInjector) {
        GenerationBatch batch = ACTIVE.get();
        if (batch == null) throw new IllegalStateException("No generation transaction is active");
        ACTIVE.remove(); // subsequent low-level writes must now reach disk
        List<GenerationBatch.Artifact> artifacts = batch.artifacts();
        validate(project, artifacts);

        VirtualFile baseDir = StudioProjectFiles.getProjectBaseDir(project);
        if (baseDir == null) throw new StudioRuntimeException("Project base directory is unavailable; no generated files were changed");
        List<Snapshot> snapshots = prepareSnapshots(project, baseDir, artifacts);
        // Preparation pumps modal events; reject edits before writing any member of the batch.
        for (Snapshot snapshot : snapshots) snapshot.verifyUnchanged(baseDir);
        for (int index = 0; index < artifacts.size(); index++) {
            GenerationBatch.Artifact artifact = artifacts.get(index);
            VirtualFile existing = snapshots.get(index).file();
            if (existing != null && artifact.relativePath().startsWith("user/")
                    && !batch.isUserReplacementAuthorised(artifact.relativePath())) {
                byte[] oldBytes = snapshots.get(index).bytes();
                String oldContent = new String(oldBytes, StandardCharsets.UTF_8);
                if (!oldContent.equals(preserveLineEndings(oldBytes, artifact.content()))) {
                    throw new StudioRuntimeException("Refusing to replace developer-owned file without explicit confirmation: "
                            + artifact.relativePath());
                }
            }
        }
        List<Original> originals = new ArrayList<>();
        int created = 0, updated = 0, unchanged = 0;
        try {
            for (int index = 0; index < artifacts.size(); index++) {
                GenerationBatch.Artifact artifact = artifacts.get(index);
                Snapshot snapshot = snapshots.get(index);
                snapshot.verifyUnchanged(baseDir);
                VirtualFile existing = snapshot.file();
                byte[] oldBytes = snapshot.bytes();
                originals.add(new Original(artifact.relativePath(), oldBytes));
                String committedContent = preserveLineEndings(oldBytes, artifact.content());
                if (oldBytes == null) created++;
                else if (committedContent.equals(new String(oldBytes, StandardCharsets.UTF_8))
                        || StudioProjectFiles.canReuseGeneratedContent(existing, committedContent, oldBytes)) unchanged++;
                else updated++;
                failureInjector.beforeWrite(artifact.relativePath(), originals.size() - 1);
                StudioProjectFiles.createFileWithDirectories(project, "/" + artifact.relativePath(),
                        committedContent, artifact.viewHandler());
            }
            batch.runAfterCommit();
            return new Summary(created, updated, unchanged);
        } catch (RuntimeException | IOException failure) {
            StudioRuntimeException result = new StudioRuntimeException(
                    "Generation failed; previously existing generated files were restored", failure);
            RuntimeException rollbackFailure = rollback(baseDir, originals);
            if (rollbackFailure != null) {
                result.addSuppressed(rollbackFailure);
                throw new StudioRuntimeException(
                        "Generation failed and automatic restoration was incomplete; inspect the IDE log before continuing",
                        result);
            }
            throw result;
        }
    }

    private static void validate(Project project, List<GenerationBatch.Artifact> artifacts) {
        for (GenerationBatch.Artifact artifact : artifacts) {
            safePath(artifact.relativePath());
            byte[] bytes = artifact.content().getBytes(StandardCharsets.UTF_8);
            if (!artifact.content().equals(new String(bytes, StandardCharsets.UTF_8))) {
                throw new StudioRuntimeException("Generated artifact is not valid UTF-8: " + artifact.relativePath());
            }
            if (artifact.relativePath().endsWith(".java")) {
                PsiFile candidate = PsiFileFactory.getInstance(project).createFileFromText(
                        artifact.relativePath(), JavaFileType.INSTANCE, artifact.content());
                PsiErrorElement error = PsiTreeUtil.findChildOfType(candidate, PsiErrorElement.class);
                if (error != null) {
                    String prefix = artifact.content().substring(0, Math.min(error.getTextOffset(), artifact.content().length()));
                    long line = prefix.chars().filter(c -> c == '\n').count() + 1;
                    int column = prefix.length() - prefix.lastIndexOf('\n');
                    throw new StudioRuntimeException("Generated Java is invalid before replacement: "
                            + artifact.relativePath() + " (line " + line + ", column " + column + "): "
                            + error.getErrorDescription() + ". No source files in this generation batch were replaced.");
                }
            } else if (artifact.relativePath().endsWith("pom.xml")) {
                try {
                    new MavenXpp3Reader().read(new StringReader(artifact.content()));
                } catch (IOException | XmlPullParserException error) {
                    throw new StudioRuntimeException("Generated Maven XML is invalid before replacement: "
                            + artifact.relativePath() + ": " + error.getMessage(), error);
                }
            }
        }
    }

    record Snapshot(String path, VirtualFile file, byte[] bytes, long stamp,
                            com.intellij.openapi.editor.Document document, long documentStamp) {
        void verifyUnchanged(VirtualFile baseDir) {
            VirtualFile current = baseDir.findFileByRelativePath(path);
            var manager = com.intellij.openapi.fileEditor.FileDocumentManager.getInstance();
            var currentDocument = current == null ? null : manager.getCachedDocument(current);
            if (!java.util.Objects.equals(current, file) || (file != null && (!file.isValid() || file.getModificationStamp() != stamp))
                    || (document != null && (currentDocument != document || document.getModificationStamp() != documentStamp))
                    || (document == null && currentDocument != null && manager.isDocumentUnsaved(currentDocument)))
                throw new StudioRuntimeException("File changed while preparing generation; regenerate: " + path);
        }
    }

    /** Read rollback bytes once off the EDT, before entering any file write action. */
    static List<Snapshot> prepareSnapshots(Project project, VirtualFile baseDir,
                                                   List<GenerationBatch.Artifact> artifacts) {
        List<Snapshot> snapshots = new ArrayList<>();
        java.util.concurrent.atomic.AtomicReference<RuntimeException> failure = new java.util.concurrent.atomic.AtomicReference<>();
        Runnable prepare = () -> {
            try {
                com.intellij.openapi.application.ReadAction.run(() -> {
                    for (var artifact : artifacts) {
                        VirtualFile file = baseDir.findFileByRelativePath(artifact.relativePath());
                        try {
                            byte[] bytes = file == null ? null : readBytes(file);
                            var document = file == null ? null : com.intellij.openapi.fileEditor.FileDocumentManager.getInstance().getCachedDocument(file);
                            Snapshot snapshot = new Snapshot(artifact.relativePath(), file, bytes, file == null ? -1 : file.getModificationStamp(),
                                    document, document == null ? -1 : document.getModificationStamp());
                            snapshot.verifyUnchanged(baseDir);
                            snapshots.add(snapshot);
                        } catch (IOException error) {
                            throw new StudioRuntimeException("Could not snapshot generated file " + artifact.relativePath(), error);
                        }
                    }
                });
            } catch (RuntimeException error) { failure.set(error); }
        };
        if (com.intellij.openapi.application.ApplicationManager.getApplication().isDispatchThread()) {
            com.intellij.openapi.progress.ProgressManager.getInstance().runProcessWithProgressSynchronously(
                    prepare, org.ikasan.studio.ui.StudioBundle.message("generation.preparingFiles"), false, project);
        } else prepare.run();
        if (failure.get() != null) throw failure.get();
        return snapshots;
    }

    /** Raw VFS access retains support for remote projects; callers run in background preparation. */
    private static byte[] readBytes(VirtualFile file) throws IOException {
        if (com.intellij.openapi.application.ApplicationManager.getApplication().isDispatchThread())
            throw new IllegalStateException("Generation snapshots must be read off the EDT");
        try (java.io.InputStream in = file.getInputStream()) {
            return in.readAllBytes();
        }
    }

    private static String preserveLineEndings(byte[] existing, String generated) {
        if (existing == null) return generated;
        String old = new String(existing, StandardCharsets.UTF_8);
        String normalised = generated.replace("\r\n", "\n").replace("\r", "\n");
        return old.contains("\r\n") ? normalised.replace("\n", "\r\n") : normalised;
    }

    private static String safePath(String supplied) {
        if (supplied == null) throw new StudioRuntimeException("Generated artifact path is null");
        String candidate = supplied.replace('\\', '/');
        while (candidate.startsWith("/")) candidate = candidate.substring(1);
        Path path = Path.of(candidate).normalize();
        if (candidate.isBlank() || path.isAbsolute() || path.startsWith("..")) {
            throw new StudioRuntimeException("Refusing generated artifact outside the project: " + supplied);
        }
        return path.toString().replace('\\', '/');
    }

    private record Original(String relativePath, byte[] bytes) { }

    private static RuntimeException rollback(VirtualFile baseDir, List<Original> originals) {
        List<IOException> failures = new ArrayList<>();
        WriteAction.run(() -> {
            for (int index = originals.size() - 1; index >= 0; index--) {
                Original original = originals.get(index);
                VirtualFile file = baseDir.findFileByRelativePath(original.relativePath());
                try {
                    if (original.bytes() == null) {
                        if (file != null && file.isValid()) file.delete(GenerationTransactionManager.class);
                    } else if (file != null && file.isValid()) {
                        file.setBinaryContent(original.bytes());
                    }
                } catch (IOException rollbackFailure) {
                    failures.add(rollbackFailure);
                }
            }
        });
        if (failures.isEmpty()) return null;
        StudioRuntimeException result = new StudioRuntimeException(
                "One or more generated files could not be restored", failures.get(0));
        failures.stream().skip(1).forEach(result::addSuppressed);
        return result;
    }
}
