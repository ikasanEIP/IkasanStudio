package org.ikasan.studio.intellij.onboarding;

import com.intellij.diff.DiffContentFactory;
import com.intellij.diff.DiffManager;
import com.intellij.diff.requests.SimpleDiffRequest;
import com.intellij.diff.util.DiffUserDataKeys;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTextArea;
import com.intellij.ui.table.JBTable;
import com.intellij.util.ui.JBUI;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import org.ikasan.studio.core.project.MavenProjectBootstrap.Change;
import org.ikasan.studio.core.project.MavenProjectBootstrap.Plan;
import org.ikasan.studio.ui.StudioBundle;
import org.ikasan.studio.ui.StudioDialogWrapper;

final class SetupStudioProjectReviewDialog extends StudioDialogWrapper {
    private final Project project;
    private final Plan plan;
    private JBTable table;
    SetupStudioProjectReviewDialog(Project project, Plan plan) {
        super(project); this.project=project; this.plan=plan;
        setTitle(StudioBundle.message("setup.ReviewTitle")); setOKButtonText(StudioBundle.message("setup.Apply")); init();
    }
    @Override protected JComponent createStudioContent() {
        JPanel panel = new JPanel(new BorderLayout(JBUI.scale(8), JBUI.scale(8)));
        JBTextArea summary = new JBTextArea(StudioBundle.message("setup.Summary", plan.options().pack(), plan.sources().size()));
        summary.setEditable(false); summary.setLineWrap(true); summary.setWrapStyleWord(true); summary.setOpaque(false);
        panel.add(summary, BorderLayout.NORTH);
        table = new JBTable(new AbstractTableModel() {
            @Override public int getRowCount() { return plan.changes().size(); }
            @Override public int getColumnCount() { return 2; }
            @Override public String getColumnName(int column) { return StudioBundle.message(column==0 ? "setup.File" : "setup.Change"); }
            @Override public Object getValueAt(int row, int column) {
                Change change = plan.changes().get(row);
                return column==0 ? change.path() : StudioBundle.message("setup.Action." + change.action().replace(' ', '_'));
            }
        });
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        for (int i=0; i<plan.changes().size(); i++) if (plan.changes().get(i).path().equals("pom.xml")) {
            table.setRowSelectionInterval(i,i); break;
        }
        table.addMouseListener(new MouseAdapter() { @Override public void mouseClicked(MouseEvent event) { if (event.getClickCount()==2) compare(); } });
        panel.add(new JBScrollPane(table), BorderLayout.CENTER);
        JButton compare = new JButton(StudioBundle.message("setup.Compare")); compare.addActionListener(event -> compare());
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT)); buttons.add(compare); panel.add(buttons,BorderLayout.SOUTH);
        panel.setPreferredSize(JBUI.size(820,480));
        return panel;
    }
    private void compare() {
        int row=table.getSelectedRow(); if (row<0) return;
        Change change=plan.changes().get(table.convertRowIndexToModel(row));
        try {
            String before=text(change.before()), after=text(change.after());
            DiffContentFactory factory=DiffContentFactory.getInstance();
            SimpleDiffRequest request = new SimpleDiffRequest(change.path(), factory.create(project,before), factory.create(project,after),
                    StudioBundle.message("setup.Before"), StudioBundle.message("setup.After"));
            request.putUserData(DiffUserDataKeys.FORCE_READ_ONLY,true);
            DiffManager.getInstance().showDiff(project,request);
        } catch (Exception binary) { Messages.showInfoMessage(project,StudioBundle.message("setup.Binary"),StudioBundle.message("setup.ReviewTitle")); }
    }
    private static String text(String encoded) throws Exception {
        if (encoded==null) return "";
        String text=StandardCharsets.UTF_8.newDecoder().decode(ByteBuffer.wrap(Base64.getDecoder().decode(encoded))).toString();
        if (text.indexOf('\0')>=0) throw new IllegalArgumentException("Binary data");
        return text;
    }
}
