package fixture.corepipeline;
@org.springframework.stereotype.Component("fixture.corepipeline.AcceptNamedOrders")
public class AcceptNamedOrders implements org.ikasan.spec.component.filter.Filter<fixture.Order> {
public fixture.Order filter(fixture.Order o) { return o.getId().isEmpty()?null:o; }
}
