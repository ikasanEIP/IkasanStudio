package org.ikasan.studio.intellij.configuration;

import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.testFramework.HeavyPlatformTestCase;
import org.ikasan.studio.core.configuration.EnvironmentProperties;
import org.ikasan.studio.intellij.project.StudioProjectFiles;

import javax.swing.*;
import java.nio.file.Files;
import java.util.List;

public class ImportEnvironmentPropertiesDialogHeavyTest extends HeavyPlatformTestCase {
    public void testDiscoveredSourcePropertiesCanBeLoadedWithoutChoosingTheFileAgain() throws Exception {
        var discovered = new org.ikasan.studio.core.importer.SourceModuleImport.PropertyFile(
                "src/main/resources/application-int.properties", "custom.value=${SERVICE_VALUE}", "int");
        var dialog = new ImportEnvironmentPropertiesDialog(getProject(), List.of(discovered));
        try {
            var environment = (JTextField) field(dialog, "environment");
            boolean loaded = false;
            for (var component : environment.getParent().getComponents()) {
                if (component instanceof JButton button && button.getText().equals(org.ikasan.studio.ui.StudioBundle.message("sourceImport.useProperties"))) {
                    button.doClick(); loaded = true;
                }
            }
            assertTrue(loaded);
            assertEquals("int", environment.getText());
            assertEquals("custom.value=${SERVICE_VALUE}", ((JTextArea) field(dialog, "contents")).getText());
            assertEquals(0, ((List<?>) field(dialog, "staged")).size());
        } finally { dialog.close(DialogWrapper.CANCEL_EXIT_CODE); }
    }

    public void testMultipleFileReviewIsValueFreeAndCancelWritesNothing() throws Exception {
        var root = Files.createTempDirectory("studio-configuration-review");
        var dialog = new ImportEnvironmentPropertiesDialog(getProject(), root);
        try (var files = org.mockito.Mockito.mockStatic(StudioProjectFiles.class)) {
            var environment = (JTextField) field(dialog, "environment");
            var contents = (JTextArea) field(dialog, "contents");
            assertTrue(((JCheckBox) field(dialog, "local")).isSelected());
            contents.setText("custom.value=shared-secret");
            stage(dialog);
            environment.setText("dev");
            contents.setText("custom.value=dev-secret");
            stage(dialog);
            String review = ((JTextArea) field(dialog, "review")).getText();
            assertTrue(review.contains("config/local/application.properties"));
            assertTrue(review.contains("config/local/application-dev.properties"));
            assertTrue(review.contains("custom.value"));
            assertFalse(review.contains("shared-secret"));
            assertFalse(review.contains("dev-secret"));
            assertEquals(2, ((List<?>) field(dialog, "staged")).size());
            assertEquals("", contents.getText());
            assertNull(dialog.doValidate());
            contents.setText("forgotten=pending");
            assertNotNull(dialog.doValidate());
            dialog.close(DialogWrapper.CANCEL_EXIT_CODE);
            assertFalse(Files.exists(root.resolve("config")));
            assertFalse(Files.exists(root.resolve(".gitignore")));
            files.verify(() -> StudioProjectFiles.saveConfigurationDocuments(getProject(), List.of(
                    "config/local/application-dev.properties", "generated/src/main/resources/application.properties", ".gitignore")));
        } finally {
            if (!dialog.isDisposed()) dialog.close(DialogWrapper.CANCEL_EXIT_CODE);
            Files.deleteIfExists(root);
        }
    }

    public void testReplacementNeedsExplicitApprovalEvenWhenContentsHaveNotChanged() throws Exception {
        var root = Files.createTempDirectory("studio-configuration-replace");
        var dialog = new ImportEnvironmentPropertiesDialog(getProject(), root);
        try {
            @SuppressWarnings("unchecked")
            var staged = (List<EnvironmentProperties.Preview>) field(dialog, "staged");
            staged.add(new EnvironmentProperties.Preview(new EnvironmentProperties.Selection("x=1", "dev", false),
                    "x=1", List.of(), java.util.Set.of(), java.util.Set.of()));
            assertNotNull(dialog.doValidate());
            ((JCheckBox) field(dialog, "replace")).setSelected(true);
            assertNull(dialog.doValidate());
        } finally {
            dialog.close(DialogWrapper.CANCEL_EXIT_CODE);
            Files.deleteIfExists(root);
        }
    }

    private void stage(ImportEnvironmentPropertiesDialog dialog) throws Exception {
        var method = dialog.getClass().getDeclaredMethod("stage");
        method.setAccessible(true);
        method.invoke(dialog);
    }

    private Object field(Object owner, String name) throws Exception {
        var field = owner.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.get(owner);
    }
}
