package org.tinystruct.http;

/**
 * Monitor
 *
 * @param <T> listener
 */
public interface Monitor<T> {

    /**
     * Add a listener to this component.
     *
     * @param listener the T instance that should be notified
     */
    void addListener(T listener);
}
