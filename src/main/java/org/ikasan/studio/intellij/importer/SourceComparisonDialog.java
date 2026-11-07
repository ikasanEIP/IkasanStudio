package org.ikasan.studio.intellij.importer;

import com.intellij.diff.DiffContentFactory;
import com.intellij.diff.DiffManager;
import com.intellij.diff.contents.DocumentContent;
import com.intellij.diff.requests.SimpleDiffRequest;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.project.Project;
import org.ikasan.studio.intellij.project.ImportDialogWrapper;
import org.ikasan.studio.ui.StudioBundle;

import javax.swing.JComponent;
import java.awt.Component;
import java.awt.Dimension;

/** Edits an isolated preview; cancellation never modifies the pending import. */
public final class SourceComparisonDialog extends ImportDialogWrapper {
    private final Project project;
    private final DocumentContent destination;
    private final DocumentContent preview;
    private final SimpleDiffRequest request;
    private final java.util.function.Function<String, String> validation;

    public SourceComparisonDialog(Project project, Component parent, SimpleDiffRequest original) {
        this(project, parent, original, text -> null);
    }

    public SourceComparisonDialog(Project project, Component parent, SimpleDiffRequest original,
                                  java.util.function.Function<String, String> validation) {
        super(project, parent);
        this.validation = validation;
        this.project = project;
        destination = (DocumentContent) original.getContents().get(1);
        preview = DiffContentFactory.getInstance().create(destination.getDocument().getText(),
                destination.getContentType());
        preview.getDocument().setReadOnly(false);
        request = new SimpleDiffRequest(original.getTitle(), original.getContents().get(0), preview,
                original.getContentTitles().get(0), original.getContentTitles().get(1));
        setTitle(original.getTitle());
        setOKButtonText(StudioBundle.message("sourceImport.savePreview"));
        init();
    }

    @Override protected JComponent createCenterPanel() {
        var panel = DiffManager.getInstance().createRequestPanel(project, getDisposable(), getWindow());
        panel.setRequest(request);
        var component = panel.getComponent();
        component.setPreferredSize(new Dimension(1000, 650));
        return component;
    }

    @Override protected void doOKAction() {
        String problem = validation.apply(preview.getDocument().getText());
        if (problem != null) {
            com.intellij.openapi.ui.Messages.showErrorDialog(getContentPane(), problem, getTitle());
            return;
        }
        WriteAction.run(() -> destination.getDocument().setText(preview.getDocument().getText()));
        super.doOKAction();
    }
}
