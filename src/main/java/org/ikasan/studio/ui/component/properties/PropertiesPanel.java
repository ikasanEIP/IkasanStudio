package org.ikasan.studio.ui.component.properties;

import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.openapi.wm.IdeFocusManager;
import org.ikasan.studio.Context;
import org.ikasan.studio.model.ikasan.*;
import org.ikasan.studio.ui.component.ScrollableGridbagPanel;
import org.ikasan.studio.ui.model.IkasanFlowUIComponent;
import org.ikasan.studio.ui.model.IkasanFlowUIComponentFactory;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.List;

public abstract class PropertiesPanel extends JPanel {
    private static final String PROPERTIES_TAG = "Properties";
    private transient IkasanBaseComponent selectedComponent;
    protected String projectKey;
    protected boolean popupMode;
    private JLabel propertiesHeaderLabel = new JLabel(PROPERTIES_TAG);

    protected JButton okButton;
    protected ScrollableGridbagPanel scrollableGridbagPanel;

    public PropertiesPanel(String projectKey, boolean popupMode) {
        super();
        this.projectKey = projectKey ;
        this.popupMode = popupMode;
        setLayout(new BorderLayout());
        setBackground(Color.WHITE);

        if (! popupMode) {
            JPanel propertiesHeaderPanel = new JPanel();
            propertiesHeaderLabel.setBorder(new EmptyBorder(12,0,12,0));
            propertiesHeaderPanel.add(propertiesHeaderLabel);
            propertiesHeaderPanel.setBorder(BorderFactory.createLineBorder(Color.GRAY));
            add(propertiesHeaderPanel, BorderLayout.NORTH);
        }

        JPanel propertiesBodyPanel = new JPanel(new BorderLayout());
        propertiesBodyPanel.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        propertiesBodyPanel.setBackground(Color.WHITE);


        if (! popupMode) {
            okButton = new JButton("Update code");
            okButton.addActionListener(e -> {
                    OkActionListener(e);
                    doOKAction();
                   //@todo popup a temp message 'no changes detected'
               }
            );
            JPanel footerPanel = new JPanel();
            footerPanel.add(okButton);
            footerPanel.setBorder(BorderFactory.createLineBorder(Color.GRAY));
            add(footerPanel, BorderLayout.SOUTH);
        }

        JPanel propertiesEditorPanel = populatePropertiesEditorPanel();
        scrollableGridbagPanel = new ScrollableGridbagPanel(propertiesEditorPanel);
        JScrollPane scrollPane = new JScrollPane(scrollableGridbagPanel, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setBorder(BorderFactory.createEmptyBorder(0,0,0,0));
        propertiesBodyPanel.add(scrollPane, BorderLayout.CENTER);
        scrollPane.setBackground(Color.WHITE);
        scrollPane.getViewport().setBackground(Color.WHITE);
        add(propertiesBodyPanel, BorderLayout.CENTER);
        setFocusOnFirstComponent();
    }

//    protected abstract void OkActionListener(ActionEvent ae);
    protected void OkActionListener(ActionEvent ae) {
        List<ValidationInfo> infoList = doValidateAll();
        if (!infoList.isEmpty()) {
            ValidationInfo firstInfo = infoList.get(0);
            if (firstInfo.component != null && firstInfo.component.isVisible()) {
                IdeFocusManager.getInstance(null).requestFocus(firstInfo.component, true);
            }
            StringBuilder validationErrors = new StringBuilder();
            for (ValidationInfo info : infoList) {
                validationErrors.append(info.message).append('\n');
            }
            JOptionPane.showMessageDialog(((JButton)ae.getSource()).getParent().getParent(),
                    validationErrors.toString(),
                    "Validation Error",
                    JOptionPane.ERROR_MESSAGE);
            if (infoList.stream().anyMatch(info1 -> !info1.okEnabled)) {
                return;
            }
        }
    }
    /**
     * This method is invoked when we have checked its OK to process the panel i.e. all items are valid
     */
    protected abstract void doOKAction();

    /**
     * Given the component within, generate an appropriate Panel title
     * @return A String containing the panel title.
     */
    public String getPropertiesPanelTitle() {
        String propertyType = "";
        if (selectedComponent instanceof IkasanModule) {
            propertyType = "Module " + PROPERTIES_TAG;
        } else if (selectedComponent instanceof IkasanFlow) {
            propertyType = "Flow " + PROPERTIES_TAG;
        } else if (selectedComponent instanceof IkasanFlowComponent) {
            IkasanFlowUIComponent type = IkasanFlowUIComponentFactory
                    .getInstance()
                    .getIkasanFlowUIComponentFromType((((IkasanFlowComponent) selectedComponent).getType()));
            if (type.getIkasanComponentType() != IkasanComponentType.UNKNOWN) {
                propertyType = type.getTitle() + " " + PROPERTIES_TAG;
            } else {
                propertyType = "Component " + PROPERTIES_TAG;
            }
        }
        return propertyType;

    }

    /**
     * External actors will update the component to be exposed / displayed.
     * @param selectedComponent that now needs to be displayed.
     */
    public void updateTargetComponent(IkasanBaseComponent selectedComponent) {
        this.selectedComponent = selectedComponent;
        if (! popupMode) {
            propertiesHeaderLabel.setText(getPropertiesPanelTitle());
        }
        populatePropertiesEditorPanel();
        redrawPanel();
    }

    protected void redrawPanel() {
        scrollableGridbagPanel.revalidate();
        scrollableGridbagPanel.repaint();
        setFocusOnFirstComponent();
    }

    /**
     * For the given component, get all the editable properties and add them the to properties edit panel.
     * @return the fully populated editor panel
     */
    protected abstract JPanel populatePropertiesEditorPanel();

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

    protected IkasanBaseComponent getSelectedComponent() {
        return selectedComponent;
    }
    protected abstract List<ValidationInfo> doValidateAll();
    public abstract void processEditedFlowComponents();
    public abstract boolean dataHasChanged();
}
