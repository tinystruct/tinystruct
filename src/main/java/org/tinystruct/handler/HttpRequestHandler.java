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
import org.tinystruct.system.util.StringUtilities;

import java.util.Collections;
import java.util.Set;

import static io.netty.buffer.Unpooled.copiedBuffer;

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
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest msg) throws Exception {
        if (this.context == null)
            this.context = new ApplicationContext();

        this.request = new RequestBuilder(msg);

        this.service(ctx, request, context);
        context.removeAttribute("REQUEST_BODY");
        context.removeAttribute("CLAIMS");
    }


    private void service(final ChannelHandlerContext ctx, final Request request, final Context context) {

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
        try {
            context.setAttribute("HTTP_REQUEST", request);

            String query = request.query();
            if (query != null && query.length() > 1) {
                query = StringUtilities.htmlSpecialChars(query);
                if (null == (message = ApplicationManager.call(query, context))) {
                    message = "No response retrieved!";
                }
            } else {
                message = ApplicationManager.call(this.configuration.get("default.home.page").toString(), context);
            }
        } catch (Exception e) {
            StackTraceElement[] trace = e.getStackTrace();
            message = e.getMessage();
            if (trace.length > 0 && null != e.getCause()) {
                message = e.getCause().getStackTrace()[0] + ":" + trace[0].toString();
            }

            status = HttpResponseStatus.NOT_FOUND;
        }

        ByteBuf resp;
        try {
            resp = copiedBuffer(message.toString(), CharsetUtil.UTF_8);
        } catch (Exception e) {
            resp = copiedBuffer(e.getMessage(), CharsetUtil.UTF_8);
        }

        FullHttpResponse _response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, status, resp);
        Response<HttpResponse> response = new ResponseBuilder(_response);
        ResponseHeaders responseHeaders = new ResponseHeaders(response);
        Cookie cookie = new CookieImpl("jsessionid");
        cookie.setValue(request.getSession().getId());

        String host = request.headers().get(Header.HOST).toString();
        cookie.setDomain(host.substring(0, host.indexOf(":")));
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge(1800);

        response.addHeader(Header.SET_COOKIE.toString(), cookie.toString());

        responseHeaders.add(Header.CONTENT_TYPE.set("text/html; charset=UTF-8"));
        responseHeaders.add(Header.CONTENT_LENGTH.setInt(resp.readableBytes()));
        ctx.write(response.get());
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