package org.ikasan.studio.model.ikasan.meta;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

import java.util.Map;

@Data
@SuperBuilder
@Jacksonized
@AllArgsConstructor
public class ExceptionAction {
    String actionName;
    Map<String, String> actionProperties;
}
