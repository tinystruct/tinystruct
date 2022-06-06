package org.tinystruct.http;

import org.tinystruct.ApplicationContext;

import javax.servlet.http.HttpServletRequest;

public abstract class RequestWrapper extends ApplicationContext implements Request {
    final HttpServletRequest request;

    public RequestWrapper(HttpServletRequest request) {
        this.request = request;
    }
}