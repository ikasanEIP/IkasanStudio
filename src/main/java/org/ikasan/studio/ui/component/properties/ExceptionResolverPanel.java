package org.ikasan.studio.ui.component.properties;

import com.intellij.openapi.ui.ValidationInfo;
import org.apache.log4j.Logger;
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
public class ExceptionResolverPanel extends PropertiesPanel {
    private static final Logger log = Logger.getLogger(ExceptionResolverPanel.class);
    private transient ExceptionResolverEditBox exceptionResolverEditBox;

    /**
     * Create the ExceptionResolutionPanel
     *
     * Note that this panel could be reused for different ExceptionResolutionProperties, it is the super.updateTargetComponent
     * that will set the property to be exposed / edited.
     *
     * @param projectKey for this project
     * @param componentInitialisation true if this is for the popup version, false if this is for the canvas sidebar.
     */
    public ExceptionResolverPanel(String projectKey, boolean componentInitialisation) {
        super(projectKey, componentInitialisation);
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
            log.info("Data hos not changed in exception resolver, code will not be updated");
        }
    }

    @Override
    protected IkasanExceptionResolver getSelectedComponent() {
        return (IkasanExceptionResolver)super.getSelectedComponent();
    }

    @Override
    public boolean dataHasChanged() {
        return exceptionResolverEditBox != null && exceptionResolverEditBox.propertyValueHasChanged();
    }

    /**
     * Called once the OK button is pressed.
     * Check to see if any new values have been entered, update the model and return true if that is the case.
     * @return true if the model has been updated with new values.
     */
    public void processEditedFlowComponents() {
        if (dataHasChanged()) {
            exceptionResolverEditBox.updateValueObjectWithEnteredValues();
        }
    }

    /**
     * When updateTargetComponent is called, it will set the component to be exposed / edited, it will then
     * delegate update of the editor pane to this component so that we can specialise for different components.
     *
     * For the given component, get all the editable properties and add them the to properties edit panel.
     */
    protected void populatePropertiesEditorPanel() {
        if (okButton != null) {
            if (dataHasChanged()) {
                okButton.setEnabled(true);
            } else {
                okButton.setEnabled(false);
            }
        }

        if (getSelectedComponent() != null) {
            propertiesEditorPanel = new JPanel(new GridBagLayout());
            propertiesEditorPanel.setBackground(Color.WHITE);
            propertiesEditorScrollingContainer.removeAll();

            // Only initialise on the first pass.
            if (exceptionResolverEditBox == null) {
                exceptionResolverEditBox = new ExceptionResolverEditBox(this, projectKey, getSelectedComponent(), componentInitialisation);
            }

            JPanel exceptionResolutionTablePanel = new JPanel(new GridBagLayout());
            int exceptionResolutionTabley = 0;
            addDisplayDataToTable(true, exceptionResolutionTablePanel, exceptionResolutionTabley++,
                exceptionResolverEditBox.getAddButton(),
                exceptionResolverEditBox.getExceptionTitleField(),
                exceptionResolverEditBox.getActionTitleField(),
                exceptionResolverEditBox.getParamsTitleField());

            // Populate the list of params to be displayed and add to respective panels
            if (exceptionResolverEditBox.getExceptionResolutionList() != null &&
                    !exceptionResolverEditBox.getExceptionResolutionList().isEmpty()) {
                for (ExceptionResolution exceptionResolution : exceptionResolverEditBox.getExceptionResolutionList()) {
                    JPanel paramsSubPanel = new JPanel(new GridBagLayout());
                    paramsSubPanel.setBackground(Color.WHITE);
                    int subPanelY = 0;

                    for (ComponentPropertyEditBox componentPropertyEditBox : exceptionResolution.getActionParamsEditBoxList()) {
                        addParamsToTable(paramsSubPanel, subPanelY++, componentPropertyEditBox.getPropertyTitleField(), componentPropertyEditBox.getInputField());
                    }
                    addDisplayDataToTable(false, exceptionResolutionTablePanel, exceptionResolutionTabley++,
                        exceptionResolution.getDeleteButton(),
                        exceptionResolution.getExceptionField(),
                        exceptionResolution.getActionField(),
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
            setSubPanel(propertiesEditorPanel, exceptionResolutionTablePanel, "", Color.LIGHT_GRAY, gc1);
            propertiesEditorScrollingContainer.add(propertiesEditorPanel);
        }
    }

    /**
     * Get the field that should be given the focus in popup or inscreen form
     * @return the component that should be given focus or null
     */
    public JComponent getFirstFocusField() {
        JComponent firstField = null;
        if (exceptionResolverEditBox != null) {
            firstField = exceptionResolverEditBox.getAddButton();
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

    private void addDisplayDataToTable(boolean isHeader, JPanel jPanel, int tabley,
                                       JComponent actionButton, JComponent theException, JComponent action, JComponent params) {
        GridBagConstraints gc = new GridBagConstraints();
        gc.fill = GridBagConstraints.BOTH;
        gc.weightx = 0.0;
        gc.gridx = 0;
        gc.gridy = tabley;
        gc.weighty = 1;
        Color backGroundColor = Color.WHITE;

        if (isHeader) {
            backGroundColor = new Color(242, 242, 242);
        }
        actionButton.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY,1));
        jPanel.add(formatCell(actionButton, backGroundColor, false), gc);
        gc.gridx += 1;
        jPanel.add(formatCell(theException, backGroundColor, true), gc);
        gc.gridx += 1;
        jPanel.add(formatCell(action, backGroundColor, true), gc);
        gc.weightx = 1.0;
        gc.gridx += 1;
        jPanel.add(formatCell(params, backGroundColor, true), gc);
    }

    private JComponent formatCell(JComponent theComponent, Color backgroundColor, boolean isButton) {
        if (isButton) {
            theComponent.setOpaque(true);
            theComponent.setBackground(backgroundColor);
            theComponent.setBorder(BorderFactory.createEmptyBorder(0,5,0,5));
        }
        JPanel tableCell = new JPanel();
        tableCell.setBackground(backgroundColor);
        tableCell.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));
        tableCell.add(theComponent);
        return tableCell;
    }

    private void addParamsToTable(JPanel jPanel, int tabley, JLabel propertyLabel, ComponentInput componentInput) {
        if (componentInput != null) {
            GridBagConstraints gc = new GridBagConstraints();
            gc.fill = GridBagConstraints.BOTH;
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
    }

    /**
     * Ensure the fields are valid and the exception / action combo does not already exist.
     *
     * This Object holds the metadata for the object
     *
     * @return a list of ValidationInfo that will only be populated if there are validation errors on the form.
     */
    protected List<ValidationInfo> doValidateAll() {
        List<ValidationInfo> result = exceptionResolverEditBox.doValidateAll();
        return result;
    }
}
