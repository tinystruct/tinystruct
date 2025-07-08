package org.tinystruct.http;

import org.tinystruct.data.component.Builder;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * SSEClient manages a single Server-Sent Events (SSE) client connection.
 * It runs as a separate thread, pulling messages from a queue and sending them to the client.
 */
public class SSEClient implements Runnable {
    // The response object used to send data to the client
    private final Response out;
    // Thread-safe queue for messages to be sent to the client
    private final BlockingQueue<Builder> messageQueue;
    // Indicates if the client connection is active
    private volatile boolean active = true;
    private static final Logger logger = Logger.getLogger(SSEClient.class.getName());

    /**
     * Constructs an SSEClient with a new message queue.
     * @param out The response object for sending data
     */
    public SSEClient(Response out) {
        this(out, new LinkedBlockingQueue<>());
    }

    /**
     * Constructs an SSEClient with a provided message queue.
     * @param out The response object for sending data
     * @param messageQueue The queue for messages to send
     */
    public SSEClient(Response out, BlockingQueue<Builder> messageQueue) {
        this.out = out;
        this.messageQueue = messageQueue;
    }

    /**
     * Enqueue a message to be sent to the client.
     * @param message The message to send
     */
    public void send(Builder message) {
        if (active) {
            messageQueue.offer(message);
        }
    }

    /**
     * Close the client connection and stop the thread.
     */
    public void close() {
        this.active = false;
        // Interrupt the thread if it's waiting on the queue
        Thread.currentThread().interrupt();
    }

    /**
     * Main loop: waits for messages and sends them to the client as SSE events.
     */
    @Override
    public void run() {
        try {
            while (active && !Thread.currentThread().isInterrupted()) {
                Builder message = messageQueue.take(); // Blocks until a message is available
                if (message != null) {
                    String event = formatSSEMessage(message);
                    out.writeAndFlush(event.getBytes(StandardCharsets.UTF_8));
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            this.active = false;
            logger.info("SSEClient thread interrupted for session");
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error in SSE client: " + e.getMessage(), e);
            this.active = false;
        }
    }

    /**
     * Formats a message as an SSE event string.
     * @param message The message to format
     * @return The formatted SSE event string
     */
    private String formatSSEMessage(Builder message) {
        StringBuilder sb = new StringBuilder();
        sb.append("event: message\ndata: ").append(message.toString()).append("\n\n");
        return sb.toString();
    }

    /**
     * Checks if the client connection is still active.
     * @return true if active, false otherwise
     */
    public boolean isActive() {
        return active;
    }
}