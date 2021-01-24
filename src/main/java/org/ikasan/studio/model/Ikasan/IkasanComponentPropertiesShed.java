package org.ikasan.studio.model.Ikasan;

import java.util.Map;
import java.util.TreeMap;

/**
 * Represents the actual configured properties for the component.
 *
 * This will always include
 *
 *   * All of the mandatory properties
 *   * All of the non-mandatory properties that have been given values
 *
 * Since it holds mandatory and non-mandatory properties, it makes sense to always present
 * the mandatory properties first, hence it has its own comparator.
 *
 * A Map is preferable to a list so that we can ensure no propertye is accidentally duplicated.
 *
 */
public class IkasanComponentPropertiesShed {
    Map<String, IkasanComponentPropertyMeta> componentProperties = new TreeMap<>();


}
