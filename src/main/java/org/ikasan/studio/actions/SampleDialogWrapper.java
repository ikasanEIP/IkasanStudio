package org.ikasan.studio.actions;

import com.intellij.openapi.ui.DialogWrapper;
import org.ikasan.studio.model.Ikasan.IkasanFlowComponent;
import org.ikasan.studio.ui.viewmodel.IkasanFlowElementViewHandler;

import javax.swing.*;
import java.awt.*;

public class SampleDialogWrapper extends DialogWrapper {
    private String projectKey;
    private IkasanFlowComponent flowElement;
    public SampleDialogWrapper(String projectKey, IkasanFlowComponent flowElement) {
        super(true); // use current window as parent
        this.projectKey = projectKey;
        this.flowElement = flowElement;
        init();
        setTitle("Generator Help");
    }

//    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        JPanel dialogPanel = new JPanel(new BorderLayout());
        final IkasanFlowElementViewHandler viewHandler = (IkasanFlowElementViewHandler) flowElement.getViewHandler();
//        JLabel helpTextDisplay = new JLabel(viewHandler.getIkasanFlowUIComponent().getHelpText());
        JTextArea helpTextDisplay = new JTextArea(viewHandler.getIkasanFlowUIComponent().getHelpText());
        helpTextDisplay.setLineWrap(true);
        dialogPanel.add(helpTextDisplay, BorderLayout.CENTER);
        return dialogPanel;
    }
}
