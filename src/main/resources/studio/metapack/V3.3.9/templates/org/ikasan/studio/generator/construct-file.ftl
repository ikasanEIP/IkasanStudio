            if (filename.isBlank()) throw new TransformationException("A filename is required for file transfer");
            org.ikasan.filetransfer.component.DefaultPayload result = new org.ikasan.filetransfer.component.DefaultPayload(java.util.UUID.randomUUID().toString(), bytes(body, charset));
            result.setAttribute(org.ikasan.filetransfer.FilePayloadAttributeNames.FILE_NAME, filename);
            return result;
