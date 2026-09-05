<#assign StudioBuildUtils=statics['org.ikasan.studio.core.StudioBuildUtils']>
<#assign className=StudioBuildUtils.toPascalCase(flowElement.getPropertyValue('userImplementedClassName'))>
package ${studioPackageTag};

import org.ikasan.filetransfer.Payload;
import org.ikasan.spec.component.transformation.Converter;
import org.ikasan.spec.component.transformation.TransformationException;

/**
 * Extracts the raw bytes from an FTP/SFTP Payload for producers (e.g. JMS) that accept byte[] content.
 */
@org.springframework.stereotype.Component("${studioPackageTag}.${className}")
public class ${className} implements Converter<Payload, byte[]>
{
    @Override
    public byte[] convert(Payload source) throws TransformationException
    {
        return source == null ? new byte[0] : source.getContent();
    }
}
