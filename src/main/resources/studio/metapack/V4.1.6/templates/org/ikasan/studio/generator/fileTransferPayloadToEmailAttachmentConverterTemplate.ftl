<#assign StudioBuildUtils=statics['org.ikasan.studio.core.StudioBuildUtils']>
<#assign className=StudioBuildUtils.toPascalCase(flowElement.getPropertyValue('userImplementedClassName'))>
package ${studioPackageTag};

import org.ikasan.filetransfer.Payload;
import org.ikasan.component.endpoint.email.producer.DefaultEmailPayload;
import org.ikasan.component.endpoint.email.producer.EmailPayload;
import org.ikasan.spec.component.transformation.Converter;
import org.ikasan.spec.component.transformation.TransformationException;

/** Preserves an incoming FTP/SFTP file as an email attachment without decoding its bytes. */
@org.springframework.stereotype.Component("${studioPackageTag}.${className}")
public class ${className} implements Converter<Payload, EmailPayload>
{
    @Override
    public EmailPayload convert(Payload payload) throws TransformationException
    {
        if (payload == null || payload.getContent() == null) {
            throw new TransformationException("Cannot attach a file without payload content");
        }
        String filename = payload.getAttribute("fileName");
        if (filename == null || filename.isBlank()) {
            filename = "attachment.dat";
        }
        DefaultEmailPayload email = (DefaultEmailPayload) EmailPayload.newInstance();
        // Review the body and attachment media type for your integration.
        email.setEmailBody("Please see the attached file.");
        email.addAttachment(filename, "application/octet-stream", payload.getContent());
        return email;
    }
}
