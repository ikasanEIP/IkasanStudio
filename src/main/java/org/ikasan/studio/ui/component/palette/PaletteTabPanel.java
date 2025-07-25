package org.ikasan.studio.ui.component.palette;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBList;
import org.ikasan.studio.core.StudioBuildException;
import org.ikasan.studio.core.model.ikasan.instance.Module;
import org.ikasan.studio.core.model.ikasan.meta.ComponentMeta;
import org.ikasan.studio.core.model.ikasan.meta.IkasanComponentLibrary;
import org.ikasan.studio.ui.StudioUIUtils;
import org.ikasan.studio.ui.UiContext;
import org.ikasan.studio.ui.component.properties.HtmlScrollingDisplayPanel;
import org.ikasan.studio.ui.model.PaletteItem;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import javax.swing.plaf.basic.BasicSplitPaneDivider;
import javax.swing.plaf.basic.BasicSplitPaneUI;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

import static org.ikasan.studio.core.model.ikasan.instance.Module.DUMB_MODULE_VERSION;

public class PaletteTabPanel extends JPanel {
    private static final Logger LOG = Logger.getInstance("#JPanel");
    private static final int INITIAL_DIVIDER_LOCATION = 2000;  // Set to push description off the screen
    private final String projectKey;
    JScrollPane paletteScrollPane;
    PaletteExportTransferHandler paletteExportTransferHandler;
    JBList<PaletteItem> paletteList;
    final JSplitPane paletteSplitPane;
    JPanel paletteBodyPanel;
    HtmlScrollingDisplayPanel htmlScrollingDisplayPanel = new HtmlScrollingDisplayPanel("Description", null);


    public PaletteTabPanel(String projectKey) {
        super();
        this.projectKey = projectKey;
        this.setLayout(new BorderLayout());
        htmlScrollingDisplayPanel.setBorder(null);
        paletteExportTransferHandler = new PaletteExportTransferHandler(projectKey);

        paletteSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        paletteSplitPane.setBorder(new EmptyBorder(0, 0, 0, 0));
        paletteSplitPane.setDividerSize(2);
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

        setPaletteList();

        paletteSplitPane.setRightComponent(htmlScrollingDisplayPanel);
        paletteBodyPanel = new JPanel(new BorderLayout());
        paletteBodyPanel.setBorder(null);
        paletteBodyPanel.add(paletteSplitPane, BorderLayout.CENTER);
        paletteBodyPanel.setBackground(JBColor.WHITE);

        JPanel linePanel = new JPanel();
        linePanel.setBorder(new MatteBorder(1,0,0,0, StudioUIUtils.getLineColor()));
        add(linePanel, BorderLayout.NORTH);
        add(paletteBodyPanel, BorderLayout.CENTER);
    }

    public void setPaletteList() {
        paletteList = new JBList(buildPaletteItems(projectKey).toArray());
        paletteList.setCellRenderer(new PaletteListCellRenderer());
        paletteList.setDragEnabled(true);
        paletteList.setTransferHandler(paletteExportTransferHandler);

        paletteScrollPane = new JScrollPane();
        paletteScrollPane.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
        paletteScrollPane.getViewport().add(paletteList);

        paletteList.addListSelectionListener(listSelectionEvent -> {
            if (paletteList.getSelectedValue() != null) {
                PaletteItem paletteItem = paletteList.getSelectedValue();
                paletteSplitPane.setDividerLocation(0.8);
                htmlScrollingDisplayPanel.setText(paletteItem.getIkasanPaletteElementViewHandler().getHelpText());
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
        paletteSplitPane.setLeftComponent(paletteScrollPane);
        paletteSplitPane.setDividerLocation(INITIAL_DIVIDER_LOCATION);
    }

    public void resetPallette() {
        setPaletteList();
        UiContext.setRightTabbedPaneFocus(projectKey, UiContext.PALETTE_TAB_INDEX);
    }


    /**
     * Create a list of all known ikasan components
     * @return a list of all known ikasan components
     */
    private java.util.List<PaletteItem> buildPaletteItems(String projectKey) {
        java.util.List<PaletteItem> paletteItems = new ArrayList<>();
        // New project created, no module yet
        Module module = UiContext.getIkasanModule(projectKey);
        if (module == null || module.getMetaVersion() == null) {
            LOG.info("STUDIO: New project, no model version available yet");
        } else {
            Collection<ComponentMeta> componentMetaList = null;
            if (!DUMB_MODULE_VERSION.equals(UiContext.getIkasanModule(projectKey).getMetaVersion())) {
                try {
                    componentMetaList = IkasanComponentLibrary.getPaletteComponentList(UiContext.getIkasanModule(projectKey).getMetaVersion());
                } catch (StudioBuildException e) {
                    StudioUIUtils.displayIdeaWarnMessage(projectKey, "A problem occurred trying to get the meta pack information (" + e.getMessage() + "), please review the logs.");
                }
                if (componentMetaList != null) {
                    List<ComponentMeta> componentMetaInDisplayOrder = componentMetaList
                            .stream()
                            .filter(meta -> !meta.isModule())
                            .sorted(Comparator
                                    .comparing(ComponentMeta::getDisplayOrder)
                                    .thenComparing(ComponentMeta::getName))
                            .toList();

                    String category = "";

                    for (ComponentMeta componentMeta : componentMetaInDisplayOrder) {
                        if (!category.equals(componentMeta.getComponentTypeMeta().getComponentShortType())) {
                            category = componentMeta.getComponentTypeMeta().getComponentShortType();
                            paletteItems.add(new PaletteItem(category, componentMeta));
                        }
                        paletteItems.add(new PaletteItem(componentMeta));
                    }
                }
            }
        }
        return paletteItems;
    }

    @Override
    public void paint(Graphics g) {
        super.paint(g);
    }
}
