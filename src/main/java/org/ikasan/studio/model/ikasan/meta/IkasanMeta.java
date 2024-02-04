
package org.ikasan.studio.model.ikasan.meta;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        property = "name",
        defaultImpl = IkasanComponentMeta.class,
        visible = true
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = IkasanComponentMeta.class),
        @JsonSubTypes.Type(value = IkasanExceptionResolutionMeta.class, name = "Exception Resolver")
})

public interface IkasanMeta {
}
