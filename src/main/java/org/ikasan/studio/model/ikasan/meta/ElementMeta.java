package org.ikasan.studio.model.ikasan.meta;

import org.ikasan.studio.StudioUtils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Holds the meta data for all Ikasan elements
 * enum is used as a convenient singleton implementation
 */
public enum ElementMeta implements Serializable {
    INSTANCE;
    private final List<Element> elements;

    ElementMeta() {
        elements = new ArrayList<>();
//        StudioUtils.refreshComponentLibrary("");
    }

    public static final List<Element> getElementMeta() {
        return INSTANCE.elements;
    }
}
