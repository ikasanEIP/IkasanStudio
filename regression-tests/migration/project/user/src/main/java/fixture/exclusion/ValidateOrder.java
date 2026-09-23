package fixture.exclusion;
@org.springframework.stereotype.Component("fixture.exclusion.ValidateOrder")
public class ValidateOrder implements org.ikasan.spec.component.transformation.Converter<fixture.Order,fixture.Order> {
public fixture.Order convert(fixture.Order o) { if(o.getQuantity()<=0)throw new org.ikasan.spec.component.transformation.TransformationException("Invalid quantity "+o.getId());return o; }
}
