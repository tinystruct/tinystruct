package org.tinystruct.data.tools;

import org.tinystruct.ApplicationException;

public class SQLInjectionException extends ApplicationException {
    public SQLInjectionException(String message) {
        super(message);
    }
}
