package org.tinystruct.http.servlet;

import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.tinystruct.ApplicationException;
import org.tinystruct.data.Attachment;
import org.tinystruct.data.FileEntity;
import org.tinystruct.data.component.Builder;
import org.tinystruct.http.*;
import org.tinystruct.transfer.http.upload.ContentDisposition;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A specialized request builder for Servlet-based HTTP requests.
 * This builder creates Request objects from HttpServletRequest instances,
 * specifically designed for use in servlet containers.
 *
 * <p>This builder is distinct from the HTTP client request builder and should be used
 * only for handling incoming servlet requests in a web container environment.</p>
 */
public class RequestBuilder extends RequestWrapper<HttpServletRequest, ServletInputStream> {
    private final SessionManager manager = SessionManager.getInstance();
    private final Headers headers = new Headers();
    private final Cookie[] cookies;
    private final Session memorySession;
    private final boolean secure;
    private Version version;
    private Method method;
    private String uri;
    private static final Logger logger = Logger.getLogger(RequestBuilder.class.getName());
    private String bodyCache;
    private List<FileEntity> list = new ArrayList<>();

    public RequestBuilder(HttpServletRequest request, boolean secure) {
        super(request);

        Enumeration<String> headerNames = this.request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            try {
                String h = headerNames.nextElement();
                this.headers.add(Header.value0f(h).set(this.request.getHeader(h)));
            } catch (IllegalArgumentException ignored) {
            }
        }

        this.setUri(this.request.getRequestURI());
        this.setMethod(Method.valueOf(this.request.getMethod()));

        jakarta.servlet.http.Cookie[] _cookies = this.request.getCookies();
        if (_cookies != null) {
            int i = _cookies.length;

            this.cookies = new Cookie[i];
            for (jakarta.servlet.http.Cookie _cookie : _cookies) {
                Cookie cookie = new CookieImpl(_cookie.getName());
                cookie.setValue(_cookie.getValue());
                cookie.setDomain(_cookie.getDomain());
                cookie.setHttpOnly(_cookie.isHttpOnly());
                cookie.setMaxAge(_cookie.getMaxAge());
                cookie.setPath(_cookie.getPath());
                cookie.setSecure(_cookie.getSecure());
                cookies[--i] = cookie;
            }
        } else
            this.cookies = new Cookie[]{};

        HttpSession session = this.request.getSession();
        String sessionId = session.getId();
        memorySession = getSession(sessionId, true);

        Enumeration<String> attributeNames = session.getAttributeNames();
        while (attributeNames.hasMoreElements()) {
            String s = attributeNames.nextElement();
            memorySession.setAttribute(s, session.getAttribute(s));
        }

        this.secure = secure;

