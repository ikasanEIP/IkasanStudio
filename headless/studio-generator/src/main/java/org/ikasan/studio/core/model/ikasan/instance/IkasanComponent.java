package org.ikasan.studio.core.model.ikasan.instance;

/**
 * Marker interface for all Ikasan components.
 */
public interface IkasanComponent {
    /**
     * Returns the identity of the component
     * For flows and modules this is the name, otherwise this is componenName
     * @return the identity of the component.
     */
    String getIdentity();
}
