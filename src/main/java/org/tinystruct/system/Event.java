package org.tinystruct.system;

/**
 * Represents a generic event in the system.
 *
 * @param <T> the type of the payload associated with the event.
 */
public interface Event<T> {
    /**
     * Gets the name of the event.
     * This can be used to identify or categorize the event.
     *
     * @return the name of the event as a String.
     */
    String getName();

    /**
     * Gets the payload of the event.
     * The payload contains any additional data associated with the event.
     *
     * @return the payload of the event, which can be of any type specified by T.
     */
    T getPayload();
}
