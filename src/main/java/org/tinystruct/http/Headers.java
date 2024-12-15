package org.tinystruct.http;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class Headers {

    private final HashSet<Header> headers = new HashSet<>();
    private final List<String> names = new ArrayList<String>();
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
        this.names.add(set.name());
        return headers.add(set);
    }

    public boolean contains(Header header) {
        return this.names.contains(header.name());
    }

    public HashSet<Header> values() {
        return this.headers;
    }
}
