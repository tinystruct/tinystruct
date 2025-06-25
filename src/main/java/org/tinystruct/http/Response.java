package org.tinystruct.http;

import org.tinystruct.ApplicationException;

import java.io.IOException;

public interface Response<T, O> extends Protocol {
    /**
     * Returns the status of this {@link Response}.
     *
     * @return The {@link ResponseStatus} of this {@link Response}
     */
    ResponseStatus status();

    /**
     * Set the status of this {@link Response}.
     *
     * @param status The new status of this {@link Response}
     * @return This {@link Response}
     */
    Response<T, O> setStatus(ResponseStatus status);

    /**
     * Returns the headers of this message.
     * @return The headers of this message
     */
    Headers headers();

    void addHeader(String header, Object value);

    void sendRedirect(String url) throws IOException;

    void writeAndFlush(byte[] bytes) throws ApplicationException;

    O get();

    void close() throws IOException;
}
