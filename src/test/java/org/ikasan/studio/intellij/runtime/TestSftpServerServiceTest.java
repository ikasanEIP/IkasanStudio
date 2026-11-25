package org.ikasan.studio.intellij.runtime;

import com.intellij.openapi.project.Project;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.ikasan.studio.integration.sftp.LocalSftpHarness;
import org.ikasan.studio.intellij.runtime.TestSftpServerService.Settings;

class TestSftpServerServiceTest {
    @TempDir Path temporary;
    @Test void localModeRequiresProjectDirectory() {
        for (String basePath : new String[]{null, "", " "}) {
            // Configure each mock before publishing it to the service's EDT callbacks.
            Project project = mock(Project.class);
            when(project.getBasePath()).thenReturn(basePath);
            TestSftpServerService service = new TestSftpServerService(project);
            service.setLocal(true);
            try {
                IllegalStateException failure = assertThrows(IllegalStateException.class, service::start);
                assertEquals("Open a project with a directory before starting the local SFTP harness.", failure.getMessage());
                assertFalse(service.isRunning());
                assertThrows(IllegalStateException.class, service::launchProperties);
            } finally { service.dispose(); }
        }
    }
    @Test void externalModeIsUntouchedAndLocalModeRequiresOwnedServer() throws Exception {
        Project project = mock(Project.class);
        when(project.getBasePath()).thenReturn(temporary.toString());
        TestSftpServerService service = new TestSftpServerService(project);
        assertFalse(service.isSelected());
        assertFalse(service.isRunning());
        assertTrue(service.launchProperties().isEmpty());
        service.setLocal(true);
        assertFalse(service.isRunning());
        assertThrows(IllegalStateException.class, service::launchProperties);
        try {
            LocalSftpHarness server = service.start();
            assertTrue(service.isRunning());
            assertSame(server, service.start());
            assertEquals("true", service.launchProperties().get("studio.test.sftp.enabled"));
            TestSftpServerService restored = new TestSftpServerService(project);
            Settings savedState = service.getState();
            assertNotNull(savedState);
            restored.loadState(savedState);
            assertTrue(restored.isLocal());
            service.setLocal(false);
            assertFalse(service.isRunning());
            assertTrue(service.launchProperties().isEmpty());
            assertThrows(IllegalStateException.class, service::start);
        } finally { service.stop(); }
        service.setLocal(true);
        assertFalse(service.isRunning());
        assertThrows(IllegalStateException.class, service::launchProperties);
        service.dispose();
        assertFalse(service.isRunning());
        assertThrows(IllegalStateException.class, service::start);
    }
}
