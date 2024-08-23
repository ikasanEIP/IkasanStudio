package org.ikasan.studio.core.model.ikasan.instance;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.Objects;

@Getter
@Setter
@ToString
public class Decorator {
    public enum TYPE { Wiretap, LogWiretap};
    public enum POSITION { BEFORE, AFTER };

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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Decorator decorator)) return false;
        return Objects.equals(type, decorator.type) && Objects.equals(name, decorator.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, name);
    }
}
