package org.ikasan.studio.intellij.metapack;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.impl.LaterInvocator;
import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.testFramework.HeavyPlatformTestCase;
import com.intellij.testFramework.PlatformTestUtil;
import org.ikasan.studio.core.metapack.loading.MetaPackStore;
import org.ikasan.studio.ui.StudioBundle;

import javax.swing.DefaultListModel;
import javax.swing.JList;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicReference;
import static org.mockito.Mockito.*;

public class MetaPackChooserHeavyTest extends HeavyPlatformTestCase {
    public void testInstallUsesWarmedChooserWhileModal() throws Exception {
        var event = mock(AnActionEvent.class);
        when(event.getProject()).thenReturn(getProject());
        assertWarmedChooser(() -> new InstallMetaPackAction().actionPerformed(event), "metaPack.ChooseJar");
    }

    @SuppressWarnings("unchecked")
    public void testUpdateUsesWarmedChooserWhileManagerIsModal() throws Exception {
        var dialog = new MetaPackManagerDialog(getProject());
        try {
            var model = (DefaultListModel<MetaPackStore.Entry>) field(dialog, "model");
            model.addElement(new MetaPackStore.Entry("CustomPack", "1.0.0", Path.of("custom.jar"), null, null, "hash"));
            ((JList<?>) field(dialog, "packs")).setSelectedIndex(0);
            var choose = MetaPackManagerDialog.class.getDeclaredMethod("chooseUpdate");
            choose.setAccessible(true);
            assertWarmedChooser(() -> {
                try { choose.invoke(dialog); }
                catch (ReflectiveOperationException failure) { throw new AssertionError(failure); }
            }, "metaPack.Update");
        } finally { dialog.close(com.intellij.openapi.ui.DialogWrapper.CANCEL_EXIT_CODE); }
    }

    private void assertWarmedChooser(Runnable open, String titleKey) {
        var captured = new AtomicReference<FileChooserDescriptor>();
        var marker = new Object();
        try (var chooser = mockStatic(FileChooser.class, invocation -> {
            if (invocation.getMethod().getName().equals("chooseFiles")) {
                assertTrue(ApplicationManager.getApplication().isDispatchThread());
                captured.set(invocation.getArgument(0));
            }
            return null; // No selection: never perform an installation or update.
        })) {
            LaterInvocator.enterModal(marker);
            try {
                open.run();
                PlatformTestUtil.waitWithEventsDispatching("Warmed meta-pack chooser did not open in modal context",
                        () -> captured.get() != null, 10);
                assertFalse(captured.get().isChooseMultiple());
                assertEquals(StudioBundle.message(titleKey), captured.get().getTitle());
            } finally { LaterInvocator.leaveModal(marker); }
        }
    }

    private Object field(Object owner, String name) throws Exception {
        var field = owner.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.get(owner);
    }
}
