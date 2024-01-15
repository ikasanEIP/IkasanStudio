package org.ikasan.studio.ui.component.palette;

import org.ikasan.studio.ui.model.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class PalettePanel extends JPanel {
    private static final int INITIAL_DIVIDER_LOCATION = 2000;  // Workaround for nested component heights not being known at time of creation.

    private PaletteExportTransferHandler paletteExportTransferHandler;

    public PalettePanel() {
        super();
        this.setLayout(new BorderLayout());
        this.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        this.paletteExportTransferHandler = new PaletteExportTransferHandler();

        // Header
        JLabel paletteHeaderLabel =  new JLabel("Palette");
        paletteHeaderLabel.setBorder(new EmptyBorder(12,0,12,0));
        JPanel paletteHeaderPanel = new JPanel();
        paletteHeaderPanel.add(paletteHeaderLabel);
        paletteHeaderPanel.setBorder(BorderFactory.createLineBorder(Color.GRAY));

        // Body
        JPanel paletteHelpBodyPanel = new JPanel(new BorderLayout());
        JTextArea paletteHelpTextArea = new JTextArea();
        paletteHelpTextArea.setLineWrap(true);
        paletteHelpBodyPanel.add(paletteHelpTextArea, BorderLayout.CENTER);
        paletteHelpBodyPanel.setBorder(BorderFactory.createLineBorder(Color.GRAY));

        JList<PaletteItem> paletteList;
        paletteList = new JList(buildPalettItems().toArray());
        paletteList.setCellRenderer(new PaletteListCellRenderer());
        paletteList.setDragEnabled(true);
        paletteList.setTransferHandler(paletteExportTransferHandler);
        JScrollPane paletteScrollPane = new JScrollPane(paletteList);
        paletteScrollPane.setBorder(BorderFactory.createEmptyBorder(0,0,0,0));

        // Footer
        JPanel paletteHelpHeaderPanel = new JPanel();
        paletteHelpHeaderPanel.add(new JLabel("Description"));
        JPanel paletteHelpMainPanel = new JPanel(new BorderLayout());
        paletteHelpMainPanel.add(paletteHelpHeaderPanel, BorderLayout.NORTH);
        paletteHelpMainPanel.add(paletteHelpBodyPanel, BorderLayout.CENTER);


        final JSplitPane paletteSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, paletteScrollPane, paletteHelpMainPanel);
        paletteSplitPane.setDividerSize(3);

        // At this point, and even when the method ends, the preferred height only reflects the preferred height of all
        // the known components, so we deliberately send the divider off the bottom of the screen until the first time we
        // click on a palette component.
        paletteSplitPane.setDividerLocation(INITIAL_DIVIDER_LOCATION);

        JPanel paletteBodyPanel = new JPanel(new BorderLayout());
        paletteBodyPanel.add(paletteSplitPane, BorderLayout.CENTER);
        paletteBodyPanel.setBackground(Color.WHITE);

        add(paletteHeaderPanel, BorderLayout.NORTH);
        add(paletteBodyPanel, BorderLayout.CENTER);
        setBorder(new EmptyBorder(1,0,0,0));

        paletteList.addListSelectionListener(listSelectionEvent -> {
            if (paletteList.getSelectedValue() instanceof PaletteItemIkasanComponent) {
                PaletteItemIkasanComponent paletteItemIkasanComponent = (PaletteItemIkasanComponent)paletteList.getSelectedValue();
                // Only do this if its the first time, otherwise it might get annoying.
                if (paletteSplitPane.getDividerLocation() > (paletteBodyPanel.getHeight() - 10)) {
                    paletteSplitPane.setDividerLocation(0.8);
                }
                paletteHelpTextArea.setText(paletteItemIkasanComponent.getIkasanFlowElementViewHandler().getHelpText());
            }
        });

        paletteList.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent me) {
                JComponent comp = (JComponent) me.getSource();
                TransferHandler handler = comp.getTransferHandler();
                handler.exportAsDrag(comp, me, TransferHandler.COPY);
            }
        });
    }

    /**
     * Create a list of all known ikasan components
     * @return a list of all known ikasan components
     */
    private java.util.List<PaletteItem> buildPalettItems() {
        java.util.List<PaletteItem> paletteItems = new ArrayList<>();
        IkasanFlowUIComponentFactory ikasanFlowUIComponentFactory = IkasanFlowUIComponentFactory.getInstance();

        List<IkasanFlowUIComponent> ikasanFlowUIComponents = ikasanFlowUIComponentFactory.getIkasanFlowComponents();
        List<IkasanFlowUIComponent> displayOrder = ikasanFlowUIComponents
                .stream()
                .sorted(Comparator
                    .comparing((IkasanFlowUIComponent c1) -> c1.getIkasanComponentMeta().getElementCategory().getDisplayOrder())
                    .thenComparing(IkasanFlowUIComponent::getTitle))
                .collect(Collectors.toList());

        String category = "";
        for (IkasanFlowUIComponent ikasanFlowUIComponent : displayOrder) {
            if (!category.equals(ikasanFlowUIComponent.getIkasanComponentMeta().getElementCategory().toString()) ) {
                category = ikasanFlowUIComponent.getIkasanComponentMeta().getElementCategory().toString();
                paletteItems.add(new PaletteItemSeparator(category));
            }
            paletteItems.add(new PaletteItemIkasanComponent(ikasanFlowUIComponent));
        }
        return paletteItems;
    }

}
