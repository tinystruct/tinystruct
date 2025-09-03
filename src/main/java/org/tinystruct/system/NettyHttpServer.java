/*******************************************************************************
 * Copyright  (c) 2013, 2025 James M. ZHOU
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package org.tinystruct.system;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.SelfSignedCertificate;
import io.netty.handler.stream.ChunkedWriteHandler;
import org.tinystruct.AbstractApplication;
import org.tinystruct.ApplicationException;
import org.tinystruct.handler.HttpRequestHandler;
import org.tinystruct.handler.HttpStaticFileHandler;
import org.tinystruct.http.Reforward;
import org.tinystruct.http.Request;
import org.tinystruct.http.Response;
import org.tinystruct.http.Session;
import org.tinystruct.system.annotation.Action;
import org.tinystruct.system.annotation.Argument;

import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.tinystruct.http.Constants.HTTP_REQUEST;
import static org.tinystruct.http.Constants.HTTP_RESPONSE;

/**
 * NettyHttpServer is a Netty-based HTTP server implementation for the tinystruct framework.
 *
 * <p>Configuration options:</p>
 * <ul>
 *   <li>server-port: The port to listen on (default: 8080)</li>
 *   <li>logging.enabled: Enable Netty channel logging (default: false)</li>
 *   <li>logging.level: Netty logging level (TRACE, DEBUG, INFO, WARN, ERROR) (default: INFO)</li>
 * </ul>
 *
 * <p>Example usage:</p>
 * <pre>
 * bin/dispatcher start --import org.tinystruct.system.NettyHttpServer --server-port 8080 --logging.enabled true --logging.level INFO
 * </pre>
 */
public class NettyHttpServer extends AbstractApplication implements Bootstrap {
    private static final boolean SSL = System.getProperty("ssl") != null;
    private static final int MAX_CONTENT_LENGTH = 1024 * 100;
    private final EventLoopGroup bossgroup;
    private final EventLoopGroup workgroup;
    private final Logger logger = Logger.getLogger(NettyHttpServer.class.getName());
    private int port = 8080;

    public NettyHttpServer() {
        if (Epoll.isAvailable()) {
            this.bossgroup = new EpollEventLoopGroup(1);
            this.workgroup = new EpollEventLoopGroup();
        } else {
            this.bossgroup = new NioEventLoopGroup(1);
            this.workgroup = new NioEventLoopGroup();
        }
    }

    @Override
    public void init() {

    }

    @Override
    public String version() {
        return null;
    }

