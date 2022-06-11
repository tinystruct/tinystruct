package org.tinystruct.http;

public abstract class ResponseWrapper<T> implements Response {

    protected final T response;

    public ResponseWrapper(T response) {
        this.response = response;
    }
}
