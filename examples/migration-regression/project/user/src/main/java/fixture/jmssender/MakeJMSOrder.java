package fixture.jmssender;
@org.springframework.stereotype.Component("fixture.jmssender.MakeJMSOrder")
public class MakeJMSOrder implements org.ikasan.spec.component.transformation.Converter<org.quartz.JobExecutionContext,fixture.Order> {
private int count; public fixture.Order convert(org.quartz.JobExecutionContext c){return new fixture.Order("J"+(++count),1,false);}
}
