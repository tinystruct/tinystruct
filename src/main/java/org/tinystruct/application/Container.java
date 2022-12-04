package org.tinystruct.application;

import org.tinystruct.Application;
import org.tinystruct.system.scheduling.Scheduler;
import org.tinystruct.system.scheduling.TimeIterator;

import java.util.Map;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Application instance container. By default, the container will be automatically clean up per min.
 *
 * @author James M. ZHOU
 */
public class Container extends TimerTask {
    private static final Logger logger = Logger.getLogger(Container.class.getName());
    private final Map<String, Application> map = new ConcurrentHashMap<>(16);

    private Container() {
        Scheduler.getInstance().schedule(this, new TimeIterator(0, 0, 0), 1800000);
    }

    public static Container getInstance() {
        return SingletonHolder.container;
    }

    @Override
    public void run() {
        map.forEach((appId, b) -> {
            map.remove(appId);
            logger.fine(appId + " removed.");
        });
    }

    public void put(String appId, Application app) {
        map.putIfAbsent(appId, app);
    }

    public boolean containsKey(String appId) {
        return map.containsKey(appId);
    }

    public Application get(String appId) {
        return map.get(appId);
    }

    public void remove(String appId) {
        map.remove(appId);
    }

    private static final class SingletonHolder {
        static final Container container = new Container();
    }
}
