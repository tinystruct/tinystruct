package org.tinystruct.http;

import org.tinystruct.ApplicationRuntimeException;
import org.tinystruct.data.component.Builder;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;

/**
 * SSEClient manages a single Server-Sent Events (SSE) client connection.
 * It runs as a separate thread, pulling messages from a queue and sending them to the client.
 */
public class SSEClient implements Runnable {
    /**
     * Interval in seconds between SSE keepalive heartbeats.
     * A comment line "}: keepalive\n\n" is sent when the queue is idle for this long.
     * Keeps proxies / browsers from silently closing idle connections and allows
     * fast detection of a broken pipe (write failure marks the client inactive).
     */
    public static final int HEARTBEAT_INTERVAL_SEC = 20;

    /** Raw SSE keepalive comment payload. */
    private static final byte[] KEEPALIVE_PAYLOAD = ": keepalive\n\n".getBytes(StandardCharsets.UTF_8);

    // The response object used to send data to the client
    private final Response out;
    // Thread-safe queue for messages to be sent to the client
    private final BlockingQueue<Builder> messageQueue;
    // Indicates if the client connection is active
    private volatile boolean active = true;
    private static final Logger logger = Logger.getLogger(SSEClient.class.getName());
    private volatile Thread workerThread;
    /** Epoch-ms timestamp of the last successful write (message or heartbeat). */
    private final AtomicLong lastActivityTime = new AtomicLong(System.currentTimeMillis());

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
                // Poll with a timeout instead of blocking take().
                // If no message arrives within HEARTBEAT_INTERVAL_SEC, send a keepalive
                // comment so the TCP connection stays alive through proxies/browsers and
                // so that a broken-pipe is detected quickly (write failure → active=false).
                Builder message = messageQueue.poll(HEARTBEAT_INTERVAL_SEC, TimeUnit.SECONDS);
                if (message == null) {
                    // Queue was idle — send an SSE comment as a keepalive heartbeat
                    out.writeAndFlush(KEEPALIVE_PAYLOAD);
                    lastActivityTime.set(System.currentTimeMillis());
                    logger.fine("SSEClient sent keepalive heartbeat");
                } else {
                    String event = SSEPushManager.formatSSEMessage(message);
                    out.writeAndFlush(event.getBytes(StandardCharsets.UTF_8));
                    lastActivityTime.set(System.currentTimeMillis());
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
     * Checks if the client connection is still active.
     *
     * @return true if active, false otherwise
     */
    public boolean isActive() {
        return active;
    }

    /**
     * Returns the epoch-millisecond timestamp of the last successful write
     * (either a real message or a keepalive heartbeat).
     * Useful for monitoring stale connections.
     *
     * @return last activity time in epoch milliseconds
     */
    public long getLastActivityTime() {
        return lastActivityTime.get();
    }
}
