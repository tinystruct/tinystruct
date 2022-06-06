package org.tinystruct.http;

import java.util.HashSet;

public class Headers extends HashSet<Header> {

    public Headers() {
    }

    public Object get(Header header) {
        //@TODO
        return header.value();
    }
}
