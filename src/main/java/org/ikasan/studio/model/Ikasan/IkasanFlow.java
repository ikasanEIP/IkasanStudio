package org.ikasan.studio.model.Ikasan;

import org.ikasan.studio.StudioUtils;
import org.ikasan.studio.ui.viewmodel.IkasanFlowViewHandler;
import org.ikasan.studio.ui.viewmodel.ViewHandler;

import java.util.ArrayList;
import java.util.List;

public class IkasanFlow {
    private ViewHandler viewHandler;
    private String name;
    private String javaVariableName;
    private String description;
    private IkasanFlowComponent input;
    private IkasanFlowComponent output;

    private List<IkasanFlowComponent> flowElementList = new ArrayList<>();

    public IkasanFlow() {
        viewHandler = new IkasanFlowViewHandler(this);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {

        this.name = name;
        if (name != null && name.length() > 0) {
            javaVariableName = StudioUtils.toJavaIdentifier(name);
        }
    }

    public String getJavaVariableName() {
        return javaVariableName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<IkasanFlowComponent> getFlowElementList() {
        return flowElementList;
    }

    public boolean addFlowElement(IkasanFlowComponent ikasanFlowComponent) {
        return flowElementList.add(ikasanFlowComponent);
    }

    public ViewHandler getViewHandler() {
        return viewHandler;
    }

    public IkasanFlowComponent getInput() {
        return input;
    }

    public void setInput(IkasanFlowComponent input) {
        this.input = input;
    }

    public IkasanFlowComponent getOutput() {
        return output;
    }

    public void setOutput(IkasanFlowComponent output) {
        this.output = output;
    }

    @Override
    public String toString() {
        return "IkasanFlow{" +
                "name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", flowElementList=" + flowElementList +
                '}';
    }


}
