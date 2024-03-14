package org.ikasan.studio.model.ikasan.meta;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

@Data
@Builder
@Jacksonized
@AllArgsConstructor
public class ComponentTypeMeta {
    @lombok.NonNull
    Integer displayOrder;
    @lombok.NonNull
    String componentType;
    @lombok.NonNull
    String componentShortType;
}
