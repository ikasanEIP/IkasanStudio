package org.ikasan.studio.ui.component.palette;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBList;
import com.intellij.util.ui.JBUI;
import org.ikasan.studio.core.StudioBuildException;
import org.ikasan.studio.core.model.ikasan.meta.ComponentMeta;
import org.ikasan.studio.core.model.ikasan.meta.IkasanComponentLibrary;
import org.ikasan.studio.ui.StudioUIUtils;
import org.ikasan.studio.ui.UiContext;
import org.ikasan.studio.ui.model.PaletteItem;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

public class PalettePanel extends JPanel {
    private static final Logger LOG = Logger.getInstance("#JPanel");
    private static final int INITIAL_DIVIDER_LOCATION = 2000;  // Workaround for nested component heights not being known at time of creation.
    private String projectKey;
    JScrollPane paletteScrollPane;
    PaletteExportTransferHandler paletteExportTransferHandler;
    JPanel paletteHelpBodyPanel;
    JPanel paletteHeaderPanel;
    JTextArea paletteHelpTextArea;
    JBList<PaletteItem> paletteList;
    final JSplitPane paletteSplitPane;
    JPanel paletteBodyPanel;

    public PalettePanel(String projectKey) {
        super();
        this.projectKey = projectKey;
        this.setLayout(new BorderLayout());
        this.setBorder(BorderFactory.createLineBorder(JBColor.GRAY));
        paletteExportTransferHandler = new PaletteExportTransferHandler(projectKey);

        // Header
        JLabel paletteHeaderLabel =  new JLabel("Palette");
        paletteHeaderLabel.setBorder(JBUI.Borders.empty(12, 0));
        paletteHeaderPanel = new JPanel();
        paletteHeaderPanel.add(paletteHeaderLabel);
        paletteHeaderPanel.setBorder(BorderFactory.createLineBorder(JBColor.GRAY));

        // Body
        paletteHelpBodyPanel = new JPanel(new BorderLayout());
        paletteHelpTextArea = new JTextArea();
        paletteHelpTextArea.setLineWrap(true);
        paletteHelpBodyPanel.add(paletteHelpTextArea, BorderLayout.CENTER);
        paletteHelpBodyPanel.setBorder(BorderFactory.createLineBorder(JBColor.GRAY));

//        JBList<PaletteItem> paletteList;
        paletteList = new JBList(buildPaletteItems(projectKey).toArray());
        paletteList.setCellRenderer(new PaletteListCellRenderer());
        paletteList.setDragEnabled(true);
        paletteList.setTransferHandler(paletteExportTransferHandler);
        paletteScrollPane = new JScrollPane(paletteList);
//        paletteScrollPane = new JScrollPane();
//        paletteScrollPane.add(paletteList);
        paletteScrollPane.setBorder(BorderFactory.createEmptyBorder(0,0,0,0));
//
        // Footer
        JPanel paletteHelpHeaderPanel = new JPanel();
        paletteHelpHeaderPanel.add(new JLabel("Description"));
        JPanel paletteHelpMainPanel = new JPanel(new BorderLayout());
        paletteHelpMainPanel.add(paletteHelpHeaderPanel, BorderLayout.NORTH);
        paletteHelpMainPanel.add(paletteHelpBodyPanel, BorderLayout.CENTER);


        paletteSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, paletteScrollPane, paletteHelpMainPanel);
        paletteSplitPane.setDividerSize(3);

        // At this point, and even when the method ends, the preferred height only reflects the preferred height of all
        // the known components, so we deliberately send the divider off the bottom of the screen until the first time we
        // click on a palette component.
        paletteSplitPane.setDividerLocation(INITIAL_DIVIDER_LOCATION);

        paletteBodyPanel = new JPanel(new BorderLayout());
        paletteBodyPanel.add(paletteSplitPane, BorderLayout.CENTER);
        paletteBodyPanel.setBackground(JBColor.WHITE);

        add(paletteHeaderPanel, BorderLayout.NORTH);
        add(paletteBodyPanel, BorderLayout.CENTER);
        setBorder(JBUI.Borders.emptyTop(1));

        paletteList.addListSelectionListener(listSelectionEvent -> {
            if (paletteList.getSelectedValue() != null) {
                PaletteItem PaletteItem = paletteList.getSelectedValue();
                // Only do this if it's the first time, otherwise it might get annoying.
                if (paletteSplitPane.getDividerLocation() > (paletteBodyPanel.getHeight() - 10)) {
                    paletteSplitPane.setDividerLocation(0.8);
                }
                paletteHelpTextArea.setText(PaletteItem.getIkasanPaletteElementViewHandler().getHelpText());
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

    public void resetPallette() {
        paletteList = new JBList(buildPaletteItems(projectKey).toArray());
        paletteList.setCellRenderer(new PaletteListCellRenderer());
        paletteList.setDragEnabled(true);
        paletteList.setTransferHandler(paletteExportTransferHandler);
        paletteScrollPane = new JScrollPane(paletteList);

        paletteList.addListSelectionListener(listSelectionEvent -> {
            if (paletteList.getSelectedValue() != null) {
                PaletteItem PaletteItem = paletteList.getSelectedValue();
                // Only do this if it's the first time, otherwise it might get annoying.
                if (paletteSplitPane.getDividerLocation() > (paletteBodyPanel.getHeight() - 10)) {
                    paletteSplitPane.setDividerLocation(0.8);
                }
                paletteHelpTextArea.setText(PaletteItem.getIkasanPaletteElementViewHandler().getHelpText());
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
        paletteScrollPane = new JScrollPane(paletteList);
        paletteSplitPane.setLeftComponent(paletteList);
    }


    /**
     * Create a list of all known ikasan components
     * @return a list of all known ikasan components
     */
    private java.util.List<PaletteItem> buildPaletteItems(String projectKey) {
        java.util.List<PaletteItem> paletteItems = new ArrayList<>();

        // New project created, no module yet
        if (UiContext.getIkasanModule(projectKey) == null) {
            LOG.info("New project, no model available yet");
        } else {
            Collection<ComponentMeta> componentMetaList = null;
            try {
                componentMetaList = IkasanComponentLibrary.getPaletteComponentList(UiContext.getIkasanModule(projectKey).getMetaVersion());
            } catch (StudioBuildException e) {
                StudioUIUtils.displayIdeaInfoMessage(projectKey, "A problem occurred trying to get the meta pack information (" + e.getMessage() + "), please review the logs.");
            }
            if (componentMetaList != null) {
                List<ComponentMeta> displayOrder = componentMetaList
                        .stream()
                        .filter(meta -> !meta.isModule())
                        .sorted(Comparator
                                .comparing(ComponentMeta::getDisplayOrder)
                                .thenComparing(ComponentMeta::getName))
                        .toList();

                String category = "";
                for (ComponentMeta componentMeta : displayOrder) {
                    if (!category.equals(componentMeta.getDisplayComponentType())) {
                        category = componentMeta.getDisplayComponentType();
                        paletteItems.add(new PaletteItem(category));
                    }
                    paletteItems.add(new PaletteItem(componentMeta));
                }
            }
        }
        return paletteItems;
    }
}
