            if (filename.isBlank()) throw new TransformationException("An attachment filename is required");
            org.ikasan.component.endpoint.email.producer.DefaultEmailPayload result = (org.ikasan.component.endpoint.email.producer.DefaultEmailPayload) org.ikasan.component.endpoint.email.producer.EmailPayload.newInstance();
            result.setEmailBody("${((flowElement.getPropertyValue('recipeEmailBody'))!'Please see the attached file.')?j_string}");
            result.addAttachment(filename, "${((flowElement.getPropertyValue('recipeMediaType'))!'application/octet-stream')?j_string}", bytes(body, charset));
            return result;
