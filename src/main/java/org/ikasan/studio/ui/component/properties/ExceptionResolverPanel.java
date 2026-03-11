package org.ikasan.studio.ui.component.properties;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.ui.components.JBPanel;
import com.intellij.util.ui.JBUI;
import org.ikasan.studio.core.model.ikasan.instance.ExceptionResolver;
import org.ikasan.studio.ui.StudioUIUtils;
import org.ikasan.studio.ui.UiContext;
import org.ikasan.studio.ui.model.StudioPsiUtils;
import org.ikasan.studio.ui.theme.ThemeAwareColors;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.util.List;

/**
 * Encapsulate the properties entry from a UI and validity perspective.
 * This panel contains the data entry for the exception and action
 */
@SuppressWarnings("rawtypes")
public class ExceptionResolverPanel extends PropertiesPanel {
    private transient ExceptionResolutionTableDisplay exceptionResolutionTableDisplay;

    /**
     * Convenience wrapper for cleaner code in this class.
     * Delegates to centralized ThemeAwareColors utility.
     */
    private static Color getThemeAwareBackgroundColor() {
        return ThemeAwareColors.getBackgroundColor();
    }

    /**
     * Convenience wrapper for cleaner code in this class.
     * Delegates to centralized ThemeAwareColors utility.
     */
    private static Color getThemeAwareHeaderColor() {
        return ThemeAwareColors.getHeaderColor();
    }

    /**
     * Create the ExceptionResolutionPanel
     * Note that this panel could be reused for different ExceptionResolutionProperties, it is the super.updateTargetComponent
     * that will set the property to be exposed / edited.
     *
     * @param project is the Intellij project instance
     * @param componentInitialisation true if this is for the popup version, false if this is for the canvas sidebar.
     */
    public ExceptionResolverPanel(Project project, boolean componentInitialisation) {
        super(project, componentInitialisation);
    }

    /**
     * This method is invoked when we have checked it's OK to process the panel i.e. all items are valid
     */
    protected void doOKAction() {
        // maybe validate and either force to correct or add the data back to the model
        if (dataHasChangedAndOKToProcess()) {
            UiContext uiContext = project.getService(UiContext.class);
            StudioUIUtils.displayIdeaInfoMessage(project, "Code generation in progress, please wait.");
            StudioPsiUtils.refreshCodeFromModel(project);
            uiContext.getDesignerCanvas().setInitialiseAllDimensions(true);
            uiContext.getDesignerCanvas().repaint();
        } else {
            StudioUIUtils.displayIdeaWarnMessage(project, "Data hos not changed in exception resolver, code will not be updated.");
        }
    }

    @Override
    protected ExceptionResolver getSelectedComponent() {
        return (ExceptionResolver)super.getSelectedComponent();
    }

    @Override
    public boolean dataHasChangedAndOKToProcess() {
        return exceptionResolutionTableDisplay != null && exceptionResolutionTableDisplay.propertyValueHasChanged();
    }

    /**
     * Called once the OK button is pressed.
     * Check to see if any new values have been entered, update the model and return true if that is the case.
     */
    public void updateComponentsWithNewValues() {
        if (dataHasChangedAndOKToProcess()) {
            exceptionResolutionTableDisplay.updateValueObjectWithEnteredValues();
        }
    }

