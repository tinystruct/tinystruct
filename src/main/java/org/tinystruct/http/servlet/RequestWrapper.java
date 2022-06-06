package org.tinystruct.http.servlet;

import org.tinystruct.http.Request;

import javax.servlet.http.HttpServletRequest;

public abstract class RequestWrapper implements Request {
    final HttpServletRequest request;

    public RequestWrapper(HttpServletRequest request) {
        this.request = request;
    }
}