<#assign StudioBuildUtils=statics['org.ikasan.studio.core.StudioBuildUtils']>
<#assign className=StudioBuildUtils.toPascalCase(flowElement.getPropertyValue('userImplementedClassName'))>
package ${studioPackageTag};

import org.ikasan.component.endpoint.email.producer.DefaultEmailPayload;
import org.ikasan.component.endpoint.email.producer.EmailPayload;
import org.ikasan.spec.component.transformation.Converter;
import org.ikasan.spec.component.transformation.TransformationException;

import jakarta.jms.BytesMessage;
import jakarta.jms.JMSException;
import jakarta.jms.MapMessage;
import jakarta.jms.Message;
import jakarta.jms.ObjectMessage;
import jakarta.jms.TextMessage;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.Map;

/** Converts common JMS message bodies into the EmailPayload required by an Email Producer. */
@org.springframework.stereotype.Component("${studioPackageTag}.${className}")
public class ${className} implements Converter<Message, EmailPayload>
{
    @Override
    public EmailPayload convert(Message message) throws TransformationException
    {
        try {
            DefaultEmailPayload emailPayload = new DefaultEmailPayload();
            emailPayload.setEmailBody(extractBody(message));
            // TODO Add attachments when required, for example:
            // emailPayload.addAttachment("report.pdf", "application/pdf", attachmentBytes);
            return emailPayload;
        } catch (JMSException exception) {
            throw new TransformationException("Unable to convert JMS message to an email payload", exception);
        }
    }

    private String extractBody(Message message) throws JMSException
    {
        if (message instanceof TextMessage) {
            return ((TextMessage) message).getText();
        }
        if (message instanceof BytesMessage) {
            BytesMessage bytesMessage = (BytesMessage) message;
            long length = bytesMessage.getBodyLength();
            if (length > Integer.MAX_VALUE) {
                throw new TransformationException("JMS BytesMessage is too large to hold in memory: " + length);
            }
            byte[] bytes = new byte[(int) length];
            bytesMessage.reset();
            bytesMessage.readBytes(bytes);
            return new String(bytes, StandardCharsets.UTF_8);
        }
        if (message instanceof MapMessage) {
            MapMessage mapMessage = (MapMessage) message;
            Map<String, Object> values = new LinkedHashMap<>();
            Enumeration<String> names = mapMessage.getMapNames();
            while (names.hasMoreElements()) {
                String name = names.nextElement();
                values.put(name, mapMessage.getObject(name));
            }
            // TODO Replace this with JSON, XML, or another recipient-specific format.
            return values.toString();
        }
        if (message instanceof ObjectMessage) {
            Object value = ((ObjectMessage) message).getObject();
            if (value instanceof byte[]) {
                return new String((byte[]) value, StandardCharsets.UTF_8);
            }
            return String.valueOf(value);
        }
        // TODO Add explicit handling if the flow deliberately uses StreamMessage or a custom type.
        return message.toString();
    }
}
