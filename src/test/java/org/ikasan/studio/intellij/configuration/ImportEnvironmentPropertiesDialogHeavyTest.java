package org.ikasan.studio.intellij.configuration;

import com.intellij.diff.contents.DocumentContent;
import com.intellij.diff.requests.SimpleDiffRequest;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.testFramework.HeavyPlatformTestCase;
import java.awt.Component;
import java.awt.Container;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import javax.swing.*;
import javax.swing.JButton;
import javax.swing.JTable;
import org.ikasan.studio.core.configuration.EnvironmentProperties;
import org.ikasan.studio.core.importer.SourceModuleImport.PropertyFile;
import org.ikasan.studio.intellij.project.StudioProjectFiles;
import org.ikasan.studio.ui.StudioBundle;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

public class ImportEnvironmentPropertiesDialogHeavyTest extends HeavyPlatformTestCase {
    public void testDiscoveredPropertiesShowOriginalAndDeveloperOwnedDestinationWithoutWriting() throws Exception {
        PropertyFile discovered = new PropertyFile(
                "src/main/resources/application-int.properties", "custom.value=${SERVICE_VALUE}", "int");
        ImportEnvironmentPropertiesDialog dialog = new ImportEnvironmentPropertiesDialog(getProject(), List.of(discovered));
        try (MockedStatic<StudioProjectFiles> projectFiles = Mockito.mockStatic(StudioProjectFiles.class)) {
            JButton use = findButton(Objects.requireNonNull(dialog.createCenterPanel()),
                    StudioBundle.message("sourceImport.useProperties"));
            assertNotNull(use);
            use.doClick();
            assertEquals(discovered.text(), contents(dialog).getText());
            JTable table = table(dialog);
            assertEquals(discovered.source(), table.getValueAt(0, 0));
            assertEquals(EnvironmentProperties.USER_CONFIGURATION, table.getValueAt(0, 2));
            assertNotNull(dialog.doValidate());
            SimpleDiffRequest diff = dialog.comparison();
            assertNotNull(diff);
            assertEquals(discovered.text(), ((DocumentContent) diff.getContents().get(0)).getDocument().getText());
            assertFalse(((DocumentContent) diff.getContents().get(0)).getDocument().isWritable());
            assertNull(field(dialog, "pending"));
            projectFiles.verify(() -> StudioProjectFiles.refreshConfigurationDirectory(getProject()), Mockito.never());
        } finally { dialog.close(DialogWrapper.CANCEL_EXIT_CODE); }
    }

    public void testCopyRevertAndInputEditsRequireFreshExplicitSelectionAndCancelWritesNothing() throws Exception {
        Path root = Files.createTempDirectory("studio-properties-copy");
        Path target = root.resolve(EnvironmentProperties.USER_CONFIGURATION);
        Files.createDirectories(target.getParent());
        Files.writeString(target, "custom.value=destination\nkeep=true\n");
        ImportEnvironmentPropertiesDialog dialog = new ImportEnvironmentPropertiesDialog(getProject(), root);
        try (MockedStatic<StudioProjectFiles> projectFiles = Mockito.mockStatic(StudioProjectFiles.class)) {
            contents(dialog).setText("custom.value=original-secret\n");
            table(dialog).getModel().setValueAt(true, 0, 1);
            assertNull(dialog.doValidate());
            assertEquals("custom.value=original-secret\n", field(dialog, "pending"));
            assertEquals("custom.value=original-secret\n", contents(dialog).getText());
            SimpleDiffRequest diff = dialog.comparison();
            assertNotNull(diff);
            Document pendingDocument = ((DocumentContent) diff.getContents().get(1)).getDocument();
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
            projectFiles.verify(() -> StudioProjectFiles.refreshConfigurationDirectory(getProject()), Mockito.never());
        } finally {
            if (!dialog.isDisposed()) dialog.close(DialogWrapper.CANCEL_EXIT_CODE);
            FileUtil.delete(root.toFile());
        }
    }

    public void testPartialPreviewPreservesOriginalAndDestinationUntilImport() throws Exception {
        Path root = Files.createTempDirectory("studio-properties-merge");
        Path target = root.resolve(EnvironmentProperties.USER_CONFIGURATION);
        Files.createDirectories(target.getParent());
        Files.writeString(target, "host=destination\r\nkeep=true\r\n");
        ImportEnvironmentPropertiesDialog dialog = new ImportEnvironmentPropertiesDialog(getProject(), root);
        try (MockedStatic<StudioProjectFiles> projectFiles = Mockito.mockStatic(StudioProjectFiles.class)) {
            String original = "host=original\nextra=not-selected\n";
            contents(dialog).setText(original);
            SimpleDiffRequest diff = dialog.comparison();
            assertNotNull(diff);
            assertEquals(original, ((DocumentContent) diff.getContents().get(0)).getDocument().getText());
            Document right = ((DocumentContent) diff.getContents().get(1)).getDocument();
            assertEquals("host=destination\nkeep=true\n", right.getText());
            WriteAction.run(() -> right.setText("host=original\nkeep=true\n"));
            dialog.acceptComparison();
            assertNull(dialog.doValidate());
            assertEquals("host=original\r\nkeep=true\r\n", field(dialog, "pending"));
            assertEquals(original, contents(dialog).getText());
            assertEquals("host=destination\r\nkeep=true\r\n", Files.readString(target));
            projectFiles.verify(() -> StudioProjectFiles.refreshConfigurationDirectory(getProject()), Mockito.never());
        } finally {
            dialog.close(DialogWrapper.CANCEL_EXIT_CODE);
            FileUtil.delete(root.toFile());
        }
    }

    private JButton findButton(Container parent, String text) {
        for (Component component : parent.getComponents()) {
            if (component instanceof JButton button && text.equals(button.getText())) return button;
            if (component instanceof Container child) {
                JButton found = findButton(child, text);
                if (found != null) return found;
            }
        }
        return null;
    }

    private JTextArea contents(ImportEnvironmentPropertiesDialog dialog) throws Exception { return (JTextArea) field(dialog, "contents"); }
    private JTable table(ImportEnvironmentPropertiesDialog dialog) throws Exception { return (JTable) field(dialog, "files"); }
    private Object field(Object owner, String name) throws Exception {
        Field field = owner.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.get(owner);
    }
}
