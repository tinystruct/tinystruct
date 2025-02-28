package org.tinystruct.net;

import org.tinystruct.ApplicationException;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public interface URLHandler {
    URLResponse handleRequest(URLRequest request) throws ApplicationException;
    URLResponse handleRequest(URLRequest request, Consumer<String> consumer) throws ApplicationException;
    CompletableFuture<URLResponse> handleRequestAsync(URLRequest request);
}