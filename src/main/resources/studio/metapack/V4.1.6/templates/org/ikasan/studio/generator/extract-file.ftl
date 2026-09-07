            Object body = source.getContent();
            String originalFilename = source.getAttribute("fileName");
            if (originalFilename != null && !originalFilename.isBlank()) filename = originalFilename;
