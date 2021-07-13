package org.ikasan.studio.ui.component.properties;

import com.intellij.openapi.ui.ValidationInfo;
import org.ikasan.studio.Context;
import org.ikasan.studio.model.ikasan.IkasanExceptionResolver;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.util.List;

/**
 * Encapsulate the properties entry from a UI and validity perspective.
 *
 * This panel contains the data entry for the exception and action
 */
public class ExceptionResolverPropertiesPanel extends PropertiesPanel {
    private transient ExceptionResolverPropertyEditBox exceptionResolverPropertyEditBox;

    /**
     * Create the ExceptionResolutionPropertiesPanel
     *
     * Note that this panel could be reused for different ExceptionResolutionProperties, it is the super.updateTargetComponent
     * that will set the property to be exposed / edited.
     *
     * @param projectKey for this project
     * @param popupMode true if this is for the popup version, false if this is for the canvas sidebar.
     */
    public ExceptionResolverPropertiesPanel(String projectKey, boolean popupMode) {
        super(projectKey, popupMode);
    }

//    public void ExceptionResolutionPropertiesPanel(IkasanExceptionResolver ikasanExceptionResolver, IkasanExceptionResolution ikasanExceptionResolution) {
//    public void ExceptionResolutionPropertiesPanel(String projectKey, boolean popupMode) {
//        super(projectKey, popupMode);
//        super(projectKey, popupMode);
////        this.ikasanExceptionResolution = ikasanExceptionResolution;
//        this.propertyEditBox = new ExceptionResolutionPropertyEditBox(ikasanExceptionResolver, ikasanExceptionResolution, popupMode);
//    }
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
        }
    }

    protected IkasanExceptionResolver getSelectedComponent() {
        return (IkasanExceptionResolver)super.getSelectedComponent();
    }

    @Override
    public boolean dataHasChanged() {
        return exceptionResolverPropertyEditBox != null && exceptionResolverPropertyEditBox.propertyValueHasChanged();
    }

    /**
     * Called once the OK button is pressed.
     * Check to see if any new values have been entered, update the model and return true if that is the case.
     * @return true if the model has been updated with new values.
     */
    public void processEditedFlowComponents() {
        if (dataHasChanged()) {
            exceptionResolverPropertyEditBox.updateValueObjectWithEnteredValues();
        }
    }

    /**
     * When updateTargetComponent is called, it will set the component to be exposed / edited, it will then
     * delegate update of the editor pane to this component so that we can specialise for different components.
     *
     * For the given component, get all the editable properties and add them the to properties edit panel.
     * @return the fully populated editor panel
     */
    protected JPanel populatePropertiesEditorPanel() {
        JPanel containerPanel = new JPanel(new GridBagLayout());
        JPanel exceptionResolutionTablePanel = new JPanel(new GridBagLayout());
        containerPanel.setBackground(Color.WHITE);

        if (!popupMode) {
            okButton.setEnabled(false);
        }

        if (getSelectedComponent() != null) {
            scrollableGridbagPanel.removeAll();
            exceptionResolverPropertyEditBox = new ExceptionResolverPropertyEditBox(projectKey, getSelectedComponent(), popupMode);
            int exceptionResolutionTabley = 0;
//
//            if (getSelectedComponent().getProperty(IkasanComponentPropertyMeta.NAME) != null) {
//                propertyEditBox.add(addNameValueToPropertiesEditPanel(mandatoryPropertiesEditorPanel,
//                        getSelectedComponent().getProperty(IkasanComponentPropertyMeta.NAME), gc, mandatoryTabley++));
//            }
//            if (  getSelectedComponent().getType().getMetadataMap().size() > 0) {

            GridBagConstraints gc = new GridBagConstraints();
            gc.fill = GridBagConstraints.HORIZONTAL;
            gc.insets = new Insets(3, 4, 3, 4);

            addLabelAndSimpleInput(exceptionResolutionTablePanel, gc, exceptionResolutionTabley++,
                exceptionResolverPropertyEditBox.getAddButton(),
                exceptionResolverPropertyEditBox.getExceptionTitleField(),
                exceptionResolverPropertyEditBox.getActionTitleField(),
                exceptionResolverPropertyEditBox.getParamsTitleField());

            // Populate the list of params to be displayed and add to respective panels
            if (getSelectedComponent().getIkasanExceptionResolutionMap() != null &&
                    !getSelectedComponent().getIkasanExceptionResolutionMap().isEmpty()) {
                for (ExceptionResolutionPropertyDisplayBox exceptionResolutionPropertyDisplayBox : exceptionResolverPropertyEditBox.getExceptionResolutionPropertyDisplayBoxList()) {
                    JPanel paramsSubPanel = new JPanel(new GridBagLayout());
                    int subPanelY = 0;
                    GridBagConstraints subPanelgc = new GridBagConstraints();
                    subPanelgc.fill = GridBagConstraints.HORIZONTAL;
                    subPanelgc.insets = new Insets(3, 4, 3, 4);
                    for (ComponentPropertyEditBox componentPropertyEditBox : exceptionResolutionPropertyDisplayBox.getActionParamsEditBoxList()) {
                        addLabelAndParamInput(paramsSubPanel, subPanelgc, subPanelY, componentPropertyEditBox.getPropertyTitleField(), componentPropertyEditBox.getInputField());
                    }
                    addLabelAndSimpleInput(exceptionResolutionTablePanel, gc, exceptionResolutionTabley++,
                        exceptionResolutionPropertyDisplayBox.getDeleteButton(),
                        exceptionResolutionPropertyDisplayBox.getExceptionField(),
                        exceptionResolutionPropertyDisplayBox.getActionField(),
                        paramsSubPanel);
                }
            }

            GridBagConstraints gc1 = new GridBagConstraints();
            gc1.fill = GridBagConstraints.HORIZONTAL;
            gc1.insets = new Insets(3, 4, 3, 4);
            gc1.gridx = 0;
            gc1.weightx = 1;
            gc1.gridy = 0;

            // Add the params to the display panels.
            setSubPanel(containerPanel, exceptionResolutionTablePanel, "", Color.LIGHT_GRAY, gc1);
            scrollableGridbagPanel.add(containerPanel);
            if (okButton != null) {
                    okButton.setEnabled(true);
            }
        }

        return containerPanel;
    }

