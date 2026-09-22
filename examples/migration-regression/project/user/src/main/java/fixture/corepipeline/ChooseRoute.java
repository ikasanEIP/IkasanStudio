package fixture.corepipeline;
@org.springframework.stereotype.Component("fixture.corepipeline.ChooseRoute")
public class ChooseRoute implements org.ikasan.spec.component.routing.SingleRecipientRouter<fixture.Order> {
public String route(fixture.Order o) { return o.getQuantity()<=0?"Rejected":o.isPriority()?"Priority":"Standard"; }
}
