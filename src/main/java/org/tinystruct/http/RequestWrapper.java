package org.tinystruct.http;

public abstract class RequestWrapper<T, I> implements Request<T, I> {
    protected final T request;
    public RequestWrapper(T request) {
        this.request = request;
    }
}