<#assign StudioBuildUtils=statics['org.ikasan.studio.core.StudioBuildUtils']>
<#assign className=StudioBuildUtils.toPascalCase(flowElement.getPropertyValue('userImplementedClassName'))>
package ${studioPackageTag};

import org.ikasan.spec.component.transformation.Converter;
import org.ikasan.spec.component.transformation.TransformationException;

/** ${conversionRecipe.displayName}. Unsupported content is rejected; provide a custom mapping when needed. */
@org.springframework.stereotype.Component("${studioPackageTag}.${className}")
public class ${className} implements Converter<${conversionRecipe.sourceType}, ${conversionRecipe.targetType}> {
    @Override
    public ${conversionRecipe.targetType} convert(${conversionRecipe.sourceType} source) throws TransformationException {
        if (source == null) throw new TransformationException("Cannot convert null content");
        try {
            String filename = "${((flowElement.getPropertyValue('recipeFilename'))!'message.dat')?j_string}";
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
