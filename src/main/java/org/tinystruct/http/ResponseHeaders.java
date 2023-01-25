package org.tinystruct.http;

public class ResponseHeaders extends Headers {
    private final Response response;

    public ResponseHeaders(Response response) {
        this.response = response;
    }

    @Override
    public boolean add(Header header) {
        this.response.addHeader(header.name(), header.value() != null ? header.value().toString() : "");

        return super.add(header);
    }
}
