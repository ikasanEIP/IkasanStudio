package org.ikasan.studio.ui.component.properties;

import com.intellij.ide.util.TreeClassChooser;
import com.intellij.ide.util.TreeClassChooserFactory;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiClass;
import org.ikasan.studio.core.metapack.ComponentLibrary;
import org.ikasan.studio.core.model.ikasan.instance.ComponentProperty;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import javax.swing.SwingUtilities;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class ComponentPropertyEditRowClassChooserTest {
    @ParameterizedTest
    @ValueSource(strings = {"V3.3.9", "V4.1.6"})
    void routerInputCanChooseJdkClassAndCancellationPreservesIt(String pack) throws Exception {
        var meta = ComponentLibrary.getIkasanComponentByKeyMandatory(pack, "Multi Recipient Router")
                .getMetadata("fromType");
        Project project = mock(Project.class);
        TreeClassChooserFactory factory = mock(TreeClassChooserFactory.class);
        TreeClassChooser chooser = mock(TreeClassChooser.class);
        PsiClass stringClass = mock(PsiClass.class);
        when(project.getService(TreeClassChooserFactory.class)).thenReturn(factory);
        when(factory.createAllProjectScopeChooser(anyString())).thenReturn(chooser);
        when(chooser.getSelected()).thenReturn(stringClass).thenReturn(null);
        when(stringClass.getQualifiedName()).thenReturn("java.lang.String");
        var property = new ComponentProperty(meta, "javax.jms.Message");

        SwingUtilities.invokeAndWait(() -> {
            var row = new ComponentPropertyEditRow(project, property, false, () -> {}, null);
            row.resetDataEntryComponentsWithNewValues();
            row.getChooseValueButton().doClick();
            assertEquals("java.lang.String", row.getInputField().getPropertyValueField().getText());
            assertTrue(row.propertyValueHasChanged());
            assertTrue(row.doValidateAll().isEmpty());
            row.updateValueObjectWithEnteredValues();
            assertEquals("java.lang.String", property.getValue());
            row.getChooseValueButton().doClick();
            assertEquals("java.lang.String", row.getInputField().getPropertyValueField().getText());
        });
        verify(factory, times(2)).createAllProjectScopeChooser(anyString());
        verify(factory, never()).createProjectScopeChooser(anyString());
        verify(chooser, times(2)).showDialog();
    }
}
