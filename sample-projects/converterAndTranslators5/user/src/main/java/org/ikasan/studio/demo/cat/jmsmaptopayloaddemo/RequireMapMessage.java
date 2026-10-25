package org.ikasan.studio.demo.cat.jmsmaptopayloaddemo;

import jakarta.jms.MapMessage;
import jakarta.jms.Message;
import org.ikasan.spec.component.transformation.Converter;
import org.ikasan.spec.component.transformation.TransformationException;
import org.springframework.stereotype.Component;

/** A raw JMS consumer can receive any message kind: fail clearly before the map-specific converter. */
@Component("org.ikasan.studio.demo.cat.jmsmaptopayloaddemo.RequireMapMessage")
public class RequireMapMessage implements Converter<Message, MapMessage> {
    @Override
    public MapMessage convert(Message message) {
        if (message instanceof MapMessage) {
            return (MapMessage) message;
        }
        throw new TransformationException("Payload map demo expects a JMS MapMessage, but received "
                + (message == null ? "null" : message.getClass().getName()));
    }
}
