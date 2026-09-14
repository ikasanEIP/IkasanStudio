package org.ikasan.studio.ui.actions;

import com.intellij.openapi.project.Project;
import org.ikasan.studio.core.model.ikasan.instance.BasicElement;
import org.ikasan.studio.core.model.ikasan.instance.Flow;
import org.ikasan.studio.core.model.ikasan.instance.FlowElement;
import org.ikasan.studio.ui.viewmodel.ViewHandlerCache;

/** Shared eligibility for canvas context menus and the unsaved-property dialog. */
public record ComponentNavigationAvailability(boolean code, boolean properties) {
    public static ComponentNavigationAvailability forComponent(Project project, BasicElement component) {
        if (!(component instanceof Flow || component instanceof FlowElement)) {
            return new ComponentNavigationAvailability(false, false);
        }
        var handler = ViewHandlerCache.getAbstractViewHandler(project, component);
        return new ComponentNavigationAvailability(
                handler != null && handler.getCodeNavigationTarget() != null && handler.getCodeNavigationTarget().isPresent(),
                handler != null && handler.hasPropertiesNavigationTarget());
    }
}
