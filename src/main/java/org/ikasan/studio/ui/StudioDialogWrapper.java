package org.ikasan.studio.ui;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import javax.swing.JComponent;
import java.awt.Component;

/** DialogWrapper behavior with copy support limited to Studio-owned content. */
// Studio dialogs run in the local IDE. A transitive dependency classifies this module as backend.
@SuppressWarnings("SplitModeApiUsage")
public abstract class StudioDialogWrapper extends DialogWrapper {
    protected StudioDialogWrapper(Project project) { super(project); }
    protected StudioDialogWrapper(Project project, boolean canBeParent) { super(project, canBeParent); }
    protected StudioDialogWrapper(Project project, Component parent, boolean canBeParent, IdeModalityType modality) {
        super(project, parent, canBeParent, modality);
    }

    protected abstract JComponent createStudioContent();

    @Override public final JComponent createCenterPanel() {
        JComponent content = createStudioContent();
        StudioTextCopySupport.install(content);
        return content;
    }

    @Override protected JComponent createSouthPanel() {
        JComponent buttons = super.createSouthPanel();
        StudioTextCopySupport.install(buttons);
        return buttons;
    }

    @Override protected void init() {
        super.init();
        // The peer can have no content pane in headless tests; the center hook above
        // still covers the actual content, and the normal peer also gets header/status text.
        StudioTextCopySupport.install(getContentPane());
    }
}
