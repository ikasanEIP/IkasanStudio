package org.ikasan.studio.boot;
import java.lang.reflect.*;
import java.nio.file.*;
import java.util.*;
import org.ikasan.component.endpoint.quartz.consumer.ScheduledConsumer;
import org.ikasan.component.endpoint.filesystem.messageprovider.*;
public class VerifyLocalFiles {
 static Object received;
 static Object proxy(Class<?> type, java.util.function.BiFunction<Method,Object[],Object> handler) {
  return Proxy.newProxyInstance(type.getClassLoader(),new Class[]{type},(p,m,a)->handler.apply(m,a));
 }
 static void check(boolean ok, Object detail) { if(!ok) throw new AssertionError(detail); }
 public static void main(String[] args) throws Exception {
  var scheduler=(org.quartz.Scheduler)proxy(org.quartz.Scheduler.class,(m,a)->null);
  var consumer=new ScheduledConsumer(scheduler, (org.ikasan.component.endpoint.quartz.recovery.service.ScheduledJobRecoveryService)proxy(org.ikasan.component.endpoint.quartz.recovery.service.ScheduledJobRecoveryService.class,(m,a)->null));
  var config=new FileConsumerConfiguration();
  config.setFilenames(List.of("myFile\\.txt", "anotherFile\\.txt"));
  consumer.setConfiguration(config);
  consumer.setMessageProvider(new FileMessageProvider());
  consumer.setEventFactory((org.ikasan.spec.event.EventFactory)proxy(org.ikasan.spec.event.EventFactory.class,(m,a)->a[1]));
  Class<?> fc=org.ikasan.spec.flow.Flow.class.getMethod("getFlowConfiguration").getReturnType();
  Class<?> fe=fc.getMethod("getConsumerFlowElement").getReturnType();
  Object wrappedConsumer = new org.springframework.aop.framework.ProxyFactory(consumer).getProxy();
  check(!(wrappedConsumer instanceof ScheduledConsumer), "Expected an interface proxy");
  Object element=proxy(fe,(m,a)->m.getName().equals("getFlowComponent")?wrappedConsumer:null);
  Object configuration=proxy(fc,(m,a)->m.getName().equals("getConsumerFlowElement")?element:null);
  Object flow=Proxy.newProxyInstance(fc.getClassLoader(),new Class[]{org.ikasan.spec.flow.Flow.class,org.ikasan.spec.event.EventListener.class},(p,m,a)->{
   if(m.getName().equals("getFlowConfiguration"))return configuration;
   if(m.getName().equals("invoke")){received=a[0];return null;}return null;
  });
  var controller=new StudioInjectController();
  controller.myModule=(org.ikasan.spec.module.Module)proxy(org.ikasan.spec.module.Module.class,(m,a)->m.getName().equals("getFlow")?flow:null);
  var file=Files.createTempFile("studio-selected-", ".txt");
  try {
   var request=new StudioInjectController.InjectRequest();
   request.setPayloadAdapter("studio-local-file-list");
   request.setPayload(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(List.of(file.toString())));
   var result=controller.inject("flow4",request);
   check(result.getStatusCode().value()==200,result.getBody());
   check(received.equals(List.of(file.toFile())),received);
   received=null;
   request.setPayload("[\"/missing/studio-file\"]");
   check(controller.inject("flow4",request).getStatusCode().value()!=200,"Missing file accepted");
   check(received==null,"Missing file invoked flow");
   var dirs=controller.scanDirectories("flow4");
   check(dirs.getStatusCode().value()==200,dirs.getBody());
   check(((Map<?,?>)dirs.getBody()).get("directories").toString().contains(Path.of("").toAbsolutePath().toString()),dirs.getBody());
   config.setFilePath(file.getParent().toString());
   check(((Map<?,?>)controller.scanDirectories("flow4").getBody()).get("directories").toString().contains(file.getParent().toString()),"Override ignored");
   config.setDynamicFileName(true);
   check(controller.scanDirectories("flow4").getStatusCode().value()==400,"Dynamic path guessed");
   request.setPayloadAdapter(null);
   check(controller.inject("flow4",request).getStatusCode().value()!=200,"Normal scan bypassed missing scheduler");
   check(received==null,"Normal scan injected payload");
   System.out.println("Selected files reach flow; missing files rejected; default/overridden/dynamic directories and scheduler separation verified");
  } finally { Files.delete(file); }
 }
}
