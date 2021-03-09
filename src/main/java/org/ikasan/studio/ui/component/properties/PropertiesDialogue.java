package org.ikasan.studio.ui.component.properties;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.ValidationInfo;
import org.ikasan.studio.model.ikasan.IkasanComponentProperty;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PropertiesDialogue extends DialogWrapper {
    PropertiesPanel propertiesPanel;
    Component parentComponent;

    /**
     * Popup Modal window to support entry of properties.
     *
     * The main purpose is to force the developer to enter mandatory properties when a component is first dragged
     * onto the canvas. This solves a number of issues and is much more efficient then drag and fix later.
     *
     * @todo maybe use this in non modal form for the in-canvas properties editing.
     *
     * @param project currently being worked upon
     * @param parentComponent of this popup
     * @param propertiesPanel to display and have entries taken on.
     */
    public PropertiesDialogue(Project project, Component parentComponent, PropertiesPanel propertiesPanel) {
        super(project, parentComponent, true, IdeModalityType.PROJECT); // use current window as parent
        this.propertiesPanel = propertiesPanel;
        this.parentComponent = parentComponent;
        init();  // which calls createCenterPanel() below so make sure any state is initialised first.
        setTitle(propertiesPanel.getPropertiesPanelTitle());
        setOKButtonText("Update Code");
    }

    /**
     * Inject the payload i.e. the ikasan UI element we wish to expose / edit.
     */
    @Override
    protected JComponent createCenterPanel() {
        int niceWidth = (int) (parentComponent.getWidth() * 0.6);
        propertiesPanel.setPreferredSize(new Dimension(niceWidth, parentComponent.getHeight()));
        return propertiesPanel;
    }

    /**
     * This method is invoked by default implementation of "OK" action. It just closes dialog
     * with {@code OK_EXIT_CODE}. This is convenient place to override functionality of "OK" action.
     * Note that the method does nothing if "OK" action isn't enabled.
     */
    @Override
    protected void doOKAction() {
        if (getOKAction().isEnabled()) {
            propertiesPanel.processEditedFlowComponents();
            close(OK_EXIT_CODE);
        }
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
