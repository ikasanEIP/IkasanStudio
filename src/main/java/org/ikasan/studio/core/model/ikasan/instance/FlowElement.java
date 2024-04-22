package org.ikasan.studio.core.model.ikasan.instance;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.ikasan.studio.core.model.ikasan.instance.serialization.FlowElementSerializer;
import org.ikasan.studio.core.model.ikasan.meta.ComponentMeta;
import org.ikasan.studio.core.model.ikasan.meta.ComponentPropertyMeta;

import java.util.List;

/**
 * The component that resides in a flow e.g. broker, splitter, consumer, producer
 */
@JsonSerialize(using = FlowElementSerializer.class)
@Getter
@Setter
public class FlowElement extends BasicElement {
    public static final String DECORATORS_JSON_TAG = "decorators";
    @JsonIgnore
    private Flow containingFlow;
    private FlowRoute containingFlowRoute;
    private List<Decorator> decorators;

    public FlowElement() {}

    @Builder (builderMethodName = "flowElementBuilder")
    protected FlowElement(ComponentMeta componentMeta, Flow containingFlow, FlowRoute containingFlowRoute, String componentName, List<Decorator> decorators) {
        super(componentMeta, null);
        if (!componentMeta.isExceptionResolver()) {
            setPropertyValue(ComponentPropertyMeta.COMPONENT_NAME, componentName);
        }
        this.containingFlow = containingFlow;
        this.containingFlowRoute = containingFlowRoute;
        this.decorators = decorators;
    }

    public void setContainingFlowRoute(FlowRoute containingFlowRoute) {
        if (containingFlowRoute != null) {
            this.containingFlowRoute = containingFlowRoute;
            this.containingFlow = containingFlowRoute.getFlow();
        }
    }

    @Override
    public String toString() {
        return "IkasanFlowComponent {" +
                ", name='" + getComponentName() + '\'' +
                ", containingFlow ='" + (containingFlow == null ? null : containingFlow.getName()) + '\'' +
                ", containingFlowRoute ='" + (containingFlowRoute == null ? null : containingFlowRoute.getRouteName()) + '\'' +
                ", flowComponent='" + getComponentMeta().getName() + '\'' +
                ", properties=" + componentProperties +
                '}';
    }

    @Override
    public String toSimpleString() {
        return "{" +
                super.toSimpleString() +
                ", flowComponent='" + getComponentMeta().getName() + '\'' +
                ", flowName='" + getComponentName() + '\'' +
                ", containingFlow ='" + (containingFlow == null ? null : containingFlow.getName()) + '\'' +
                ", containingFlowRoute ='" + (containingFlowRoute == null ? null : containingFlowRoute.getRouteName()) + '\'' +
                '}';
    }
}
