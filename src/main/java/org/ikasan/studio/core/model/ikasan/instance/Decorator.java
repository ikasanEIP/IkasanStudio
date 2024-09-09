package org.ikasan.studio.core.model.ikasan.instance;

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
    public enum TYPE {
        Wiretap, LogWiretap, Unknown;

        public static TYPE safeValueOf(String name) {
            for (TYPE type : TYPE.values()) {
                if (type.name().equals(name)) {
                    return type;
                }
            }
            return Unknown;
        }
    }
    public enum POSITION {
        BEFORE, AFTER, UNKNOWN;
        public static POSITION safeValueOf(String name) {
            for (POSITION position : POSITION.values()) {
                if (position.name().equals(name)) {
                    return position;
                }
            }
            return UNKNOWN;
        }
    }

    private TYPE type;
    @JsonIgnore
    private POSITION position;
    private String name;
    private String configurationId;
    private boolean configurable;

    /**
     * Create a new decorator.
     * It is essential that the IDE does not encounter an exception, the builder will tollarate invalid data
     * but will identify the decorator as invalid, it is left for the consumer of the builder to call isValid
     * before using the decorator.
     * @param type e.g. Wiretap.
     * @param name e.g. BEFORE widget xx.
     * @param configurationId for the component.
     * @param configurable true if this is exposed via blue console.
     */
    @Builder(builderMethodName = "decoratorBuilder")
    protected Decorator(String type, String name, String configurationId, boolean configurable) {
        this.type = TYPE.safeValueOf(type);
        String prefix = name != null && !name.isBlank() ? name.split(" ")[0] : "BEFORE";
        this.position = POSITION.safeValueOf(prefix);
        this.name = name;
        this.configurationId = configurationId;
        this.configurable = configurable;
    }

    @JsonIgnore
    public boolean isInvalid() {
        return type.equals(TYPE.Unknown) || position.equals(POSITION.UNKNOWN);
    }

    @JsonIgnore
    public boolean isValid() {
        return !isInvalid();
    }

    @JsonIgnore
    public boolean isWiretap() {
        return TYPE.Wiretap.equals(type);
    }

    @JsonIgnore
    public boolean isLogWiretap() {
        return TYPE.LogWiretap.equals(type);
    }

    @JsonIgnore
    public boolean isBefore() {
        return POSITION.BEFORE.equals(position);
    }

    @JsonIgnore
    public boolean isAfter() {
        return POSITION.AFTER.equals(position);
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
