package org.tinystruct.http;

import java.util.EventListener;

public interface SessionListener extends EventListener {
    /**
     * Trigger on a session creation.
     *
     * @param event session event
     */
    void onSessionEvent(SessionEvent event);
}
