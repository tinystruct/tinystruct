package org.tinystruct.http;


public interface Protocol {
    /**
     * Returns the protocol version of this {@link Protocol}
     */
    Version protocolVersion();

    /**
     * Set the protocol version of this {@link Protocol}
     */
    void setProtocolVersion(Version version);
}
