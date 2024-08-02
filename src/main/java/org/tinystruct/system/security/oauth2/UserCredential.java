package org.tinystruct.system.security.oauth2;

import org.tinystruct.ApplicationException;
import org.tinystruct.data.component.Builder;
import org.tinystruct.system.security.Credential;
import org.tinystruct.system.util.TextFileLoader;

import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

public class UserCredential implements Credential {
    private static final Logger logger = Logger.getLogger(UserCredential.class.getName());

    @Override
    public String get(String key) throws ApplicationException {
        TextFileLoader loader = new TextFileLoader();
        try (InputStream stream = UserCredential.class.getResourceAsStream("/clients_secrets.json")) {
            loader.setInputStream(stream);
        } catch (IOException e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
        }

        Builder builder = new Builder();
        builder.parse(loader.getContent().toString());

        if (builder.get("oauth2") instanceof Builder) {
            builder = (Builder) builder.get("oauth2");
        }

        return builder.get(key).toString();
    }

}
