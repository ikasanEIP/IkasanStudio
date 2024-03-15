package org.ikasan.studio.ui.actions;

import com.intellij.openapi.diagnostic.Logger;
import org.ikasan.studio.ui.Context;
import org.ikasan.studio.build.io.ComponentIO;
import org.ikasan.studio.build.model.ikasan.instance.Module;

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
        Module module = Context.getIkasanModule(projectKey);
        LOG.info("ikasan module was " + ComponentIO.toJson(module));
    }
}
