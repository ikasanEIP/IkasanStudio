package org.ikasan.studio.intellij.ai;

import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.ProjectActivity;
import org.jetbrains.annotations.NotNull;
import kotlin.Unit;
import kotlin.coroutines.Continuation;

/** Restores a previously configured connection unless automatic access was disabled. */
public final class StudioAiStartupActivity implements ProjectActivity {
    static final String RECONNECT = "ikasan.studio.ai.reconnect";
    @Override public Object execute(@NotNull Project project, @NotNull Continuation<? super Unit> continuation) {
        project.getService(StudioAiProposalInboxService.class).start();
        if (PropertiesComponent.getInstance(project).getBoolean(RECONNECT, false)) {
            ApplicationManager.getApplication().executeOnPooledThread(() -> {
                if (project.isDisposed()) return;
                try { project.getService(StudioAiService.class).start(); }
                catch (Exception failure) { Logger.getInstance(StudioAiStartupActivity.class).warn("Could not restore Studio AI connection", failure); }
            });
        }
        return Unit.INSTANCE;
    }
}
