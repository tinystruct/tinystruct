package org.tinystruct.http;

import java.io.IOException;

public interface Response<T> extends Protocol {
    /**
     * Returns the status of this {@link Response}.
     *
     * @return The {@link ResponseStatus} of this {@link Response}
     */
    ResponseStatus status();

    /**
     * Set the status of this {@link Response}.
     */
    Response<T> setStatus(ResponseStatus status);

    /**
     * Returns the headers of this message.
     */
    Headers headers();

    void addHeader(String header, Object value);

    void sendRedirect(String url) throws IOException;

    T get();
}
