package org.ikasan.studio.ui.component.properties;

import com.intellij.ui.components.JBPanel;
import com.intellij.ui.JBSplitter;
import com.intellij.util.ui.JBUI;
import org.ikasan.studio.core.model.ikasan.instance.IkasanObject;
import org.ikasan.studio.ui.StudioBundle;
import org.ikasan.studio.ui.StudioUIUtils;

import javax.swing.*;
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
    final JBSplitter paletteSplitPane;

    ComponentPropertiesPanel componentPropertiesPanel;
    HtmlScrollingDisplayPanel htmlScrollingDisplayPanel = new HtmlScrollingDisplayPanel(StudioBundle.message("dialog.Description"), null);
    public ComponentPropertiesTabPanel(ComponentPropertiesPanel componentPropertiesPanel) {
        super();
        this.componentPropertiesPanel = componentPropertiesPanel;
        componentPropertiesPanel.setComponentDescription(htmlScrollingDisplayPanel);
        this.setLayout(new BorderLayout());
        this.setBorder(JBUI.Borders.empty());

        paletteSplitPane = new JBSplitter(true, 0.8f, 0.0f, 1.0f);
        paletteSplitPane.setFirstComponent(componentPropertiesPanel);
        paletteSplitPane.setSecondComponent(htmlScrollingDisplayPanel);
        paletteSplitPane.setBorder(JBUI.Borders.empty());
        paletteSplitPane.setDividerWidth(JBUI.scale(2));

        @SuppressWarnings("rawtypes")
        JBPanel linePanel = new JBPanel();
        linePanel.setBorder(BorderFactory.createMatteBorder(JBUI.scale(1),0,0,0, StudioUIUtils.getLineColor()));
        add(linePanel, BorderLayout.NORTH);
        add(paletteSplitPane, BorderLayout.CENTER);
    }
    /**
     * The natural width needed to show the current component's properties without horizontal
     * scrolling/clipping - see {@link ComponentPropertiesPanel#getPreferredWidth()}.
     */
    public int getPropertiesPreferredWidth() {
        return componentPropertiesPanel.getPreferredWidth();
    }

    /**
     * External actors will update the component to be exposed / displayed.
     * @param selectedComponent that now needs to be updated.
     */
    public void updateTargetComponent(IkasanObject selectedComponent) {
        paletteSplitPane.setProportion(0.8f);
        componentPropertiesPanel.updateTargetComponent(selectedComponent);
        this.repaint();
    }
}
