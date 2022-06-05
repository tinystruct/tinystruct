package org.tinystruct.http;

import java.io.OutputStream;

public abstract class ResponseWrapper implements Response {

    private OutputStream stream;

    public ResponseWrapper(OutputStream stream) {
        this.stream = stream;
    }

    public OutputStream getOutputStream() {
        return this.stream;
    }
}
