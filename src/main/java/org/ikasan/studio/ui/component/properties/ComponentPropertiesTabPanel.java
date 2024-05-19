package org.ikasan.studio.ui.component.properties;

import com.intellij.ui.JBColor;
import com.intellij.util.ui.JBUI;
import org.ikasan.studio.core.model.ikasan.instance.IkasanObject;

import javax.swing.*;
import java.awt.*;

public class ComponentPropertiesTabPanel extends JPanel {
    private static final int INITIAL_DIVIDER_LOCATION = 2000;  // Workaround for nested component heights not being known at time of creation.
    final JSplitPane paletteSplitPane;

    ComponentPropertiesPanel componentPropertiesPanel;
    ComponentDescription componentDescription = new ComponentDescription();
    public ComponentPropertiesTabPanel(ComponentPropertiesPanel componentPropertiesPanel) {
        super();
        this.componentPropertiesPanel = componentPropertiesPanel;
        componentPropertiesPanel.setComponentDescription(componentDescription);

        this.setLayout(new BorderLayout());
        this.setBorder(BorderFactory.createLineBorder(JBColor.GRAY));
        paletteSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, componentPropertiesPanel, componentDescription);
        paletteSplitPane.setDividerSize(3);
//        paletteSplitPane.setDividerLocation(INITIAL_DIVIDER_LOCATION);
        paletteSplitPane.setDividerLocation(0.8);

        add(paletteSplitPane, BorderLayout.CENTER);
        setBorder(JBUI.Borders.emptyTop(1));
    }
    /**
     * External actors will update the component to be exposed / displayed.
     * @param selectedComponent that now needs to be updated.
     */
    public void updateTargetComponent(IkasanObject selectedComponent) {
//        if (paletteSplitPane.getDividerLocation() > (getHeight() - 10)) {
            paletteSplitPane.setDividerLocation(0.8);
//        }
        componentPropertiesPanel.updateTargetComponent(selectedComponent);
        this.repaint();
    }

}

