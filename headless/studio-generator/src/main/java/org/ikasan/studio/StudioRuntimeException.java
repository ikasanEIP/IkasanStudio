package org.ikasan.studio;

public class StudioRuntimeException extends RuntimeException {
    public StudioRuntimeException(String message) {
        super(message);
    }

    public StudioRuntimeException(String message, Throwable cause) {
        super(message, cause);
    }
}
