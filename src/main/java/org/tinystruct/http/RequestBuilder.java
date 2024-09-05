package org.tinystruct.http;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.handler.codec.http.cookie.ServerCookieDecoder;
import io.netty.handler.codec.http.multipart.*;
import io.netty.util.CharsetUtil;
import jakarta.annotation.Nullable;
import org.tinystruct.data.Attachment;
import org.tinystruct.data.FileEntity;

import java.io.IOException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.tinystruct.http.Constants.JSESSIONID;

public class RequestBuilder extends RequestWrapper<FullHttpRequest, Object> {
    private final SessionManager manager = SessionManager.getInstance();
    private final Headers headers = new Headers();
    private final Cookie[] cookies;
    private final HashMap<String, List<String>> params = new HashMap<>();
    private final boolean secure;
    private List<FileEntity> attachments;
    private String query;
    private Version version;
    private Method method;
    private String uri;
    private String sessionId;
    private static final Logger logger = Logger.getLogger(RequestBuilder.class.getName());

    public RequestBuilder(FullHttpRequest request, boolean secure) {
        super(request);

        this.uri = request.uri();

        ByteBuf content = request.content();
        content.readableBytes();

        HttpHeaders headers = request.headers();
        headers.forEach(h -> this.headers.add(Header.value0f(h.getKey()).set(h.getValue())));

        Set<io.netty.handler.codec.http.cookie.Cookie> _cookies;
        String value = request.headers().get(HttpHeaderNames.COOKIE);
        if (value == null) {
            _cookies = Collections.emptySet();
        } else {
            _cookies = ServerCookieDecoder.STRICT.decode(value);
        }

        int i = _cookies.size();
        this.cookies = new Cookie[i];
        for (io.netty.handler.codec.http.cookie.Cookie _cookie : _cookies) {
            if (_cookie.name().equalsIgnoreCase(JSESSIONID)) {
                this.sessionId = _cookie.value();
            }
            Cookie cookie = new CookieImpl(_cookie.name());
            cookie.setValue(_cookie.value());
            cookie.setDomain(_cookie.domain());
            cookie.setHttpOnly(_cookie.isHttpOnly());
            cookie.setMaxAge(_cookie.maxAge());
            cookie.setPath(_cookie.path());
            cookie.setSecure(secure);
            cookies[--i] = cookie;
        }

        if (content.capacity() > 0) {
            String requestBody;
            String contentType = null;
            Object originHeader;
            if ((originHeader = this.headers.get(Header.CONTENT_TYPE)) != null && (contentType = originHeader.toString()).indexOf(';') != -1) {
                contentType = contentType.substring(0, contentType.indexOf(';'));
            }

            switch (Objects.requireNonNull(contentType)) {
                case "multipart/form-data":
                    requestBody = content.toString(CharsetUtil.UTF_8);
                    String boundary = null;
                    int index;
                    if ((index = originHeader.toString().lastIndexOf("boundary=")) != -1) {
                        boundary = "--" + originHeader.toString().substring(index + 9);
                        if (boundary.endsWith("\n")) {
                            //strip it off
                            boundary = boundary.substring(0, boundary.length() - 1);
                        }
                        requestBody = requestBody.substring(0, requestBody.indexOf(boundary) + boundary.length());
                    }

                    if (boundary != null && !requestBody.startsWith(boundary)) {
                        parseQuery(requestBody, false);
                    }

                    // TODO
                    final HttpDataFactory factory = new DefaultHttpDataFactory(DefaultHttpDataFactory.MINSIZE); // Disk if size exceed
                    HttpPostRequestDecoder decoder = new HttpPostRequestDecoder(factory, request);
                    InterfaceHttpData fileData;
                    List<FileEntity> list = new ArrayList<>();
                    while (decoder.hasNext()) {
                        fileData = decoder.next();
                        if (fileData != null && fileData.getHttpDataType() == InterfaceHttpData.HttpDataType.FileUpload) {
                            MixedFileUpload upload = (MixedFileUpload) fileData;
                            Attachment attachment = new Attachment();
                            attachment.setContentType(upload.getContentType());
                            attachment.setFilename(upload.getFilename());
                            attachment.setContentTransferEncoding(upload.getContentTransferEncoding());
                            try {
                                attachment.setContent(upload.get());
                            } catch (IOException e) {
                                logger.log(Level.SEVERE, e.getMessage(), e);
                            }

                            list.add(attachment);
                        }
                    }
                    this.attachments = list;
                    break;

                case "text/plain":
                    break;
                case "application/json":
                    break;
                case "application/x-www-form-urlencoded":
                default:
                    parseQuery(content.toString(CharsetUtil.UTF_8), false);
                    break;
            }
        }

        QueryStringDecoder q = parseQuery(request.uri(), true);
        if (!q.path().isEmpty()) {
            if (q.parameters().get("q") != null) {
                query = q.parameters().get("q").get(0);
            } else {
                query = "";
            }
        }
        this.secure = secure;
    }

    public RequestBuilder(FullHttpRequest request) {
        this(request, false);
    }

    @Override
    public List<FileEntity> getAttachments() {
        return this.attachments != null ? this.attachments : null;
    }

    private QueryStringDecoder parseQuery(String uri, boolean hasPath) {
        QueryStringDecoder decoder = new QueryStringDecoder(uri, hasPath);
        Map<String, List<String>> parameters = decoder.parameters();
        Iterator<Map.Entry<String, List<String>>> iterator = parameters.entrySet().iterator();
        Map.Entry<String, List<String>> element;
        while (iterator.hasNext()) {
            element = iterator.next();
            this.setParameter(element.getKey(), element.getValue());
        }

        return decoder;
    }

    private void setParameter(String name, List<String> values) {
        this.params.put(name, values);
    }

    public Session getSession(String id) {
        return manager.getSession(id);
    }

    public Session getSession(String id, boolean generated) {
        if (manager.getSession(id) == null && generated) {
            manager.setSession(id, new MemorySession(id));
        }

        return manager.getSession(id);
    }

    @Override
    public Session getSession() {
        if (sessionId != null) {
            return getSession(sessionId, true);
        }

        sessionId = UUID.randomUUID().toString().replaceAll("-", "");
        return getSession(sessionId, true);
    }

    @Override
    public String query() {
        return this.query;
    }

    @Override
    public boolean isSecure() {
        return this.secure;
    }

    @Override
    public Nullable stream() {
        return null;
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
        this.setMethod(Method.valueOf(this.request.method().name()));

        return this.method;
    }

    @Override
    public Request<FullHttpRequest, Object> setMethod(Method method) {
        this.method = method;
        return this;
    }

    @Override
    public String uri() {
        return this.uri;
    }

    @Override
    public Request<FullHttpRequest, Object> setUri(String uri) {
        this.uri = uri;
        return this;
    }

    @Override
    public String getParameter(String name) {
        if (null != this.params.get(name) && !this.params.get(name).isEmpty()) {
            return this.params.get(name).get(0);
        }
        return null;
    }

    @Override
    public Cookie[] cookies() {
        return cookies;
    }
}