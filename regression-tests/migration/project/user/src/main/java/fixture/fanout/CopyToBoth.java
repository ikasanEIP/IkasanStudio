package fixture.fanout;
@org.springframework.stereotype.Component("fixture.fanout.CopyToBoth")
public class CopyToBoth implements org.ikasan.spec.component.routing.MultiRecipientRouter<fixture.Order> {
public java.util.List<String> route(fixture.Order o) { return java.util.List.of("Audit","Fulfilment"); }
}