    @Action(value = "start", description = "Start up the netty-based http server.", options = {
            @Argument(key = "server-port", description = "Server port"),
            @Argument(key = "http.proxyHost", description = "Proxy host for http"),
            @Argument(key = "http.proxyPort", description = "Proxy port for http"),
            @Argument(key = "https.proxyHost", description = "Proxy host for https"),
            @Argument(key = "https.proxyPort", description = "Proxy port for https"),
            @Argument(key = "logging.enabled", description = "Enable Netty logging (default: false)"),
            @Argument(key = "logging.level", description = "Netty logging level (TRACE, DEBUG, INFO, WARN, ERROR) (default: INFO)")
    }, example = "bin/dispatcher start --import org.tinystruct.system.NettyHttpServer --server-port 777", mode = org.tinystruct.application.Action.Mode.CLI)
    @Override
    public void start() throws ApplicationException {

        if (getContext() != null) {
            if (getContext().getAttribute("--server-port") != null) {
                this.port = Integer.parseInt(getContext().getAttribute("--server-port").toString());
            }

            if (getContext().getAttribute("--http.proxyHost") != null && getContext().getAttribute("--http.proxyPort") != null) {
                System.setProperty("http.proxyHost", getContext().getAttribute("--http.proxyHost").toString());
                System.setProperty("http.proxyPort", getContext().getAttribute("--http.proxyPort").toString());
            }

            if (getContext().getAttribute("--https.proxyHost") != null && getContext().getAttribute("--https.proxyPort") != null) {
                System.setProperty("https.proxyHost", getContext().getAttribute("--https.proxyHost").toString());
                System.setProperty("https.proxyPort", getContext().getAttribute("--https.proxyPort").toString());
            }

            // Handle logging configuration from command line arguments
            if (getContext().getAttribute("--logging.enabled") != null) {
                getConfiguration().set("logging.enabled", getContext().getAttribute("--logging.enabled").toString());
            }

            if (getContext().getAttribute("--logging.level") != null) {
                getConfiguration().set("logging.level", getContext().getAttribute("--logging.level").toString());
            }
        }

        System.out.println(ApplicationManager.call("--logo", null, org.tinystruct.application.Action.Mode.CLI));

        String charsetName = null;
        Settings settings = new Settings();
        if (settings.get("default.file.encoding") != null) charsetName = settings.get("default.file.encoding");

        if (charsetName != null && !charsetName.trim().isEmpty()) System.setProperty("file.encoding", charsetName);

        settings.set("language", "zh_CN");
        if (settings.get("system.directory") == null) settings.set("system.directory", System.getProperty("user.dir"));

        try {
            // Initialize the application manager with the configuration.
            ApplicationManager.init(settings);
        } catch (ApplicationException e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
        }
        // Add LoggingHandler only if logging is enabled in configuration
        final boolean loggingEnabled = Boolean.parseBoolean(settings.getOrDefault("logging.enabled", "false"));
        if (!loggingEnabled) {
            logger.info("Netty channel logging is disabled. Set logging.enabled=true to enable.");
        }
        long start = System.currentTimeMillis();
        try {
            // Configure SSL.
            final SslContext sslCtx;
            if (SSL) {
                SelfSignedCertificate ssc = new SelfSignedCertificate();
                sslCtx = SslContextBuilder.forServer(ssc.certificate(), ssc.privateKey()).build();
            } else {
                sslCtx = null;
            }

            final int maxContentLength = "".equalsIgnoreCase(settings.get("default.http.max_content_length")) ? MAX_CONTENT_LENGTH : Integer.parseInt(getConfiguration().get("default.http.max_content_length"));
            ServerBootstrap bootstrap = new ServerBootstrap().group(bossgroup, workgroup).channel(Epoll.isAvailable() ? EpollServerSocketChannel.class : NioServerSocketChannel.class).childHandler(new ChannelInitializer<SocketChannel>() {
                @Override
                public void initChannel(SocketChannel ch) {
                    ChannelPipeline p = ch.pipeline();
                    if (sslCtx != null) {
                        p.addLast(sslCtx.newHandler(ch.alloc()));
                    }

                    // Add LoggingHandler only if logging is enabled in configuration
                    if (loggingEnabled) {
                        // Get log level from configuration or default to INFO
                        String logLevelStr = settings.getOrDefault("logging.level", "INFO");
                        LogLevel logLevel;
                        try {
                            logLevel = LogLevel.valueOf(logLevelStr);
                        } catch (IllegalArgumentException e) {
                            logger.warning("Invalid log level: " + logLevelStr + ", using INFO");
                            logLevel = LogLevel.INFO;
                        }
                        p.addLast(new LoggingHandler(logLevel));
                    }

                    p.addLast(new HttpServerCodec(), new HttpObjectAggregator(maxContentLength), new ChunkedWriteHandler(), new HttpStaticFileHandler(), new HttpRequestHandler(getConfiguration()));
                }
            }).option(ChannelOption.SO_BACKLOG, 1024).option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 1000).childOption(ChannelOption.SO_KEEPALIVE, false).childOption(ChannelOption.TCP_NODELAY, true);

            // Bind and start to accept incoming connections.
            ChannelFuture future = bootstrap.bind(port).sync();
            logger.info("Netty server (" + port + ") startup in " + (System.currentTimeMillis() - start) + " ms");

            // Open the default browser
            getContext().setAttribute("--url", "http://localhost:" + this.port);
            ApplicationManager.call("open", getContext(), org.tinystruct.application.Action.Mode.CLI);

            // Wait until the server socket is closed.
            future.channel().closeFuture().sync();
        } catch (Exception e) {
            throw new ApplicationException(e.getMessage(), e.getCause());
        } finally {
            this.stop();
        }
    }

    @Override
    public void stop() {
        bossgroup.shutdownGracefully();
        workgroup.shutdownGracefully();
    }

    @Action(value = "error", description = "Error page")
    public Object exceptionCaught() throws ApplicationException {
        Request request = (Request) getContext().getAttribute(HTTP_REQUEST);
        Response response = (Response) getContext().getAttribute(HTTP_RESPONSE);

        Reforward reforward = new Reforward(request, response);
        this.setVariable("from", reforward.getFromURL());

        Session session = request.getSession();
        if (session.getAttribute("error") != null) {
            ApplicationException exception = (ApplicationException) session.getAttribute("error");

            String message = exception.getRootCause().getMessage();
            this.setVariable("exception.message", Objects.requireNonNullElse(message, "Unknown error"));

            StackTraceElement[] stackTrace = exception.getStackTrace();
            StringBuilder builder = new StringBuilder();
            builder.append(exception).append("\n");
            for (StackTraceElement stackTraceElement : stackTrace) {
                builder.append(stackTraceElement.toString()).append("\n");
            }
            logger.severe(builder.toString());

            return this.getVariable("exception.message").getValue().toString();
        } else {
            reforward.forward();
        }

        return "This request is forbidden!";
    }

}
