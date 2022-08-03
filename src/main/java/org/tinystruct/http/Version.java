package org.tinystruct.http;

public enum Version {
    /**
     * HTTP/1.0
     */
    HTTP1_0("HTTP/1.0"),

    /**
     * HTTP/1.1
     */
    HTTP1_1("HTTP/1.1"),

    /**
     * HTTP/2.0
     */
    HTTP2_0("HTTP/2.0");

    /**
     * HTTP message text
     */
    private final String message;

    Version(String message) {
        this.message = message;
    }

    @Override
    public String toString() {
        return this.message;
    }
}