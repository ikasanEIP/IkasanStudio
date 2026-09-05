package org.ikasan.studio.intellij.runtime;

import com.intellij.openapi.project.Project;
import org.ikasan.studio.core.model.analysis.TestFtpServerConfiguration;
import org.ikasan.studio.ui.UiContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

// java.nio.file.Files here reads/writes this plugin's own local test fixtures, never a remote-dev project -
// the Eel-optimized alternative exists for the latter, so it doesn't apply here.
@SuppressWarnings("UseOptimizedEelFunctions")
class TestFtpServerServiceTest {
    @Test
    void startsAuthenticatesAndStopsAnEmbeddedServer(@TempDir Path projectDirectory) throws Exception {
        Project project = mock(Project.class);
        UiContext uiContext = mock(UiContext.class);
        when(project.getBasePath()).thenReturn(projectDirectory.toString());
        when(project.getService(UiContext.class)).thenReturn(uiContext);
        int port = availablePort();
        TestFtpServerConfiguration configuration =
                new TestFtpServerConfiguration("127.0.0.1", port, "ikasan", "ikasan");
        TestFtpServerService service = new TestFtpServerService(project);

        try {
            Path root = service.start(configuration);
            assertThat(root).isEqualTo(projectDirectory.resolve("test-data/ftp"));
            assertThat(service.isRunningAt(configuration)).isTrue();
            assertThat(service.start(configuration)).isEqualTo(root);
            assertThat(Files.readString(root.resolve("test-file.txt"))).contains("Ikasan Studio");
            Files.delete(root.resolve("test-file.txt"));
            assertThat(service.getOrCreateTestFile()).exists();

            try (Socket socket = new Socket(configuration.host(), configuration.port());
                 BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
                 BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8))) {
                assertThat(reader.readLine()).startsWith("220");
                command(writer, "USER ikasan");
                assertThat(reader.readLine()).startsWith("331");
                command(writer, "PASS ikasan");
                assertThat(reader.readLine()).startsWith("230");
                command(writer, "PWD");
                assertThat(reader.readLine()).startsWith("257");
                command(writer, "QUIT");
                assertThat(reader.readLine()).startsWith("221");
            }
        } finally {
            service.stop();
        }
        assertThat(service.isRunning()).isFalse();
        service.stop();
        assertThat(service.isRunning()).isFalse();
    }

    @Test
    void rejectsAnExternallyOwnedPortWithoutClosingIt(@TempDir Path projectDirectory) throws Exception {
        Project project = mock(Project.class);
        when(project.getBasePath()).thenReturn(projectDirectory.toString());
        try (ServerSocket external = new ServerSocket(0)) {
            TestFtpServerService service = new TestFtpServerService(project);
            TestFtpServerConfiguration configuration = new TestFtpServerConfiguration(
                    "127.0.0.1", external.getLocalPort(), "ikasan", "secret");

            org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.start(configuration))
                    .isInstanceOf(java.net.BindException.class);
            assertThat(external.isClosed()).isFalse();
            service.stop();
            assertThat(external.isClosed()).isFalse();
        }
    }

    @Test
    void derivesItsDataDirectoryFromTheCurrentProjectLocation(@TempDir Path parent) {
        Project first = mock(Project.class);
        Project relocated = mock(Project.class);
        when(first.getBasePath()).thenReturn(parent.resolve("before").toString());
        when(relocated.getBasePath()).thenReturn(parent.resolve("after").toString());

        assertThat(TestFtpServerService.testRoot(first)).isEqualTo(parent.resolve("before/test-data/ftp"));
        assertThat(TestFtpServerService.testRoot(relocated)).isEqualTo(parent.resolve("after/test-data/ftp"));
    }

    private static void command(BufferedWriter writer, String command) throws Exception {
        writer.write(command + "\r\n");
        writer.flush();
    }

    private static int availablePort() throws Exception {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }
}
