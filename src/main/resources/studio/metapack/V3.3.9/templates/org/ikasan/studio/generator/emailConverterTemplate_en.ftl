<#assign StudioBuildUtils=statics['org.ikasan.studio.core.StudioBuildUtils']>
<#assign fromType=StudioBuildUtils.toJavaTypeLiteral(flowElement.getPropertyValue('fromType'))>
package ${studioPackageTag};

/**
* Builds the EmailPayload the Email Producer needs to send a message. EmailPayload.newInstance() always
* returns a DefaultEmailPayload - cast to it to reach setEmailBody(...) and addAttachment(name, type, bytes),
* neither of which are on the EmailPayload interface itself.
*
* Unlike Broker, this component detects "full event" mode safely: set the input type below to the real payload
* type (e.g. String) to receive just the payload, or to org.ikasan.spec.flow.FlowEvent to receive the full event
* (call payload.getPayload() inside convert() to get the payload out) - Ikasan checks your class for a literal
* convert(FlowEvent) method rather than catching a runtime cast failure, so java.lang.Object is not a trap here.
*
* This is an auto generated stub. The user is expected to fill in the details of the conversion below.
* This stub will not be overwritten unless the overwrite checkbox is explicitly selected.
*/

import org.ikasan.component.endpoint.email.producer.DefaultEmailPayload;
import org.ikasan.component.endpoint.email.producer.EmailPayload;
import org.ikasan.spec.component.transformation.Converter;
import org.ikasan.spec.component.transformation.TransformationException;

@org.springframework.stereotype.Component("${studioPackageTag}.${StudioBuildUtils.toPascalCase(flowElement.getPropertyValue('userImplementedClassName'))}")
public class ${StudioBuildUtils.toPascalCase(flowElement.getPropertyValue('userImplementedClassName'))} implements Converter<${fromType}, EmailPayload>
{
public EmailPayload convert(${fromType} payload) throws TransformationException
{
DefaultEmailPayload emailPayload = (DefaultEmailPayload) EmailPayload.newInstance();
emailPayload.setEmailBody(payload.toString());
// TODO review the default body above, and populate any attachments, from the incoming payload, e.g.
// emailPayload.addAttachment("report.pdf", "application/pdf", attachmentBytes);
return emailPayload;
}
}