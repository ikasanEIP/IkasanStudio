package org.ikasan.studio.ui.component.properties;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.openapi.wm.IdeFocusManager;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.JBUI;
import org.ikasan.studio.core.model.ikasan.instance.Flow;
import org.ikasan.studio.core.model.ikasan.instance.IkasanObject;
import org.ikasan.studio.core.model.ikasan.instance.Module;
import org.ikasan.studio.ui.StudioBundle;
import org.ikasan.studio.ui.component.ScrollableGridbagPanel;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Implements Disposable (default no-op dispose()) purely so every subclass has a panel-scoped lifecycle to hang
 * background work off - see ComponentPropertiesPanel#isConfirmedSerializable's own comment for why a narrower
 * Disposable than the whole Project matters here (JetBrains' own "Choosing a Disposable Parent" guidance warns
 * against using Project itself for exactly this kind of panel-scoped async task). PropertiesPopupDialogue
 * registers whichever panel it wraps against its own getDisposable() (see there); DesignerUI's persistent
 * canvas-sidebar instance is registered the same way CanvasPanel already is (Disposer.register(this, ...)).
 */
@SuppressWarnings("rawtypes")
public abstract class PropertiesPanel extends JBPanel implements Disposable {
    private static final String UPDATE_CODE_BUTTON_TEXT = StudioBundle.message("button.UpdateCode");
    protected transient IkasanObject selectedComponent;
    protected final Project project;
    protected final boolean componentInitialisation;    // Indicates the component is being first initialised, therefore dealt with via popup panel
    private final JLabel propertiesHeaderLabel = new JLabel(StudioBundle.message("label.Properties"));
    private transient PropertiesPopupDialogue propertiesPopupDialogue;

    protected final ScrollableGridbagPanel propertiesEditorScrollingContainer;
    protected JBPanel propertiesEditorPanel = new JBPanel();
    private final JBScrollPane scrollPane;
    // Null unless this panel was constructed with supportsPropertySearch=true - large components (SFTP Consumer,
    // SpringJmsConsumer) can have 20-30+ properties, and hunting through collapsed optional groups for a
    // half-remembered name is a real pain point. Live-filters rows by name/help text as you type - see
    // onPropertySearchChanged().
    protected final JBTextField propertySearchField;

    protected JButton updateCodeButton;
    // Null when componentInitialisation is true (the first-configuration popup has no footer at all) - exposed
    // so subclasses can add their own footer buttons (e.g. ComponentPropertiesPanel's "Regenerate Class") after
    // super() returns, without this base class needing to know about every subclass-specific action.
    protected JBPanel footerPanel;
    private boolean dataValid = true;

    protected PropertiesPanel(Project project, boolean componentInitialisation) {
        this(project, componentInitialisation, false);
    }

    /**
     * @param supportsPropertySearch true to show a live search/filter field above the property rows - see
     *                                {@link #onPropertySearchChanged(String)}. Only meaningful for subclasses
     *                                whose rows are individually filterable (ComponentPropertiesPanel); panels
     *                                like ExceptionResolutionPanel/ExceptionResolverPanel don't opt in.
     */
    protected PropertiesPanel(Project project, boolean componentInitialisation, boolean supportsPropertySearch) {
        super();
        this.project = project;
        this.componentInitialisation = componentInitialisation;
        this.setBorder(null);
        propertiesEditorPanel.setBorder(null);
        setLayout(new BorderLayout());
        setBackground(JBColor.WHITE);

        if (! componentInitialisation) {
            JBPanel propertiesHeaderPanel = new JBPanel();
            propertiesHeaderPanel.setBorder(null);
            propertiesHeaderLabel.setBorder(JBUI.Borders.empty(12, 0));
            propertiesHeaderPanel.add(propertiesHeaderLabel);
            add(propertiesHeaderPanel, BorderLayout.NORTH);
        }

        JBPanel propertiesBodyPanel = new JBPanel(new BorderLayout());
        propertiesBodyPanel.setBorder(null);
        propertiesBodyPanel.setBackground(JBColor.WHITE);

        if (supportsPropertySearch) {
            propertySearchField = new JBTextField();
            propertySearchField.getEmptyText().setText(StudioBundle.message("label.SearchProperties"));
            propertySearchField.getDocument().addDocumentListener(new DocumentListener() {
                @Override public void insertUpdate(DocumentEvent e) { onPropertySearchChanged(propertySearchField.getText()); }
                @Override public void removeUpdate(DocumentEvent e) { onPropertySearchChanged(propertySearchField.getText()); }
                @Override public void changedUpdate(DocumentEvent e) { onPropertySearchChanged(propertySearchField.getText()); }
            });
            JBPanel searchPanel = new JBPanel(new BorderLayout());
            searchPanel.setBorder(JBUI.Borders.empty(4, 8));
            searchPanel.setBackground(JBColor.WHITE);
            searchPanel.add(propertySearchField, BorderLayout.CENTER);
            propertiesBodyPanel.add(searchPanel, BorderLayout.NORTH);
        } else {
            propertySearchField = null;
        }

        // Palette editor mode, add an OK button at the bottom.
        if (! componentInitialisation) {
            updateCodeButton = new JButton(UPDATE_CODE_BUTTON_TEXT);
            updateCodeButton.addActionListener(e -> {
                    okActionListener(e);
                    if (dataValid) {
                        doOKAction();
                    }
               }
            );
            footerPanel = new JBPanel();
            footerPanel.setBorder(null);
            footerPanel.add(updateCodeButton);
            add(footerPanel, BorderLayout.SOUTH);
        }

        populatePropertiesEditorPanel();
        propertiesEditorScrollingContainer = new ScrollableGridbagPanel(propertiesEditorPanel);
        propertiesEditorScrollingContainer.setBorder(null);
        scrollPane = new JBScrollPane(propertiesEditorScrollingContainer, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);

        scrollPane.setBorder(null);
        propertiesBodyPanel.add(scrollPane, BorderLayout.CENTER);
        scrollPane.setBackground(JBColor.WHITE);
        scrollPane.getViewport().setBackground(JBColor.WHITE);
        add(propertiesBodyPanel, BorderLayout.CENTER);
        setFocusOnFirstComponent();
    }


