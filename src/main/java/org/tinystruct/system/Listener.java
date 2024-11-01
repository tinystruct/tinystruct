package org.tinystruct.system;

public interface Listener<T> {
    void on(T event, Callback<T> callback);
}