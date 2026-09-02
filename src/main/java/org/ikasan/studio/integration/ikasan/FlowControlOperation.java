package org.ikasan.studio.integration.ikasan;

/** A UI-independent flow state transition understood by Ikasan's module-control REST API. */
public enum FlowControlOperation {
    START("start"),
    STOP("stop"),
    PAUSE("pause"),
    START_PAUSE("startPause"),
    RESUME("resume");

    private final String wireValue;

    FlowControlOperation(String wireValue) {
        this.wireValue = wireValue;
    }

    public String getWireValue() {
        return wireValue;
    }
}
