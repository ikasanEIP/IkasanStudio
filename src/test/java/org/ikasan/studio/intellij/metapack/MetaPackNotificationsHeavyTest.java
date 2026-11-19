package org.ikasan.studio.intellij.metapack;

import com.intellij.notification.NotificationAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.testFramework.HeavyPlatformTestCase;
import org.ikasan.studio.ui.StudioBundle;
import static org.mockito.Mockito.*;

public class MetaPackNotificationsHeavyTest extends HeavyPlatformTestCase {
    public void testRecoveryGuidancePrecedesEscapedValidationDetailsAndOffersManager() {
        var notification = MetaPackNotifications.unavailable(getProject(), "broken-pack: invalid <rule> & value");
        try {
            assertEquals(StudioBundle.message("metaPack.UnavailableTitle"), notification.getTitle());
            String content = notification.getContent();
            String manager = StudioBundle.message("metaPack.Manage");
            assertTrue(content.contains(manager));
            assertTrue(content.contains("ikasan-studio/meta-packs"));
            assertTrue(content.indexOf(manager) < content.indexOf("broken-pack"));
            assertTrue(content.contains("&lt;rule&gt; &amp; value"));
            assertFalse(content.contains("invalid <rule>"));
            assertEquals(1, notification.getActions().size());
            assertEquals(StudioBundle.message("metaPack.Manage"),
                    notification.getActions().get(0).getTemplatePresentation().getText());
        } finally { notification.expire(); }
    }

    public void testManagerActionDoesNotReopenADisposedProject() {
        var project = mock(Project.class);
        when(project.isDisposed()).thenReturn(true);
        var notification = MetaPackNotifications.unavailable(project, "broken-pack");
        try {
            ((NotificationAction) notification.getActions().get(0)).actionPerformed(mock(AnActionEvent.class), notification);
            verify(project).isDisposed();
            verifyNoMoreInteractions(project);
        } finally { notification.expire(); }
    }
}
