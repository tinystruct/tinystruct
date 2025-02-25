package org.tinystruct.system.security.oauth2;

import org.tinystruct.ApplicationException;
import org.tinystruct.data.component.Builder;
import org.tinystruct.http.Header;
import org.tinystruct.http.Headers;
import org.tinystruct.http.Method;
import org.tinystruct.http.URLRequest;
import org.tinystruct.http.client.HttpRequestBuilder;
import org.tinystruct.system.security.Authentication;
import org.tinystruct.system.security.Credential;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class OAuth2Client implements Authentication {
    boolean status;
    Object token;
    private static final Logger logger = Logger.getLogger(OAuth2Client.class.getName());

    @Override
    public void identify(Credential credential, Map<String, Object> parameters) throws ApplicationException {
        try {
            URL uri = URI.create(credential.get("token_uri")).toURL();
            Headers headers = new Headers();
            headers.add(Header.CONTENT_TYPE.set("application/x-www-form-urlencoded"));
            headers.add(Header.CACHE_CONTROL.set("no-cache"));

            String userCredentials = credential.get("client_id") + ":" + credential.get("client_secret");
            String basicAuth = "Basic " + new String(Base64.getEncoder().encode(userCredentials.getBytes()));
            headers.add(Header.AUTHORIZATION.set(basicAuth));

            HttpRequestBuilder builder = new HttpRequestBuilder();
            builder.setMethod(Method.GET).setHeaders(headers);
            parameters.forEach(builder::setParameter);

            URLRequest request = new URLRequest(uri);
            String resp = new String(request.send(builder), StandardCharsets.UTF_8);
            Builder struct = new Builder();
            struct.parse(resp);
            if ((this.token = struct.get("access_token")) != null) {
                this.status = true;
            }

        } catch (MalformedURLException e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
        } catch (URISyntaxException e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
        }
    }

    @Override
    public Object grant() {
        return token;
    }

    @Override
    public boolean approved() {
        return status;
    }

    public byte[] resource(URL url, Map<String, Object> parameters) {
        String bearer = "Bearer " + token;

        Headers headers = new Headers();
        headers.add(Header.CONTENT_TYPE.set("application/x-www-form-urlencoded"));
        headers.add(Header.CACHE_CONTROL.set("no-cache"));
        headers.add(Header.AUTHORIZATION.set(bearer));

        URLRequest request = new URLRequest(url);
        HttpRequestBuilder builder = new HttpRequestBuilder();
        builder.setMethod(Method.GET).setHeaders(headers);
        parameters.forEach(builder::setParameter);

        try {
            return request.send(builder);
        } catch (ApplicationException e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
        } catch (URISyntaxException e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
        }
        return null;
    }

    public enum GRANT_TYPE {
        client_credentials, password, authorization_code, refresh_token, token
    }

}
