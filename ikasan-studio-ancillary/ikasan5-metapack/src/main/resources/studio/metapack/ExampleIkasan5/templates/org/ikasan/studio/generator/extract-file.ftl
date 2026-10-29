            Object body = payload.getContent();
            String originalFilename = payload.getAttribute("fileName");
            if (originalFilename != null && !originalFilename.isBlank()) filename = originalFilename;