//    public void setFocusOnFirstComponent() {
//        JComponent firstComponent = getFirstFocusField();
//        if (firstComponent != null) {
//            Context.getProject(projectKey);
//            IdeFocusManager.getInstance(Context.getProject(projectKey)).requestFocus(firstComponent, true);
//        }
//    }

    /**
     * Get the field that should be given the focus in popup or inscreen form
     * @return the component that should be given focus or null
     */
    public JComponent getFirstFocusField() {
        JComponent firstField = null;
        if (exceptionResolverPropertyEditBox != null) {
            firstField = exceptionResolverPropertyEditBox.getAddButton();
        }
        return firstField;
    }

    /**
     * The properties panel has a series of subsections for mandatory, options and code regenerating components
     * @param allPropertiesEditorPanel is the parent
     * @param subPanel is the subsection (e.g. mandatory, optional, code regenerating)
     * @param title to place on the subsection
     * @param borderColor of the subsection
     * @param gc1 is used to dictate layout and relay layout to the next subsection.
     */
    private void setSubPanel(JPanel allPropertiesEditorPanel, JPanel subPanel, String title, Color borderColor, GridBagConstraints gc1) {
        subPanel.setBackground(Color.WHITE);
        subPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(borderColor), title,
                TitledBorder.LEFT,
                TitledBorder.TOP));
        allPropertiesEditorPanel.add(subPanel, gc1);
        gc1.gridy += 1;
    }


    private void addLabelAndSimpleInput(JPanel jPanel, GridBagConstraints gc, int tabley,
                    JComponent actionButton, JComponent theException, JComponent action, JComponent params) {
        gc.weightx = 0.0;
        gc.gridx = 0;
        gc.gridy = tabley;
        jPanel.add(actionButton, gc);
        gc.gridx += 1;
        jPanel.add(theException, gc);
        gc.gridx += 1;
        jPanel.add(action, gc);
        gc.weightx = 1.0;
        gc.gridx += 1;
        jPanel.add(params, gc);
    }

    private void addLabelAndParamInput(JPanel jPanel, GridBagConstraints gc, int tabley, JLabel propertyLabel, ComponentInput componentInput) {
        gc.weightx = 0.0;
        gc.gridx = 0;
        gc.gridy = tabley;
        jPanel.add(propertyLabel, gc);
        ++gc.gridx;
        if (!componentInput.isBooleanInput()) {
            gc.weightx = 1.0;
            jPanel.add(componentInput.getFirstFocusComponent(), gc);
        } else {
            JPanel booleanPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
            booleanPanel.setBackground(Color.WHITE);
            booleanPanel.add(new JLabel("true"));
            booleanPanel.add(componentInput.getTrueBox());
            booleanPanel.add(new JLabel("false"));
            booleanPanel.add(componentInput.getFalseBox());
            jPanel.add(booleanPanel, gc);
        }
        componentInput.setEnabled(false);
    }

