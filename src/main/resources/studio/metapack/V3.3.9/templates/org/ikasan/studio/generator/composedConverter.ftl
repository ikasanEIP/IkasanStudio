<#assign StudioBuildUtils=statics['org.ikasan.studio.core.StudioBuildUtils']>
<#assign className=StudioBuildUtils.toPascalCase(flowElement.getPropertyValue('userImplementedClassName'))>
package ${studioPackageTag};

import org.ikasan.spec.component.transformation.Converter;
import org.ikasan.spec.component.transformation.TransformationException;

/** ${conversionRecipe.displayName?html}. Unsupported content is rejected; provide a custom mapping when needed. */
@org.springframework.stereotype.Component("${studioPackageTag}.${className}")
public class ${className} implements Converter<${conversionRecipe.sourceType}, ${conversionRecipe.targetType}> {
private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(${className}.class);

<#if conversionRecipe.constructionTemplate == 'construct-file.ftl'>
<#if flowElement.getContainingFlow()??>
<#assign filenameFlowPart = (flowElement.getContainingFlow().getIdentity()!'flow')?replace('[^A-Za-z0-9._-]+', '_', 'r')>
<#else>
<#assign filenameFlowPart = 'flow'>
</#if>
<#assign filenameConverterPart = (flowElement.getComponentName()!'converter')?replace('[^A-Za-z0-9._-]+', '_', 'r')>
    /** Prefix for automatically generated output filenames; fixed or incoming filenames take precedence. */
    public static final String FILENAME_PREFIX = "${filenameFlowPart?j_string}-${filenameConverterPart?j_string}-";
</#if>

    @Override
    public ${conversionRecipe.targetType} convert(${conversionRecipe.sourceType} payload) throws TransformationException {
// Uncomment for diagnostics without logging message contents.
// LOG.debug("Processing payload type {}", payload == null ? "null" : payload.getClass().getName());
        if (payload == null) throw new TransformationException("Cannot convert null content");
        try {
            // Blank unless the developer has explicitly configured a fixed name - each construction template
            // (see construct-file.ftl/construct-email-attachment.ftl) applies its own independent fallback when
            // this is still blank at the point it's used, since FTP/SFTP and email attachments need different
            // fallback behaviour. Extraction templates (e.g. extract-file.ftl) may also overwrite this with a
            // filename preserved from the incoming payload, which always takes priority over both.
            String filename = "${((flowElement.getPropertyValue('recipeFilename'))!'')?j_string}";
            java.nio.charset.Charset charset = java.nio.charset.Charset.forName("<#if conversionRecipe.configurationProperties?seq_contains('recipeCharset')>${((flowElement.getPropertyValue('recipeCharset'))!'UTF-8')?j_string}<#else>UTF-8</#if>");
<#include conversionRecipe.extractionTemplate>
<#include conversionRecipe.constructionTemplate>
        } catch (TransformationException e) {
            throw e;
        } catch (Exception e) {
            throw new TransformationException("Conversion failed; check the recipe and incoming content type", e);
        }
    }

    private byte[] bytes(Object body, java.nio.charset.Charset charset) throws Exception {
        if (body instanceof byte[]) return (byte[]) body;
        if (body instanceof String) {
            java.nio.ByteBuffer encoded = charset.newEncoder().encode(java.nio.CharBuffer.wrap((String) body));
            byte[] result = new byte[encoded.remaining()];
            encoded.get(result);
            return result;
        }
        throw new TransformationException("Expected String or byte[] content; select a custom converter for structured objects");
    }

    private String text(Object body, java.nio.charset.Charset charset) throws Exception {
        if (body instanceof String) return (String) body;
        return charset.newDecoder().decode(java.nio.ByteBuffer.wrap(bytes(body, charset))).toString();
    }
}
