package org.tinystruct.http;

import java.io.IOException;
import java.io.PrintWriter;

public interface Response extends Protocol {
    /**
     * Returns the status of this {@link Response}.
     *
     * @return The {@link ResponseStatus} of this {@link Response}
     */
    ResponseStatus status();

    /**
     * Set the status of this {@link Response}.
     */
    Response setStatus(ResponseStatus status);

    /**
     * Returns the headers of this message.
     */
    Headers headers();

    void addHeader(String header, String value);

    PrintWriter getWriter();

    void sendRedirect(String url) throws IOException;
}
