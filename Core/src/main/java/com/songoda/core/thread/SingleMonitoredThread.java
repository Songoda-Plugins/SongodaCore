package com.songoda.core.thread;

import java.util.concurrent.TimeUnit;

public class SingleMonitoredThread extends MonitoredThreadPool {
    public SingleMonitoredThread(String name, int timeout, TimeUnit timeUnit) {
        super(name, 1, timeout, timeUnit);
    }
}
