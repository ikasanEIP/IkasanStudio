package org.ikasan.studio.core;

public class StudioBuildException extends Exception {
    public StudioBuildException(String message) {
        super(message);
    }

    public StudioBuildException(String message, Throwable cause) {
        super(message, cause);
    }

    public StudioBuildException(Throwable cause) {
        super(cause);
    }
}
