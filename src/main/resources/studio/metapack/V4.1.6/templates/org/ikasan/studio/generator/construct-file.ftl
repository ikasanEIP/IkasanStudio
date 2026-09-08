<#if flowElement.getContainingFlow()??>
<#assign filenameFlowPart = (flowElement.getContainingFlow().getIdentity()!'flow')?replace('[^A-Za-z0-9._-]+', '_', 'r')>
<#else>
<#assign filenameFlowPart = 'flow'>
</#if>
<#assign filenameConverterPart = (flowElement.getComponentName()!'converter')?replace('[^A-Za-z0-9._-]+', '_', 'r')>
            // No filename preserved from upstream and none explicitly configured (see recipeFilename) - many
            // FTP/SFTP servers, including strict test emulators, refuse to overwrite an existing file and would
            // stop the flow the moment a fixed/reused name collided with one already delivered. Generate one
            // that's unique on every call instead: it also carries the flow and converter name, so with several
            // converters delivering into the same directory you can tell which one produced which file, and the
            // timestamp doubles as a delivery history.
            if (filename.isBlank()) {
                filename = "${filenameFlowPart?j_string}-${filenameConverterPart?j_string}-"
                        + java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss-SSS").format(java.time.LocalDateTime.now())
                        + "-" + java.util.UUID.randomUUID() + ".dat";
            }
            org.ikasan.filetransfer.component.DefaultPayload result = new org.ikasan.filetransfer.component.DefaultPayload(java.util.UUID.randomUUID().toString(), bytes(body, charset));
            result.setAttribute(org.ikasan.filetransfer.FilePayloadAttributeNames.FILE_NAME, filename);
            return result;
