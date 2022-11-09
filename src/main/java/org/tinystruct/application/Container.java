package org.tinystruct.application;

import org.tinystruct.Application;
import org.tinystruct.system.scheduling.Scheduler;
import org.tinystruct.system.scheduling.TimeIterator;

import java.util.Map;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;

public class Container extends TimerTask {
    private static final Map<String, Application> map = new ConcurrentHashMap<>(16);
    private static final Scheduler scheduler = Scheduler.getInstance();
    public static Container getInstance() {
        return SingletonHolder.container;
    }

    private Container(){
        scheduler.schedule(this, new TimeIterator(0, 0, 0), 18000);
    }

    @Override
    public void run() {
        map.forEach((appId, b)->{
            map.remove(appId);
        });
    }

    public void put(String appId, Application app) {
        map.put(appId, app);
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
