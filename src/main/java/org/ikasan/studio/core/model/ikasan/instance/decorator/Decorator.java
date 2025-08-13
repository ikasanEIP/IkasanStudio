package org.ikasan.studio.core.model.ikasan.instance.decorator;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.Objects;

@Getter
@Setter
@ToString
public class Decorator {
    private DECORATOR_TYPE type;
    @JsonIgnore
    private DECORATOR_POSITION position;
    private String name;
    private String configurationId;
    private boolean configurable;

    /**
     * Create a new decorator i.e. A wiretap, log wiretap designated as before the current compponent (e.g. broker) or after it.
     * It is essential that the IDE does not encounter an exception, the builder will tollarate invalid data
     * but will identify the decorator as invalid, it is left for the consumer of the builder to call isValid
     * before using the decorator.
     * @param type e.g. Wiretap.
     * @param name e.g. Contains its position and the name of the component it is decorating, e.g. "BEFORE Broker", "AFTER Producer".
     * @param configurationId for the component.
     * @param configurable true if this is exposed via blue console.
     */
    @Builder(builderMethodName = "decoratorBuilder")
    protected Decorator(String type, String name, String configurationId, boolean configurable) {
        this.type = DECORATOR_TYPE.safeValueOf(type);
        String prefix = name != null && !name.isBlank() ? name.split(" ")[0] : "BEFORE";
        this.position = DECORATOR_POSITION.safeValueOf(prefix);
        this.name = name;
        this.configurationId = configurationId;
        this.configurable = configurable;
    }

    @JsonIgnore
    public boolean isInvalid() {
        return type.equals(DECORATOR_TYPE.Unknown) || position.equals(DECORATOR_POSITION.UNKNOWN);
    }

    @JsonIgnore
    public boolean isValid() {
        return !isInvalid();
    }

    @JsonIgnore
    public boolean isWiretap() {
        return DECORATOR_TYPE.Wiretap.equals(type);
    }

    @JsonIgnore
    public boolean isLogWiretap() {
        return DECORATOR_TYPE.LogWiretap.equals(type);
    }

    @JsonIgnore
    public boolean isBefore() {
        return DECORATOR_POSITION.BEFORE.equals(position);
    }

    @JsonIgnore
    public boolean isAfter() {
        return DECORATOR_POSITION.AFTER.equals(position);
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
