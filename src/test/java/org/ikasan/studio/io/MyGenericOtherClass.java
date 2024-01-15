package org.ikasan.studio.io;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

@Data
@Builder
@Jacksonized
@AllArgsConstructor
public class MyGenericOtherClass {
    String field1SubclassString;
}
