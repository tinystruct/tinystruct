package org.tinystruct.http.servlet;

import org.tinystruct.http.Response;

import javax.servlet.http.HttpServletResponse;

public abstract class ResponseWrapper implements Response {

    final HttpServletResponse response;

    public ResponseWrapper(HttpServletResponse response) {
        this.response = response;
    }

}
