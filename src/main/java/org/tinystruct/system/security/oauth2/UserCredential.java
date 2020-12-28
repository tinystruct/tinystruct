package org.tinystruct.system.security.oauth2;

import org.tinystruct.ApplicationException;
import org.tinystruct.data.component.Builder;
import org.tinystruct.system.security.Credential;
import org.tinystruct.system.util.TextFileLoader;

public class UserCredential implements Credential {

    @Override
    public String get(String key) throws ApplicationException {
        TextFileLoader loader = new TextFileLoader();
        loader.setInputStream(UserCredential.class.getResourceAsStream("/clients_secrets.json"));
        Builder builder = new Builder();
        builder.parse(loader.getContent().toString());

        if (builder.get("oauth2") instanceof Builder) {
            builder = (Builder) builder.get("oauth2");
        }

        return builder.get(key).toString();
    }

}
