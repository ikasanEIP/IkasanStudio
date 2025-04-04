package org.ikasan.studio.core.model.ikasan.instance;

import org.ikasan.studio.core.StudioBuildException;
import org.ikasan.studio.core.model.ikasan.meta.ComponentMeta;

public class FlowElementFactory {
    /**
     * Any component that belongs in the flow
     * @param type e.g. EVENT_DRIVEN_CONSUMER, PAYLOAD_TO_MAP_CONVERTER
     * @param containinfFlow flow that contains this element
     */
    public static FlowElement createFlowElement(String metapackVersion, ComponentMeta type, Flow containinfFlow, FlowRoute containingFlowRoute, String componentName) throws StudioBuildException {
        FlowElement flowElement = null;
        if (type.isGeneratesUserImplementedClass()) {
            flowElement = FlowUserImplementedElement.flowUserImplementedElementBuilder()
                    .componentMeta(type)
                    .containingFlowRoute(containingFlowRoute)
                    .containingFlow(containinfFlow)
                    .componentName(componentName)
                    .build();
        } else if (type.isExceptionResolver()) {
            flowElement = new ExceptionResolver(metapackVersion, containinfFlow);
        } else {
            flowElement = FlowElement.flowElementBuilder()
                    .componentMeta(type)
                    .containingFlowRoute(containingFlowRoute)
                    .containingFlow(containinfFlow)
                    .componentName(componentName)
                    .build();
        }

//        flowElement.componentProperties()
        return flowElement;
    }
}
