package org.tinystruct.http;

import org.tinystruct.data.component.Builder;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SSEClient implements Runnable {
    private final Response out;
    private final BlockingQueue<Builder> messageQueue;
    private volatile boolean active = true;
    private static final Logger logger = Logger.getLogger(SSEClient.class.getName());

    public SSEClient(Response out) {
        this(out, new LinkedBlockingQueue<>());
    }

    public SSEClient(Response out, BlockingQueue<Builder> messageQueue) {
        this.out = out;
        this.messageQueue = messageQueue;
        // Set SSE headers
        if (out instanceof ResponseBuilder) {
            ResponseBuilder builder = (ResponseBuilder) out;
            builder.setContentType("text/event-stream");
            builder.addHeader("Cache-Control", "no-cache");
            builder.addHeader("Connection", "keep-alive");
        }
    }

    public void send(Builder message) {
        messageQueue.offer(message);
    }

    public void close() {
        this.active = false;
        messageQueue.clear();
    }

    @Override
    public void run() {
        try {
            while (active) {
                Builder message = messageQueue.take();
                if (message != null) {
                    String event = formatSSEMessage(message);
                    out.writeAndFlush(event.getBytes(StandardCharsets.UTF_8));
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error in SSE client: " + e.getMessage(), e);
        } finally {
            close();
        }
    }

    private String formatSSEMessage(Builder message) {
        StringBuilder sb = new StringBuilder();
        sb.append("data: ").append(message.toString()).append("\n\n");
        return sb.toString();
    }

    public boolean isActive() {
        return active;
    }
}
