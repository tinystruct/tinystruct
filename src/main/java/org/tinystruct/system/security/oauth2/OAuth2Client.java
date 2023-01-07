package org.tinystruct.system.security.oauth2;

import org.tinystruct.ApplicationException;
import org.tinystruct.data.component.Builder;
import org.tinystruct.http.URLRequest;
import org.tinystruct.system.security.Authentication;
import org.tinystruct.system.security.Credential;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

public class OAuth2Client implements Authentication {
    boolean status;
    Object token;

    public enum GRANT_TYPE {
        client_credentials, password, authorization_code, refresh_token, token
    }

    @Override
    public void identify(Credential credential, Map<String, Object> parameters) throws ApplicationException {
        try {
            URL uri = new URL(credential.get("token_uri"));
            Map<String, String> headers = new HashMap<String, String>();
            headers.put("Content-Type", "application/x-www-form-urlencoded");
            headers.put("cache-control", "no-cache");

            String userCredentials = credential.get("client_id") + ":" + credential.get("client_secret");
            String basicAuth = "Basic " + new String(Base64.getEncoder().encode(userCredentials.getBytes()));
            headers.put("Authorization", basicAuth);

            URLRequest request = new URLRequest(uri);
            String resp = new String(request.setHeaders(headers).send(parameters), StandardCharsets.UTF_8);
            Builder struct = new Builder();
            struct.parse(resp);
            if ((this.token = struct.get("access_token")) != null) {
                this.status = true;
            }

        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (URISyntaxException e) {
            e.printStackTrace();
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

        Map<String, String> headers = new HashMap<String, String>();
        headers.put("Content-Type", "application/x-www-form-urlencoded");
        headers.put("Authorization", bearer);
        headers.put("cache-control", "no-cache");
        URLRequest request = new URLRequest(url);
        try {
            return request.setHeaders(headers).send(parameters);
        } catch (ApplicationException e) {
            e.printStackTrace();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        return null;
    }

}
