package org.tinystruct.http;

import org.tinystruct.ApplicationRuntimeException;
import org.tinystruct.data.component.Builder;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
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
    private volatile Thread workerThread;

    /**
     * Constructs an SSEClient with a new message queue.
     *
     * @param out The response object for sending data
     */
    public SSEClient(Response out) {
        this(out, new LinkedBlockingQueue<>());
    }

    /**
     * Constructs an SSEClient with a provided message queue.
     *
     * @param out          The response object for sending data
     * @param messageQueue The queue for messages to send
     */
    public SSEClient(Response out, BlockingQueue<Builder> messageQueue) {
        this.out = out;
        this.messageQueue = messageQueue;
    }

    /**
     * Enqueue a message to be sent to the client.
     *
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
        if (workerThread != null) {
            workerThread.interrupt();
        }
    }

    /**
     * Return the response.
     *
     * @return out
     */
    public Response getResponse(){
        return this.out;
    }

    /**
     * Main loop: waits for messages and sends them to the client as SSE events.
     */
    @Override
    public void run() {
        workerThread = Thread.currentThread();
        try {
            while (active && !workerThread.isInterrupted()) {
                Builder message = messageQueue.take(); // Blocks until a message is available
                if (message != null) {
                    String event = formatSSEMessage(message);
                    out.writeAndFlush(event.getBytes(StandardCharsets.UTF_8));
                }
            }
        } catch (InterruptedException e) {
            workerThread.interrupt();
            this.active = false;
            logger.info("SSEClient thread interrupted for session");
        } catch (Exception e) {
            // Check if this is a normal connection closure
            String message = e.getMessage();
            if (message != null && (
                    message.contains("你的主机中的软件中止了一个已建立的连接") ||
                            message.contains("An established connection was aborted") ||
                            message.contains("Connection reset by peer") ||
                            message.contains("Broken pipe"))) {

                // This is a normal SSE disconnection - log at DEBUG level
                logger.fine("SSE client disconnected normally: " + message);
                this.active = false;
            } else {
                // This is for unexpected exceptions; still stop the client.
                this.active = false;
                // This is an unexpected error - log at SEVERE level
                logger.severe("Unexpected error in SSE client: " + message);
                throw new ApplicationRuntimeException(message, e);
            }
        }
    }

    /**
     * Formats a message as an SSE event string.
     *
     * @param message The message to format
     * @return The formatted SSE event string
     */
    private String formatSSEMessage(Builder message) {
        String type = message.get("type") != null ? message.get("type").toString() : null;
        if ("connect".equals(type)) {
            return "event: connect\ndata: Connected\n\n";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("data: ").append(message).append("\n\n");
        return sb.toString();
    }

    /**
     * Checks if the client connection is still active.
     *
     * @return true if active, false otherwise
     */
    public boolean isActive() {
        return active;
    }
}