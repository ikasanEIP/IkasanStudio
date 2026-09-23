package fixture.fanout;
@org.springframework.stereotype.Component("fixture.fanout.ControlledInput")
public class ControlledInput implements org.ikasan.spec.component.endpoint.Consumer<org.ikasan.spec.event.EventListener,org.ikasan.spec.event.EventFactory> {
 private org.ikasan.spec.event.EventListener listener; private org.ikasan.spec.event.EventFactory factory; private volatile boolean running;
 public void setListener(org.ikasan.spec.event.EventListener l){listener=l;} public void setEventFactory(org.ikasan.spec.event.EventFactory f){factory=f;}
 public org.ikasan.spec.event.EventFactory getEventFactory(){return factory;} public void start(){running=true;} public void stop(){running=false;} public boolean isRunning(){return running;}
 public void send(fixture.Order order){if(!running)throw new IllegalStateException("Not running");listener.invoke(factory.newEvent(order.getId(),order));}
}
