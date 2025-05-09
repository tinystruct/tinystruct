package org.tinystruct.transfer;

import org.tinystruct.AbstractApplication;
import org.tinystruct.ApplicationException;
import org.tinystruct.data.component.Builder;
import org.tinystruct.system.ApplicationManager;
import org.tinystruct.system.annotation.Action;
import org.tinystruct.valve.Lock;
import org.tinystruct.valve.Watcher;

import java.util.ArrayDeque;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DistributedMessageQueue extends AbstractApplication implements MessageQueue<String> {

    protected static final int DEFAULT_MESSAGE_POOL_SIZE = 10;
    private static final long TIMEOUT = 100;
    protected final Map<String, BlockingQueue<Builder>> groups = Maps.GROUPS;
    protected final Map<String, Queue<Builder>> list = Maps.LIST;
    protected final Map<String, Set<String>> sessions = Maps.SESSIONS;
    private final Lock lock = Watcher.getInstance().acquire();
    private ExecutorService service;
    private static final Logger logger = Logger.getLogger(DistributedMessageQueue.class.getName());

    @Override
    public void init() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (service != null) {
                service.shutdown();
                try {
                    if (!service.awaitTermination(5, TimeUnit.SECONDS)) {
                        service.shutdownNow();
                    }
                } catch (InterruptedException e) {
                    service.shutdownNow();
                    Thread.currentThread().interrupt();
                    logger.log(Level.SEVERE, "Service shutdown interrupted", e);
                }
            }
        }));
    }

    /**
     * To be used for testing.
     *
     * @param groupId   group id
     * @param sessionId session id
     * @param message   message
     * @return message
     */
    @Action("message/put")
    @Override
    public final String put(final Object groupId, final String sessionId, final String message) {
        if (groupId == null || message == null || message.isEmpty()) {
            return "{}";
        }

        final Builder builder = new Builder();
        builder.put("user", "user_" + sessionId);
        builder.put("time", System.nanoTime());
        builder.put("message", filter(message));
        builder.put("session_id", sessionId);

        return this.save(groupId, builder);
    }

    /**
     * Save message and create a thread for copying it to message list of each
     * session.
     *
     * @param groupId group id
     * @param builder message
     * @return builder
     */
    public final String save(final Object groupId, final Builder builder) {
        return this.save(groupId, builder, null);
    }

    /**
     * Save message and create a thread for copying it to message list of each
     * session.
     *
     * @param groupId  group id
     * @param builder  message
     * @param listener listener
     * @return builder
     */
    public final String save(final Object groupId, final Builder builder, Runnable listener) {
        String group = groupId.toString();
        this.groups.computeIfAbsent(group, k -> new ArrayBlockingQueue<Builder>(DEFAULT_MESSAGE_POOL_SIZE));

        try {
            this.groups.get(group).put(builder);

            final BlockingQueue<Builder> messages = this.groups.get(groupId.toString());
            this.getService().execute(new Runnable() {
                @Override
                public void run() {
                    Builder message;
                    if ((message = messages.poll()) == null) {
                        return;
                    }
                    copy(groupId, message);

                    if (listener != null)
                        getService().execute(listener);
                }
            });

            return builder.toString();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.log(Level.SEVERE, "Message save interrupted", e);
            return "{}";
        }
    }

    private ExecutorService getService() {
        return this.service != null ? this.service
                : (this.service = new ThreadPoolExecutor(10, 20, TIMEOUT, TimeUnit.MILLISECONDS, new SynchronousQueue<>(true), new ThreadPoolExecutor.CallerRunsPolicy()));
    }

    /**
     * Poll message from the messages of the session specified sessionId.
     *
     * @param sessionId session id
     * @return message
     * @throws ApplicationException application exception
     */
    @Action("message/take")
    @Override
    public final String take(final String sessionId) throws ApplicationException {
        Builder message;
        Queue<Builder> messages = this.list.get(sessionId);
        // If there is a new message, then return it directly
        if ((message = messages.poll()) != null)
            return message.toString();

        long startTime = System.currentTimeMillis();
        // Wait for new messages within the timeout period
        try {
            lock.tryLock(TIMEOUT, TimeUnit.MILLISECONDS);
            while ((message = messages.poll()) == null && (System.currentTimeMillis() - startTime) <= TIMEOUT) {
                // Allow for other threads to execute while waiting
            }
        } finally {
            lock.unlock();
        }

        return message != null ? message.toString() : "{}";
    }

    /**
     * Copy message to the list of each session.
     *
     * @param groupId group ID
     * @param builder message
     */
    private void copy(final Object groupId, final Builder builder) {
        Set<String> groupSessions = sessions.get(groupId.toString());
        if (groupSessions == null || groupSessions.isEmpty()) {
            return;
        }

        // Pre-filter sessions that should receive the message
        for (String sessionId : groupSessions) {
            Queue<Builder> sessionQueue = list.get(sessionId);
            if (sessionQueue != null) {
                try {
                    lock.lock();
                    if (!sessionQueue.offer(builder)) {
                        logger.warning("Failed to copy message to session: " + sessionId);
                    }
                } finally {
                    lock.unlock();
                }
            }
        }
    }

    /**
     * Wake up those threads are working in message update.
     */
    final protected void wakeup() {

    }

    /**
     * This function can be overridden.
     *
     * @param text text
     * @return filtered text
     */
    protected String filter(String text) {
        return text;
    }

    @Override
    public String version() {
        return "Talk core version:1.0 stable; Released on 2023-07-24";
    }

    /**
     * This is a testing. It can be executed with the command:
     * $ bin/dispatcher --import org.tinystruct.transfer.DistributedMessageQueue message/testing/100
     *
     * @param n number of tests.
     * @return a boolean value
     * @throws ApplicationException application exception
     */
    @Action("message/testing")
    public boolean testing(final int n) throws ApplicationException {
        this.sessions.put("[M001]", Set.of("{A}", "{B}"));
        this.groups.put("[M001]", new ArrayBlockingQueue<Builder>(DEFAULT_MESSAGE_POOL_SIZE));
        this.list.put("{A}", new ArrayDeque<Builder>());
        this.list.put("{B}", new ArrayDeque<Builder>());

        this.getService().execute(new Runnable() {
            int i = 0;

            @Override
            public void run() {
                while (i++ < n)
                    try {
                        ApplicationManager.call("message/put/[M001]/{A}/A post " + i, null);
                        Thread.sleep(1);
                    } catch (ApplicationException e) {
                        // TODO Auto-generated catch block
                        logger.log(Level.SEVERE, e.getMessage(), e);
                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        logger.log(Level.SEVERE, e.getMessage(), e);
                    }
            }
        });

        this.getService().execute(new Runnable() {
            int i = 0;

            @Override
            public void run() {
                while (i++ < n)
                    try {
                        ApplicationManager.call("message/put/[M001]/{B}/B post " + i, null);
                        Thread.sleep(1);
                    } catch (ApplicationException e) {
                        // TODO Auto-generated catch block
                        logger.log(Level.SEVERE, e.getMessage(), e);
                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        logger.log(Level.SEVERE, e.getMessage(), e);
                    }
            }
        });

        this.getService().execute(new Runnable() {
            @Override
            public void run() {
                // TODO Auto-generated method stub
                System.out.println("[A] is started...");
                while (true)
                    try {
                        System.out.println("**A**:" + ApplicationManager.call("message/take/{A}", null));
                        Thread.sleep(1);
                    } catch (ApplicationException e) {
                        logger.log(Level.SEVERE, e.getMessage(), e);
                    } catch (InterruptedException e) {
                        logger.log(Level.SEVERE, e.getMessage(), e);
                    }
            }
        });

        this.getService().execute(new Runnable() {
            @Override
            public void run() {
                // TODO Auto-generated method stub
                System.out.println("[B] is started...");
                while (true)
                    try {
                        System.out.println("**B**:" + ApplicationManager.call("message/take/{B}", null));
                        Thread.sleep(1);
                    } catch (ApplicationException e) {
                        // TODO Auto-generated catch block
                        logger.log(Level.SEVERE, e.getMessage(), e);
                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        logger.log(Level.SEVERE, e.getMessage(), e);
                    }
            }
        });

        return true;
    }

}

class Maps {
    public static final Map<String, BlockingQueue<Builder>> GROUPS = new ConcurrentHashMap<String, BlockingQueue<Builder>>();
    public static final Map<String, Queue<Builder>> LIST = new ConcurrentHashMap<String, Queue<Builder>>();
    public static final Map<String, Set<String>> SESSIONS = new ConcurrentHashMap<String, Set<String>>();
}
