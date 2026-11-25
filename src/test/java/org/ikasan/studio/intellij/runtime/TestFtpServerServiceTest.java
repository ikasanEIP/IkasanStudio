package org.ikasan.studio.intellij.runtime;

import com.intellij.openapi.project.Project;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.BindException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.assertj.core.api.Assertions;
import org.ikasan.studio.core.model.analysis.TestFtpServerConfiguration;
import org.ikasan.studio.ui.UiContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

// java.nio.file.Files here reads/writes this plugin's own local test fixtures, never a remote-dev project -
// the Eel-optimized alternative exists for the latter, so it doesn't apply here.
@SuppressWarnings("UseOptimizedEelFunctions")
class TestFtpServerServiceTest {
    @Test
    void projectsUseIndependentPortsAndFilesAndRemainReadyForLaterDelivery(@TempDir Path directory) throws Exception {
        TestFtpServerService first = new TestFtpServerService(projectAt(directory.resolve("first")));
        TestFtpServerService second = new TestFtpServerService(projectAt(directory.resolve("second")));
        try {
            assertThat(first.localConfiguration().port()).isZero();
            Assertions.assertThatThrownBy(first::launchProperties).isInstanceOf(IllegalStateException.class);
            Path firstRoot = first.startLocal();
            Path secondRoot = second.startLocal();
            TestFtpServerConfiguration firstAddress = first.localConfiguration();
            assertThat(first.listeningEndpoint()).isEqualTo(new HarnessStopImpact.Endpoint("FTP", firstAddress.host(), firstAddress.port()));
            TestFtpServerConfiguration secondAddress = second.localConfiguration();
            assertThat(firstAddress.port()).isPositive().isNotEqualTo(secondAddress.port());
            assertThat(secondAddress.port()).isPositive();
            assertThat(first.startLocal()).isEqualTo(firstRoot);
            assertThat(first.localConfiguration()).isEqualTo(firstAddress);
            assertThat(first.launchProperties().get("studio.harness.ftp.port")).isEqualTo(Integer.toString(firstAddress.port()));
            assertThat(second.launchProperties().get("studio.harness.ftp.port")).isEqualTo(Integer.toString(secondAddress.port()));
            assertThat(first.launchProperties().get("studio.harness.ftp.home")).isEqualTo(firstRoot.toString());
            assertThat(firstRoot).isNotEqualTo(secondRoot);

            transferAndReadBack(firstAddress, "payload.txt", "first project");
            transferAndReadBack(secondAddress, "payload.txt", "second project");
            assertThat(Files.readString(firstRoot.resolve("payload.txt"))).isEqualTo("first project");
            assertThat(Files.readString(secondRoot.resolve("payload.txt"))).isEqualTo("second project");
            assertThat(first.isRunningAt(firstAddress)).isTrue();
            assertThat(second.isRunningAt(secondAddress)).isTrue();
            // Later delivery uses the same live servers, without reset or restart.
            transferAndReadBack(firstAddress, "later.txt", "later delivery");
            first.stop();
            assertThat(first.listeningEndpoint()).isNull();
            assertThat(first.localConfiguration().port()).isZero();
            Assertions.assertThatThrownBy(first::launchProperties).isInstanceOf(IllegalStateException.class);
            transferAndReadBack(secondAddress, "later.txt", "second still running");
            assertThat(second.isRunningAt(secondAddress)).isTrue();
            first.startLocal();
            assertThat(first.localConfiguration().port()).isPositive().isNotEqualTo(secondAddress.port());
            assertThat(first.launchProperties().get("studio.harness.ftp.port"))
                    .isEqualTo(Integer.toString(first.localConfiguration().port()));
        } finally { first.stop(); second.stop(); }
    }

    @Test
    void explicitServerIsNotSilentlyReplacedByLocalOverrides(@TempDir Path directory) throws Exception {
        TestFtpServerService service = new TestFtpServerService(projectAt(directory));
        try {
            TestFtpServerConfiguration explicit = new TestFtpServerConfiguration("127.0.0.1", availablePort(), "original", "password");
            service.start(explicit);
            Assertions.assertThatThrownBy(service::startLocal).isInstanceOf(IllegalStateException.class);
            Assertions.assertThatThrownBy(service::launchProperties).isInstanceOf(IllegalStateException.class);
            assertThat(service.isRunningAt(explicit)).isTrue();
        } finally { service.stop(); }
    }

    private static Project projectAt(Path directory) {
        Project project = mock(Project.class);
        when(project.getBasePath()).thenReturn(directory.toString());
        when(project.getService(UiContext.class)).thenReturn(mock(UiContext.class));
        return project;
    }

    /** Exercise both FTP producer delivery and consumer retrieval over real passive data connections. */
    private static void transferAndReadBack(TestFtpServerConfiguration configuration, String filename, String payload) throws Exception {
        try (Socket socket = new Socket(configuration.host(), configuration.port());
             BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
             BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8))) {
            socket.setSoTimeout(5000);
            assertThat(reader.readLine()).startsWith("220");
            command(writer, "USER " + configuration.username()); assertThat(reader.readLine()).startsWith("331");
            command(writer, "PASS " + configuration.password()); assertThat(reader.readLine()).startsWith("230");
            command(writer, "TYPE I"); assertThat(reader.readLine()).startsWith("200");
            try (Socket data = passive(configuration, reader, writer)) {
                command(writer, "STOR " + filename); assertThat(reader.readLine()).startsWith("150");
                data.getOutputStream().write(payload.getBytes(StandardCharsets.UTF_8));
            }
            assertThat(reader.readLine()).startsWith("226");
            try (Socket data = passive(configuration, reader, writer)) {
                command(writer, "RETR " + filename); assertThat(reader.readLine()).startsWith("150");
                assertThat(new String(data.getInputStream().readAllBytes(), StandardCharsets.UTF_8)).isEqualTo(payload);
            }
            assertThat(reader.readLine()).startsWith("226");
            command(writer, "QUIT"); assertThat(reader.readLine()).startsWith("221");
        }
    }

    private static Socket passive(TestFtpServerConfiguration configuration, BufferedReader reader, BufferedWriter writer) throws Exception {
        command(writer, "PASV");
        String response = reader.readLine(); assertThat(response).startsWith("227");
        String[] address = response.substring(response.indexOf('(') + 1, response.indexOf(')')).split(",");
        Socket socket = new Socket(configuration.host(), Integer.parseInt(address[4]) * 256 + Integer.parseInt(address[5]));
        socket.setSoTimeout(5000);
        return socket;
    }

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
            assertThat(root).isEqualTo(projectDirectory.resolve("temporary-files/test-data/ftp"));
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
        when(project.getService(UiContext.class)).thenReturn(mock(UiContext.class));
        try (ServerSocket external = new ServerSocket(0)) {
            TestFtpServerService service = new TestFtpServerService(project);
            TestFtpServerConfiguration configuration = new TestFtpServerConfiguration(
                    "127.0.0.1", external.getLocalPort(), "ikasan", "secret");

            Assertions.assertThatThrownBy(() -> service.start(configuration))
                    .isInstanceOf(BindException.class);
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

        assertThat(TestFtpServerService.testRoot(first)).isEqualTo(parent.resolve("before/temporary-files/test-data/ftp"));
        assertThat(TestFtpServerService.testRoot(relocated)).isEqualTo(parent.resolve("after/temporary-files/test-data/ftp"));
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
