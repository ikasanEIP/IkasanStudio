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

    public void testFailedFetchClearsPasswordAndLeavesDialogOpenForCorrection() throws Exception {
        exerciseFetch(403, "sensitive server response", false);
    }

    private void exerciseFetch(int status, String body, boolean success) throws Exception {
        var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(status, bytes.length);
            try (var stream = exchange.getResponseBody()) { stream.write(bytes); }
        });
        server.start();
        var dialog = new RuntimeImportConnectionDialog(getProject());
        try {
            ((JTextField) field(dialog, "url")).setText("http://127.0.0.1:" + server.getAddress().getPort());
            ((JTextField) field(dialog, "module")).setText("Demo");
            ((JTextField) field(dialog, "username")).setText("demo-user");
            var password = (JPasswordField) field(dialog, "password"); password.setText("temporary-secret");
            assertNull(dialog.doValidate());
            dialog.doOKAction();
            assertEquals(0, password.getPassword().length);
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
