package fixture.corepipeline;
@org.springframework.stereotype.Component("fixture.corepipeline.MakeBatch")
public class MakeBatch implements org.ikasan.spec.component.transformation.Converter<org.quartz.JobExecutionContext,java.util.List> {
private int batch; public java.util.List convert(org.quartz.JobExecutionContext input) { int b=++batch; return java.util.List.of(java.util.List.of(new fixture.Order("B"+b+"P",2,true),new fixture.Order("B"+b+"S",1,false),new fixture.Order("B"+b+"R",0,false),new fixture.Order("",1,false),new fixture.Order("B"+b+"DROP",-1,false))); }
}
