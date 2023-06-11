package org.tinystruct.system;

public interface Listener<T> {
    void on(Event event, Callback callback);
}