    /**
     * When updateTargetComponent is called, it will set the component to be exposed / edited, it will then
     * delegate update of the editor pane to this component so that we can specialise for different components.
     * For the given component, get all the editable properties and add them the to properties edit panel.
     */
    protected void populatePropertiesEditorPanel() {
        if (updateCodeButton != null) {
            updateCodeButton.setEnabled(dataHasChangedAndOKToProcess());
        }

        if (getSelectedComponent() != null) {
            propertiesEditorPanel = new JBPanel(new GridBagLayout());
            propertiesEditorPanel.setBorder(null);
            propertiesEditorPanel.setBackground(getThemeAwareBackgroundColor());
            propertiesEditorScrollingContainer.removeAll();

            // Only initialise on the first pass.
            if (exceptionResolutionTableDisplay == null) {
                exceptionResolutionTableDisplay = new ExceptionResolutionTableDisplay(this, project, getSelectedComponent(), componentInitialisation);
            }

            JBPanel exceptionResolutionTablePanel = new JBPanel(new GridBagLayout());
            exceptionResolutionTablePanel.setBorder(null);
            int exceptionResolutionTabley = 0;
            addDisplayDataToTable(true, exceptionResolutionTablePanel, exceptionResolutionTabley++,
                exceptionResolutionTableDisplay.getAddButton(),
                exceptionResolutionTableDisplay.getExceptionTitleField(),
                exceptionResolutionTableDisplay.getActionTitleField(),
                exceptionResolutionTableDisplay.getParamsTitleField());

            // Populate the list of params to be displayed and add to respective panels
            if (exceptionResolutionTableDisplay.getExceptionResolutionRowDisplayList() != null &&
                    !exceptionResolutionTableDisplay.getExceptionResolutionRowDisplayList().isEmpty()) {
                for (ExceptionResolutionRowDisplay exceptionResolutionRowDisplay : exceptionResolutionTableDisplay.getExceptionResolutionRowDisplayList()) {
                    JBPanel paramsSubPanel = new JBPanel(new GridBagLayout());
                    paramsSubPanel.setBorder(null);
                    paramsSubPanel.setBackground(getThemeAwareBackgroundColor());
                    int subPanelY = 0;

                    for (ComponentPropertyEditBox componentPropertyEditBox : exceptionResolutionRowDisplay.getActionParamsEditBoxList()) {
                        addParamsToTable(paramsSubPanel, subPanelY++, componentPropertyEditBox.getPropertyTitleField(), componentPropertyEditBox.getInputField());
                    }
                    addDisplayDataToTable(false, exceptionResolutionTablePanel, exceptionResolutionTabley++,
                        exceptionResolutionRowDisplay.getDeleteButton(),
                        exceptionResolutionRowDisplay.getExceptionField(),
                        exceptionResolutionRowDisplay.getActionField(),
                        paramsSubPanel);
                }
            }

            GridBagConstraints gc1 = new GridBagConstraints();
            gc1.fill = GridBagConstraints.HORIZONTAL;
            gc1.insets = JBUI.insets(3, 4);
            gc1.gridx = 0;
            gc1.weightx = 1;
            gc1.gridy = 0;

            // Add the params to the display panels.
            setSubPanel(propertiesEditorPanel, exceptionResolutionTablePanel, "", StudioUIUtils.getLineColor(), gc1);
            propertiesEditorScrollingContainer.add(propertiesEditorPanel);
        }
    }

    /**
     * Get the field that should be given the focus in popup or inscreen form
     * @return the component that should be given focus or null
     */
    public JComponent getFirstFocusField() {
        JComponent firstField = null;
        if (exceptionResolutionTableDisplay != null) {
            firstField = exceptionResolutionTableDisplay.getAddButton();
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
        subPanel.setBackground(getThemeAwareBackgroundColor());
        if (!title.isEmpty()) {
            subPanel.setBorder(BorderFactory.createTitledBorder(
                    BorderFactory.createLineBorder(borderColor), title,
                    TitledBorder.LEFT,
                    TitledBorder.TOP));
        }
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
        Color backGroundColor = getThemeAwareBackgroundColor();

        if (isHeader) {
            backGroundColor = getThemeAwareHeaderColor();
        }
        actionButton.setBorder(BorderFactory.createLineBorder(StudioUIUtils.getLineColor(),1));
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
            theComponent.setBorder(JBUI.Borders.empty(0, 5));
        }
        JBPanel tableCell = new JBPanel();
        tableCell.setBackground(backgroundColor);
        tableCell.setBorder(BorderFactory.createLineBorder(StudioUIUtils.getLineColor(), JBUI.scale(1)));
        tableCell.add(theComponent);
        return tableCell;
    }

    private void addParamsToTable(@SuppressWarnings("rawtypes") JBPanel jPanel, int tabley, JLabel propertyLabel, ComponentInput componentInput) {
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
                booleanPanel.setBackground(getThemeAwareBackgroundColor());
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
     * This Object holds the metadata for the object
     * @return a list of ValidationInfo that will only be populated if there are validation errors on the form.
     */
    protected List<ValidationInfo> doValidateAll() {
        return exceptionResolutionTableDisplay.doValidateAll();
    }
}
