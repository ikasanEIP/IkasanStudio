package org.ikasan.studio.actions;

import org.apache.log4j.Logger;
import org.ikasan.studio.Context;
import org.ikasan.studio.StudioUtils;
import org.ikasan.studio.model.ikasan.IkasanModule;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class DebugAction implements ActionListener {
    private static final Logger log = Logger.getLogger(DebugAction.class);
    private String projectKey;

    public DebugAction(String projectKey) {
        this.projectKey = projectKey;
    }

    @Override
    public void actionPerformed(ActionEvent actionEvent) {
        IkasanModule module = Context.getIkasanModule(projectKey);
        log.info("ikasan module was " + StudioUtils.toJson(module));
    }
}
