package org.ikasan.studio.ui.actions;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import org.ikasan.studio.core.io.ComponentIO;
import org.ikasan.studio.core.model.ikasan.instance.Module;
import org.ikasan.studio.ui.StudioUIUtils;
import org.ikasan.studio.ui.UiContext;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class DebugAction implements ActionListener {
    private static final Logger LOG = Logger.getInstance("#DebugAction");
    private final Project project;

    public DebugAction(Project project) {
        this.project = project;
    }

    @Override
    public void actionPerformed(ActionEvent actionEvent) {
        Module module = project.getService(UiContext.class).getIkasanModule();

        if (module != null) {
            StudioUIUtils.displayIdeaInfoMessage(project, "Check idea logs for debug output.");
            LOG.info("STUDIO: ikasan module JSON " + ComponentIO.toJson(module));
            LOG.info("STUDIO: ikasan module simpleString " + module.toSimpleString());
            LOG.info("STUDIO: project " + project + " status " + project.isDisposed());
        } else {
            StudioUIUtils.displayIdeaWarnMessage(project, "Debug can't be launched unless a module is defined.");
        }
    }
}
