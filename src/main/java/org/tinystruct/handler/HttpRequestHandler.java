package org.tinystruct.handler;

import io.jsonwebtoken.*;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.multipart.DiskAttribute;
import io.netty.handler.codec.http.multipart.DiskFileUpload;
import io.netty.util.CharsetUtil;
import org.tinystruct.ApplicationContext;
import org.tinystruct.ApplicationException;
import org.tinystruct.application.Context;
import org.tinystruct.http.Cookie;
import org.tinystruct.http.Header;
import org.tinystruct.http.*;
import org.tinystruct.http.security.JWTManager;
import org.tinystruct.system.ApplicationManager;
import org.tinystruct.system.Configuration;
import org.tinystruct.system.Language;
import org.tinystruct.system.util.StringUtilities;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

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

    public HttpRequestHandler(Configuration<String> configuration) {
        this.configuration = configuration;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest msg) {
        // Decide whether to close the connection or not.
        boolean keepAlive = HttpUtil.isKeepAlive(msg);
        boolean ssl = Boolean.parseBoolean(configuration.getOrDefault("ssl.enabled", "false"));

        Request<FullHttpRequest, Object> request = new RequestBuilder(msg, ssl);
        Context context = new ApplicationContext();
        context.setId(request.getSession().getId());
        this.service(ctx, request, context, keepAlive);
    }

    private void service(final ChannelHandlerContext ctx, final Request<FullHttpRequest, Object> request, final Context context, boolean keepAlive) {
        if (!authenticateRequest(request, context)) {
            sendErrorResponse(ctx, HttpResponseStatus.UNAUTHORIZED, "Invalid or expired token.");
            return;
        }

        String[] parameterNames = request.parameterNames();
        for (String parameter: parameterNames) {
            if(parameter.startsWith("--")) {
                context.setAttribute(parameter, request.getParameter(parameter));
            }
        }

        HttpResponseStatus status = HttpResponseStatus.OK;
        ResponseBuilder response = new ResponseBuilder(new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, status), ctx);
        String host = request.headers().get(Header.HOST).toString();
        Object message;
        try {
            String lang = request.getParameter("lang");
            if (lang != null && !lang.trim().isEmpty()) {
                String name = lang.replace('-', '_');

                if (Language.support(name) && !lang.equalsIgnoreCase(this.configuration.get("language"))) {
                    context.setAttribute(LANGUAGE, name);
                }
            }

            String url_prefix = "/";
            if (this.configuration.get("default.url_rewrite") != null && !"enabled".equalsIgnoreCase(this.configuration.get("default.url_rewrite"))) {
                url_prefix = "/?q=";
            }

            String hostName;
            if ((hostName = this.configuration.get("default.hostname")) != null) {
                if (hostName.length() <= 3) {
                    hostName = host;
                }
            } else {
                hostName = host;
            }

            String http_protocol = "http://";
            if (request.isSecure()) {
                http_protocol = "https://";
            }

            context.setAttribute(HTTP_HOST, http_protocol + hostName + url_prefix);
            context.setAttribute(METHOD, request.method());
            context.setAttribute(HTTP_REQUEST, request);
            context.setAttribute(HTTP_RESPONSE, response);

            // Check if this is an SSE request
            if (isSSE(request)) {
                handleSSE(ctx, request, response, context, keepAlive);
                return;
            }

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
                message = ApplicationManager.call(this.configuration.getOrDefault("default.home.page", "say/Praise the Lord."), context);
            }
        } catch (ApplicationException e) {
            StackTraceElement[] trace = e.getStackTrace();
            message = e.getMessage();
            if (trace.length > 0 && null != e.getCause()) {
                message = e.getCause().toString();
            }

            response.setStatus(Objects.requireNonNull(ResponseStatus.valueOf(e.getStatus())));
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
        response = new ResponseBuilder(replacement, ctx);
        ResponseHeaders responseHeaders = new ResponseHeaders(response);
        boolean sessionCookieExists = false;
        for (Cookie cookie : request.cookies()) {
            if (cookie.name().equalsIgnoreCase(JSESSIONID)) {
                sessionCookieExists = true;
                break;
            }
        }

        if (!sessionCookieExists) {
            Cookie cookie = new CookieImpl(JSESSIONID);
            if (host.contains(":"))
                cookie.setDomain(host.substring(0, host.indexOf(":")));
            cookie.setValue(context.getId());
            cookie.setHttpOnly(true);
            cookie.setPath("/");
            cookie.setMaxAge(-1);

            responseHeaders.add(Header.SET_COOKIE.set(cookie));
        }

        if (!responseHeaders.contains(Header.CONTENT_TYPE))
            response.setContentType("text/html; charset=UTF-8");

        switch (response.status()) {
            case TEMPORARY_REDIRECT:
            case MOVED_PERMANENTLY:
            case PERMANENT_REDIRECT:
                keepAlive = false;
                break;
            default:
                responseHeaders.add(Header.CONTENT_LENGTH.setInt(resp.readableBytes()));
                break;
        }

        // Write the response.
        ChannelFuture future = ctx.writeAndFlush(response.get());
        // Close the connection after the write operation is done if necessary.
        if (!keepAlive) {
            future.addListener(ChannelFutureListener.CLOSE);
        }
    }

    private boolean authenticateRequest(Request<FullHttpRequest, Object> request, Context context) {
        Object authorization;
        if ((authorization = request.headers().get(Header.AUTHORIZATION)) != null) {
            String authHeader = authorization.toString();
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                JWTManager jwtManager = new JWTManager();
                jwtManager.withSecret(configuration.get("jwt.secret"));

                try {
                    Jws<Claims> claims = jwtManager.parseToken(token);
                    context.setAttribute("CLAIMS", claims);
                    return true;
                } catch (JwtException e) {
                    // Log authentication failure
                    return false;
                }
            }
        }
        return true; // Allow requests without a token
    }

    private boolean isSSE(Request<FullHttpRequest, Object> request) {
        Object acceptHeader = request.headers().get(Header.ACCEPT);
        return acceptHeader != null && acceptHeader.toString().contains("text/event-stream");
    }

    private void handleSSE(final ChannelHandlerContext ctx, final Request<FullHttpRequest, Object> request,
                           Response<FullHttpResponse, FullHttpResponse> response, final Context context, boolean keepAlive) {
        // Use the existing response parameter and configure it for SSE
        ResponseHeaders responseHeaders = new ResponseHeaders(response);

        // Set SSE headers using the existing response infrastructure
        responseHeaders.add(Header.CONTENT_TYPE.set("text/event-stream"));
        responseHeaders.add(Header.CACHE_CONTROL.set("no-cache"));
        responseHeaders.add(Header.CONNECTION.set("keep-alive"));
        responseHeaders.add(Header.TRANSFER_ENCODING.set("chunked"));
        responseHeaders.add(new Header("X-Accel-Buffering").set("no"));

        // Handle CORS if needed
        Object origin = request.headers().get(Header.ORIGIN);
        if (origin != null) {
            responseHeaders.add(new Header("Access-Control-Allow-Origin").set(origin.toString()));
            responseHeaders.add(new Header("Access-Control-Allow-Credentials").set("true"));
        }

        // CRITICAL: Write the SSE response headers first
        HttpResponse initialResponse = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
        // Copy headers from our response to the initial response
        FullHttpResponse fullResponse = response.get();
        initialResponse.headers().setAll(fullResponse.headers());
        ctx.writeAndFlush(initialResponse);

        // Send initial connection event
        String connectEvent = "event: connect\ndata: Connected\n\n";
        ByteBuf connectData = copiedBuffer(connectEvent, StandardCharsets.UTF_8);
        DefaultHttpContent connectContent = new DefaultHttpContent(connectData);
        ChannelFuture future = ctx.writeAndFlush(connectContent);

        try {
            String query = request.query();
            if (query != null && query.length() > 1) {
                query = StringUtilities.htmlSpecialChars(query);
                ApplicationManager.call(query, context);
            }
        } catch (ApplicationException e) {
            // Send error event to client
            String errorEvent = "event: error\ndata: " + e.getMessage() + "\n\n";
            ByteBuf errorData = copiedBuffer(errorEvent, StandardCharsets.UTF_8);
            DefaultHttpContent errorContent = new DefaultHttpContent(errorData);
            ctx.writeAndFlush(errorContent);

            // Log the exception but don't close the connection for SSE
            System.err.println("SSE Application Exception: " + e.getMessage());
        }
        
        // For SSE, we keep the connection alive
        if (!keepAlive) {
            future.addListener(ChannelFutureListener.CLOSE);
        }
    }

    private void sendErrorResponse(ChannelHandlerContext ctx, HttpResponseStatus status, String message) {
        ByteBuf content = copiedBuffer(message, CharsetUtil.UTF_8);
        FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, status, content);
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain; charset=UTF-8");
        response.headers().set(HttpHeaderNames.CONTENT_LENGTH, content.readableBytes());
        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        ctx.flush();
    }

}