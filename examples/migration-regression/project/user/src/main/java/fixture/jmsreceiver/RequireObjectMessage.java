package fixture.jmsreceiver;
@org.springframework.stereotype.Component("fixture.jmsreceiver.RequireObjectMessage")
public class RequireObjectMessage implements org.ikasan.spec.component.transformation.Converter<org.ikasan.spec.flow.FlowEvent,Object> {
public Object convert(org.ikasan.spec.flow.FlowEvent e){ Object p=e.getPayload(); if(!p.getClass().getSimpleName().contains("ObjectMessage"))throw new IllegalArgumentException("Expected ObjectMessage"); return p; }
}
