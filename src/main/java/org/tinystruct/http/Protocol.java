package org.tinystruct.http;


public interface Protocol {
    /**
     * Returns the protocol version of this {@link Protocol}
     */
    Version version();

    /**
     * Set the protocol version of this {@link Protocol}
     */
    void setVersion(Version version);
}