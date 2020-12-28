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
import org.tinystruct.system.ApplicationManager;
import org.tinystruct.system.Configuration;
import org.tinystruct.system.util.StringUtilities;

import java.util.*;

import static io.netty.buffer.Unpooled.copiedBuffer;

public class HttpRequestHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

    private final Configuration configuration;
    private final String charsetName;
    private Context context;
    private HttpPostRequestDecoder decoder;
    private static final HttpDataFactory factory =
            new DefaultHttpDataFactory(DefaultHttpDataFactory.MINSIZE); // Disk if size exceed

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
        this.charsetName = (this.configuration.get("default.file.encoding")).toString();
    }

    public HttpRequestHandler(Configuration<String> configuration, Context context) {
        this(configuration);
        this.context = context;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest msg) throws Exception {
        this.request = msg;

        try {
            decoder = new HttpPostRequestDecoder(factory, this.request);
        } catch (HttpPostRequestDecoder.ErrorDataDecoderException e) {
            e.printStackTrace();
            writeResponse(ctx.channel(), e.getMessage(), true);
            return;
        }

        ByteBuf content = msg.content();
        content.readableBytes();
        if (this.context == null)
            this.context = new ApplicationContext();

        if (content.capacity() > 0) {
            String requestBody = content.toString(CharsetUtil.UTF_8);
            context.setAttribute("REQUEST_BODY", requestBody);
        }

 /*       switch (this.request.headers().get(HttpHeaderNames.CONTENT_TYPE)) {
            case "application/json":
                Builder data = new Builder();
                data.parse(content.toString(CharsetUtil.UTF_8));
                Set<String> keys = data.keySet();
                Iterator<String> k = keys.iterator();
                String key;
                while (k.hasNext()) {
                    key = k.next();
                    context.setAttribute(key, data.get(key));
                }
                break;
            case "application/x-www-form-urlencoded":
                String[] args = content.toString(CharsetUtil.UTF_8).split("&"), pair;
                for (int i = 0; i < args.length; i++) {
                    pair = args[i].split("=");
                    context.setAttribute(pair[0], pair[1]);
                }
                break;
            case "multipart/form-data":
                // TODO
                final HttpDataFactory FACTORY = new DefaultHttpDataFactory(DefaultHttpDataFactory.MINSIZE); // Disk if size exceed
                List<FileUpload> list = new ArrayList<>();

                HttpPostRequestDecoder decoder = new HttpPostRequestDecoder(FACTORY, msg);
                InterfaceHttpData file_data;
                while (decoder.hasNext()) {
                    file_data = decoder.next();
                    if (file_data != null && file_data.getHttpDataType() == InterfaceHttpData.HttpDataType.FileUpload) {
                        list.add((FileUpload) file_data);
                    }

                }
                // TODO
                context.setAttribute("files", list);
                break;
            case "text/plain;charset=UTF-8":
            default:
                context.setAttribute("REQUEST_BODY", content.toString(CharsetUtil.UTF_8));
                break;
        }
*/
        this.service(ctx, request, context);
        context.removeAttribute("REQUEST_BODY");
        context.resetParameters();
    }

    private QueryStringDecoder parseQuery(String uri, boolean haspath) {
        QueryStringDecoder decoder = new QueryStringDecoder(uri, haspath);
        Map<String, List<String>> parameters = decoder.parameters();
        Iterator<String> iterator = parameters.keySet().iterator();
        String key;
        while(iterator.hasNext()) {
            key = iterator.next();
            context.setParameter(key, parameters.get(key));
        }

        return decoder;
    }

    private void service(ChannelHandlerContext ctx, HttpRequest request, Context context) {
        String query = request.uri();
        HttpResponseStatus status = HttpResponseStatus.OK;
        Object message;
        try {
            if (query != null && query.length() > 1) {
                QueryStringDecoder q = parseQuery(query, true);

                if(!q.path().isEmpty()) {
                    query = q.path().substring(1);
                }

                if( null != context.getParameterValues("q") && context.getParameterValues("q").size()>0 && context.getParameterValues("q").get(0)!=null) {
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

            status = HttpResponseStatus.BAD_REQUEST;
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
