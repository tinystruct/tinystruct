package org.tinystruct.http;

import java.util.List;

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
    List<Header> headers();
}
