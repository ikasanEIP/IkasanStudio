package fixture.fanout;
@org.springframework.stereotype.Component("fixture.fanout.AuditOrder")
public class AuditOrder implements org.ikasan.spec.component.endpoint.Producer<fixture.Order> {
public void invoke(fixture.Order o) { o.setStage("audit mutation"); }
}
