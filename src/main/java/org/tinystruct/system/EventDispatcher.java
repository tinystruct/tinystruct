package org.tinystruct.system;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * A singleton class that manages event dispatching in the system.
 * It allows registering event handlers for specific event types
 * and dispatching events to the registered handlers.
 */
public class EventDispatcher {
    // A map to hold event handlers, categorized by event type.
    private final Map<Class<? extends Event<?>>, List<Consumer<Event<?>>>> handlers = new ConcurrentHashMap<>();

    // Private constructor to prevent instantiation from outside.
    private EventDispatcher() {
    }

    /**
     * Inner static class for holding the singleton instance.
     * This ensures that the instance is created only when it is accessed.
     */
    private static final class InstanceHolder {
        private static final EventDispatcher instance = new EventDispatcher();
    }

    /**
     * Provides access to the singleton instance of EventDispatcher.
     *
     * @return the single instance of EventDispatcher
     */
    public static EventDispatcher getInstance() {
        return InstanceHolder.instance;
    }

    /**
     * Registers an event handler for a specific event type.
     *
     * @param eventType the class of the event to listen for
     * @param handler   the consumer that handles the event
     * @param <E>      the type of the event
     */
    public <E extends Event<?>> void registerHandler(Class<E> eventType, Consumer<E> handler) {
        if (handler != null) {
            // Compute the list of handlers for the given event type or create a new one if it doesn't exist
            handlers.computeIfAbsent(eventType, k -> new ArrayList<>())
                    .add(event -> handler.accept(eventType.cast(event)));
        }
    }

    /**
     * Dispatches an event to all registered handlers for the event's type.
     *
     * @param event the event to be dispatched
     */
    public void dispatch(Event<?> event) {
        // Retrieve the list of handlers for the event's type
        List<Consumer<Event<?>>> eventHandlers = handlers.get(event.getClass());

        // If handlers exist for this event type, invoke each handler
        if (eventHandlers != null) {
            eventHandlers.forEach(handler -> handler.accept(event));
        }
    }
}
