package org.tinystruct.http;

import org.tinystruct.ApplicationException;

public abstract class ResponseWrapper<T, O> implements Response<T, O> {

    protected final T response;

    public ResponseWrapper(T response) {
        this.response = response;
    }

    public void close() throws ApplicationException {
        // Default implementation does nothing, can be overridden
    }
}
