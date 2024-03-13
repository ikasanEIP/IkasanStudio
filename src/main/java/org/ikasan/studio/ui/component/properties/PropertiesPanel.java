package org.ikasan.studio.ui.component.properties;

import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.openapi.wm.IdeFocusManager;
import com.intellij.ui.JBColor;
import com.intellij.util.ui.JBUI;
import org.ikasan.studio.Context;
import org.ikasan.studio.model.ikasan.instance.Flow;
import org.ikasan.studio.model.ikasan.instance.FlowElement;
import org.ikasan.studio.model.ikasan.instance.IkasanObject;
import org.ikasan.studio.model.ikasan.instance.Module;
import org.ikasan.studio.ui.component.ScrollableGridbagPanel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.List;
import java.util.stream.Collectors;

public abstract class PropertiesPanel extends JPanel {
    private static final String PROPERTIES_TAG = "Properties";
    private static final String OK_BUTTON_TEXT = "Update Code";
    private transient IkasanObject selectedComponent;
//    private transient IkasanObject componentDefaults;
    protected final String projectKey;
    protected final boolean componentInitialisation;    // Indicates the component is being first initialised, therefore dealt with via popup panel
    private final JLabel propertiesHeaderLabel = new JLabel(PROPERTIES_TAG);
    private transient PropertiesDialogue propertiesDialogue;

    protected JButton okButton;
    protected final ScrollableGridbagPanel propertiesEditorScrollingContainer;
    protected JPanel propertiesEditorPanel = new JPanel();
    private boolean dataValid = true;

    protected PropertiesPanel(String projectKey, boolean componentInitialisation) {
        super();
        this.projectKey = projectKey ;
        this.componentInitialisation = componentInitialisation;
        setLayout(new BorderLayout());
        setBackground(JBColor.WHITE);

        if (! componentInitialisation) {
            JPanel propertiesHeaderPanel = new JPanel();
            propertiesHeaderLabel.setBorder(JBUI.Borders.empty(12, 0));
            propertiesHeaderPanel.add(propertiesHeaderLabel);
            propertiesHeaderPanel.setBorder(BorderFactory.createLineBorder(JBColor.GRAY));
            add(propertiesHeaderPanel, BorderLayout.NORTH);
        }

        JPanel propertiesBodyPanel = new JPanel(new BorderLayout());
        propertiesBodyPanel.setBorder(BorderFactory.createLineBorder(JBColor.GRAY));
        propertiesBodyPanel.setBackground(JBColor.WHITE);

        // Palette editor mode, add an OK button at the bottom.
        if (! componentInitialisation) {
            okButton = new JButton(OK_BUTTON_TEXT);
            okButton.addActionListener(e -> {
                    okActionListener(e);
                    if (dataValid) {
                        doOKAction();
                    }
                   //@todo popup a temp message 'no changes detected'
               }
            );
            JPanel footerPanel = new JPanel();
            footerPanel.add(okButton);
            footerPanel.setBorder(BorderFactory.createLineBorder(JBColor.GRAY));
            add(footerPanel, BorderLayout.SOUTH);
        }

        populatePropertiesEditorPanel();
        propertiesEditorScrollingContainer = new ScrollableGridbagPanel(propertiesEditorPanel);
        JScrollPane scrollPane = new JScrollPane(propertiesEditorScrollingContainer, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setBorder(BorderFactory.createEmptyBorder(0,0,0,0));
        propertiesBodyPanel.add(scrollPane, BorderLayout.CENTER);
        scrollPane.setBackground(JBColor.WHITE);
        scrollPane.getViewport().setBackground(JBColor.WHITE);
        add(propertiesBodyPanel, BorderLayout.CENTER);
        setFocusOnFirstComponent();
    }

    protected void okActionListener(ActionEvent ae) {
        List<ValidationInfo> infoList = doValidateAll();
        if (!infoList.isEmpty()) {
            dataValid = false;
            ValidationInfo firstInfo = infoList.get(0);
            if (firstInfo.component != null && firstInfo.component.isVisible()) {
                IdeFocusManager.getInstance(null).requestFocus(firstInfo.component, true);
            }
            JOptionPane.showMessageDialog(((JButton)ae.getSource()).getParent().getParent(),
                    infoList.stream().map(x -> x.message).collect(Collectors.joining("\n")),
                    "Validation Error",
                    JOptionPane.ERROR_MESSAGE);
        } else {
            dataValid = true;
        }
    }
    /**
     * This method is invoked when we have checked it's OK to process the panel i.e. all items are valid
     */
    protected abstract void doOKAction();

    /**
     * Given the component within, generate an appropriate Panel title
     * @return A String containing the panel title.
     */
    public String getPropertiesPanelTitle() {
        String propertyType = "";
        if (selectedComponent instanceof Module) {
            propertyType = "Module " + PROPERTIES_TAG;
        } else if (selectedComponent instanceof Flow) {
            propertyType = "Flow " + PROPERTIES_TAG;
        } else if (selectedComponent instanceof FlowElement) {
            propertyType = selectedComponent.getComponentMeta().getName() + " " + PROPERTIES_TAG;
        }
        return propertyType;
    }

    /**
     * External actors will update the component to be exposed / displayed.
     * @param selectedComponent that now needs to be updated.
     */
    public void updateTargetComponent(IkasanObject selectedComponent) {
        this.selectedComponent = selectedComponent;
        if (! componentInitialisation) {
            propertiesHeaderLabel.setText(getPropertiesPanelTitle());
        }
        populatePropertiesEditorPanel();
        redrawPanel();
    }

    public void setTargetComponent(IkasanObject selectedComponent) {
        this.selectedComponent = selectedComponent;
        if (! componentInitialisation) {
            propertiesHeaderLabel.setText(getPropertiesPanelTitle());
        }
        populatePropertiesEditorPanel();
        redrawPanel();
    }

    protected void redrawPanel() {
        propertiesEditorScrollingContainer.revalidate();
        if (propertiesDialogue != null) {
            propertiesDialogue.pack();
            propertiesDialogue.repaint();
        }
        setFocusOnFirstComponent();
    }

    /**
     * Called by the Properties Dialogue, then passed upto the Intellij DialogueWrapper which creates the button.
     * @return the text to be used in the OK Button.
     */
    protected String getOKButtonText() {
        return OK_BUTTON_TEXT;
    }

    /**
     * For the given component, get all the editable properties and add them the to properties edit panel.
     */
    protected abstract void populatePropertiesEditorPanel();

    public void setFocusOnFirstComponent() {
        JComponent firstComponent = getFirstFocusField();
        if (firstComponent != null) {
            Context.getProject(projectKey);
            IdeFocusManager.getInstance(Context.getProject(projectKey)).requestFocus(firstComponent, true);
        }
    }

    /**
     * Get the field that should be given the focus in popup or inscreen form
     * @return the component that should be given focus or null
     */
    public abstract JComponent getFirstFocusField();

    protected IkasanObject getSelectedComponent() {
        return selectedComponent;
    }

    protected abstract List<ValidationInfo> doValidateAll();
    public abstract void processEditedFlowComponents();
    public abstract boolean dataHasChanged();

    public PropertiesDialogue getPropertiesDialogue() {
        return propertiesDialogue;
    }

    public void setPropertiesDialogue(PropertiesDialogue propertiesDialogue) {
        this.propertiesDialogue = propertiesDialogue;
    }
}
