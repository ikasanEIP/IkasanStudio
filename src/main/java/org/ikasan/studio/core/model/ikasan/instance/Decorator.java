package org.ikasan.studio.core.model.ikasan.instance;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Decorator {
    private String type;
    private String name;
    private String configurationId;
    private boolean configurable;

    @Builder(builderMethodName = "decoratorBuilder")
    protected Decorator(String type, String name, String configurationId, boolean configurable) {
        this.type = type;
        this.name = name;
        this.configurationId = configurationId;
        this.configurable = configurable;
    }
}
