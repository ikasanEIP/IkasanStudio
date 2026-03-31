package org.ikasan.studio.ui.component.properties;

import lombok.Data;
import org.ikasan.studio.core.model.ikasan.instance.ComponentProperty;
import org.ikasan.studio.core.model.ikasan.instance.ExceptionResolution;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;


/**
 * Encapsulates the UI component functionality e.g. Label and appropriate editor box for a property,
 * including validation and subsequent value access.
 */
@Data
public class ExceptionResolutionRowDisplay {
    private final JLabel actionField = new JLabel();
    private List<ComponentPropertyEditRow> componentPropertyEditRowList = new ArrayList<>();
    private final ExceptionResolution exceptionResolution;
    private final JButton deleteButton = new JButton("DEL");
    JLabel exceptionField = new JLabel();

    public ExceptionResolutionRowDisplay(ExceptionResolutionTableDisplay parent, ExceptionResolution exceptionResolution, boolean componentInitialisation) {
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
            componentPropertyEditRowList = new ArrayList<>();
            for (ComponentProperty property : exceptionResolution.getComponentProperties().values()) {
                ComponentPropertyEditRow actionParam = new ComponentPropertyEditRow(parent.getProject(), property, componentInitialisation);
                componentPropertyEditRowList.add(actionParam);
            }
        }
    }

    public List<ComponentPropertyEditRow> getActionParamsEditBoxList() {
        return componentPropertyEditRowList;
    }

    public ExceptionResolution getIkasanExceptionResolution() {
        return exceptionResolution;
    }
}
