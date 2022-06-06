package org.tinystruct.http;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;

public abstract class ResponseWrapper implements Response {

    final HttpServletResponse response;

    public ResponseWrapper(HttpServletResponse response) {
        this.response = response;
    }

    public OutputStream getOutputStream() {
        try {
            return this.response.getOutputStream();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }
}
