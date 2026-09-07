package org.ikasan.studio.intellij.execution;

import com.intellij.testFramework.HeavyPlatformTestCase;
import com.intellij.testFramework.DumbModeTestUtils;
import com.intellij.testFramework.LightVirtualFile;
import com.intellij.testFramework.PlatformTestUtil;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.util.Disposer;
import java.util.concurrent.atomic.AtomicBoolean;

public class RunDuringIndexingHeavyTest extends HeavyPlatformTestCase {
    public void testRunDefersDuringIndexingAndExpiresWhenServiceCloses() {
        var service = new IkasanRunConfigurationService(myProject);
        var completed = new AtomicBoolean();
        DumbModeTestUtils.runInDumbModeSynchronously(myProject, () -> {
            assertTrue(DumbService.isDumb(myProject));
            service.selectAndRun(new LightVirtualFile("Application.java"), null, result -> completed.set(true));
            PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue();
            assertFalse(completed.get());
            Disposer.dispose(service);
        });
        PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue();
        assertFalse(completed.get());
    }
}
