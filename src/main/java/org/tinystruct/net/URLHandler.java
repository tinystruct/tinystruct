package org.tinystruct.net;

import org.tinystruct.ApplicationException;

import java.util.concurrent.CompletableFuture;

public interface URLHandler {
    URLResponse handleRequest(URLRequest request) throws ApplicationException;
    CompletableFuture<URLResponse> handleRequestAsync(URLRequest request);
}
