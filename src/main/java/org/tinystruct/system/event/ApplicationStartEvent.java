package org.tinystruct.system.event;

import org.tinystruct.Application;
import org.tinystruct.system.Event;

/**
 * Event published when an {@link Application} has started.
 * The payload is the running {@link Application} instance.
 */
public class ApplicationStartEvent implements Event<Application> {
    private final Application app;

    /**
     * Create a new {@code ApplicationStartEvent}.
     *
     * @param app the application instance that has just started
     */
    public ApplicationStartEvent(Application app) {
        this.app = app;
    }

    /**
     * The logical name of this event. Empty string indicates unnamed/default.
     *
     * @return event name (may be empty)
     */
    @Override
    public String getName() {
        return "";
    }

    /**
     * Returns the {@link Application} instance associated with this start event.
     *
     * @return started application instance
     */
    @Override
    public Application getPayload() {
        return this.app;
    }
}
