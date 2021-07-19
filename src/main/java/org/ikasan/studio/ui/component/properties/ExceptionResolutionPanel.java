package org.ikasan.studio.ui.component.properties;

import com.intellij.openapi.ui.ValidationInfo;
import org.apache.log4j.Logger;
import org.ikasan.studio.Context;
import org.ikasan.studio.model.ikasan.IkasanExceptionResolution;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.util.List;

/**
 * Encapsulate the properties entry from a UI and validity perspective.
 *
 * This panel contains the data entry for the exception and action
 */
public class ExceptionResolutionPanel extends PropertiesPanel {
    private static final Logger log = Logger.getLogger(PropertiesPanel.class);
    private ExceptionResolverPanel resolverPanel;
    private List<ExceptionResolution> exceptionResolutionList;
    private transient ExceptionResolutionEditBox exceptionResolutionEditBox;
    private JPanel exceptionActionEditorPanel = new JPanel(new GridBagLayout());      // contains the Exception and action
    private JPanel mandatoryPropertiesEditorPanel = new JPanel(new GridBagLayout());  // contains the Mandatory properties for the action
    private JPanel optionalPropertiesEditorPanel = new JPanel(new GridBagLayout());   // contains the Optional properties for the action
    private boolean updateActionPropertiesOnly = false;

    /**
     * Create the ExceptionResolutionPanel
     *
     * Note that this panel could be reused for different ExceptionResolutionProperties, it is the super.updateTargetComponent
     * that will set the property to be exposed / edited.
     *
     * @param projectKey for this project
     * @param popupMode true if this is for the popup version, false if this is for the canvas sidebar.
     */
    public ExceptionResolutionPanel(List<ExceptionResolution> exceptionResolutionList, ExceptionResolverPanel resolverPanel, String projectKey, boolean popupMode) {
        super(projectKey, popupMode);
        this.exceptionResolutionList = exceptionResolutionList;
        this.resolverPanel = resolverPanel;
    }

    /**
     * This method is invoked when we have checked its OK to process the panel i.e. all items are valid
     */
    protected void doOKAction() {
        // maybe validate and either force to correct or add the data back to the model
        if (dataHasChanged()) {
            processEditedFlowComponents();
            Context.getPipsiIkasanModel(projectKey).generateSourceFromModel();
            Context.getDesignerCanvas(projectKey).setInitialiseAllDimensions(true);
            Context.getDesignerCanvas(projectKey).repaint();
        } else {
            log.info("Data has not changed in Exception Resolution, code will not be updated");
        }
    }

    protected IkasanExceptionResolution getSelectedComponent() {
        return (IkasanExceptionResolution)super.getSelectedComponent();
    }

    @Override
    public boolean dataHasChanged() {
        return exceptionResolutionEditBox != null && exceptionResolutionEditBox.propertyValueHasChanged();
    }

    /**
     * Called once the OK button is pressed.
     * Check to see if any new values have been entered, update the model and return true if that is the case.
     * @return true if the model has been updated with new values.
     */
    @Override
    public void processEditedFlowComponents() {
        if (dataHasChanged()) {
                exceptionResolutionEditBox.updateValueObjectWithEnteredValues();
        }
    }

    /**
     * When updateTargetComponent is called, it will set the component to be exposed / edited, it will then
     * delegate update of the editor pane to this component so that we can specialise for different components.
     * @See PropertiesPanel.updateTargetComponent
     * For the given component, get all the editable properties and add them the to properties edit panel.
     */
    protected void populatePropertiesEditorPanel() {
        if (!popupMode) {
            okButton.setEnabled(false);
        }

        if (getSelectedComponent() != null) {
            // For this properties panel, the reset will only be for the action.
            if (! updateActionPropertiesOnly) {
                propertiesEditorScrollingContainer.removeAll();
                propertiesEditorPanel = new JPanel(new GridBagLayout());
                propertiesEditorPanel.setBackground(Color.WHITE);
                propertiesEditorScrollingContainer.add(propertiesEditorPanel);
                updateExceptionAndAction();

                updateActionPropertiesOnly = true;
            }
            updatePropertiesForAction();
            if (okButton != null) {
                okButton.setEnabled(true);
            }
        }
    }

    private void updateExceptionAndAction() {
        exceptionActionEditorPanel.removeAll();
        exceptionResolutionEditBox = new ExceptionResolutionEditBox(this, getSelectedComponent(), popupMode);
        GridBagConstraints gc = new GridBagConstraints();
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.insets = new Insets(3, 4, 3, 4);

        addLabelAndSimpleInput(exceptionActionEditorPanel, gc, 0, exceptionResolutionEditBox.getExceptionTitleField(), exceptionResolutionEditBox.getExceptionJComboBox());
        addLabelAndSimpleInput(exceptionActionEditorPanel, gc, 1, exceptionResolutionEditBox.getActionTitleField(), exceptionResolutionEditBox.getActionJComboBox());
        addToScrollPanelContent(exceptionActionEditorPanel, "", Color.LIGHT_GRAY, 0);
    }

