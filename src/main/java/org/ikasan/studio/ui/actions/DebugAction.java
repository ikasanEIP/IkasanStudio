package org.ikasan.studio.ui.actions;

import com.intellij.notification.NotificationType;
import com.intellij.openapi.diagnostic.Logger;
import org.ikasan.studio.core.io.ComponentIO;
import org.ikasan.studio.core.model.ikasan.instance.Module;
import org.ikasan.studio.ui.UiContext;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import static org.ikasan.studio.ui.UiContext.IKASAN_NOTIFICATION_GROUP;

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
            IKASAN_NOTIFICATION_GROUP
                    .createNotification("Check idea logs for debug output.", NotificationType.INFORMATION)
                    .notify(UiContext.getProject(projectKey));
            LOG.info("ikasan module was " + ComponentIO.toJson(module));
        } else {
            IKASAN_NOTIFICATION_GROUP
                    .createNotification("Debug can't be launched unless a module is defined.", NotificationType.INFORMATION)
                    .notify(UiContext.getProject(projectKey));
        }


    }
}
