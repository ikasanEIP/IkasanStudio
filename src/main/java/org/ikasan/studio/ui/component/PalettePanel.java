package org.ikasan.studio.ui.component;

import org.apache.log4j.Logger;
import org.ikasan.studio.ui.component.palette.IkasanFlowUIComponentSelection;
import org.ikasan.studio.ui.component.palette.PaletteListCellRenderer;
import org.ikasan.studio.ui.model.IkasanFlowUIComponent;
import org.ikasan.studio.ui.model.IkasanFlowUIComponentFactory;
import org.ikasan.studio.ui.model.PaletteItem;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

public class PalettePanel extends JPanel {
    private static final int INITIAL_DIVIDER_LOCATION = 2000;  // Workaround for nested component heights not being known at time of creation.
    private static final Logger log = Logger.getLogger(PalettePanel.class);
    private int clickStartMouseX = 0 ;
    private int clickStartMouseY = 0 ;
    private boolean componentMoved = false;
    private String projectKey;

    private IkasanFlowUIComponentSelection ikasanFlowUIComponentSelection ;

    public PalettePanel(String projectKey) {
        super();
        this.projectKey = projectKey;
        this.setLayout(new BorderLayout());
        this.ikasanFlowUIComponentSelection= new IkasanFlowUIComponentSelection();

        JLabel paletteHeaderLabel =  new JLabel("Palette");
        paletteHeaderLabel.setBorder(new EmptyBorder(9,0,9,0));
        JPanel paletteHeaderPanel = new JPanel();
        paletteHeaderPanel.add(paletteHeaderLabel);
        paletteHeaderPanel.setBorder(BorderFactory.createLineBorder(Color.GRAY));

        JPanel paletteHelpHeaderPanel = new JPanel();
        paletteHelpHeaderPanel.add(new JLabel("Description"));
        paletteHelpHeaderPanel.setBorder(BorderFactory.createLineBorder(Color.GRAY));

        JPanel paletteHelpBodyPanel = new JPanel(new BorderLayout());
        JTextArea paletteHelpTextArea = new JTextArea();
        paletteHelpTextArea.setLineWrap(true);
        paletteHelpBodyPanel.add(paletteHelpTextArea, BorderLayout.CENTER);

        JPanel paletteHelpMainPanel = new JPanel(new BorderLayout());
        paletteHelpMainPanel.add(paletteHelpHeaderPanel, BorderLayout.NORTH);
        paletteHelpMainPanel.add(paletteHelpBodyPanel, BorderLayout.CENTER);

        JList<PaletteItem> paletteList;
        paletteList = new JList(buildPalettItems().toArray());
        paletteList.setCellRenderer(new PaletteListCellRenderer());
        paletteList.setDragEnabled(true);

        paletteList.setTransferHandler(ikasanFlowUIComponentSelection);

        JScrollPane paletteScrollPane = new JScrollPane(paletteList);
        final JSplitPane paletteSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, paletteScrollPane, paletteHelpMainPanel);
        paletteSplitPane.setDividerSize(3);

        // At this point, and even when thie method ends, the preferred height only reflects the preferred height of all
        // the known components, so we deliberatly send the divider off the bottom of the screen until the first time we
        // click on a palett component.
        paletteSplitPane.setDividerLocation(INITIAL_DIVIDER_LOCATION);

        JPanel paletteBodyPanel = new JPanel(new BorderLayout());
        paletteBodyPanel.add(paletteSplitPane, BorderLayout.CENTER);
        paletteBodyPanel.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        paletteBodyPanel.setBackground(Color.WHITE);

        add(paletteHeaderPanel, BorderLayout.NORTH);
        add(paletteBodyPanel, BorderLayout.CENTER);
        setBorder(new EmptyBorder(1,0,0,0));

        // Please place all listeners at the end of the method
        paletteList.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent listSelectionEvent) {
                // Only do this if its the first time, otherwise it might get annoying.
                if (paletteSplitPane.getDividerLocation() > (paletteBodyPanel.getHeight() - 10)) {
                    paletteSplitPane.setDividerLocation(0.8);
                }
                paletteHelpTextArea.setText(paletteList.getSelectedValue().getIkasanFlowElementViewHandler().getHelpText());
            }
        });

        paletteList.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent me) {
                JComponent comp = (JComponent) me.getSource();
                TransferHandler handler = comp.getTransferHandler();
                handler.exportAsDrag(comp, me, TransferHandler.COPY);
            }
        });
    }


    /**
     * Create a list of all known Ikasan components
     * @return a list of all known Ikasan components
     */
    private java.util.List<PaletteItem> buildPalettItems() {
        java.util.List<PaletteItem> paletteItems = new ArrayList<>();
        IkasanFlowUIComponentFactory ikasanFlowUIComponentFactory = IkasanFlowUIComponentFactory.getInstance();
        List<IkasanFlowUIComponent> ikasanFlowUIComponents = ikasanFlowUIComponentFactory.getIkasanFlowComponents();
        for (IkasanFlowUIComponent ikasanFlowUIComponent : ikasanFlowUIComponents) {
            paletteItems.add(new PaletteItem(ikasanFlowUIComponent));
        }
        return paletteItems;
    }

}
