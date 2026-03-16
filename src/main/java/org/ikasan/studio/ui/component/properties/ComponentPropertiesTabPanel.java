package org.ikasan.studio.ui.component.properties;

import com.intellij.ui.components.JBPanel;
import com.intellij.util.ui.JBUI;
import org.ikasan.studio.core.model.ikasan.instance.IkasanObject;
import org.ikasan.studio.ui.StudioUIUtils;

import javax.swing.*;
import javax.swing.plaf.basic.BasicSplitPaneDivider;
import javax.swing.plaf.basic.BasicSplitPaneUI;
import java.awt.*;

/**
 * This Panel resides in the left Nav. It consists of a SplitPane
 * -----------------------------
 * | The propertied CRUD Panel
 * -------------------------------
 * | A Description panel at the bottom, containing text to describe the currently edit component.
 * -----------------------------
 * The Panel is reusable in line with Intellij guidelines, updateTargetComponent will update the panel so it can be used to
 * maintain the component currently selected in the canvas.
 */
@SuppressWarnings("rawtypes")
public class ComponentPropertiesTabPanel extends JBPanel {
    final JSplitPane paletteSplitPane;

    ComponentPropertiesPanel componentPropertiesPanel;
    HtmlScrollingDisplayPanel htmlScrollingDisplayPanel = new HtmlScrollingDisplayPanel("Description", null);
    public ComponentPropertiesTabPanel(ComponentPropertiesPanel componentPropertiesPanel) {
        super();
        this.componentPropertiesPanel = componentPropertiesPanel;
        componentPropertiesPanel.setComponentDescription(htmlScrollingDisplayPanel);
        this.setLayout(new BorderLayout());
        this.setBorder(JBUI.Borders.empty());

        paletteSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, componentPropertiesPanel, htmlScrollingDisplayPanel);
        paletteSplitPane.setBorder(JBUI.Borders.empty());
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
        paletteSplitPane.setDividerSize(2);
        paletteSplitPane.setDividerLocation(0.8);

        @SuppressWarnings("rawtypes")
        JBPanel linePanel = new JBPanel();
        linePanel.setBorder(BorderFactory.createMatteBorder(JBUI.scale(1),0,0,0, StudioUIUtils.getLineColor()));
        add(linePanel, BorderLayout.NORTH);
        add(paletteSplitPane, BorderLayout.CENTER);
    }
    /**
     * External actors will update the component to be exposed / displayed.
     * @param selectedComponent that now needs to be updated.
     */
    public void updateTargetComponent(IkasanObject selectedComponent) {
        paletteSplitPane.setDividerLocation(0.8);
        componentPropertiesPanel.updateTargetComponent(selectedComponent);
        this.repaint();
    }
}
