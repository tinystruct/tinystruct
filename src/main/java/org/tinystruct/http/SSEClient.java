package org.tinystruct.http;

import org.tinystruct.ApplicationException;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.LinkedBlockingQueue;

public class SSEClient implements Runnable {
    private final Response out;
    private final LinkedBlockingQueue<String> messageQueue = new LinkedBlockingQueue<>();
    private volatile boolean active = true;

    public SSEClient(Response out) {
        this.out = out;
    }

    public void send(String message) {
        messageQueue.offer(message);
    }

    public void close() {
        this.active = false;
    }

    @Override
    public void run() {
        try {
            while (active) {
                String message = messageQueue.poll();
                if (message != null) {
                    writeEvent("message", message);
                } else {
                    writeEvent("heartbeat", "{\"time\": " + System.currentTimeMillis() + "}");
                }
                Thread.sleep(2000); // 每2秒一次
            }
        } catch (Exception e) {
            this.active = false;
        }
    }

    private void writeEvent(String event, String data) throws ApplicationException {
        String payload = "event: " + event + "\n" +
                "data: " + data.replace("\n", "\ndata: ") + "\n\n";
        out.writeAndFlush(payload.getBytes(StandardCharsets.UTF_8));
    }

    public boolean isActive() {
        return active;
    }
}
