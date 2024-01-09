package org.ikasan.studio.model.ikasan.meta;

import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

@Data
@Builder
@Jacksonized
public class Element {
      String elementCategory;
      Boolean isBespokeClass;
      String standardBuilderMethod;
      String componentType;
}
