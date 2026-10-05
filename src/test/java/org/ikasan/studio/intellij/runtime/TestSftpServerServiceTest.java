package org.ikasan.studio.intellij.runtime;

import com.intellij.openapi.project.Project;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TestSftpServerServiceTest {
    @TempDir Path temporary;
    @Test void localModeRequiresProjectDirectory() {
        Project project = mock(Project.class);
        var service = new TestSftpServerService(project);
        service.setLocal(true);
        try {
            for (String basePath : new String[]{null, "", " "}) {
                when(project.getBasePath()).thenReturn(basePath);
                var failure = assertThrows(IllegalStateException.class, service::start);
                assertEquals("Open a project with a directory before starting the local SFTP harness.", failure.getMessage());
                assertFalse(service.isRunning());
                assertThrows(IllegalStateException.class, service::launchProperties);
            }
        } finally { service.dispose(); }
    }
    @Test void externalModeIsUntouchedAndLocalModeRequiresOwnedServer() throws Exception {
        Project project = mock(Project.class);
        when(project.getBasePath()).thenReturn(temporary.toString());
        var service = new TestSftpServerService(project);
        assertFalse(service.isSelected());
        assertFalse(service.isRunning());
        assertTrue(service.launchProperties().isEmpty());
        service.setLocal(true);
        assertFalse(service.isRunning());
        assertThrows(IllegalStateException.class, service::launchProperties);
        try {
            var server = service.start();
            assertTrue(service.isRunning());
            assertSame(server, service.start());
            assertEquals("true", service.launchProperties().get("studio.test.sftp.enabled"));
            var restored = new TestSftpServerService(project);
            restored.loadState(service.getState());
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
