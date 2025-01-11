package org.tinystruct.http.servlet;

import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.tinystruct.ApplicationException;
import org.tinystruct.data.Attachment;
import org.tinystruct.data.FileEntity;
import org.tinystruct.http.*;
import org.tinystruct.transfer.http.upload.ContentDisposition;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

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

    /**
     * @return body string.
     */
    @Override
    public String body() {
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

        return lines.toString();
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
        return this.request.getParameter(name);
    }

    /**
     * Return the attachments if there are.
     *
     * @return list of {@link FileEntity}
     */
    @Override
    public List<FileEntity> getAttachments() throws ApplicationException {
        List<FileEntity> list = new ArrayList<>();
        try {
            final MultipartFormData iterator = new MultipartFormData(this);
            ContentDisposition e;
            while ((e = iterator.getNextPart()) != null) {
                Attachment attachment = new Attachment();
                attachment.setContentType(e.getContentType());
                attachment.setFilename(e.getFileName());
                attachment.setContent(e.getData());

                list.add(attachment);
            }
        } catch (ServletException e) {
            throw new ApplicationException(e.getMessage(), e);
        }

        return list;
    }

    @Override
    public Cookie[] cookies() {
        return cookies;
    }
}

