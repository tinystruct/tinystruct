package org.tinystruct.http;

public enum Method {
    /**
     * The OPTIONS method represents a request for information about the communication options
     * available on the request/response chain identified by the Request-URI. This method allows
     * the client to determine the options and/or requirements associated with a resource, or the
     * capabilities of a server, without implying a resource action or initiating a resource
     * retrieval.
     */
    OPTIONS ("OPTIONS"),

    /**
     * The GET method means retrieve whatever information (in the form of an entity) is identified
     * by the Request-URI.  If the Request-URI refers to a data-producing process, it is the
     * produced data which shall be returned as the entity in the response and not the source text
     * of the process, unless that text happens to be the output of the process.
     */
    GET ("GET"),

    /**
     * The HEAD method is identical to GET except that the server MUST NOT return a message-body
     * in the response.
     */
    HEAD ("HEAD"),

    /**
     * The POST method is used to request that the origin server accept the entity enclosed in the
     * request as a new subordinate of the resource identified by the Request-URI in the
     * Request-Line.
     */
    POST ("POST"),

    /**
     * The PUT method requests that the enclosed entity be stored under the supplied Request-URI.
     */
    PUT ("PUT"),

    /**
     * The PATCH method requests that a set of changes described in the
     * request entity be applied to the resource identified by the Request-URI.
     */
    PATCH ("PATCH"),

    /**
     * The DELETE method requests that the origin server delete the resource identified by the
     * Request-URI.
     */
    DELETE ("DELETE"),

    /**
     * The TRACE method is used to invoke a remote, application-layer loop-back of the request
     * message.
     */
    TRACE ("TRACE"),

    /**
     * This specification reserves the method name CONNECT for use with a proxy that can dynamically
     * switch to being a tunnel
     */
    CONNECT ("CONNECT");

    private final String name;

    Method(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return this.name;
    }
}