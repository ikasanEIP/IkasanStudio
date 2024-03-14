
package org.ikasan.studio.model.ikasan.meta;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        property = "name",
        defaultImpl = ComponentMeta.class,
        visible = true
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = ComponentMeta.class),
        @JsonSubTypes.Type(value = ComponentTypeMeta.class),
        @JsonSubTypes.Type(value = ExceptionResolutionMeta.class, name = "Exception Resolver")
})

public interface IkasanMeta {
}
