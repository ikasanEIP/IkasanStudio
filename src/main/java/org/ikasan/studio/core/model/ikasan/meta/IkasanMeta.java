
package org.ikasan.studio.core.model.ikasan.meta;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * IkasanMeta is the base interface for all Ikasan metadata objects.
 * It is annotated for Jackson polymorphic deserialization, allowing JSON to be mapped to different subclasses
 * (ComponentMeta, ComponentTypeMeta, ExceptionResolverMeta) based on the name property.
 * This enables flexible handling of various metadata types in the application.
 */
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        property = "name",
        defaultImpl = ComponentMeta.class,
        visible = true
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = ComponentMeta.class),
        @JsonSubTypes.Type(value = ComponentTypeMeta.class),
        @JsonSubTypes.Type(value = ExceptionResolverMeta.class, name = "Exception Resolver")
})


public interface IkasanMeta {
}
