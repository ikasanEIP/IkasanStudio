package org.ikasan.studio.ui.actions;

import com.intellij.openapi.project.Project;
import org.ikasan.studio.intellij.navigation.StudioNavigator;
import org.ikasan.studio.core.model.ikasan.instance.BasicElement;
import org.ikasan.studio.ui.StudioBundle;
import org.ikasan.studio.ui.StudioUIUtils;
import org.ikasan.studio.ui.viewmodel.AbstractViewHandlerIntellij;
import org.ikasan.studio.ui.viewmodel.ViewHandlerCache;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * "Jump to Properties" - navigates to this component's (or flow's) first externalized property in
 * application.properties. Only wired up on the menu when {@link AbstractViewHandlerIntellij#hasPropertiesNavigationTarget()}
 * is true, so unlike {@link NavigateToCodeAction} there's no fallback "jump to top of file" path needed here.
 */
public class NavigateToPropertiesAction implements ActionListener {
    private final Project project;
    private final BasicElement ikasanBasicElement;

    public NavigateToPropertiesAction(Project project, BasicElement ikasanBasicElement) {
        this.project = project;
        this.ikasanBasicElement = ikasanBasicElement;
    }

    @Override
    public void actionPerformed(ActionEvent actionEvent) {
        AbstractViewHandlerIntellij viewHandler = ViewHandlerCache.getAbstractViewHandler(project, ikasanBasicElement);
        if (viewHandler != null && viewHandler.hasPropertiesNavigationTarget()) {
            StudioUIUtils.displayMessage(project, StudioBundle.message("message.JumpToOffset", viewHandler.getOffsetInPropertiesFileToNavigateTo()));
            StudioNavigator.navigateToSource(project, viewHandler.getPropertiesPsiFile(), viewHandler.getOffsetInPropertiesFileToNavigateTo());
        }
    }
}
