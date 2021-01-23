package org.tinystruct.handler;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.cookie.ServerCookieDecoder;
import io.netty.handler.codec.http.cookie.ServerCookieEncoder;
import io.netty.handler.codec.http.multipart.*;
import io.netty.util.CharsetUtil;
import org.tinystruct.ApplicationContext;
import org.tinystruct.application.Context;
import org.tinystruct.data.FileEntity;
import org.tinystruct.data.component.Builder;
import org.tinystruct.system.ApplicationManager;
import org.tinystruct.system.Configuration;
import org.tinystruct.system.util.StringUtilities;

import java.util.*;

import static io.netty.buffer.Unpooled.copiedBuffer;

public class HttpRequestHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

    private final Configuration configuration;
    private Context context;
    private HttpRequest request;

    static {
        DiskFileUpload.deleteOnExitTemporaryFile = true; // should delete file
        // on exit (in normal
        // exit)
        DiskFileUpload.baseDirectory = null; // system temp directory
        DiskAttribute.deleteOnExitTemporaryFile = true; // should delete file on
        // exit (in normal exit)
        DiskAttribute.baseDirectory = null; // system temp directory
    }

    public HttpRequestHandler(Configuration<String> configuration) {
        this.configuration = configuration;
    }

    public HttpRequestHandler(Configuration<String> configuration, Context context) {
        this(configuration);
        this.context = context;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest msg) throws Exception {
        this.request = msg;

        ByteBuf content = msg.content();
        content.readableBytes();
        if (this.context == null)
            this.context = new ApplicationContext();

        if (content.capacity() > 0) {
            switch (this.request.headers().get(HttpHeaderNames.CONTENT_TYPE)) {
                case "application/json":
                    Builder data = new Builder();
                    data.parse(content.toString(CharsetUtil.UTF_8));
                    Set<String> keys = data.keySet();
                    Iterator<String> k = keys.iterator();
                    String key;
                    while (k.hasNext()) {
                        key = k.next();
                        List<String> values = new ArrayList<>();
                        values.add(data.get(key).toString());
                        context.setParameter(key, values);
                    }
                    break;
                case "multipart/form-data":
                    // TODO
                    final HttpDataFactory factory = new DefaultHttpDataFactory(DefaultHttpDataFactory.MINSIZE); // Disk if size exceed
                    List<FileEntity> list = new ArrayList<>();

                    HttpPostRequestDecoder decoder = new HttpPostRequestDecoder(factory, msg);
                    InterfaceHttpData fileData;
                    while (decoder.hasNext()) {
                        fileData = decoder.next();
                        if (fileData != null && fileData.getHttpDataType() == InterfaceHttpData.HttpDataType.FileUpload) {
                            list.add((FileEntity) fileData);
                        }
                    }
                    // TODO
                    context.setFiles(list);
                case "application/x-www-form-urlencoded":
                    String requestBody = content.toString(CharsetUtil.UTF_8);
                    context.setAttribute("REQUEST_BODY", requestBody);
                    String[] args = requestBody.split("&"), pair;
                    for (int i = 0; i < args.length; i++) {
                        if (args[i].indexOf("=") != -1) {
                            pair = args[i].split("=");
                            context.setParameter(pair[0], Arrays.asList(pair[1]));
                        }
                    }
                    break;
                case "text/plain;charset=UTF-8":
                default:
                    context.setAttribute("REQUEST_BODY", content.toString(CharsetUtil.UTF_8));
                    break;
            }
        }

        this.service(ctx, request, context);
        context.removeAttribute("REQUEST_BODY");
        context.resetParameters();
    }

    private QueryStringDecoder parseQuery(String uri, boolean hasPath) {
        QueryStringDecoder decoder = new QueryStringDecoder(uri, hasPath);
        Map<String, List<String>> parameters = decoder.parameters();
        Iterator<Map.Entry<String, List<String>>> iterator = parameters.entrySet().iterator();
        Map.Entry<String, List<String>> element;
        while (iterator.hasNext()) {
            element = iterator.next();
            context.setParameter(element.getKey(), element.getValue());
        }

        return decoder;
    }

    private void service(final ChannelHandlerContext ctx, final HttpRequest request, final Context context) {
        String query = request.uri();
        HttpResponseStatus status = HttpResponseStatus.OK;
        Object message;
        try {
            if (query != null && query.length() > 1) {
                QueryStringDecoder q = parseQuery(query, true);

                if (!q.path().isEmpty()) {
                    query = q.path().substring(1);
                }

                if (null != context.getParameterValues("q") && context.getParameterValues("q").size() > 0 && context.getParameterValues("q").get(0) != null) {
                    query = context.getParameterValues("q").get(0);
                }

                query = StringUtilities.htmlSpecialChars(query);

                if (null == (message = ApplicationManager.call(query, context)))
                    message = "No response retrieved!";
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
            resp = Unpooled.copiedBuffer(message.toString(), CharsetUtil.UTF_8);
        } catch (Exception e) {
            resp = Unpooled.copiedBuffer(e.getMessage(), CharsetUtil.UTF_8);
        }

        FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, status, resp);
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/html");
        response.headers().set(HttpHeaderNames.CONTENT_LENGTH, resp.readableBytes());
        ctx.write(response);
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        ctx.flush();
    }

    private void writeResponse(Channel channel, String content, boolean forceClose) {
        // Convert the response content to a ChannelBuffer.
        ByteBuf buf = copiedBuffer(content, CharsetUtil.UTF_8);

        // Decide whether to close the connection or not.
        boolean keepAlive = HttpUtil.isKeepAlive(request) && !forceClose;

        // Build the response object.
        FullHttpResponse response = new DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1, HttpResponseStatus.OK, buf);
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain; charset=UTF-8");
        response.headers().setInt(HttpHeaderNames.CONTENT_LENGTH, buf.readableBytes());

        if (!keepAlive) {
            response.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE);
        } else if (request.protocolVersion().equals(HttpVersion.HTTP_1_0)) {
            response.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
        }

        Set<io.netty.handler.codec.http.cookie.Cookie> cookies;
        String value = request.headers().get(HttpHeaderNames.COOKIE);
        if (value == null) {
            cookies = Collections.emptySet();
        } else {
            cookies = ServerCookieDecoder.STRICT.decode(value);
        }
        if (!cookies.isEmpty()) {
            // Reset the cookies if necessary.
            for (io.netty.handler.codec.http.cookie.Cookie cookie : cookies) {
                response.headers().add(HttpHeaderNames.SET_COOKIE, ServerCookieEncoder.STRICT.encode(cookie));
            }
        }
        // Write the response.
        ChannelFuture future = channel.writeAndFlush(response);
        // Close the connection after the write operation is done if necessary.
        if (!keepAlive) {
            future.addListener(ChannelFutureListener.CLOSE);
        }
    }
}
