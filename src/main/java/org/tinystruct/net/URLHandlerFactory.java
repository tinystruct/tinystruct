package org.tinystruct.net;

import org.tinystruct.ApplicationException;
import org.tinystruct.net.handlers.FileHandler;
import org.tinystruct.net.handlers.FTPHandler;
import org.tinystruct.net.handlers.HTTPHandler;

import java.net.URL;

public class URLHandlerFactory {
    public static URLHandler getHandler(URL url) throws ApplicationException {
        String protocol = url.getProtocol().toLowerCase();
        switch (protocol) {
            case "http":
            case "https":
                return new HTTPHandler();
            case "ftp":
                return new FTPHandler();
            case "file":
                return new FileHandler();
            default:
                throw new ApplicationException("Unsupported protocol: " + protocol);
        }
    }
}
