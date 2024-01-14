package org.ikasan.studio;

public class StudioException extends Exception {
    public StudioException(String message) {
        super(message);
    }

    public StudioException(String message, Throwable cause) {
        super(message, cause);
    }

    public StudioException(Throwable cause) {
        super(cause);
    }
}
