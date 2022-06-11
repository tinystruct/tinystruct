package org.tinystruct.http;

public abstract class RequestWrapper<T> implements Request {
    protected final T request;

    public RequestWrapper(T request) {
        this.request = request;
    }
}