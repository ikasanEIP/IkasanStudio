package org.ikasan.studio.intellij.metapack;

import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.project.Project;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicReference;

/** Modal platform progress keeps filesystem and template work off the EDT while preserving review ordering. */
final class MetaPackUiTasks {
    private MetaPackUiTasks() { }
    static <T> T run(Project project, String title, Callable<T> work) throws Exception {
        var result = new AtomicReference<T>();
        var failure = new AtomicReference<Exception>();
        ProgressManager.getInstance().runProcessWithProgressSynchronously(() -> {
            try { result.set(work.call()); }
            catch (ProcessCanceledException cancelled) { throw cancelled; }
            catch (Exception e) { failure.set(e); }
        }, title, false, project);
        if (failure.get() != null) throw failure.get();
        return result.get();
    }
}
