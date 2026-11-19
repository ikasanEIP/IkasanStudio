package org.ikasan.studio.intellij.metapack;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationAction;
import com.intellij.notification.NotificationGroupManager;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import org.ikasan.studio.ui.StudioBundle;
import org.ikasan.studio.ui.StudioUIUtils;

/** Unavailable-pack recovery guidance with access to the existing external-pack manager. */
public final class MetaPackNotifications {
    private static final com.intellij.openapi.diagnostic.Logger LOG =
            com.intellij.openapi.diagnostic.Logger.getInstance(MetaPackNotifications.class);
    private MetaPackNotifications() { }

    public static void showUnavailable(Project project, String details) {
        if (project.isDisposed()) return;
        LOG.warn(org.ikasan.studio.core.diagnostics.StudioDiagnosticEvent.format(
                org.ikasan.studio.core.diagnostics.StudioDiagnosticEvent.Event.NOTIFICATION_WARNING,
                null, null, null, null));
        unavailable(project, details).notify(project);
    }

    static Notification unavailable(Project project, String details) {
        // Guidance comes first so a collapsed balloon does not hide the recovery steps.
        // External pack diagnostics are plain text, never notification HTML or links.
        String content = StringUtil.escapeXmlEntities(StudioBundle.message("metaPack.Unavailable", details))
                .replace("\n", "<br/>");
        return NotificationGroupManager.getInstance().getNotificationGroup(StudioUIUtils.NOTIFICATION_GROUP_ID)
                .createNotification(StudioBundle.message("metaPack.UnavailableTitle"), content, NotificationType.WARNING)
                .addAction(NotificationAction.createSimple(StudioBundle.message("metaPack.Manage"),
                        () -> ManageMetaPacksAction.open(project)));
    }
}
