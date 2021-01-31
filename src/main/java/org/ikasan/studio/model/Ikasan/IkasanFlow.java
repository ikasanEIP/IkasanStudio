package org.ikasan.studio.model.Ikasan;

import org.ikasan.studio.ui.viewmodel.IkasanFlowViewHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

public class IkasanFlow extends IkasanComponent {
//    private String name;
//    private String description;
    private IkasanFlowComponent input;
    private IkasanFlowComponent output;
    private List<IkasanFlowComponent> flowComponentList = new ArrayList<>();

    public IkasanFlow () {
        super();
        this.properties = new TreeMap<String, IkasanComponentProperty>();
        this.properties.put(IkasanComponentPropertyMeta.NAME, new IkasanComponentProperty(IkasanComponentPropertyMeta.STD_NAME_META_COMPONENT));
        this.properties.put(IkasanComponentPropertyMeta.DESCRIPTION, new IkasanComponentProperty(IkasanComponentPropertyMeta.STD_DESCIPTION_META_COMPONENT));

        this.viewHandler = new IkasanFlowViewHandler(this);
    }
    public List<IkasanFlowComponent> getFlowComponentList() {
        return flowComponentList;
    }

    public boolean addFlowElement(IkasanFlowComponent ikasanFlowComponent) {
        return flowComponentList.add(ikasanFlowComponent);
    }

    public void removeFlowElement(IkasanFlowComponent ikasanFlowComponentToBeRemoved) {
        if (ikasanFlowComponentToBeRemoved != null && flowComponentList.size() > 0) {
            getFlowComponentList().removeIf(x -> x.equals(ikasanFlowComponentToBeRemoved));
        }
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
                "name='" + getName() + '\'' +
                ", description='" + getDescription() + '\'' +
                ", flowElementList=" + flowComponentList +
                '}';
    }


}
