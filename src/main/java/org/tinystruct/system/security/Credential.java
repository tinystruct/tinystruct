package org.tinystruct.system.security;

import org.tinystruct.ApplicationException;

public interface Credential {
    String get(String string) throws ApplicationException;
}
