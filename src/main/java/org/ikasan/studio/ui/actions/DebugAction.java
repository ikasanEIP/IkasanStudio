package org.ikasan.studio.ui.actions;

import com.intellij.openapi.diagnostic.Logger;
import org.ikasan.studio.core.io.ComponentIO;
import org.ikasan.studio.core.model.ikasan.instance.Module;
import org.ikasan.studio.ui.StudioUIUtils;
import org.ikasan.studio.ui.UiContext;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class DebugAction implements ActionListener {
    private static final Logger LOG = Logger.getInstance("#DebugAction");
    private final String projectKey;

    public DebugAction(String projectKey) {
        this.projectKey = projectKey;
    }

    @Override
    public void actionPerformed(ActionEvent actionEvent) {
        Module module = UiContext.getIkasanModule(projectKey);

        if (module != null) {
            StudioUIUtils.displayIdeaInfoMessage(projectKey, "Check idea logs for debug output.");
            LOG.info("STUDIO: ikasan module JSON " + ComponentIO.toJson(module));
            LOG.info("STUDIO: ikasan module simpleString " + module.toSimpleString());
            LOG.info("STUDIO: project " + UiContext.getProject(projectKey) + " status " + UiContext.getProject(projectKey).isDisposed());
        } else {
            StudioUIUtils.displayIdeaWarnMessage(projectKey, "Debug can't be launched unless a module is defined.");
        }
    }
}
