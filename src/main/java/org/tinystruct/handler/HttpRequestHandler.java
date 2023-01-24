package org.tinystruct.handler;

import io.jsonwebtoken.*;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.cookie.ServerCookieDecoder;
import io.netty.handler.codec.http.cookie.ServerCookieEncoder;
import io.netty.handler.codec.http.multipart.DiskAttribute;
import io.netty.handler.codec.http.multipart.DiskFileUpload;
import io.netty.util.CharsetUtil;
import org.tinystruct.ApplicationContext;
import org.tinystruct.application.Context;
import org.tinystruct.http.Cookie;
import org.tinystruct.http.Header;
import org.tinystruct.http.*;
import org.tinystruct.http.security.JWTManager;
import org.tinystruct.system.ApplicationManager;
import org.tinystruct.system.Configuration;
import org.tinystruct.system.Language;
import org.tinystruct.system.util.StringUtilities;

import java.util.Collections;
import java.util.Set;

import static io.netty.buffer.Unpooled.copiedBuffer;
import static org.tinystruct.Application.LANGUAGE;
import static org.tinystruct.Application.METHOD;
import static org.tinystruct.http.Constants.*;

public class HttpRequestHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

    static {
        DiskFileUpload.deleteOnExitTemporaryFile = true; // should delete file
        // on exit (in normal
        // exit)
        DiskFileUpload.baseDirectory = null; // system temp directory
        DiskAttribute.deleteOnExitTemporaryFile = true; // should delete file on
        // exit (in normal exit)
        DiskAttribute.baseDirectory = null; // system temp directory
    }

    private final Configuration<String> configuration;
    private Context context;
    private Request<FullHttpRequest> request;

    public HttpRequestHandler(Configuration<String> configuration) {
        this.configuration = configuration;
    }

    public HttpRequestHandler(Configuration<String> configuration, Context context) {
        this(configuration);
        this.context = context;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest msg) {
        if (this.context == null)
            this.context = new ApplicationContext();
        // Decide whether to close the connection or not.
        boolean keepAlive = HttpUtil.isKeepAlive(msg);

        this.request = new RequestBuilder(msg);
        this.context.setId(request.getSession().getId());
        this.service(ctx, request, context, keepAlive);
    }

    private void service(final ChannelHandlerContext ctx, final Request request, final Context context, boolean keepAlive) {
        Headers headers = request.headers();
        String auth, token;
        Object message;
        if (headers.get(Header.AUTHORIZATION) != null && (auth = headers.get(Header.AUTHORIZATION).toString()) != null && auth.startsWith("Bearer ")) {
            JWTManager manager = new JWTManager();
            String secret;
            if ((secret = configuration.get("jwt.secret")) != null) {
                manager.withSecret(secret);
            }

            token = auth.substring(7);
            try {
                Jws<Claims> claims = manager.parseToken(token);
                context.setAttribute("CLAIMS", claims);
            } catch (ExpiredJwtException | UnsupportedJwtException | MalformedJwtException | SignatureException | IllegalArgumentException e) {
                ByteBuf resp = copiedBuffer(e.getMessage(), CharsetUtil.UTF_8);
                FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.UNAUTHORIZED, resp);
                response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/html");
                response.headers().set(HttpHeaderNames.CONTENT_LENGTH, resp.readableBytes());
                ctx.write(response);
                ctx.close();
                return;
            }
        }

        HttpResponseStatus status = HttpResponseStatus.OK;
        ResponseBuilder response = new ResponseBuilder(new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, status));
        try {
            context.setAttribute(HTTP_REQUEST, request);
            context.setAttribute(HTTP_RESPONSE, response);
            context.setAttribute(HTTP_HOST, request.headers().get(Header.HOST));

            String lang = request.getParameter("lang"), language = "";
            if (lang != null && lang.trim().length() > 0) {
                String name = lang.replace('-', '_');

                if (Language.support(name) && !lang.equalsIgnoreCase(this.configuration.get("language"))) {
                    String[] local = name.split("_");
                    context.setAttribute(LANGUAGE, name);
                    language = "lang=" + local[0] + "-" + local[1].toUpperCase() + "&";
                }
            }

            String url_prefix = "/";
            if (this.configuration.get("default.url_rewrite") != null && !"enabled".equalsIgnoreCase(this.configuration.get("default.url_rewrite"))) {
                url_prefix = "/?" + language + "q=";
            }

            String hostName;
            if ((hostName = this.configuration.get("default.hostname")) != null) {
                if (hostName.length() <= 3) {
                    hostName = request.headers().get(Header.HOST).toString();
                }
            } else {
                hostName = request.headers().get(Header.HOST).toString();
            }

            String ssl_enabled, http_protocol = "http://";
            boolean ssl;
            if ((ssl_enabled = this.configuration.get("ssl.enabled")) != null) {
                ssl = Boolean.parseBoolean(ssl_enabled);

                if (ssl) http_protocol = "https://";
            }

            context.setAttribute(HTTP_HOST, http_protocol + hostName + url_prefix);
            context.setAttribute(METHOD, request.method());

            String query = request.query();
            if (query != null && query.length() > 1) {
                query = StringUtilities.htmlSpecialChars(query);
                if (null == (message = ApplicationManager.call(query, context))) {
                    message = "No response retrieved!";
                } else if (message instanceof Response) {
                    // Write the response.
                    ChannelFuture future = ctx.writeAndFlush(((Response) message).get());
                    // Close the connection after the write operation is done if necessary.
                    if (!keepAlive) {
                        future.addListener(ChannelFutureListener.CLOSE);
                    }
                    return;
                }
            } else {
                message = ApplicationManager.call(this.configuration.get("default.home.page"), context);
            }
        } catch (Exception e) {
            StackTraceElement[] trace = e.getStackTrace();
            message = e.getMessage();
            if (trace.length > 0 && null != e.getCause()) {
                message = e.getCause().toString();
            }

            status = HttpResponseStatus.NOT_FOUND;
        }

        ByteBuf resp;
        try {
            if (message instanceof byte[]) {
                resp = copiedBuffer((byte[]) message);
            } else {
                resp = copiedBuffer(message.toString(), CharsetUtil.UTF_8);
            }
        } catch (Exception e) {
            resp = copiedBuffer(e.getMessage(), CharsetUtil.UTF_8);
        }

        FullHttpResponse replacement = response.get().replace(resp);
        response = new ResponseBuilder(replacement);
        Headers responseHeaders = response.headers();
        Cookie cookie = new CookieImpl(JSESSIONID);
        cookie.setValue(request.getSession().getId());

        String host = request.headers().get(Header.HOST).toString();
        if (host.contains(":"))
            cookie.setDomain(host.substring(0, host.indexOf(":")));
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge(1800);

        responseHeaders.add(Header.SET_COOKIE.set(cookie));
        if (!responseHeaders.contains(Header.CONTENT_TYPE))
            responseHeaders.add(Header.CONTENT_TYPE.set("text/html; charset=UTF-8"));
        responseHeaders.add(Header.CONTENT_LENGTH.setInt(resp.readableBytes()));

        // Write the response.
        ChannelFuture future = ctx.writeAndFlush(response.get());
        // Close the connection after the write operation is done if necessary.
        if (!keepAlive) {
            future.addListener(ChannelFutureListener.CLOSE);
        }
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        ctx.flush();
    }

    private void writeResponse(Channel channel, String content, boolean forceClose, HttpMessage msg) {
        // Convert the response content to a ChannelBuffer.
        ByteBuf buf = copiedBuffer(content, CharsetUtil.UTF_8);

        // Decide whether to close the connection or not.
        boolean keepAlive = HttpUtil.isKeepAlive(msg) && !forceClose;

        // Build the response object.
        FullHttpResponse _response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, buf);
        Response<HttpResponse> response = new ResponseBuilder(_response);
        ResponseHeaders responseHeaders = new ResponseHeaders(response);
        responseHeaders.add(Header.CONTENT_TYPE.set("text/plain; charset=UTF-8"));
        responseHeaders.add(Header.CONTENT_LENGTH.setInt(buf.readableBytes()));

        if (!keepAlive) {
            responseHeaders.add(Header.CONNECTION.set(Header.StandardValue.CLOSE));
        } else if (request.version() == Version.HTTP1_0) {
            responseHeaders.add(Header.CONNECTION.set(Header.StandardValue.KEEP_ALIVE));
        }

        Set<io.netty.handler.codec.http.cookie.Cookie> cookies;
        Object value = request.headers().get(Header.COOKIE);
        if (value == null) {
            cookies = Collections.emptySet();
        } else {
            cookies = ServerCookieDecoder.STRICT.decode(value.toString());
        }
        if (!cookies.isEmpty()) {
            // Reset the cookies if necessary.
            for (io.netty.handler.codec.http.cookie.Cookie cookie : cookies) {
                responseHeaders.add(Header.SET_COOKIE.set(ServerCookieEncoder.STRICT.encode(cookie)));
            }
        }
        // Write the response.
        ChannelFuture future = channel.writeAndFlush(response.get());
        // Close the connection after the write operation is done if necessary.
        if (!keepAlive) {
            future.addListener(ChannelFutureListener.CLOSE);
        }
    }
}