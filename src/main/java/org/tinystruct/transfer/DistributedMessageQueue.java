package org.tinystruct.transfer;

import org.tinystruct.AbstractApplication;
import org.tinystruct.ApplicationContext;
import org.tinystruct.ApplicationException;
import org.tinystruct.data.component.Builder;
import org.tinystruct.system.ApplicationManager;
import org.tinystruct.valve.Lock;
import org.tinystruct.valve.Watcher;

import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.*;

public class DistributedMessageQueue extends AbstractApplication implements MessageQueue<String> {

    private static final long TIMEOUT = 100;
    protected static final int DEFAULT_MESSAGE_POOL_SIZE = 10;
    protected final Map<String, BlockingQueue<Builder>> groups = Maps.GROUPS;
    protected final Map<String, Queue<Builder>> list = Maps.LIST;
    protected final Map<String, List<String>> sessions = Maps.SESSIONS;
    private ExecutorService service;
    private final Lock lock = Watcher.getInstance().acquire();

    @Override
    public void init() {
        this.setAction("message/take", "take");
        this.setAction("message/put", "put");
        this.setAction("message/version", "version");
        this.setAction("message/testing", "testing");

        if (this.service != null) {
            Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
                @Override
                public void run() {
                    service.shutdown();
                    while (true) {
                        try {
                            System.out.println("Waiting for the service to terminate...");
                            if (service.awaitTermination(5, TimeUnit.SECONDS)) {
                                System.out.println("Service will be terminated soon.");
                                break;
                            }
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }));
        }
    }

    /**
     * To be used for testing.
     *
     * @param groupId   group id
     * @param sessionId session id
     * @param message   message
     * @return message
     */
    public final String put(final Object groupId, final String sessionId, final String message) {
        if (groupId != null) {
            if (message != null && !message.isEmpty()) {
                final Builder builder = new Builder();
                builder.put("user", "user_" + sessionId);
                builder.put("time", System.nanoTime());
                builder.put("message", filter(message));
                builder.put("session_id", sessionId);

                return this.save(groupId, builder);
            }
        }

        return "{}";
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
        if ((this.groups.get(groupId)) == null) {
            this.groups.put(groupId.toString(), new ArrayBlockingQueue<Builder>(DEFAULT_MESSAGE_POOL_SIZE));
        }

        try {
            this.groups.get(groupId).put(builder);

            final BlockingQueue<Builder> messages = this.groups.get(groupId);
            this.getService().execute(new Runnable() {
                @Override
                public void run() {
                    Builder message;
                    if ((message = messages.poll()) == null)
                        return;
                    copy(groupId, message);
                }
            });

            return builder.toString();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return "{}";
    }

    private ExecutorService getService() {
        return this.service != null ? this.service
                : new ThreadPoolExecutor(0, 10, TIMEOUT, TimeUnit.MILLISECONDS, new SynchronousQueue<Runnable>());
    }

    /**
     * Poll message from the messages of the session specified sessionId.
     *
     * @param sessionId session id
     * @return message
     * @throws ApplicationException application exception
     */
    public final String take(final String sessionId) throws ApplicationException {
        Builder message;
        Queue<Builder> messages = this.list.get(sessionId);
        // If there is a new message, then return it directly
        if ((message = messages.poll()) != null)
            return message.toString();
        long startTime = System.currentTimeMillis();
        // If waited less than 10 seconds, then continue to wait
        try {
            lock.tryLock(TIMEOUT, TimeUnit.MILLISECONDS);
            while ((message = messages.poll()) == null && (System.currentTimeMillis() - startTime) <= TIMEOUT) {
            }
        } finally {
            lock.unlock();
        }

        return message != null ? message.toString() : "{}";
    }

    /**
     * Copy message to the list of each session.
     *
     * @param groupId group Id
     * @param builder
     */
    private void copy(final Object groupId, final Builder builder) {
        final List<String> _sessions;

        if ((_sessions = this.sessions.get(groupId)) != null) {
            final Collection<Entry<String, Queue<Builder>>> set = this.list.entrySet();
            final Iterator<Entry<String, Queue<Builder>>> iterator = set.iterator();
            try {
                lock.lock();
                while (iterator.hasNext()) {
                    Entry<String, Queue<Builder>> list = iterator.next();
                    if (_sessions.contains(list.getKey())) {
                        list.getValue().add(builder);
                    }
                }
            } finally {
                lock.unlock();
            }
        }
    }

    /**
     * Wake up those threads are working in message update.
     */
    final protected void wakeup() {

    }

    /**
     * This function can be override.
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
    public boolean testing(final int n) throws ApplicationException {
        this.sessions.put("[M001]", List.of("{A}","{B}"));
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
                        e.printStackTrace();
                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
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
                        e.printStackTrace();
                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
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
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
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
                        e.printStackTrace();
                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
            }
        });

        return true;
    }

}

class Maps {
    public static final Map<String, BlockingQueue<Builder>> GROUPS = new ConcurrentHashMap<String, BlockingQueue<Builder>>();
    public static final Map<String, Queue<Builder>> LIST = new ConcurrentHashMap<String, Queue<Builder>>();
    public static final Map<String, List<String>> SESSIONS = new ConcurrentHashMap<String, List<String>>();
}
