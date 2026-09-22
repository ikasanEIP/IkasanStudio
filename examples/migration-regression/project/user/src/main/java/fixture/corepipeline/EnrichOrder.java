package fixture.corepipeline;
@org.springframework.stereotype.Component("fixture.corepipeline.EnrichOrder")
public class EnrichOrder implements org.ikasan.spec.component.endpoint.Broker<fixture.Order,fixture.Order> {
public fixture.Order invoke(fixture.Order o) { o.setStage(o.getStage()+" enriched"); return o; }
}
