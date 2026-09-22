package fixture.eventsource;
@org.springframework.stereotype.Component("fixture.eventsource.PacedEvents")
public class PacedEvents implements org.ikasan.component.endpoint.consumer.api.spec.EndpointEventProvider<String> {
 private int count; private String pending;
 public String getEvent(){try { Thread.sleep(1000); } catch(InterruptedException e){Thread.currentThread().interrupt();return null;} if(pending!=null){String p=pending;pending=null;return p;}return "EVENT-"+(++count);}
 public void rollback(){pending="EVENT-"+count;}
}
