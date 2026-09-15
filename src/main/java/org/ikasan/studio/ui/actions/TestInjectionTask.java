package org.ikasan.studio.ui.actions;

import com.fasterxml.jackson.databind.JsonNode;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import org.ikasan.studio.integration.ikasan.StudioInjectClient;
import org.ikasan.studio.ui.StudioBundle;
import org.ikasan.studio.ui.StudioUIUtils;

import java.net.ConnectException;
import java.net.http.HttpResponse;
import java.util.concurrent.Callable;
import java.util.function.Function;

/** Shared background execution and feedback for the module's test-injection endpoint. */
final class TestInjectionTask {
    private static final Logger LOG = Logger.getInstance(TestInjectionTask.class);

    private TestInjectionTask() {}

    record Notice(boolean warning, String message) {
        static Notice warning(String message) { return new Notice(true, message); }
        static Notice info(String message) { return new Notice(false, message); }
    }

    static void run(Project project, String title, String flowName,
                    Callable<HttpResponse<String>> request, Function<JsonNode, Notice> onSuccess) {
        ProgressManager.getInstance().run(new Task.Backgroundable(project, title) {
            @SuppressWarnings("NullableProblems")
            @Override
            public void run(ProgressIndicator indicator) {
                indicator.checkCanceled();
                Notice notice = perform(flowName, request, onSuccess);
                indicator.checkCanceled();
                if (notice == null) return;
                ApplicationManager.getApplication().invokeLater(() -> {
                    if (project.isDisposed()) return;
                    if (notice.warning()) StudioUIUtils.displayIdeaWarnMessage(project, notice.message());
                    else StudioUIUtils.displayIdeaInfoMessage(project, notice.message());
                });
            }
        });
    }

    // Return feedback separately from its presentation so response handling stays testable without dialogs.
    static Notice perform(String flowName, Callable<HttpResponse<String>> request,
                          Function<JsonNode, Notice> onSuccess) {
        try {
            HttpResponse<String> response = request.call();
            if (response.statusCode() == 200) {
                return onSuccess.apply(StudioInjectClient.readJson(response.body()));
            }
            if (response.statusCode() == 401) {
                return Notice.warning(StudioBundle.message("message.TestMessageAuthenticationFailed"));
            }
            return Notice.warning(StudioBundle.message("message.CouldNotSendTestMessage",
                    response.statusCode() + ": " + response.body()));
        } catch (ProcessCanceledException e) {
            throw e;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        } catch (ConnectException e) {
            // A live debug process can still be waiting for Spring Boot to bind its HTTP port.
            LOG.warn("STUDIO: Test injection for flow " + flowName + " - module not yet accepting connections", e);
            return Notice.warning(StudioBundle.message("message.ModuleNotYetAcceptingConnections"));
        } catch (Exception e) {
            LOG.warn("STUDIO: Could not perform test injection for flow " + flowName, e);
            return Notice.warning(StudioBundle.message("message.CouldNotSendTestMessage",
                    e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName()));
        }
    }
}
