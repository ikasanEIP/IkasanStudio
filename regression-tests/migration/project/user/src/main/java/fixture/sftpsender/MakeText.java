package fixture.sftpsender;
@org.springframework.stereotype.Component("fixture.sftpsender.MakeText")
public class MakeText implements org.ikasan.spec.component.transformation.Converter<org.quartz.JobExecutionContext,String> {
public String convert(org.quartz.JobExecutionContext input) { return "SFTP migration payload"; }
}
