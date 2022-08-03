package org.tinystruct.transfer;

import org.tinystruct.AbstractApplication;
import org.tinystruct.ApplicationException;
import org.tinystruct.data.component.Builder;
import org.tinystruct.system.ApplicationManager;

import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.*;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class DistributedMessageQueue extends AbstractApplication implements MessageQueue<String> {

    protected static final int DEFAULT_MESSAGE_POOL_SIZE = 10;
    private static final long TIMEOUT = 10000;
    protected final Map<String, BlockingQueue<Builder>> groups = new ConcurrentHashMap<String, BlockingQueue<Builder>>();
    protected final Map<String, Queue<Builder>> list = new ConcurrentHashMap<String, Queue<Builder>>();
    private final Lock lock = new ReentrantLock();
    private final Condition consumer = lock.newCondition();
    private ExecutorService service;

    public static void main(String[] args) throws ApplicationException {
        new DistributedMessageQueue().testing(100);
    }

    @Override
    public void init() {
        this.setAction("message/update", "take");
        this.setAction("message/save", "put");
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
    public String put(Object groupId, String sessionId, String message) {
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
        while ((message = messages.poll()) == null && (System.currentTimeMillis() - startTime) <= TIMEOUT) {
            // If waited less than 10 seconds, then continue to wait
            lock.lock();
            try {
                consumer.await(TIMEOUT, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                throw new ApplicationException(e.getMessage(), e);
            } finally {
                lock.unlock();
            }
        }

        return message != null ? message.toString() : "{}";
    }

    /**
     * Copy message to the list of each session.
     *
     * @param meetingCode
     * @param builder
     */
    private void copy(Object meetingCode, Builder builder) {

        final Collection<Entry<String, Queue<Builder>>> set = this.list.entrySet();
        final Iterator<Entry<String, Queue<Builder>>> iterator = set.iterator();
        lock.lock();
        try {
            while (iterator.hasNext()) {
                Entry<String, Queue<Builder>> list = iterator.next();
                list.getValue().add(builder);
                consumer.signalAll();
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * Wake up those threads are working in message update.
     */
    final protected void wakeup() {
        lock.lock();
        try {
            consumer.signalAll();
        } finally {
            lock.unlock();
        }
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
        return "Talk core version:1.0 stable; Released on 2017-07-24";
    }

    /**
     * This is a testing. It can be executed with the command:
     * $ bin/dispatcher --import-applications=tinystruct.examples.custom.application.talk custom.application.talk/testing/100
     *
     * @param n number of tests.
     * @return a boolean value
     * @throws ApplicationException application exception
     */
    public boolean testing(final int n) throws ApplicationException {
        this.groups.put("[M001]", new ArrayBlockingQueue<Builder>(DEFAULT_MESSAGE_POOL_SIZE));
        this.list.put("{A}", new ArrayDeque<Builder>());
        this.list.put("{B}", new ArrayDeque<Builder>());

        this.getService().execute(new Runnable() {
            @Override
            public void run() {
                int i = 0;
                while (i++ < n)
                    try {
                        ApplicationManager.call("custom.application.talk/save/[M001]/{A}/A post " + i, null);
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
                int i = 0;
                while (i++ < n)
                    try {
                        ApplicationManager.call("custom.application.talk/save/[M001]/{B}/B post " + i, null);
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
                        System.out.println("**A**:" + ApplicationManager.call("custom.application.talk/update/{A}", null));
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
                        System.out.println("**B**:" + ApplicationManager.call("custom.application.talk/update/{B}", null));
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
