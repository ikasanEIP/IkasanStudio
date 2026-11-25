package org.ikasan.studio.ui.actions;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.ui.Messages;
import java.util.*;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.ikasan.studio.intellij.runtime.HarnessStopImpact.Impact;
import org.ikasan.studio.intellij.runtime.HarnessStopImpact;
import org.ikasan.studio.ui.StudioBundle;

/** Warn before user-requested shutdown; project disposal retains its existing cleanup behavior. */
final class HarnessStopWarning {
    private HarnessStopWarning() { }
    static boolean confirm(Project project, List<HarnessStopImpact.Endpoint> endpoints) {
        if (endpoints.isEmpty()) return true;
        return confirm(project, endpoints, Arrays.asList(ProjectManager.getInstance().getOpenProjects()), impacts -> {
            String details = impacts.stream().map(i -> i.project() + " — " + i.endpoint().label()).collect(Collectors.joining("\n"));
            return Messages.showDialog(project, StudioBundle.message("harness.stop.shared.message", details),
                    StudioBundle.message("harness.stop.shared.title"), new String[]{StudioBundle.message("harness.stop.shared.stop"),
                    StudioBundle.message("harness.stop.shared.cancel")}, 1, Messages.getWarningIcon()) == 0;
        });
    }
    static boolean confirm(Project owner, List<HarnessStopImpact.Endpoint> endpoints, List<Project> open,
                           Predicate<List<HarnessStopImpact.Impact>> confirmation) {
        List<Impact> impact = HarnessStopImpact.affected(owner, open, endpoints);
        return impact.isEmpty() || confirmation.test(impact);
    }
}