        try {
            parseRequest();
        } catch (ApplicationException e) {
            throw new RuntimeException(e);
        }
    }

    public RequestBuilder(HttpServletRequest request) {
        this(request, false);
    }

    public Session getSession(String id) {
        return manager.getSession(id);
    }

    @Override
    public Session getSession(String id, boolean generated) {
        if (manager.getSession(id) == null && generated) {
            manager.setSession(id, new MemorySession(id));
        }

        return manager.getSession(id);
    }

    @Override
    public Session getSession() {
        return memorySession;
    }

    @Override
    public String query() {
        return this.request.getQueryString();
    }

    private boolean isJsonRequest() {
        String contentType = this.request.getContentType();
        return contentType != null && contentType.contains("application/json");
    }

    /**
     * @return body string.
     */
    @Override
    public String body() {
        if (bodyCache != null) return bodyCache;
        StringBuilder lines = new StringBuilder();
        String line;
        while (true) {
            try {
                if ((line = this.request.getReader().readLine()) == null) break;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            lines.append(line);
        }
        bodyCache = lines.toString();  // cache the body
        return bodyCache;
    }

    @Override
    public boolean isSecure() {
        return this.request.isSecure();
    }

    @Override
    public ServletInputStream stream() {
        try {
            return this.request.getInputStream();
        } catch (IOException e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
        }

        return null;
    }

    /**
     * @return Parameter Names
     */
    @Override
    public String[] parameterNames() {
        ArrayList<String> names = new ArrayList<>();

        if (this.isJsonRequest()) {
            // Handle query parameters when the request is JSON
            String queryString = this.query();
            if (queryString != null) {
                // Split query string by '&' to get individual key-value pairs
                String[] queryParams = queryString.split("&");
                for (String param : queryParams) {
                    // Only add parameter names, avoid duplicates
                    String[] keyValue = param.split("=");
                    if (keyValue.length == 2 && !names.contains(keyValue[0])) {
                        names.add(keyValue[0]);
                    }
                }
            }
            return names.toArray(new String[0]);
        }

        Enumeration<String> parameterNames = this.request.getParameterNames();
        while (parameterNames.hasMoreElements()) {
            names.add(parameterNames.nextElement());
        }
        return names.toArray(new String[0]);
    }

    @Override
    public Version version() {
        return this.version;
    }

    @Override
    public void setVersion(Version version) {
        this.version = version;
    }

    @Override
    public Headers headers() {
        return this.headers;
    }

    @Override
    public Method method() {
        return this.method;
    }

    @Override
    public Request<HttpServletRequest, ServletInputStream> setMethod(Method method) {
        this.method = method;
        return this;
    }

    @Override
    public String uri() {
        return this.uri;
    }

    @Override
    public Request<HttpServletRequest, ServletInputStream> setUri(String uri) {
        this.uri = uri;
        return this;
    }

    @Override
    public String getParameter(String name) {
        if (this.isJsonRequest()) {
            String queryString = this.query();
            if (queryString != null && queryString.contains(name + "=")) {
                // Find the value of the parameter
                String value = queryString.substring(queryString.indexOf(name + "=") + name.length() + 1);
                try {
                    // Decode the value to handle any encoded characters (e.g., '%20' for spaces)
                    return URLDecoder.decode(value, "UTF-8");
                } catch (UnsupportedEncodingException e) {
                    // Log the error and return the raw value if decoding fails
                    logger.log(Level.WARNING, "Failed to decode query parameter: " + name, e);
                    return value;
                }
            }
            return null;
        }
        return this.request.getParameter(name);
    }

    /**
     * Return the attachments if there are.
     *
     * @return list of {@link FileEntity}
     */
    @Override
    public List<FileEntity> getAttachments() throws ApplicationException {
        return list;
    }

    /**
     * Gets the character encoding of the request.
     *
     * @return the character encoding of the request
     */
    public String getCharacterEncoding() {
        return this.request.getCharacterEncoding();
    }

    private void parseRequest() throws ApplicationException {
        // Check if the content type is multipart/form-data
        String contentType = this.request.getContentType();
        if (contentType == null || !contentType.toLowerCase().startsWith("multipart/form-data")) {
            return; // Not a multipart form, nothing to parse
        }

        try {
            final MultipartFormData iterator = new MultipartFormData(this);
            ContentDisposition part;
            while ((part = iterator.getNextPart()) != null) {
                String name = part.getName();
                if (name == null) continue; // Skip parts without a name

                // Handle file uploads (parts with a content type and filename)
                if (part.getContentType() != null && part.getFileName() != null && !part.getFileName().isEmpty()) {
                    Attachment attachment = new Attachment();
                    attachment.setContentType(part.getContentType().trim());
                    attachment.setFilename(part.getFileName().trim());
                    attachment.setContent(part.getData());
                    attachment.setName(name); // Set the field name for the attachment

                    // Add to the attachments list
                    list.add(attachment);

                    // Also add the filename as a parameter for easy access
                    this.request.setAttribute(name, part.getFileName().trim());
                }
                // Handle regular form fields (parts without a content type or with empty filename)
                else {
                    // Convert the data to a string using the request's character encoding
                    String charset = this.getCharacterEncoding();
                    if (charset == null) {
                        charset = "UTF-8"; // Default to UTF-8 if no encoding specified
                    }

                    String value = "";
                    if (part.getData() != null) {
                        value = new String(part.getData(), charset);
                    }

                    // Set the parameter
                    this.request.setAttribute(name, value);
                }
            }
        } catch (ServletException e) {
            throw new ApplicationException("Error parsing multipart request: " + e.getMessage(), e);
        } catch (UnsupportedEncodingException e) {
            throw new ApplicationException("Unsupported character encoding: " + e.getMessage(), e);
        }
    }

    @Override
    public Cookie[] cookies() {
        return cookies;
    }
}

