package fixture.corepipeline;
@org.springframework.stereotype.Component("fixture.corepipeline.AcceptKnownQuantities")
public class AcceptKnownQuantities implements org.ikasan.spec.component.filter.FilterRule<fixture.Order> {
public boolean accept(fixture.Order o) { return o.getQuantity()>=0; }
}
