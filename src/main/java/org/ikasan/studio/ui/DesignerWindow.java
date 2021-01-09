package org.ikasan.studio.ui;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.ikasan.studio.Context;

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
//        ImageIcon icon = new ImageIcon(getClass().getResource("/studio/icons/palette/ftp-consumer.png"));
//        JLabel label = new JLabel("ftp consumer", icon, JLabel.CENTER);
//        paletteList.add(label);
//        paletteList.setVisible(true);
//        refreshButton.addActionListener(e -> getModel());
        paletteList.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent listSelectionEvent) {

            }
        });
    }

//    public void getModel() {
//        PsiUtils psiUtils = new PsiUtils();
//        canvasTextArea.setText("Starting to initliase Ikasan design from XML ....");
////        canvasTextArea.setText(psiUtils.getModelData());
//    }

//    private void populatePalette(JList paletteList) {
//        ImageIcon icon = new ImageIcon(getClass().getResource("/studio/icons/palette/ftp-consumer.png"));
//        JLabel label = new JLabel("ftp consumer", icon, JLabel.CENTER);
//        paletteList.add(label);
//        paletteList.setVisible(true);


//    }

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
//        JLabel label = new JLabel("ftp consumer", icon, JLabel.CENTER);
        JLabel label = new JLabel("ftp consumer");
        paletteList.add(label);

//        paletteList.setCellRenderer(new PaletteCellRenderer(paleteListItems));
//        paletteList.setCellRenderer(new paleteListItems() {
//                @Override
//                public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
//                    // Get the renderer component from parent class
//                    JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
//                    // Get icon to use for the list item value
//                    Icon icon = icons.get(index);
//                    // Set icon to display for value
//                    label.setIcon(icon);
//                    return label;
//                }
//            }
//        );
//        JLabel label = new JLabel("ftp consumer");
//        paletteList.add(label);
//        paletteList.setVisible(true);
//        paletteList.setVisibleRowCount(1);
//        pack();
//        repaint();

//        DefaultListModel listModel = new DefaultListModel();
////        JLabel label = new JLabel("ftp consumer");
////        label.setText("bob");
//        listModel.add(0, new JLabel("ftp consumer"));
//        listModel.add(1, new JLabel("sftp consumer"));
//        paletteList.setModel(listModel);
////        paletteList.setVisibleRowCount(1);
    }


}
