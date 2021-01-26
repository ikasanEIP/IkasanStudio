package org.ikasan.studio.model.Ikasan;

import org.ikasan.studio.ui.viewmodel.IkasanFlowElementViewHandler;

public class IkasanFlowComponent extends IkasanComponent {
    private IkasanFlow parent;
    private IkasanFlowComponentType type;

    /**
     *
     * @param parent flow that contains this element
     * @param name of the element
     * @param description of the element
     */
    public IkasanFlowComponent(IkasanFlowComponentType type, IkasanFlow parent, String name, String description) {
        super ();
        this.type = type;
        this.parent = parent;
        this.properties = type.getMandatoryProperties();
        setPropertyValue(IkasanComponentPropertyMeta.NAME, name);
        setPropertyValue(IkasanComponentPropertyMeta.DESCRIPTION, description);
        viewHandler = new IkasanFlowElementViewHandler(this);
    }

    /**
     *
     * @param type
     * @param parent
     */
    public IkasanFlowComponent(IkasanFlowComponentType type, IkasanFlow parent) {
        this(type, parent, "", "");
    }

//    /**
//     * The type influences the view handler
//     * @param type
//     */
//    public void setTypeAndViewHandler(IkasanFlowComponentType type) {
//        this.type = type;
//        viewHandler = new IkasanFlowElementViewHandler(this);
//    }

//    public void setParent(IkasanFlow parent) {
//        this.parent = parent;
//    }

    public IkasanFlowComponentType getType() {
        return type;
    }

    @Override
    public String toString() {
        return "IkasanFlowComponent{" +
//                "viewHandler=" + viewHandler +
                ", name='" + getName() + '\'' +
//                ", description='" + description + '\'' +
                ", type=" + type +
                ", properties=" + properties +
                '}';
    }
}
