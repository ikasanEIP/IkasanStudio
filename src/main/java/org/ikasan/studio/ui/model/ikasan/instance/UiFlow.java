package org.ikasan.studio.ui.model.ikasan.instance;

import org.ikasan.studio.build.model.ikasan.instance.Flow;
import org.ikasan.studio.build.model.ikasan.instance.FlowElement;
import org.ikasan.studio.ui.viewmodel.AbstractViewHandler;

import java.util.ArrayList;
import java.util.List;

//@Data
//@EqualsAndHashCode(callSuper=true)
//public class UiFlow extends Flow {
public class UiFlow extends UiBasicElement {
    protected AbstractViewHandler viewHandler;
    private Flow flow;
    private List<UiFlowElement> uiFlowElements;
    private UiExceptionResolver uiExceptionResolver;
    private UiFlowElement consumer;

//    @Builder(builderMethodName = "uiFlowBuilder")
//    public UiFlow(
//            FlowElement consumer,
//            List<Transition> transitions,
//            List<FlowElement> flowElements,
//            ExceptionResolver exceptionResolver,
//            String name,
//            String description) {
//        super(consumer, transitions, flowElements, exceptionResolver, name, description);
//        this.viewHandler = ViewHandlerFactory.getInstance(this);
//    }

    public UiFlow(Flow flow) {
        this.flow = flow;
        this.uiFlowElements = new ArrayList<>();
        if (this.flow.hasConsumer()) {
            this.consumer = new UiFlowElement(this.flow.getConsumer());
        }
        if (this.flow.hasExceptionResolver()) {
            this.uiExceptionResolver = new UiExceptionResolver(this.flow.getExceptionResolver());
        }
        if (!this.flow.getFlowElements().isEmpty()) {
            for (FlowElement flowElement : this.flow.getFlowElements()) {
                uiFlowElements.add(new UiFlowElement(flowElement));
            }
        }
    }
}
