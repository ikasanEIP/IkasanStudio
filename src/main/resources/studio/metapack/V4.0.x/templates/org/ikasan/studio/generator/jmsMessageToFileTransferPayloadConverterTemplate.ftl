<#assign StudioBuildUtils=statics['org.ikasan.studio.core.StudioBuildUtils']>
<#assign className=StudioBuildUtils.toPascalCase(flowElement.getPropertyValue('userImplementedClassName'))>
package ${studioPackageTag};

import org.ikasan.filetransfer.FilePayloadAttributeNames;
import org.ikasan.filetransfer.Payload;
import org.ikasan.filetransfer.component.DefaultPayload;
import org.ikasan.spec.component.transformation.Converter;
import org.ikasan.spec.component.transformation.TransformationException;

import javax.jms.BytesMessage;
import javax.jms.JMSException;
import javax.jms.MapMessage;
import javax.jms.Message;
import javax.jms.ObjectMessage;
import javax.jms.TextMessage;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Converts common JMS message bodies into the Payload required by FTP and SFTP producers.
 * Review filenameFor() and the MapMessage/ObjectMessage representation for your integration contract.
 */
@org.springframework.stereotype.Component("${studioPackageTag}.${className}")
public class ${className} implements Converter<Message, Payload>
{
    @Override
    public Payload convert(Message message) throws TransformationException
    {
        try {
            byte[] content = extractContent(message);
            String id = message.getJMSMessageID();
            if (id == null || id.isBlank()) {
                id = UUID.randomUUID().toString();
            }
            Payload payload = new DefaultPayload(id, content);
            payload.setAttribute(FilePayloadAttributeNames.FILE_NAME, filenameFor(message));
            return payload;
        } catch (JMSException exception) {
            throw new TransformationException("Unable to convert JMS message to a file-transfer payload", exception);
        }
    }

    private byte[] extractContent(Message message) throws JMSException
    {
        if (message instanceof TextMessage textMessage) {
            String text = textMessage.getText();
            return text == null ? new byte[0] : text.getBytes(StandardCharsets.UTF_8);
        }
        if (message instanceof BytesMessage bytesMessage) {
            long length = bytesMessage.getBodyLength();
            if (length > Integer.MAX_VALUE) {
                throw new TransformationException("JMS BytesMessage is too large to hold in memory: " + length);
            }
            byte[] bytes = new byte[(int) length];
            bytesMessage.reset();
            bytesMessage.readBytes(bytes);
            return bytes;
        }
        if (message instanceof MapMessage mapMessage) {
            Map<String, Object> values = new LinkedHashMap<>();
            Enumeration<String> names = mapMessage.getMapNames();
            while (names.hasMoreElements()) {
                String name = names.nextElement();
                values.put(name, mapMessage.getObject(name));
            }
            // TODO Replace this representation if the receiving system requires JSON, XML, or another format.
            return values.toString().getBytes(StandardCharsets.UTF_8);
        }
        if (message instanceof ObjectMessage objectMessage) {
            Object value = objectMessage.getObject();
            if (value instanceof byte[] bytes) {
                return bytes;
            }
            // TODO Replace this representation when the contained object has a defined wire format.
            return String.valueOf(value).getBytes(StandardCharsets.UTF_8);
        }
        throw new TransformationException("Unsupported JMS message type: " + message.getClass().getName());
    }

    private String filenameFor(Message message) throws JMSException
    {
        if (message.propertyExists(FilePayloadAttributeNames.FILE_NAME)) {
            String supplied = message.getStringProperty(FilePayloadAttributeNames.FILE_NAME);
            if (supplied != null && !supplied.isBlank()) {
                return supplied;
            }
        }
        // TODO Choose the filename required by the receiving system.
        return "message.dat";
    }
}
