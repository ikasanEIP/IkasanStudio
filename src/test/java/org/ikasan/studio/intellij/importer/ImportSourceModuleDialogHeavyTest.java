package org.ikasan.studio.intellij.importer;

import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.testFramework.HeavyPlatformTestCase;
import org.ikasan.studio.core.importer.SourceModuleImport;

import javax.swing.*;
import javax.swing.table.TableModel;
import java.nio.file.Files;
import java.util.*;

public class ImportSourceModuleDialogHeavyTest extends HeavyPlatformTestCase {
    public void testReviewedChoicesDoNotImportOriginalConfigurationOrDependenciesByDefaultAndCancelDoesNotWrite() throws Exception {
        var root = Files.createTempDirectory("studio-source-wizard");
        var original = root.resolve("original"); var destination = root.resolve("studio");
        Files.createDirectories(original); Files.createDirectories(destination);
        String bytes = Base64.getEncoder().encodeToString("class Bespoke {}".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        var dependency = new org.apache.maven.model.Dependency(); dependency.setGroupId("customer"); dependency.setArtifactId("client"); dependency.setVersion("1");
        var analysis = new SourceModuleImport.Analysis(original, destination, List.of(
                new SourceModuleImport.FileChoice("src/main/java/Bespoke.java", "user/src/main/java/Bespoke.java", bytes, null, true, false, "Matched"),
                new SourceModuleImport.FileChoice("src/main/java/Config.java", "user/src/main/java/Config.java", bytes, null, false, true, "Review wiring")),
                List.of(), List.of(), List.of(new SourceModuleImport.DependencyChoice("customer:client:jar:", dependency, true, "Review")),
                List.of(), bytes, Map.of());
        var dialog = new ImportSourceModuleDialog(getProject());
        try {
            set(dialog, "analysis", analysis); set(dialog, "analysedSource", original);
            ((JTextField) field(dialog, "source")).setText(original.toString());
            var table = (JTable) field(dialog, "table"); TableModel model = table.getModel();
            ((javax.swing.table.AbstractTableModel) model).fireTableDataChanged();
            assertEquals(3, model.getRowCount());
            assertEquals(Boolean.FALSE, model.getValueAt(1, 0));
            assertEquals(Boolean.FALSE, model.getValueAt(2, 0));
            model.setValueAt(Boolean.TRUE, 0, 0);
            assertEquals(Boolean.TRUE, model.getValueAt(0, 0));
            var validate = dialog.getClass().getDeclaredMethod("doValidate"); validate.setAccessible(true);
            assertNull(validate.invoke(dialog));
            ((JTextField) field(dialog, "source")).setText(root.resolve("different").toString());
            assertNotNull(validate.invoke(dialog));
            dialog.close(DialogWrapper.CANCEL_EXIT_CODE);
            assertFalse(Files.exists(destination.resolve("user")));
        } finally {
            if (!dialog.isDisposed()) dialog.close(DialogWrapper.CANCEL_EXIT_CODE);
            Files.deleteIfExists(destination); Files.deleteIfExists(original); Files.deleteIfExists(root);
        }
    }
    private Object field(Object owner, String name) throws Exception {
        var field = owner.getClass().getDeclaredField(name); field.setAccessible(true); return field.get(owner);
    }
    private void set(Object owner, String name, Object value) throws Exception {
        var field = owner.getClass().getDeclaredField(name); field.setAccessible(true); field.set(owner, value);
    }
}
