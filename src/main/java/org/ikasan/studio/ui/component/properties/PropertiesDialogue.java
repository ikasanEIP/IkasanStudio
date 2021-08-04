package org.ikasan.studio.ui.component.properties;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.openapi.util.SystemInfoRt;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

/**
 * The popup wrapper for the Properties panel.
 */
public class PropertiesDialogue extends DialogWrapper {
    PropertiesPanel propertiesPanel;
    Component parentComponent;

//    /**
//     * Popup Modal window to support entry of properties.
//     *
//     * The main purpose is to force the developer to enter mandatory properties when a component is first dragged
//     * onto the canvas. This solves a number of issues and is much more efficient then drag and fix later.
//     *
//     * @todo maybe use this in non modal form for the in-canvas properties editing.
//     *
//     * @param project currently being worked upon
//     * @param parentComponent of this popup
//     * @param propertiesPanel to display and have entries taken on.
//     */
//    public PropertiesDialogue(Project project, Component parentComponent, PropertiesPanel propertiesPanel, boolean modal) {
//        super(project, parentComponent, true, IdeModalityType.PROJECT); // use current window as parent
//        propertiesPanel.setPropertiesDialogue(this);
//        this.propertiesPanel = propertiesPanel;
//        this.parentComponent = parentComponent;
//        init();  // which calls createCenterPanel() below so make sure any state is initialised first.
//        setTitle(propertiesPanel.getPropertiesPanelTitle());
//        setOKButtonText(propertiesPanel.getOKButtonText());
//        setModal(modal);
//    }

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
        propertiesPanel.setPropertiesDialogue(this);
        this.propertiesPanel = propertiesPanel;
        this.parentComponent = parentComponent;
        init();  // which calls createCenterPanel() below so make sure any state is initialised first.
        setTitle(propertiesPanel.getPropertiesPanelTitle());
        setOKButtonText(propertiesPanel.getOKButtonText());
    }

    /**
     * Inject the payload i.e. the ikasan UI element we wish to expose / edit.
     */
    @Override
    protected JComponent createCenterPanel() {
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
    @Override
    protected java.util.List<ValidationInfo> doValidateAll() {

        return propertiesPanel.doValidateAll();
    }

    /**
     * Override parent version to set focus to properties component
     * @return
     */
    @Nullable
    @Override
    public JComponent getPreferredFocusedComponent() {
        if (propertiesPanel != null && propertiesPanel.getFirstFocusField() != null) {
            return propertiesPanel.getFirstFocusField();
        }
        return SystemInfoRt.isMac ? myPreferredFocusedComponent : null;
    }
}
