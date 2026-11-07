package org.ikasan.studio.intellij.configuration;

import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.testFramework.HeavyPlatformTestCase;
import com.intellij.diff.contents.DocumentContent;
import org.ikasan.studio.core.configuration.EnvironmentProperties;
import org.ikasan.studio.intellij.project.StudioProjectFiles;

import javax.swing.*;
import java.nio.file.Files;
import java.util.List;

public class ImportEnvironmentPropertiesDialogHeavyTest extends HeavyPlatformTestCase {
    public void testDiscoveredPropertiesShowOriginalAndDeveloperOwnedDestinationWithoutWriting() throws Exception {
        var discovered = new org.ikasan.studio.core.importer.SourceModuleImport.PropertyFile(
                "src/main/resources/application-int.properties", "custom.value=${SERVICE_VALUE}", "int");
        var dialog = new ImportEnvironmentPropertiesDialog(getProject(), List.of(discovered));
        try (var projectFiles = org.mockito.Mockito.mockStatic(StudioProjectFiles.class)) {
            var use = findButton(dialog.createCenterPanel(),
                    org.ikasan.studio.ui.StudioBundle.message("sourceImport.useProperties"));
            assertNotNull(use);
            use.doClick();
            assertEquals(discovered.text(), contents(dialog).getText());
            var table = table(dialog);
            assertEquals(discovered.source(), table.getValueAt(0, 0));
            assertEquals(EnvironmentProperties.USER_CONFIGURATION, table.getValueAt(0, 2));
            assertNotNull(dialog.doValidate());
            var diff = dialog.comparison();
            assertNotNull(diff);
            assertEquals(discovered.text(), ((DocumentContent) diff.getContents().get(0)).getDocument().getText());
            assertFalse(((DocumentContent) diff.getContents().get(0)).getDocument().isWritable());
            assertNull(field(dialog, "pending"));
            projectFiles.verify(() -> StudioProjectFiles.refreshConfigurationDirectory(getProject()), org.mockito.Mockito.never());
        } finally { dialog.close(DialogWrapper.CANCEL_EXIT_CODE); }
    }

    public void testCopyRevertAndInputEditsRequireFreshExplicitSelectionAndCancelWritesNothing() throws Exception {
        var root = Files.createTempDirectory("studio-properties-copy");
        var target = root.resolve(EnvironmentProperties.USER_CONFIGURATION);
        Files.createDirectories(target.getParent());
        Files.writeString(target, "custom.value=destination\nkeep=true\n");
        var dialog = new ImportEnvironmentPropertiesDialog(getProject(), root);
        try (var projectFiles = org.mockito.Mockito.mockStatic(StudioProjectFiles.class)) {
            contents(dialog).setText("custom.value=original-secret\n");
            table(dialog).getModel().setValueAt(true, 0, 1);
            assertNull(dialog.doValidate());
            assertEquals("custom.value=original-secret\n", field(dialog, "pending"));
            assertEquals("custom.value=original-secret\n", contents(dialog).getText());
            var diff = dialog.comparison();
            assertNotNull(diff);
            var pendingDocument = ((DocumentContent) diff.getContents().get(1)).getDocument();
            assertEquals("custom.value=original-secret\n", pendingDocument.getText());
            table(dialog).getModel().setValueAt(false, 0, 1);
            assertNotNull(dialog.doValidate());
            assertFalse(pendingDocument.isWritable());
            table(dialog).getModel().setValueAt(true, 0, 1);
            contents(dialog).setText("custom.value=revised\n");
            assertNotNull(dialog.doValidate());
            dialog.close(DialogWrapper.CANCEL_EXIT_CODE);
            assertEquals("custom.value=destination\nkeep=true\n", Files.readString(target));
            assertFalse(Files.exists(root.resolve(".gitignore")));
            assertFalse(Files.exists(root.resolve("config")));
            projectFiles.verify(() -> StudioProjectFiles.refreshConfigurationDirectory(getProject()), org.mockito.Mockito.never());
        } finally {
            if (!dialog.isDisposed()) dialog.close(DialogWrapper.CANCEL_EXIT_CODE);
            com.intellij.openapi.util.io.FileUtil.delete(root.toFile());
        }
    }

    public void testPartialPreviewPreservesOriginalAndDestinationUntilImport() throws Exception {
        var root = Files.createTempDirectory("studio-properties-merge");
        var target = root.resolve(EnvironmentProperties.USER_CONFIGURATION);
        Files.createDirectories(target.getParent());
        Files.writeString(target, "host=destination\r\nkeep=true\r\n");
        var dialog = new ImportEnvironmentPropertiesDialog(getProject(), root);
        try (var projectFiles = org.mockito.Mockito.mockStatic(StudioProjectFiles.class)) {
            String original = "host=original\nextra=not-selected\n";
            contents(dialog).setText(original);
            var diff = dialog.comparison();
            assertNotNull(diff);
            assertEquals(original, ((DocumentContent) diff.getContents().get(0)).getDocument().getText());
            var right = ((DocumentContent) diff.getContents().get(1)).getDocument();
            assertEquals("host=destination\nkeep=true\n", right.getText());
            WriteAction.run(() -> right.setText("host=original\nkeep=true\n"));
            dialog.acceptComparison();
            assertNull(dialog.doValidate());
            assertEquals("host=original\r\nkeep=true\r\n", field(dialog, "pending"));
            assertEquals(original, contents(dialog).getText());
            assertEquals("host=destination\r\nkeep=true\r\n", Files.readString(target));
            projectFiles.verify(() -> StudioProjectFiles.refreshConfigurationDirectory(getProject()), org.mockito.Mockito.never());
        } finally {
            dialog.close(DialogWrapper.CANCEL_EXIT_CODE);
            com.intellij.openapi.util.io.FileUtil.delete(root.toFile());
        }
    }

    private JButton findButton(java.awt.Container parent, String text) {
        for (var component : parent.getComponents()) {
            if (component instanceof JButton button && text.equals(button.getText())) return button;
            if (component instanceof java.awt.Container child) {
                var found = findButton(child, text);
                if (found != null) return found;
            }
        }
        return null;
    }

    private JTextArea contents(ImportEnvironmentPropertiesDialog dialog) throws Exception { return (JTextArea) field(dialog, "contents"); }
    private JTable table(ImportEnvironmentPropertiesDialog dialog) throws Exception { return (JTable) field(dialog, "files"); }
    private Object field(Object owner, String name) throws Exception {
        var field = owner.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.get(owner);
    }
}
