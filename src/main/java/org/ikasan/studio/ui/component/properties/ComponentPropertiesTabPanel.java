package org.ikasan.studio.ui.component.properties;

import com.intellij.ui.JBColor;
import com.intellij.util.ui.JBUI;
import org.ikasan.studio.core.model.ikasan.instance.IkasanObject;
import org.ikasan.studio.ui.StudioUIUtils;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import javax.swing.plaf.basic.BasicSplitPaneDivider;
import javax.swing.plaf.basic.BasicSplitPaneUI;
import java.awt.*;

public class ComponentPropertiesTabPanel extends JPanel {
//    private static final int INITIAL_DIVIDER_LOCATION = 2000;  // Workaround for nested component heights not being known at time of creation.
    final JSplitPane paletteSplitPane;

    ComponentPropertiesPanel componentPropertiesPanel;
    HtmlScrollingDisplayPanel htmlScrollingDisplayPanel = new HtmlScrollingDisplayPanel("Description", null);
    public ComponentPropertiesTabPanel(ComponentPropertiesPanel componentPropertiesPanel) {
        super();
        this.componentPropertiesPanel = componentPropertiesPanel;
        componentPropertiesPanel.setComponentDescription(htmlScrollingDisplayPanel);
        this.setLayout(new BorderLayout());
        this.setBorder(new EmptyBorder(0, 0, 0, 0));

        paletteSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, componentPropertiesPanel, htmlScrollingDisplayPanel);
        paletteSplitPane.setBorder(new EmptyBorder(0, 0, 0, 0));
        paletteSplitPane.setUI(new BasicSplitPaneUI() {
            @Override
            public BasicSplitPaneDivider createDefaultDivider() {
                return new BasicSplitPaneDivider(this) {
                    @Override
                    public void paint(Graphics g) {
                        g.setColor(StudioUIUtils.getLineColor());
                        g.fillRect(0, 0, getSize().width, getSize().height);
                        // don't call super.paint() which would put in the bevel.
                    }
                };
            }
        });
        paletteSplitPane.setDividerSize(3);
        paletteSplitPane.setDividerLocation(0.8);

        JPanel linePanel = new JPanel();
        linePanel.setBorder(new MatteBorder(1,0,0,0, StudioUIUtils.getLineColor()));
        add(linePanel, BorderLayout.NORTH);
        add(paletteSplitPane, BorderLayout.CENTER);
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

