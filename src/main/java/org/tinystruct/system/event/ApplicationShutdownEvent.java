package org.tinystruct.system.event;

import org.tinystruct.Application;
import org.tinystruct.system.Event;

/**
 * Event published when an {@link Application} is shutting down.
 * The payload is the {@link Application} instance that is stopping.
 */
public class ApplicationShutdownEvent implements Event<Application> {

    private final Application app;

    /**
     * Create a new {@code ApplicationShutdownEvent}.
     *
     * @param app the application instance that is shutting down
     */
    public ApplicationShutdownEvent(Application app) {
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
     * Returns the {@link Application} instance associated with this shutdown event.
     *
     * @return application instance being shut down
     */
    @Override
    public Application getPayload() {
        return this.app;
    }
}
