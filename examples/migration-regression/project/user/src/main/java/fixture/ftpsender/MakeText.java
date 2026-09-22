package fixture.ftpsender;
@org.springframework.stereotype.Component("fixture.ftpsender.MakeText")
public class MakeText implements org.ikasan.spec.component.transformation.Converter<org.quartz.JobExecutionContext,String> {
public String convert(org.quartz.JobExecutionContext input) { return "FTP migration payload"; }
}