    public void updatePropertiesForAction() {
        mandatoryPropertiesEditorPanel.removeAll();
        optionalPropertiesEditorPanel.removeAll();

        GridBagConstraints gc = new GridBagConstraints();
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.insets = new Insets(3, 4, 3, 4);

        // Populate the list of params to be displayed and add to respective panels
        int mandatoryParamsTabley = 0;
        int optionalParamsTabley = 0;
        List<ComponentPropertyEditBox> actionParams = exceptionResolutionEditBox.getActionParamsEditBoxList();
        if (actionParams != null && !actionParams.isEmpty()) {
            for (ComponentPropertyEditBox actionParamEditBox : actionParams) {
                if (actionParamEditBox.isMandatory()) {
                    addLabelAndParamInput(mandatoryPropertiesEditorPanel, gc, mandatoryParamsTabley++, actionParamEditBox.getPropertyTitleField(), actionParamEditBox.getInputField());
                } else {
                    addLabelAndParamInput(optionalPropertiesEditorPanel, gc, optionalParamsTabley++, actionParamEditBox.getPropertyTitleField(), actionParamEditBox.getInputField());
                }
            }
        }

        // Add the params to the display panels.
        // The the panels even if empty so we can update subsequently.
        addToScrollPanelContent(mandatoryPropertiesEditorPanel, "Mandatory Properties", Color.red, 1);
        addToScrollPanelContent(optionalPropertiesEditorPanel, "Optional Properties", Color.LIGHT_GRAY, 2);
    }

    protected String getOKButtonText() {
        return "Add";
    }

    /**
     * Get the field that should be given the focus in popup or inscreen form
     * @return the component that should be given focus or null
     */
    public JComponent getFirstFocusField() {
        JComponent firstFocus = null;
        if (exceptionResolutionEditBox != null) {
            firstFocus = exceptionResolutionEditBox.getExceptionJComboBox();
        }
        return firstFocus;
    }

    /**
     * The scrollPanelContent panel has a series of subsections for Exceptions, actions and properties
     * @param subPanel is the subsection (e.g. Exception/Action, properties)
     * @param title to place on the subsection
     * @param borderColor of the subsection
     * @param rowNumber for this label/inpur pair
     */
    private void addToScrollPanelContent(JPanel subPanel, String title, Color borderColor, int rowNumber) {
        GridBagConstraints subPanelgc = new GridBagConstraints();
        subPanelgc.fill = GridBagConstraints.HORIZONTAL;
        subPanelgc.insets = new Insets(3, 4, 3, 4);
        subPanelgc.gridx = 0;
        subPanelgc.weightx = 1;
        subPanelgc.gridy = rowNumber;
        subPanel.setBackground(Color.WHITE);
        subPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(borderColor), title,
                TitledBorder.LEFT,
                TitledBorder.TOP));
        propertiesEditorPanel.add(subPanel, subPanelgc);
    }


    private void addLabelAndSimpleInput(JPanel propertiesEditorPanel, GridBagConstraints gc, int tabley, JLabel propertyLabel, JComponent propertyInputField) {
        gc.weightx = 0.0;
        gc.gridx = 0;
        gc.gridy = tabley;
        propertiesEditorPanel.add(propertyLabel, gc);
        gc.weightx = 1.0;
        gc.gridx = 1;
        propertiesEditorPanel.add(propertyInputField, gc);
    }

    private void addLabelAndParamInput(JPanel propertiesEditorPanel, GridBagConstraints gc, int tabley, JLabel propertyLabel, ComponentInput componentInput) {
        gc.weightx = 0.0;
        gc.gridx = 0;
        gc.gridy = tabley;
        propertiesEditorPanel.add(propertyLabel, gc);
        ++gc.gridx;
        if (!componentInput.isBooleanInput()) {
            gc.weightx = 1.0;
            propertiesEditorPanel.add(componentInput.getFirstFocusComponent(), gc);
        } else {
            JPanel booleanPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
            booleanPanel.setBackground(Color.WHITE);
            booleanPanel.add(new JLabel("true"));
            booleanPanel.add(componentInput.getTrueBox());
            booleanPanel.add(new JLabel("false"));
            booleanPanel.add(componentInput.getFalseBox());
            propertiesEditorPanel.add(booleanPanel, gc);
        }
    }

    /**
     * Ensure the fields are valid and the exception / action combo does not already exist.
     *
     * This Object holds the metadata for the object
     *
     * @return a list of ValidationInfo that will only be populated if there are validation errors on the form.
     */
    protected List<ValidationInfo> doValidateAll() {
        List<ValidationInfo> result = exceptionResolutionEditBox.doValidateAll();
        return result;
    }

    public boolean hasExceptionResolution(String exception) {
        return exceptionResolutionList.stream().anyMatch(res -> res.getIkasanExceptionResolution().getTheException().equals(exception));
    }
}
