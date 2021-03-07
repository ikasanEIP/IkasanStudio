package org.ikasan.studio.model.ikasan;

public class IkasanConsumer {
    private String name;
    private String description;

    public IkasanConsumer(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
