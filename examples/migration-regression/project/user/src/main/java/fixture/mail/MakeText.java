package fixture.mail;
@org.springframework.stereotype.Component("fixture.mail.MakeText")
public class MakeText implements org.ikasan.spec.component.transformation.Converter<org.quartz.JobExecutionContext,String> {
public String convert(org.quartz.JobExecutionContext input) { return "Migration email body"; }
}
