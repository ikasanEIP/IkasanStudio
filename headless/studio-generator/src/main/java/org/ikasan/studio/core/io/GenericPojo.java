package org.ikasan.studio.core.io;

public class GenericPojo<T> {
    private T payload;
    public T getPayload() {
        return payload;
    }
    public void setPayload(T payload) {
        this.payload = payload;
    }
}
