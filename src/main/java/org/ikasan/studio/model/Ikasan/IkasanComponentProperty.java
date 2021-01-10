package org.ikasan.studio.model.Ikasan;

public class IkasanComponentProperty {
    String option;
    Class type;
    String helpText;

    public IkasanComponentProperty(String option, Class type, String helpText) {
        this.option = option;
        this.type = type;
        this.helpText = helpText;
    }
}
