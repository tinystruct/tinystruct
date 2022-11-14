package org.tinystruct.http;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.handler.codec.http.cookie.ServerCookieDecoder;
import io.netty.handler.codec.http.multipart.DefaultHttpDataFactory;
import io.netty.handler.codec.http.multipart.HttpDataFactory;
import io.netty.handler.codec.http.multipart.HttpPostRequestDecoder;
import io.netty.handler.codec.http.multipart.InterfaceHttpData;
import io.netty.util.CharsetUtil;
import org.apache.catalina.util.StandardSessionIdGenerator;
import org.tinystruct.data.FileEntity;

import java.util.*;

public class RequestBuilder extends RequestWrapper<FullHttpRequest> {
    private final SessionManager manager = SessionManager.getInstance();
    private final Headers headers = new Headers();
    private final Cookie[] cookies;
    private final HashMap<String, List<String>> params = new HashMap<>();
    private List<FileEntity> attachments = new ArrayList<>();
    private String query;
    private Version version;
    private Method method;
    private String uri;

    public RequestBuilder(FullHttpRequest request) {
        super(request);

        this.uri = this.request.uri();

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
            Cookie cookie = new CookieImpl(_cookie.name());
            cookie.setValue(_cookie.value());
            cookie.setDomain(_cookie.domain());
            cookie.setHttpOnly(_cookie.isHttpOnly());
            cookie.setMaxAge(_cookie.maxAge());
            cookie.setPath(_cookie.path());
            cookie.setSecure(_cookie.isSecure());
            cookies[--i] = cookie;
        }

        if (content.capacity() > 0) {
            String requestBody;
            String[] args, pair;
            switch (this.headers.get(Header.CONTENT_TYPE).toString()) {
                case "multipart/form-data":
                    requestBody = content.toString(CharsetUtil.UTF_8);
                    args = requestBody.split("&");
                    for (i = 0; i < args.length; i++) {
                        if (args[i].contains("=")) {
                            pair = args[i].split("=");
                            this.setParameter(pair[0], List.of(pair[1]));
                        }
                    }
                    // TODO
                    final HttpDataFactory factory = new DefaultHttpDataFactory(DefaultHttpDataFactory.MINSIZE); // Disk if size exceed
                    HttpPostRequestDecoder decoder = new HttpPostRequestDecoder(factory, request);
                    InterfaceHttpData fileData;
                    List<FileEntity> list = new ArrayList<>();
                    while (decoder.hasNext()) {
                        fileData = decoder.next();
                        if (fileData != null && fileData.getHttpDataType() == InterfaceHttpData.HttpDataType.FileUpload) {
                            list.add((FileEntity) fileData);
                        }
                    }
                    this.attach(list);
                    break;
                case "application/x-www-form-urlencoded; charset=UTF-8":
                    requestBody = java.net.URLDecoder.decode(content.toString(CharsetUtil.UTF_8), CharsetUtil.UTF_8);
                    args = requestBody.split("&");
                    for (i = 0; i < args.length; i++) {
                        if (args[i].contains("=")) {
                            pair = args[i].split("=");
                            this.setParameter(pair[0], List.of(pair[1]));
                        }
                    }
                    break;
                case "text/plain; charset=UTF-8":
                case "application/json":
                default:
                    requestBody = content.toString(CharsetUtil.UTF_8);
                    args = requestBody.split("&");
                    for (i = 0; i < args.length; i++) {
                        if (args[i].contains("=")) {
                            pair = args[i].split("=");
                            this.setParameter(pair[0], List.of(pair[1]));
                        }
                    }

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
    }

    private void attach(List<FileEntity> list) {
        this.attachments = list;
    }

    public List<FileEntity> getAttachments() {
        return this.attachments;
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
        String sessionId = null;
        String cookieValue = this.request.headers().get(Header.COOKIE.name());
        if (cookieValue != null) {
            String[] cookies = cookieValue.split(";");
            for (String cookie1 : cookies) {
                String[] _cookie = cookie1.split("=");
                if (_cookie[0].trim().equalsIgnoreCase("jsessionid")) {
                    sessionId = _cookie[1];
                    break;
                }
            }
        }

        if (sessionId != null) {
            return getSession(sessionId, true);
        }

        return getSession(new StandardSessionIdGenerator().generateSessionId(), true);
    }

    @Override
    public String query() {
        return this.query;
    }

    @Override
    public Object stream() {
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
    public Request setMethod(Method method) {
        this.method = method;
        return this;
    }

    @Override
    public String uri() {
        return this.uri;
    }

    @Override
    public Request setUri(String uri) {
        this.uri = uri;
        return this;
    }

    @Override
    public String getParameter(String name) {
        if (null != this.params.get(name) && this.params.get(name).size() > 0) {
            return this.params.get(name).get(0);
        }
        return null;
    }

    @Override
    public Cookie[] cookies() {
        return cookies;
    }
}