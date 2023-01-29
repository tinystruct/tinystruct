package org.tinystruct.http;

import org.tinystruct.ApplicationException;
import org.tinystruct.data.FileEntity;

import java.util.List;

public interface Request<T> extends Protocol {

    /**
     * Returns the headers of this message.
     *
     * @return the headers of this message
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
     * @param method method
     * @return this {@link Request}
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
     * @param uri the URI being requested
     * @return this request
     */
    Request setUri(String uri);

    Session getSession(String id, boolean generate);

    Session getSession();

    String getParameter(String name);

    /**
     * Return the attachments if there are.
     *
     * @throws ApplicationException if there are exceptions for attachments
     * @return list of {@link FileEntity}
     */
    List<FileEntity> getAttachments() throws ApplicationException;

    Cookie[] cookies();

    String query();

    T stream();
}
