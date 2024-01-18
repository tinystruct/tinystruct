package org.tinystruct.application;

import org.tinystruct.Application;
import org.tinystruct.system.scheduling.Scheduler;
import org.tinystruct.system.scheduling.TimeIterator;

import java.util.Map;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Container for managing and cleaning up application instances.
 * By default, the container will be automatically cleaned up every 30 minutes.
 * This class uses a TimerTask for scheduled cleanup.
 *
 * The container is thread-safe and optimized for performance.
 *
 * @author James M. ZHOU
 */
public final class Container extends TimerTask {
    private static final Logger logger = Logger.getLogger(Container.class.getName());

    // Use ConcurrentHashMap for thread safety and performance
    private final Map<String, Application> applicationMap = new ConcurrentHashMap<>(16);

    private Container() {
        // Schedule the container for cleanup every 30 minutes
        Scheduler.getInstance().schedule(this, new TimeIterator(0, 0, 0), 1800000);
    }

    /**
     * Get the singleton instance of Container.
     *
     * @return The singleton instance.
     */
    public static Container getInstance() {
        return SingletonHolder.container;
    }

    @Override
    public void run() {
        // Iterate through the map and remove applications
        for (String appId : applicationMap.keySet()) {
            applicationMap.remove(appId);
            logger.fine(appId + " removed.");
        }
    }

    /**
     * Put an application into the container.
     *
     * @param appId The ID of the application.
     * @param app   The application instance to put.
     */
    public void put(String appId, Application app) {
        // Use put method for thread-safe insertion
        applicationMap.put(appId, app);
    }

    /**
     * Check if the container contains an application with the given ID.
     *
     * @param appId The ID of the application.
     * @return True if the container contains the application, otherwise false.
     */
    public boolean containsKey(String appId) {
        return applicationMap.containsKey(appId);
    }

    /**
     * Get an application from the container by its ID.
     *
     * @param appId The ID of the application.
     * @return The application instance, or null if not found.
     */
    public Application get(String appId) {
        return applicationMap.get(appId);
    }

    /**
     * Remove an application from the container by its ID.
     *
     * @param appId The ID of the application to remove.
     */
    public void remove(String appId) {
        // Use remove method for thread-safe removal
        applicationMap.remove(appId);
    }

    private static final class SingletonHolder {
        // Use static final field for thread-safe singleton instance
        static final Container container = new Container();
    }
}
