package org.tinystruct.http;


public interface Protocol {
    /**
     * Returns the protocol version of this {@link Protocol}
     * @return the protocol version
     */
    Version version();

    /**
     * Set the protocol version of this {@link Protocol}
     * @param version the protocol version
     */
    void setVersion(Version version);
}