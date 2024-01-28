
package org.ikasan.studio.model.ikasan.meta;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.DEDUCTION)
@JsonSubTypes({ @JsonSubTypes.Type(IkasanComponentMeta.class), @JsonSubTypes.Type(IkasanExceptionResolutionMeta.class)})
public interface IkasanComponentMetaIfc {
}
