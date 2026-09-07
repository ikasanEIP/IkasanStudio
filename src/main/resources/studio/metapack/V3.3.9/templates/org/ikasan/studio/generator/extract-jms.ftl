            Object body;
            if (source instanceof javax.jms.TextMessage) {
                body = ((javax.jms.TextMessage) source).getText();
            } else if (source instanceof javax.jms.BytesMessage) {
                javax.jms.BytesMessage message = (javax.jms.BytesMessage) source;
                message.reset();
                try {
                    long length = message.getBodyLength();
                    if (length < 0 || length > 16 * 1024 * 1024) throw new TransformationException("JMS body exceeds the recipe's 16 MiB limit; use a streaming custom converter");
                    java.io.ByteArrayOutputStream output = new java.io.ByteArrayOutputStream((int) length);
                    byte[] buffer = new byte[8192];
                    int count;
                    while ((count = message.readBytes(buffer)) != -1) {
                        if (count == 0 || output.size() + count > 16 * 1024 * 1024) throw new TransformationException("Invalid or oversized JMS byte stream");
                        output.write(buffer, 0, count);
                    }
                    body = output.toByteArray();
                } finally {
                    message.reset();
                }
            } else if (source instanceof javax.jms.MapMessage) {
                javax.jms.MapMessage message = (javax.jms.MapMessage) source;
                java.util.Map<String, Object> values = new java.util.LinkedHashMap<>();
                java.util.Enumeration<?> names = message.getMapNames();
                while (names.hasMoreElements()) {
                    String name = (String) names.nextElement();
                    values.put(name, message.getObject(name));
                }
                body = values;
            } else {
                throw new TransformationException("Unsupported JMS message; use a custom converter for ObjectMessage or StreamMessage");
            }
