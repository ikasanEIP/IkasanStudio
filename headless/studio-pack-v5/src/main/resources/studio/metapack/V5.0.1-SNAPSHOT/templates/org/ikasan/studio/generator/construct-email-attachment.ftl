            // Independent of the FTP/SFTP fallback in construct-file.ftl - an email attachment isn't written to
            // a directory that could reject a repeated name, so there's no collision to avoid here.
            if (filename.isBlank()) filename = "message.dat";
            // Computed eagerly, independent of whether the (commented out, see below) attachment call is ever
            // uncommented - this is what rejects unsupported content (anything but String/byte[]) for this
            // recipe; losing it just because the attachment itself is off by default would let unsupported
            // content silently through instead of failing fast.
            byte[] attachmentContent = bytes(body, charset);
            org.ikasan.component.endpoint.email.producer.DefaultEmailPayload result = (org.ikasan.component.endpoint.email.producer.DefaultEmailPayload) org.ikasan.component.endpoint.email.producer.EmailPayload.newInstance();
<#if flowElement.getPropertyValue('recipeEmailBody')??>
            result.setEmailBody("${flowElement.getPropertyValue('recipeEmailBody')?j_string}");
<#else>
            // No attachment is actually sent below unless the Email Producer's own "hasAttachments" property is
            // also enabled - see the warning further down - so the body only names the file, it never claims one
            // is attached.
            result.setEmailBody("Source file: " + filename);
</#if>
            // WARNING: the Email Producer's "hasAttachments" property defaults to false and is the ONLY thing
            // that gates whether an attachment is actually sent - it is independent of this Converter, so leaving
            // it false while this line is active would silently send the email without the attachment, no error
            // raised anywhere. Uncomment the line below AND set "hasAttachments" to true on the Email Producer
            // component for the attachment to actually be delivered.
            // result.addAttachment(filename, "${((flowElement.getPropertyValue('recipeMediaType'))!'application/octet-stream')?j_string}", attachmentContent);
            return result;
