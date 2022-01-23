package org.tinystruct.application;

import org.tinystruct.ApplicationException;

public interface Method<T> {
    T execute(Object[] args) throws ApplicationException;
}
