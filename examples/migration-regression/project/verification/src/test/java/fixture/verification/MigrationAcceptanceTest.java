package fixture.verification;

import org.junit.Test;
import org.ikasan.spec.flow.*;
import org.ikasan.spec.exclusion.ExclusionManagementService;
import org.ikasan.testharness.flow.rule.IkasanFlowTestRule;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BooleanSupplier;
import static org.junit.Assert.*;

/** Real generated application, real components and persistence; no mocked flow or broker. */
public class MigrationAcceptanceTest {
    private final List<Map<String,Object>> checks = new ArrayList<>();
    private final List<String> deliveries = new CopyOnWriteArrayList<>();
    private org.ikasan.spec.module.Module<Flow> module;
    private ConfigurableApplicationContext context;
    private LocalServices services;
    private void check(String name, Checked action) {
        try { action.run(); checks.add(Map.of("name",name,"status","PASS")); }
        catch (Throwable failure) { checks.add(Map.of("name",name,"status","FAIL","detail",failure.toString())); }
    }
    interface Checked { void run() throws Exception; }
    private void await(String reason, BooleanSupplier ready) throws Exception {
        long deadline=System.nanoTime()+java.util.concurrent.TimeUnit.SECONDS.toNanos(15);
        while (!ready.getAsBoolean() && System.nanoTime()<deadline) Thread.sleep(50);
        assertTrue(reason+"; deliveries="+deliveries,ready.getAsBoolean());
    }
    private boolean has(String flow,String component,String payload) {
        return deliveries.stream().anyMatch(s->s.startsWith(flow+"/"+component+"=") && s.contains(payload));
    }
    private IkasanFlowTestRule startScheduled(String name, String consumer) {
        var harness=new IkasanFlowTestRule(); harness.withFlow(module.getFlow(name)).scheduledConsumer(consumer);
        harness.startFlow(); return harness;
    }
    @Test public void acceptance() throws Exception {
        try {
            services=new LocalServices();
            var args=new ArrayList<>(services.arguments);args.add("--server.port=0");
            args.add("--ikasan.module.activator.startup.type.defaultStartupType=MANUAL");
            context=SpringApplication.run(Class.forName("org.ikasan.studio.boot.Application"),args.toArray(new String[0]));
            module=context.getBean(org.ikasan.spec.module.Module.class);
            for(Flow flow:module.getFlows()) flow.addFlowListener(new FlowEventListener(){
                public void beforeFlowElement(String m,String f,FlowElement e,FlowEvent event){}
                public void afterFlowElement(String m,String f,FlowElement e,FlowEvent event){
                    deliveries.add(f+"/"+e.getComponentName()+"="+String.valueOf(event.getPayload()));
                }
            });
            check("pipeline branches and later delivery",()->{
                var harness=startScheduled("Core Pipeline","Timer");
                for(int batch=1;batch<=2;batch++) {
                    harness.fireScheduledConsumer(); final int b=batch;
                    await("Three final branches for batch "+batch,()->has("Core Pipeline","Log Priority","B"+b+"P|2|true|translated enriched")
                        &&has("Core Pipeline","Log Standard","B"+b+"S|1|false|translated enriched")
                        &&has("Core Pipeline","Log Rejected","B"+b+"R|0|false|translated enriched"));
                    Thread.sleep(250); assertTrue(module.getFlow("Core Pipeline").isRunning());
                }
                assertEquals(6, deliveries.stream().filter(s->s.startsWith("Core Pipeline/Log ")).count());
            });
            check("fanout isolation and later delivery",()->{
                Flow flow=module.getFlow("Fanout"); flow.start();
                var source=context.getBean(fixture.fanout.ControlledInput.class);
                for(int i=1;i<=2;i++) {
                    source.send(new fixture.Order("F"+i,1,false));final int n=i;
                    await("Both fanout branches",()->has("Fanout","Audit Order","F"+n+"|1|false|audit mutation")
                        &&has("Fanout","Fulfil Order","F"+n+"|1|false|new"));
                    assertTrue(flow.isRunning());
                }
                assertEquals(2,deliveries.stream().filter(s->s.startsWith("Fanout/Fulfil Order=")).count());
            });
            check("persisted exclusion and subsequent valid delivery",()->{
                var harness=startScheduled("Exclusion","Timer");
                var exclusions=context.getBean(ExclusionManagementService.class);
                for(int i=1;i<=3;i++) {
                    harness.fireScheduledConsumer();final int n=i;
                    if(i==2) await("Invalid event persisted",()->exclusions.findAll().size()==1);
                    else await("Valid event "+i,()->has("Exclusion","Accepted Order","E"+n+"|1|false"));
                    assertTrue(module.getFlow("Exclusion").isRunning());
                }
                assertFalse(deliveries.stream().anyMatch(s->s.startsWith("Exclusion/Accepted Order=E2|")));
                var event=(org.ikasan.spec.exclusion.ExclusionEvent)exclusions.findAll().get(0);
                assertEquals("Exclusion",event.getFlowName()); assertTrue(event.getEvent().length>0);
            });
            check("JMS object delivery and XML conversion",()->{
                module.getFlow("JMS Receiver").start();var sender=startScheduled("JMS Sender","Timer");
                for(int i=1;i<=2;i++) {
                    sender.fireScheduledConsumer(); final int n=i;
                    await("Receiver XML output",()->has("JMS Receiver","Log XML","<id>J"+n+"</id>"));
                    assertTrue(module.getFlow("JMS Receiver").isRunning());assertTrue(module.getFlow("JMS Sender").isRunning());
                }
            });
            check("event generating consumer remains available",()->{
                module.getFlow("Event Source").start();
                await("Two generated events",()->has("Event Source","Discard Sample","EVENT-2"));
                assertTrue(module.getFlow("Event Source").isRunning());
            });
            check("local file delivery",()->{
                Files.createDirectories(Path.of("inputs"));Files.writeString(Path.of("inputs/first.txt"),"LOCAL-FIRST\n");
                var source=startScheduled("Local Files","Read Lines");source.fireScheduledConsumer();
                await("First local file",()->has("Local Files","Log Line","LOCAL-FIRST"));
                Files.writeString(Path.of("inputs/later.txt"),"LOCAL-LATER\n");source.fireScheduledConsumer();
                await("Later local file",()->has("Local Files","Log Line","LOCAL-LATER"));
                assertTrue(module.getFlow("Local Files").isRunning());
            });
            for(String transport:List.of("FTP","SFTP")) {
                check(transport+" round trip",()->{
                    var receiver=startScheduled(transport+" Receiver","Receive File");
                    var sender=startScheduled(transport+" Sender","Timer");sender.fireScheduledConsumer();
                    await("Send committed",()->has(transport+" Sender","Send File",""));
                    receiver.fireScheduledConsumer();await("Remote content",()->has(transport+" Receiver","Log File",transport+" migration payload"));
                    long first=deliveries.stream().filter(s->s.startsWith(transport+" Receiver/Log File=")).count();
                    receiver.fireScheduledConsumer();Thread.sleep(3000);
                    assertEquals("Duplicate suppressed",first,deliveries.stream().filter(s->s.startsWith(transport+" Receiver/Log File=")).count());
                    long sent=deliveries.stream().filter(s->s.startsWith(transport+" Sender/Send File=")).count();
                    sender.fireScheduledConsumer();
                    await("Later send committed",()->deliveries.stream().filter(s->s.startsWith(transport+" Sender/Send File=")).count()==sent+1);
                    receiver.fireScheduledConsumer();
                    await("Later file delivered without restart",()->deliveries.stream().filter(s->s.startsWith(transport+" Receiver/Log File=")).count()==first+1);
                    assertTrue(module.getFlow(transport+" Sender").isRunning());
                    assertTrue(module.getFlow(transport+" Receiver").isRunning());
                });
            }
            check("SMTP remote delivery and later delivery",()->{
                var access=new IkasanFlowTestRule().withFlow(module.getFlow("Mail"));
                var producer=(org.ikasan.component.endpoint.email.producer.EmailProducer)access.getComponent("Send Mail");
                producer.getConfiguration().setMailSmtpPort(services.smtpPort());
                var sender=startScheduled("Mail","Timer");
                for(int i=1;i<=2;i++){sender.fireScheduledConsumer();final int n=i;
                    await("SMTP captured body",()->services.mail.size()==n && services.mail.get(n-1).contains("Migration email body"));}
                assertTrue(module.getFlow("Mail").isRunning());
            });
            check("all exercised flows ready while idle",()->{
                Thread.sleep(1000);
                for(String name:List.of("Core Pipeline","Fanout","Exclusion","JMS Sender","JMS Receiver","Event Source","Local Files","FTP Sender","FTP Receiver","SFTP Sender","SFTP Receiver","Mail"))
                    assertTrue(name+" is "+module.getFlow(name).getState(),module.getFlow(name).isRunning());
            });
        } catch(Throwable failure) {
            checks.add(Map.of("name","application startup","status","FAIL","detail",failure.toString()));
        } finally {
            // Write delivery evidence before shutdown, which is reported separately.
            writeReport();
            if(context!=null) { context.close(); checks.add(Map.of("name","Spring context close returned","status","PASS","detail","Does not prove all JVM workers terminated")); }
            if(services!=null)services.close();
            writeReport();
        }
        assertFalse("See acceptance.json",checks.stream().anyMatch(c->"FAIL".equals(c.get("status"))));
    }
    private void writeReport() throws Exception {
        var report=new LinkedHashMap<String,Object>();report.put("checks",checks);report.put("deliveries",deliveries);
        report.put("verifiedAt",java.time.Instant.now().toString());report.put("javaVersion",System.getProperty("java.version"));
        new ObjectMapper().writerWithDefaultPrettyPrinter().writeValue(Path.of(System.getProperty("fixture.reportFile","../acceptance.json")).toFile(),report);
    }
}
