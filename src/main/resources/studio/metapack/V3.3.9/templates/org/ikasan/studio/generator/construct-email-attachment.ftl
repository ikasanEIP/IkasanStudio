            // Independent of the FTP/SFTP fallback in construct-file.ftl - an email attachment isn't written to
            // a directory that could reject a repeated name, so there's no collision to avoid here.
            if (filename.isBlank()) filename = "message.dat";
            org.ikasan.component.endpoint.email.producer.DefaultEmailPayload result = (org.ikasan.component.endpoint.email.producer.DefaultEmailPayload) org.ikasan.component.endpoint.email.producer.EmailPayload.newInstance();
            result.setEmailBody("${((flowElement.getPropertyValue('recipeEmailBody'))!'Please see the attached file.')?j_string}");
            result.addAttachment(filename, "${((flowElement.getPropertyValue('recipeMediaType'))!'application/octet-stream')?j_string}", bytes(body, charset));
            return result;