    protected void okActionListener(ActionEvent ae) {
        List<ValidationInfo> infoList = doValidateAll();
        if (!infoList.isEmpty()) {
            dataValid = false;
            ValidationInfo firstInfo = infoList.get(0);
            if (firstInfo.component != null && firstInfo.component.isVisible()) {
                IdeFocusManager.getInstance(null).requestFocus(firstInfo.component, true);
            }
            JOptionPane.showMessageDialog(((JButton)ae.getSource()).getParent().getParent(),
                    infoList.stream().map(x -> x.message).collect(Collectors.joining("\n")),
                    StudioBundle.message("dialog.ValidationError"),
                    JOptionPane.ERROR_MESSAGE);
        } else {
            dataValid = true;
        }
    }
    /**
     * This method is invoked when we have checked it's OK to process the panel i.e. all items are valid
     */
    protected abstract void doOKAction();

    /**
     * Given the component within, generate an appropriate Panel title
     * @return A String containing the panel title.
     */
    public String getPropertiesPanelTitle() {
        String propertyType;
        if (selectedComponent instanceof Module) {
            propertyType = StudioBundle.message("message.ComponentTypeProperties", StudioBundle.message("label.Module"));
        } else if (selectedComponent instanceof Flow) {
            propertyType = StudioBundle.message("message.ComponentTypeProperties", StudioBundle.message("label.Flow"));
        } else {
            propertyType = StudioBundle.message("message.ComponentTypeProperties", selectedComponent.getComponentMeta().getName());
        }
        return propertyType;
    }

    /**
     * External actors will update the component to be exposed / displayed.
     * @param selectedComponent that now needs to be updated.
     */
    public void updateTargetComponent(IkasanObject selectedComponent) {
        this.selectedComponent = selectedComponent;
        if (! componentInitialisation) {
            propertiesHeaderLabel.setText(getPropertiesPanelTitle());
        }
        populatePropertiesEditorPanel();
        redrawPanel();
    }

    /**
     * The natural width needed to show this panel's property rows without horizontal scrolling/clipping,
     * mirroring {@code PaletteTabPanel#getPaletteScrollPanePreferredWidth()} - used to size the containing
     * split pane so the Properties tab isn't cramped when it's first shown.
     */
    public int getPreferredWidth() {
        Insets insets = scrollPane.getInsets();
        int scrollBarWidth = scrollPane.getVerticalScrollBar().getPreferredSize().width;
        return propertiesEditorPanel.getPreferredSize().width + insets.left + insets.right + scrollBarWidth;
    }

    protected void redrawPanel() {
        redrawPanel(true);
    }

    /**
     * @param refocusFirstField false to skip {@link #setFocusOnFirstComponent()} - needed when the redraw is
     *                           itself a side effect of typing elsewhere (e.g. the property search filter
     *                           hiding/showing rows on every keystroke), where stealing focus back to the first
     *                           row would yank it out of the field the user is actively typing in.
     */
    protected void redrawPanel(boolean refocusFirstField) {
        propertiesEditorScrollingContainer.revalidate();
        propertiesEditorScrollingContainer.repaint();
        if (propertiesPopupDialogue != null) {
            propertiesPopupDialogue.pack();
            propertiesPopupDialogue.repaint();
        }
        if (refocusFirstField) {
            setFocusOnFirstComponent();
        }
    }

    /**
     * Called by the Properties Dialogue, then passed upto the Intellij DialogueWrapper which creates the button.
     * @return the text to be used in the OK Button.
     */
    protected String getOKButtonText() {
        return UPDATE_CODE_BUTTON_TEXT;
    }

    /**
     * For the given component, get all the editable properties and add them the to properties edit panel.
     */
    protected abstract void populatePropertiesEditorPanel();

    /**
     * Invoked on every keystroke in {@link #propertySearchField} (only ever non-null when supportsPropertySearch
     * was true at construction) - default no-op, overridden by subclasses whose rows are individually filterable.
     * @param query the search field's current, un-trimmed text.
     */
    protected void onPropertySearchChanged(String query) { }

    public void setFocusOnFirstComponent() {
        JComponent firstComponent = getFirstFocusField();
        if (firstComponent != null) {
            IdeFocusManager.getInstance(project).requestFocus(firstComponent, true);
        }
    }

    /**
     * Get the field that should be given the focus in popup or inscreen form
     * @return the component that should be given focus or null
     */
    public abstract JComponent getFirstFocusField();

    protected IkasanObject getSelectedComponent() {
        return selectedComponent;
    }

    protected abstract List<ValidationInfo> doValidateAll();
    public abstract void updateComponentsWithNewValues();
    public abstract boolean dataHasChangedAndOKToProcess();

    public PropertiesPopupDialogue getPropertiesDialogue() {
        return propertiesPopupDialogue;
    }

    public void setPropertiesDialogue(PropertiesPopupDialogue propertiesPopupDialogue) {
        this.propertiesPopupDialogue = propertiesPopupDialogue;
    }

    /**
     * No-op by default - this class exists purely to give subclasses a real Disposable to hang panel-scoped
     * background work off (see the class-level comment above). Override where a subclass actually owns
     * something that needs explicit cleanup - see CanvasPanel#dispose() for the established style.
     */
    @Override
    public void dispose() {
    }
}