//    private void addLabelInputEditorAndGenerateSource(JPanel propertiesEditorPanel, GridBagConstraints gc, int tabley,
//                                                      JLabel propertyLabel, JComponent propertyInputField) {
//        gc.weightx = 0.0;
//        gc.gridx = 0;
//        gc.gridy = tabley;
//        propertiesEditorPanel.add(propertyLabel, gc);
//        gc.weightx = 1.0;
//        gc.gridx = 1;
//        propertiesEditorPanel.add(propertyInputField, gc);
//    }



//    /**
//     * Determine if the property has been updated.
//     * @param property of the existing ikasan Component
//     * @param propertyEditBox should never be null when called.
//     * @return true if the property has been altered
//     */
//    private boolean propertyValueHasChanged(IkasanComponentProperty property, ExceptionResolutionPropertyEditBox propertyEditBox) {
//        Object propertyValue = null;
//        if (property != null) {
//            propertyValue = property.getValue();
//        }
//
//        return ((propertyValue == null && propertyEditBox.editBoxHasValue()) ||
////                (property != null && editBoxHasValue(propertyEditBox) && !property.getValue().equals(propertyEditBox.getValue())) ||
//                (propertyValue != null && !property.getValue().equals(propertyEditBox.getValue())) ||
//                (propertyEditBox != null && propertyEditBox.isUserMaintainedClassWithPermissionToRegenerate() && editBoxHasValue(propertyEditBox)) );
//    }

//    /**
//     * Determine if the edit box has a valid value that would warrant the source code to be updated
//     * If the property generates source code, also check if 'regenerate' is selected (note this is assumed true in popup mode).
//     * @param propertyEditBox to examine.
//     * @return true if the editbox has a non-whitespace / real value.
//     */
//    private boolean editBoxHasValue(ExceptionResolutionPropertyEditBox propertyEditBox) {
//        boolean hasValue = false;
//        if (propertyEditBox != null) {
//            Object value = propertyEditBox.getValue();
//            if (value instanceof String) {
//                if (((String) value).length() > 0) {
//                    hasValue = true;
//                }
//            } else {
//                hasValue = (value != null);
//            }
//        }
//        return hasValue;
//    }

//    public List<ExceptionResolutionPropertyEditBox> getExceptionHandlerPropertyEditBoxList() {
//        return propertyEditBox;
//    }

//    public IkasanComponent getSelectedComponent() {
//        return getSelectedComponent();
//    }

    /**
     * Ensure the fields are valid and the exception / action combo does not already exist.
     *
     * This Object holds the metadata for the object
     *
     * @return a list of ValidationInfo that will only be populated if there are validation errors on the form.
     */
    protected List<ValidationInfo> doValidateAll() {
        List<ValidationInfo> result = exceptionResolverPropertyEditBox.doValidateAll();
        return result;
    }
}
