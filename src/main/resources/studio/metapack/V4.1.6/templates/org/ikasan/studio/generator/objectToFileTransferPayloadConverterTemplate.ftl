<#assign StudioBuildUtils=statics['org.ikasan.studio.core.StudioBuildUtils']>
<#assign className=StudioBuildUtils.toPascalCase(flowElement.getPropertyValue('userImplementedClassName'))>
package ${studioPackageTag};

import org.ikasan.filetransfer.FilePayloadAttributeNames;
import org.ikasan.filetransfer.Payload;
import org.ikasan.filetransfer.component.DefaultPayload;
import org.ikasan.spec.component.transformation.Converter;
import org.ikasan.spec.component.transformation.TransformationException;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * Wraps content already extracted by a JMS consumer with autoContentConversion enabled in the Payload required
 * by FTP and SFTP producers. Review the fallback representation and filename for your integration contract.
 */
@org.springframework.stereotype.Component("${studioPackageTag}.${className}")
public class ${className} implements Converter<Object, Payload>
{
    @Override
    public Payload convert(Object source) throws TransformationException
    {
        if (source instanceof Payload existingPayload) {
            return existingPayload;
        }

        byte[] content;
        if (source == null) {
            content = new byte[0];
        } else if (source instanceof byte[] bytes) {
            content = bytes;
        } else if (source instanceof CharSequence text) {
            content = text.toString().getBytes(StandardCharsets.UTF_8);
        } else {
            // TODO Replace this fallback when Map/Object content has a defined JSON, XML, or binary wire format.
            content = String.valueOf(source).getBytes(StandardCharsets.UTF_8);
        }

        Payload payload = new DefaultPayload(UUID.randomUUID().toString(), content);
        // TODO Choose or derive the filename required by the receiving system.
        payload.setAttribute(FilePayloadAttributeNames.FILE_NAME, "message.dat");
        return payload;
    }
}
