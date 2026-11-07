package org.ikasan.studio.intellij.configuration;

import com.intellij.openapi.project.Project;
import com.intellij.ui.components.*;
import com.intellij.util.ui.JBUI;
import org.ikasan.studio.intellij.execution.HarnessOverrideService;
import org.ikasan.studio.ui.StudioBundle;
import java.awt.*;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.swing.*;

/** Enable harness-owned settings without editing endpoint values or the model. */
public final class HarnessOverridesDialog extends org.ikasan.studio.intellij.project.ImportDialogWrapper {
    private final Project project;
    private final com.intellij.execution.application.ApplicationConfiguration configuration;
    private final Map<String, JBCheckBox> choices = new LinkedHashMap<>();
    HarnessOverridesDialog(Project project) {
        super(project, project.getService(org.ikasan.studio.ui.UiContext.class).getDesignerCanvas());
        this.project = project;
        configuration = project.getService(HarnessOverrideService.class).selectedConfiguration();
        setTitle(StudioBundle.message("harnessOverrides.title"));
        setOKButtonText(StudioBundle.message("harnessOverrides.apply"));
        init();
        setOKActionEnabled(configuration != null);
    }
    public static void show(Project project) {
        if (project.getService(HarnessOverrideService.class).selectedConfiguration() == null) {
            project.getService(org.ikasan.studio.intellij.execution.IkasanRunConfigurationService.class)
                    .prepareEnvironments(java.util.List.of(""), ready -> {
                        if (!project.isDisposed()) new HarnessOverridesDialog(project).show();
                    });
        } else new HarnessOverridesDialog(project).show();
    }
    @Override protected JComponent createCenterPanel() {
        var service = project.getService(HarnessOverrideService.class);
        JPanel panel = new JPanel(new BorderLayout(0, JBUI.scale(10)));
        int width = JBUI.scale(620);
        String explanation = StudioBundle.message(configuration == null ?
                "harnessOverrides.noConfiguration" : "harnessOverrides.help");
        // A wrapped text area with no laid-out width can request one line per character at pack time.
        // Give the HTML label a wrapping width so its preferred height reflects the actual explanation.
        var help = new JBLabel("<html><body style='width: " + width + "px'>"
                + com.intellij.openapi.util.text.StringUtil.escapeXmlEntities(explanation) + "</body></html>");
        panel.add(help, BorderLayout.NORTH);
        JPanel rows = new JPanel();
        rows.setLayout(new BoxLayout(rows, BoxLayout.Y_AXIS));
        var selected = service.selection(configuration);
        for (var target : service.targets()) {
            var choice = new JBCheckBox(target.label(), selected.contains(target.id()));
            choice.setToolTipText(StudioBundle.message("harnessOverrides.toggleHelp"));
            choice.setEnabled(configuration != null);
            choices.put(target.id(), choice); rows.add(choice);
        }
        if (rows.getComponentCount() > 0) {
            var scroll = new JBScrollPane(rows);
            int borderHeight = scroll.getInsets().top + scroll.getInsets().bottom;
            scroll.setPreferredSize(new Dimension(width,
                    Math.min(rows.getPreferredSize().height + borderHeight, JBUI.scale(300))));
            scroll.setMinimumSize(new Dimension(0, 0));
            panel.add(scroll, BorderLayout.CENTER);
        }
        return panel;
    }
    @Override protected void doOKAction() {
        if (configuration == null) return;
        var selected = choices.entrySet().stream().filter(e -> e.getValue().isSelected()).map(Map.Entry::getKey)
                .collect(java.util.stream.Collectors.toSet());
        project.getService(HarnessOverrideService.class).saveSelection(configuration, selected);
        super.doOKAction();
    }
}
