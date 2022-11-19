package org.tinystruct.http;

import java.util.HashSet;

public class Headers {

    private final HashSet<Header> headers = new HashSet<>();

    public Headers() {
    }

    public Object get(Header header) {
        for (Header next : headers) {
            if (next.name().equalsIgnoreCase(header.name())) {
                return next.value();
            }
        }

        return null;
    }

    public boolean add(Header set) {
        return headers.add(set);
    }

    public boolean contains(Header header) {
        return headers.contains(header);
    }
}
