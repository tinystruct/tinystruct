package org.tinystruct.system.security.oauth2;

import org.tinystruct.ApplicationException;
import org.tinystruct.data.component.Builder;
import org.tinystruct.http.Header;
import org.tinystruct.http.Method;
import org.tinystruct.net.URLRequest;
import org.tinystruct.net.handlers.HTTPHandler;
import org.tinystruct.system.security.Authentication;
import org.tinystruct.system.security.Credential;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * OAuth2Client implements the OAuth 2.0 client credentials flow for authentication.
 * It supports token-based authentication and resource access using the obtained token.
 */
public class OAuth2Client implements Authentication {
    private static final Logger logger = Logger.getLogger(OAuth2Client.class.getName());
    private static final String CONTENT_TYPE = "application/x-www-form-urlencoded";
    private static final String CACHE_CONTROL = "no-cache";
    private static final String BASIC_AUTH_PREFIX = "Basic ";
    private static final String BEARER_AUTH_PREFIX = "Bearer ";
    private static final String ACCESS_TOKEN_KEY = "access_token";

    private boolean status;
    private Object token;
    private final HTTPHandler httpHandler;

    /**
     * Creates a new OAuth2Client instance.
     */
    public OAuth2Client() {
        this.httpHandler = new HTTPHandler();
    }

    /**
     * Authenticates the client using the provided credentials and parameters.
     * This method implements the OAuth 2.0 client credentials flow.
     *
     * @param credential The OAuth2 credentials containing client_id, client_secret, and token_uri
     * @param parameters Additional parameters for the token request
     * @throws ApplicationException if authentication fails or credentials are invalid
     */
    @Override
    public void identify(Credential credential, Map<String, Object> parameters) throws ApplicationException {
        Objects.requireNonNull(credential, "Credential cannot be null");
        Objects.requireNonNull(parameters, "Parameters cannot be null");

        String tokenUri = credential.get("token_uri");
        String clientId = credential.get("client_id");
        String clientSecret = credential.get("client_secret");

        if (tokenUri == null || clientId == null || clientSecret == null) {
            throw new ApplicationException("Missing required OAuth2 credentials");
        }

        try {
            URL uri = URI.create(tokenUri).toURL();
            URLRequest request = createBaseRequest(uri);
            request.setHeader(Header.AUTHORIZATION.name(), createBasicAuth(clientId, clientSecret));
            parameters.forEach(request::setParameter);

            String resp = httpHandler.handleRequest(request).getBody();
            if (resp == null || resp.trim().isEmpty()) {
                throw new ApplicationException("Empty response from OAuth2 server");
            }

            Builder struct = new Builder();
            struct.parse(resp);
            this.token = struct.get(ACCESS_TOKEN_KEY);
            this.status = this.token != null;

            if (!this.status) {
                throw new ApplicationException("Failed to obtain access token from OAuth2 server");
            }

        } catch (MalformedURLException e) {
            throw new ApplicationException("Invalid token URI: " + tokenUri, e);
        }
    }

    /**
     * Returns the obtained access token.
     *
     * @return The access token or null if not authenticated
     */
    @Override
    public Object grant() {
        return token;
    }

    /**
     * Checks if the client is authenticated.
     *
     * @return true if authenticated, false otherwise
     */
    @Override
    public boolean approved() {
        return status;
    }

    /**
     * Makes a request to a protected resource using the obtained access token.
     *
     * @param url The URL of the protected resource
     * @param parameters Additional parameters for the resource request
     * @return The response body as bytes or null if the request fails
     * @throws ApplicationException if the request fails
     */
    public byte[] resource(URL url, Map<String, Object> parameters) throws ApplicationException {
        Objects.requireNonNull(url, "URL cannot be null");
        Objects.requireNonNull(parameters, "Parameters cannot be null");

        if (!status || token == null) {
            throw new ApplicationException("Client not authenticated");
        }

        URLRequest request = createBaseRequest(url);
        request.setHeader(Header.AUTHORIZATION.name(), BEARER_AUTH_PREFIX + token);
        parameters.forEach(request::setParameter);

        try {
            String responseBody = httpHandler.handleRequest(request).getBody();
            return responseBody != null ? responseBody.getBytes(StandardCharsets.UTF_8) : null;
        } catch (ApplicationException e) {
            logger.log(Level.SEVERE, "Failed to access protected resource", e);
            throw e;
        }
    }

    /**
     * Creates a base URLRequest with common settings.
     *
     * @param url The URL for the request
     * @return A configured URLRequest instance
     */
    private URLRequest createBaseRequest(URL url) {
        URLRequest request = new URLRequest(url);
        request.setMethod(Method.GET.name());
        request.setHeader(Header.CONTENT_TYPE.name(), CONTENT_TYPE);
        request.setHeader(Header.CACHE_CONTROL.name(), CACHE_CONTROL);
        return request;
    }

    /**
     * Creates a Basic Authentication header value.
     *
     * @param clientId The OAuth2 client ID
     * @param clientSecret The OAuth2 client secret
     * @return The Basic Authentication header value
     */
    private String createBasicAuth(String clientId, String clientSecret) {
        String credentials = clientId + ":" + clientSecret;
        return BASIC_AUTH_PREFIX + Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Enumeration of supported OAuth2 grant types.
     */
    public enum GRANT_TYPE {
        client_credentials,
        password,
        authorization_code,
        refresh_token,
        token
    }
}
