package fixture.jmsreceiver;
@org.springframework.stereotype.Component("fixture.jmsreceiver.RequireOrder")
public class RequireOrder implements org.ikasan.spec.component.transformation.Converter<org.ikasan.spec.flow.FlowEvent,fixture.Order> {
public fixture.Order convert(org.ikasan.spec.flow.FlowEvent e){return (fixture.Order)e.getPayload();}
}
