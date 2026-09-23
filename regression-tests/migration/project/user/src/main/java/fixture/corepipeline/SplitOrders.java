package fixture.corepipeline;
@org.springframework.stereotype.Component("fixture.corepipeline.SplitOrders")
public class SplitOrders implements org.ikasan.spec.component.splitting.Splitter<java.util.List,fixture.Order> {
public java.util.List<fixture.Order> split(java.util.List input) { return new java.util.ArrayList<fixture.Order>(input); }
}
