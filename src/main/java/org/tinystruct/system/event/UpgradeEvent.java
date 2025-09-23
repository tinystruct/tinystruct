package org.tinystruct.system.event;

import org.tinystruct.system.Event;

/**
 * Event published when a newer application version is detected and an upgrade is available.
 * The payload is the latest version string (for example, "1.4.2").
 */
public class UpgradeEvent implements Event<String> {

    private final String latestVersion;

    /**
     * Create a new {@code UpgradeEvent}.
     *
     * @param latestVersion the newest available version identifier
     */
    public UpgradeEvent(String latestVersion){
        this.latestVersion = latestVersion;
    }

    /**
     * Returns the newest available version identifier associated with this event.
     *
     * @return latest version string
     */
    public String getLatestVersion(){
        return this.latestVersion;
    }

    @Override
    public String getName() {
        return "";
    }

    @Override
    public String getPayload() {
        return latestVersion;
    }
}
