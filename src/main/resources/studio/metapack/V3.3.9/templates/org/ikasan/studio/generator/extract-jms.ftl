            Object body;
            if (payload instanceof javax.jms.TextMessage) {
                body = ((javax.jms.TextMessage) payload).getText();
            } else if (payload instanceof javax.jms.BytesMessage) {
                javax.jms.BytesMessage jmsMessage = (javax.jms.BytesMessage) payload;
                jmsMessage.reset();
                try {
                    long length = jmsMessage.getBodyLength();
                    if (length < 0 || length > 16 * 1024 * 1024) throw new TransformationException("JMS body exceeds the recipe's 16 MiB limit; use a streaming custom converter");
                    java.io.ByteArrayOutputStream output = new java.io.ByteArrayOutputStream((int) length);
                    byte[] buffer = new byte[8192];
                    int count;
                    while ((count = jmsMessage.readBytes(buffer)) != -1) {
                        if (count == 0 || output.size() + count > 16 * 1024 * 1024) throw new TransformationException("Invalid or oversized JMS byte stream");
                        output.write(buffer, 0, count);
                    }
                    body = output.toByteArray();
                } finally {
                    jmsMessage.reset();
                }
            } else if (payload instanceof javax.jms.MapMessage) {
                javax.jms.MapMessage jmsMessage = (javax.jms.MapMessage) payload;
                java.util.Map<String, Object> values = new java.util.LinkedHashMap<>();
                java.util.Enumeration<?> names = jmsMessage.getMapNames();
                while (names.hasMoreElements()) {
                    String name = (String) names.nextElement();
                    values.put(name, jmsMessage.getObject(name));
                }
                body = values;
            } else {
                throw new TransformationException("Unsupported JMS message; use a custom converter for ObjectMessage or StreamMessage");
            }
