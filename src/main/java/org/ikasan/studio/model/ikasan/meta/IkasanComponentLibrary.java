package org.ikasan.studio.model.ikasan.meta;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;

/**
 * This class aggregates all the defined Ikasan components
 */
@Data
@AllArgsConstructor
public class IkasanComponentLibrary {
    Map<String, IkasanComponentMetan> ikasanComponentMetanMap = new HashMap<>();
}
