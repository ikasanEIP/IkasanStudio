package org.ikasan.studio.intellij.runtime;

import com.intellij.openapi.project.Project;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TestSftpServerServiceTest {
    @TempDir Path temporary;
    @Test void externalModeIsUntouchedAndLocalModeRequiresOwnedServer() throws Exception {
        Project project = mock(Project.class);
        when(project.getBasePath()).thenReturn(temporary.toString());
        var service = new TestSftpServerService(project);
        assertFalse(service.isSelected());
        assertTrue(service.launchProperties().isEmpty());
        service.setLocal(true);
        assertThrows(IllegalStateException.class, service::launchProperties);
        try {
            var server = service.start();
            assertSame(server, service.start());
            assertEquals("true", service.launchProperties().get("studio.test.sftp.enabled"));
            var restored = new TestSftpServerService(project);
            restored.loadState(service.getState());
            assertTrue(restored.isLocal());
            service.setLocal(false);
            assertTrue(service.launchProperties().isEmpty());
            assertThrows(IllegalStateException.class, service::start);
        } finally { service.stop(); }
        service.setLocal(true);
        assertThrows(IllegalStateException.class, service::launchProperties);
        service.dispose();
        assertThrows(IllegalStateException.class, service::start);
    }
}
