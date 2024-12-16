package org.tinystruct.http;

public class ResponseHeaders extends Headers {
    private final Response response;

    public ResponseHeaders(Response response) {
        this.response = response;
        if (this.response.headers() != null)
            this.response.headers().values().forEach(this::add);
    }

    @Override
    public boolean add(Header header) {
        if (!super.contains(header)) {
            String value = "";
            if (header.value() != null) {
                value = header.value().toString();
                value = value.replaceAll("[\r\n]", "");
            }
            this.response.addHeader(header.name(), value);
            return super.add(header);
        }

        return false;
    }
}
