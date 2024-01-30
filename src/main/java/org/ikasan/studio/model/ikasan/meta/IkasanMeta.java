
package org.ikasan.studio.model.ikasan.meta;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "name",
        defaultImpl = IkasanComponentMeta.class
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = IkasanComponentMeta.class, name = ""),
        @JsonSubTypes.Type(value = IkasanExceptionResolutionMeta.class, name = "Exception Resolver")
})

public interface IkasanMeta {
}
