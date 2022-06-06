package org.tinystruct.http;

import java.util.List;

public interface Request extends Protocol {

    /**
     * Returns the headers of this message.
     */
    Headers headers();

    /**
     * Returns the {@link Method} of this {@link Request}.
     *
     * @return The {@link Method} of this {@link Request}
     */
    Method method();

    /**
     * Set the {@link Method} of this {@link Request}.
     *
     * @param method
     * @return
     */
    Request setMethod(Method method);

    /**
     * Returns the requested URI (or alternatively, path)
     *
     * @return The URI being requested
     */
    String uri();

    /**
     * Set the requested URI (or alternatively, path)
     *
     * @return
     */
    Request setUri(String uri);

    Session getSession(String id, boolean generate);
    Session getSession();

    String getParameter(String name);

    Cookie[] cookies();

    String query();
}
