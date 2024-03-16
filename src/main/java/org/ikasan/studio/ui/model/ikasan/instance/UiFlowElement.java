package org.ikasan.studio.ui.model.ikasan.instance;

import org.ikasan.studio.core.model.ikasan.instance.FlowElement;
import org.ikasan.studio.ui.viewmodel.AbstractViewHandler;
import org.ikasan.studio.ui.viewmodel.ViewHandlerFactory;

//@Data
//@EqualsAndHashCode(callSuper=true)
//public class UiFlowElement extends FlowElement {
public class UiFlowElement extends UiBasicElement {
    protected AbstractViewHandler viewHandler;
    private FlowElement flowElement;
    private UiFlow containingFlow;

//    @Builder (builderMethodName = "uiFlowElementBuilder")
//    protected UiFlowElement(Flow containingFlow, ComponentMeta componentMeta, String componentName) {
//        super(containingFlow, componentMeta, componentName);
//        this.viewHandler = ViewHandlerFactory.getInstance(this);
//    }

    protected UiFlowElement(FlowElement flowElement) {
        this.flowElement = flowElement;
        this.viewHandler = ViewHandlerFactory.getInstance(this);
    }

    public void delete() {
        // Find which flow this resides in

    }

}
