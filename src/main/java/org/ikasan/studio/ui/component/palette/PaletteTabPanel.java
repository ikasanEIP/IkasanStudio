package org.ikasan.studio.ui.component.palette;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBList;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.ui.JBUI;
import org.ikasan.studio.core.StudioBuildException;
import org.ikasan.studio.core.model.ikasan.instance.Module;
import org.ikasan.studio.core.model.ikasan.meta.ComponentMeta;
import org.ikasan.studio.core.model.ikasan.meta.IkasanComponentLibrary;
import org.ikasan.studio.ui.StudioUIUtils;
import org.ikasan.studio.ui.UiContext;
import org.ikasan.studio.ui.component.properties.HtmlScrollingDisplayPanel;
import org.ikasan.studio.ui.model.PaletteItem;
import org.ikasan.studio.ui.theme.ThemeAwareColors;

import javax.swing.*;
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

/**
 * The Palette Tab Panel contains a SplitPane of :
 * ------------------------
 * | icon    component x
 * | icon    component y
 * ------------------------
 * |        Description
 * | description of currently selected component
 * ------------------------
 */
@SuppressWarnings("rawtypes")
public class PaletteTabPanel extends JBPanel {
    private static final Logger LOG = Logger.getInstance("#JPanel");
    private static final int INITIAL_DIVIDER_LOCATION = 2000;  // Set to push description off the screen
    private final Project project;
    JBScrollPane paletteScrollPane;
    PaletteExportTransferHandler paletteExportTransferHandler;
    JBList<PaletteItem> paletteList;
    final JSplitPane paletteSplitPane;
    @SuppressWarnings("rawtypes")
    JBPanel paletteBodyPanel;
    HtmlScrollingDisplayPanel htmlScrollingDisplayPanel = new HtmlScrollingDisplayPanel("Description", null);


    /**
     * Convenience wrapper for cleaner code in this class.
     * Delegates to centralized ThemeAwareColors utility.
     */
    private static Color getThemeAwareBackgroundColor() {
        return ThemeAwareColors.getBackgroundColor();
    }

    public PaletteTabPanel(Project project) {
        super();
        this.project = project;
        this.setLayout(new BorderLayout());
        htmlScrollingDisplayPanel.setBorder(null);
        paletteExportTransferHandler = new PaletteExportTransferHandler(project);

        paletteSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        paletteSplitPane.setBorder(JBUI.Borders.empty());
        paletteSplitPane.setDividerSize(3);
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
        paletteBodyPanel = new JBPanel();
        paletteBodyPanel.setBorder(null);
        paletteBodyPanel.setLayout(new BorderLayout());
        paletteBodyPanel.add(paletteSplitPane, BorderLayout.CENTER);
        paletteBodyPanel.setBackground(getThemeAwareBackgroundColor());
        add(paletteBodyPanel, BorderLayout.CENTER);
    }

    public void setPaletteList() {
        paletteList = new JBList<>(buildPaletteItems(project));
        paletteList.setCellRenderer(new PaletteListCellRenderer());
        paletteList.setDragEnabled(true);
        paletteList.setTransferHandler(paletteExportTransferHandler);

        paletteScrollPane = new JBScrollPane();
        paletteScrollPane.setBorder(JBUI.Borders.empty(5));
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

    public int getPaletteScrollPanePreferredWidth() {
        if (paletteList == null || paletteScrollPane == null) return 0;
        // JScrollPane.getPreferredSize() uses JList.getPreferredScrollableViewportSize(),
        // which returns a fixed 256px width regardless of cell content.
        // paletteList.getPreferredSize() uses BasicListUI which iterates every cell via
        // the cell renderer and returns the actual max content width.
        Insets insets = paletteScrollPane.getInsets();
        int scrollBarWidth = paletteScrollPane.getVerticalScrollBar().getPreferredSize().width;
        return paletteList.getPreferredSize().width + insets.left + insets.right + scrollBarWidth;
    }

    public void resetPallette() {
        setPaletteList();
        UiContext uiContext = project.getService(UiContext.class);
        uiContext.setRightTabbedPaneFocus(UiContext.PALETTE_TAB_INDEX);
    }


    /**
     * Create a list of all known ikasan components
     * @return a list of all known ikasan components
     */
    private java.util.List<PaletteItem> buildPaletteItems(Project project) {
        java.util.List<PaletteItem> paletteItems = new ArrayList<>();
        // New project created, no module yet
        UiContext uiContext = project.getService(UiContext.class);
        Module module = uiContext.getIkasanModule();
        if (module == null || module.getMetaVersion() == null) {
            LOG.info("STUDIO: New project, no model version available yet");
        } else {
            Collection<ComponentMeta> componentMetaList = null;
            if (!DUMB_MODULE_VERSION.equals(uiContext.getIkasanModule().getMetaVersion())) {
                try {
                    componentMetaList = IkasanComponentLibrary.getPaletteComponentList(uiContext.getIkasanModule().getMetaVersion());
                } catch (StudioBuildException e) {
                    StudioUIUtils.displayIdeaWarnMessage(project, "A problem occurred trying to get the meta pack information (" + e.getMessage() + "), please review the logs.");
                }
                if (componentMetaList != null) {
                    List<ComponentMeta> componentMetaInDisplayOrder = componentMetaList
                            .stream()
                            .filter(meta -> !meta.isModule())
                            .sorted(Comparator
                                    .comparing(ComponentMeta::getPaletteDisplayOrder)
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
