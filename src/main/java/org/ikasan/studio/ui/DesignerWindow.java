package org.ikasan.studio.ui;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import org.ikasan.studio.Context;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.util.ArrayList;
import java.util.List;


public class DesignerWindow {
    private List <Icon> paleteListItems ;
    private JTextArea mainWindow;
    private JButton hideButton;
    private JPanel mainJPanel;
    private JSplitPane propertiesCanvasSplit;
    private JLabel paletteHeader;
    private JLabel propertiesHeader;
    private JPanel propertieMainPanel;
    private JPanel canvasMainPanel;
    private JButton refreshButton;
    private JTextArea canvasTextArea;
    private JPanel paletteMainPanel;
    private JList paletteList;
    private JPanel canvasHeaderPanel;
    private JPanel canvasHeaderTitlePanl;
    private JPanel canvasHeaderButtonPanel;
    private JCheckBox checkBox1;

    public DesignerWindow(Project project, ToolWindow toolWindow) {
        hideButton.addActionListener(e -> toolWindow.hide(null));
        Context.setCanvasTextArea(project.getName(), canvasTextArea);
        canvasTextArea.setLineWrap(true);
        paleteListItems = new ArrayList<>();
        paletteList.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent listSelectionEvent) {

            }
        });
    }

    public JPanel getContent() {
        return mainJPanel;
    }

    /**
     * The callback functon to create components that are too complex for the form builder
     */
    private void createUIComponents() {
        List<Icon> paleteListItems = new ArrayList<>();

        paletteList = new JList();
        ImageIcon icon = new ImageIcon(getClass().getResource("/studio/icons/palette/ftp-consumer.png"));
        paleteListItems.add(icon);
        JLabel label = new JLabel("ftp consumer");
        paletteList.add(label);
    }


}
