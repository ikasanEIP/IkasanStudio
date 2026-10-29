            if (payload.size() != 1 || payload.get(0) == null) throw new TransformationException("This recipe requires exactly one local file; split the batch or use a custom converter");
            java.nio.file.Path path = payload.get(0).toPath();
            if (java.nio.file.Files.size(path) > 16 * 1024 * 1024) throw new TransformationException("Local file exceeds the recipe's 16 MiB limit; use a streaming custom converter");
            Object body = java.nio.file.Files.readAllBytes(path);
            filename = path.getFileName().toString();
