package org.tinystruct.system.event;

import org.tinystruct.system.Event;

public class UpgradeEvent implements Event<String> {

    private final String latestVersion;

    public UpgradeEvent(String latestVersion){
        this.latestVersion = latestVersion;
    }

    public String getLatestVersion(){
        return this.latestVersion;
    }

    @Override
    public String getName() {
        return "";
    }

    @Override
    public String getPayload() {
        return null;
    }
}
