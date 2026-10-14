            org.ikasan.component.endpoint.email.producer.EmailPayload result = org.ikasan.component.endpoint.email.producer.EmailPayload.newInstance();
            result.setEmailBody(text(body, charset));
            return result;
