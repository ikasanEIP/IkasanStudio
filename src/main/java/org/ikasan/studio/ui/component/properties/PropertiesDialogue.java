package org.ikasan.studio.ui.component.properties;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.ValidationInfo;
import jdk.internal.jline.internal.Nullable;
import org.ikasan.studio.model.Ikasan.IkasanComponentProperty;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PropertiesDialogue extends DialogWrapper {
    PropertiesPanel propertiesPanel;
    Component parentComponent;

    public PropertiesDialogue(Project project, Component parentComponent, PropertiesPanel propertiesPanel) {
        super(project, parentComponent, true, IdeModalityType.IDE); // use current window as parent
        this.propertiesPanel = propertiesPanel;
        this.parentComponent = parentComponent;
        init();  // which calls createCenterPanel() below so make sure any state is initialised first.
        setTitle(propertiesPanel.getPropertiesPanelTitle());
        setOKButtonText("Update Code");

    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
//        propertiesPanel.setMaximumSize(new Dimension(parentComponent.getWidth(), parentComponent.getHeight()));
//        propertiesPanel.setSize(new Dimension(parentComponent.getWidth(), parentComponent.getHeight()));
//        propertiesPanel.setSize(new Dimension(200, 200));
        int niceWidth = (int) (parentComponent.getWidth() * 0.6);
        propertiesPanel.setPreferredSize(new Dimension(niceWidth, parentComponent.getHeight()));
        return propertiesPanel;
    }

    @Override
    protected Action [] createActions() {
        return new Action[]{getOKAction(), getCancelAction()};
    }

    /**
     * Validates user input and returns {@code List<ValidationInfo>}.
     * If everything is fine the returned list is empty otherwise
     * the list contains all invalid fields with error messages.
     * This method should preferably be used when validating forms with multiply
     * fields that require validation.
     *
     * @return {@code List<ValidationInfo>} of invalid fields. List
     * is empty if no errors found.
     *
     * @see <a href="https://jetbrains.design/intellij/principles/validation_errors/">Validation errors guidelines</a>
     */
    @NotNull
    protected java.util.List<ValidationInfo> doValidateAll() {
        ValidationInfo vi = doValidate();

        List<ValidationInfo> result = new ArrayList<>();
        for (final ComponentPropertyEditBox editPair: propertiesPanel.getComponentPropertyEditBoxList()) {
            final String key = editPair.getLabel();
            IkasanComponentProperty componentProperty = propertiesPanel.getSelectedComponent().getProperties().get(key);
            // Only mandatory properties are always populated.
            if (componentProperty != null &&
                    componentProperty.getMeta().isMandatory() &&
                    editPair.isEmpty()) {
                result.add(new ValidationInfo("This property must be set", editPair.getInputField()));
            }
        }
        if (! result.isEmpty()) {
            return result;
        } else {
            return vi != null ? Collections.singletonList(vi) : Collections.emptyList();
        }
    }
}

