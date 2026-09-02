package org.ikasan.studio.core.persistence.json;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.ikasan.studio.core.model.ikasan.instance.BasicElement;
import org.ikasan.studio.core.model.ikasan.instance.ExceptionResolver;
import org.ikasan.studio.core.model.ikasan.instance.Flow;
import org.ikasan.studio.core.model.ikasan.instance.FlowElement;
import org.ikasan.studio.core.model.ikasan.instance.Module;

/** Owns all Jackson adapter registration so the domain model remains persistence-agnostic. */
public final class StudioJson {
    private StudioJson() {
    }

    public static ObjectMapper newObjectMapper() {
        SimpleModule model = new SimpleModule("Ikasan Studio model JSON");
        model.addSerializer(BasicElement.class, new BasicElementSerializer());
        model.addSerializer(FlowElement.class, new FlowElementSerializer());
        model.addSerializer(ExceptionResolver.class, new ExceptionResolverSerializer());
        model.addSerializer(Flow.class, new FlowSerializer());
        model.addSerializer(Module.class, new ModuleSerializer());
        model.addDeserializer(Module.class, new ModuleDeserializer());

        return new ObjectMapper()
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .registerModule(model);
    }
}
