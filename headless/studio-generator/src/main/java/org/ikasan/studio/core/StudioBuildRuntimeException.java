package org.ikasan.studio.core;

public class StudioBuildRuntimeException  extends RuntimeException {
    public StudioBuildRuntimeException(String message) {
        super(message);
    }

    public StudioBuildRuntimeException(String message, Throwable cause) {
        super(message, cause);
    }
}
