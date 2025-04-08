package org.ikasan.studio.ui.component.properties;

import lombok.Data;
import org.ikasan.studio.core.model.ikasan.instance.ComponentProperty;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Encapsulates the UI component functionality e.g. Label and appropriate editor box for a property,
 * including validation and subsequent value access.
 */
@Data
public class ExceptionResolution {
    private final JLabel actionField = new JLabel();
    private List<ComponentPropertyEditBox> componentPropertyEditBoxList = new ArrayList<>();
    private final org.ikasan.studio.core.model.ikasan.instance.ExceptionResolution exceptionResolution;
    private final JButton deleteButton = new JButton("DEL");
    JLabel exceptionField = new JLabel();

    public ExceptionResolution(ExceptionResolverEditBox parent, org.ikasan.studio.core.model.ikasan.instance.ExceptionResolution exceptionResolution, boolean componentInitialisation) {
        this.exceptionResolution = exceptionResolution;

        String theException = exceptionResolution.getExceptionsCaught();
        String theAction = exceptionResolution.getTheAction();

        if (theException != null && !theException.isEmpty()) {
            exceptionField.setText(exceptionResolution.getExceptionsCaught());
            deleteButton.addActionListener(e ->
                parent.doDelete(exceptionResolution)
            );
        }
        if (theAction != null && !theAction.isEmpty()) {
            actionField.setText(exceptionResolution.getTheAction());
        }
        if (exceptionResolution.getTheAction() != null &&
                exceptionResolution.getComponentProperties() != null &&
                !exceptionResolution.getComponentProperties().isEmpty()) {
            componentPropertyEditBoxList = new ArrayList<>();
            for (ComponentProperty property : exceptionResolution.getComponentProperties().values()) {
                ComponentPropertyEditBox actionParam = new ComponentPropertyEditBox(parent.getProjectKey(), property, componentInitialisation, () -> {});
                componentPropertyEditBoxList.add(actionParam);
            }
        }
    }

    public List<ComponentPropertyEditBox> getActionParamsEditBoxList() {
        return componentPropertyEditBoxList;
    }

    public org.ikasan.studio.core.model.ikasan.instance.ExceptionResolution getIkasanExceptionResolution() {
        return exceptionResolution;
    }
}
