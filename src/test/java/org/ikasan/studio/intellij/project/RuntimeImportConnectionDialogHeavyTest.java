package org.ikasan.studio.intellij.project;

import com.intellij.testFramework.HeavyPlatformTestCase;
import com.sun.net.httpserver.HttpServer;
import javax.swing.JTextField;
import javax.swing.JPasswordField;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

public class RuntimeImportConnectionDialogHeavyTest extends HeavyPlatformTestCase {
    public void testSuccessfulFetchClearsPasswordAndReturnsPreviewDocuments() throws Exception {
        exerciseFetch(200, "{\"name\":\"Demo\",\"flows\":[]}", true);
    }

    public void testFailedFetchKeepsPasswordAndLeavesDialogOpenForCorrection() throws Exception {
        exerciseFetch(403, "sensitive server response", false);
    }

    public void testSwaggerPageUsesAndDisplaysTheNormalizedContext() throws Exception {
        exerciseFetch(200, "{\"name\":\"Demo\",\"flows\":[]}", true,
                "/ctx/swagger-ui/index.html?token=private-query#/module");
    }

    public void testRetryReusesPasswordWithoutReentryAndClearsItOnSuccess() throws Exception {
        var headers = new java.util.concurrent.CopyOnWriteArrayList<String>();
        var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            headers.add(exchange.getRequestHeaders().getFirst("Authorization"));
            byte[] body = "{\"name\":\"Demo\",\"flows\":[]}".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(headers.size() == 1 ? 500 : 200, body.length);
            try (var output = exchange.getResponseBody()) { output.write(body); }
        });
        server.start();
        var dialog = new RuntimeImportConnectionDialog(getProject());
        try {
            ((JTextField) field(dialog, "url")).setText("http://127.0.0.1:" + server.getAddress().getPort());
            ((JTextField) field(dialog, "module")).setText("Demo");
            ((JTextField) field(dialog, "username")).setText("demo-user");
            var password = (JPasswordField) field(dialog, "password");
            password.setText("temporary-secret");
            dialog.doOKAction();
            assertFalse(dialog.isDisposed());
            assertEquals("temporary-secret".length(), password.getPassword().length);
            // Retry without setting the password again: both HTTP requests must authenticate identically.
            dialog.doOKAction();
            String expected = "Basic " + java.util.Base64.getEncoder().encodeToString(
                    "demo-user:temporary-secret".getBytes(StandardCharsets.UTF_8));
            assertEquals(java.util.List.of(expected, expected), headers);
            assertNotNull(dialog.documents());
            assertTrue(dialog.isDisposed());
            assertEquals(0, password.getPassword().length);
        } finally {
            if (!dialog.isDisposed()) dialog.close(com.intellij.openapi.ui.DialogWrapper.CANCEL_EXIT_CODE);
            server.stop(0);
        }
    }

    public void testCancelClearsPasswordWithoutPersistingItToANewDialog() throws Exception {
        var dialog = new RuntimeImportConnectionDialog(getProject());
        var password = (JPasswordField) field(dialog, "password");
        password.setText("temporary-secret");
        dialog.close(com.intellij.openapi.ui.DialogWrapper.CANCEL_EXIT_CODE);
        assertEquals(0, password.getPassword().length);
        var reopened = new RuntimeImportConnectionDialog(getProject());
        try { assertEquals(0, ((JPasswordField) field(reopened, "password")).getPassword().length); }
        finally { reopened.close(com.intellij.openapi.ui.DialogWrapper.CANCEL_EXIT_CODE); }
    }

    public void testClosingAndReopeningRemembersConnectionWithoutPassword() throws Exception {
        var dialog = new RuntimeImportConnectionDialog(getProject());
        ((JTextField) field(dialog, "url")).setText("http://localhost:12450/demo/swagger-ui/index.html?token=discard#/module");
        ((JTextField) field(dialog, "module")).setText(" Demo ");
        ((JTextField) field(dialog, "username")).setText("demo-user");
        ((javax.swing.JCheckBox) field(dialog, "configuration")).setSelected(false);
        ((JPasswordField) field(dialog, "password")).setText("never-persist-this");
        dialog.close(com.intellij.openapi.ui.DialogWrapper.CANCEL_EXIT_CODE);

        // Round-trip through the platform serializer, as when the workspace is reopened.
        var service = getProject().getService(RuntimeImportConnectionSettings.class);
        var xml = com.intellij.util.xmlb.XmlSerializer.serialize(service.getState());
        String serialized = new org.jdom.output.XMLOutputter().outputString(xml);
        assertFalse(serialized.contains("never-persist-this"));
        assertFalse(serialized.contains("token"));
        service.loadState(com.intellij.util.xmlb.XmlSerializer.deserialize(xml,
                RuntimeImportConnectionSettings.Settings.class));
        var reopened = new RuntimeImportConnectionDialog(getProject());
        try {
            assertEquals("http://localhost:12450/demo", ((JTextField) field(reopened, "url")).getText());
            assertEquals("Demo", ((JTextField) field(reopened, "module")).getText());
            assertEquals("demo-user", ((JTextField) field(reopened, "username")).getText());
            assertFalse(((javax.swing.JCheckBox) field(reopened, "configuration")).isSelected());
            assertEquals(0, ((JPasswordField) field(reopened, "password")).getPassword().length);
        } finally { reopened.close(com.intellij.openapi.ui.DialogWrapper.CANCEL_EXIT_CODE); }
    }

    public void testInvalidUrlCannotOverwriteLastUsableConnection() {
        var settings = new RuntimeImportConnectionSettings();
        settings.remember("https://localhost:8443/demo", "Demo", "user", false);
        settings.remember("https://user:secret@localhost/demo", "Other", "", true);
        assertEquals("https://localhost:8443/demo", settings.getState().applicationUrl);
        assertEquals("Other", settings.getState().moduleName);
        assertEquals("http://localhost:8080/", new RuntimeImportConnectionSettings().getState().applicationUrl);
    }

    private void exerciseFetch(int status, String body, boolean success) throws Exception {
        exerciseFetch(status, body, success, "");
    }

    private void exerciseFetch(int status, String body, boolean success, String suffix) throws Exception {
        var requested = new java.util.concurrent.atomic.AtomicReference<String>();
        var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            requested.set(exchange.getRequestURI().toASCIIString());
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(status, bytes.length);
            try (var stream = exchange.getResponseBody()) { stream.write(bytes); }
        });
        server.start();
        var dialog = new RuntimeImportConnectionDialog(getProject());
        try {
            ((JTextField) field(dialog, "url")).setText("http://127.0.0.1:" + server.getAddress().getPort() + suffix);
            ((JTextField) field(dialog, "module")).setText("Demo");
            ((JTextField) field(dialog, "username")).setText("demo-user");
            var password = (JPasswordField) field(dialog, "password"); password.setText("temporary-secret");
            assertNull(dialog.doValidate());
            dialog.doOKAction();
            assertEquals("Demo", getProject().getService(RuntimeImportConnectionSettings.class).getState().moduleName);
            assertEquals("demo-user", getProject().getService(RuntimeImportConnectionSettings.class).getState().username);
            String context = suffix.isEmpty() ? "" : "/ctx";
            assertEquals(context + "/rest/metadata/module/Demo", requested.get());
            assertEquals("http://127.0.0.1:" + server.getAddress().getPort() + context,
                    ((JTextField) field(dialog, "url")).getText());
            assertEquals(success ? 0 : "temporary-secret".length(), password.getPassword().length);
            if (success) {
                assertNotNull(dialog.documents());
                assertEquals(body, dialog.documents().moduleJson());
                assertEquals("[]", dialog.documents().configurationJson());
                assertTrue(dialog.isDisposed());
            } else {
                assertNull(dialog.documents()); assertFalse(dialog.isDisposed());
            }
        } finally {
            if (!dialog.isDisposed()) dialog.close(com.intellij.openapi.ui.DialogWrapper.CANCEL_EXIT_CODE);
            server.stop(0);
        }
    }

    private Object field(Object owner, String name) throws Exception {
        var field = owner.getClass().getDeclaredField(name); field.setAccessible(true); return field.get(owner);
    }
}
