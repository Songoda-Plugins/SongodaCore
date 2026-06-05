package com.songoda.core.thread;

import java.util.HashSet;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class MonitoredThreadPool {
    private final HashSet<MonitoredThread> threads = new HashSet<>();
    private final String name;
    private final int size;
    private int latestThread = 1;

    private final int threadTimeout;
    private final TimeUnit threadTimeoutUnit;

    public MonitoredThreadPool(String name, int size, int timeout, TimeUnit timeUnit) {
        this.name = name;
        this.size = size;
        this.threadTimeout = timeout;
        this.threadTimeoutUnit = timeUnit;
        for (int i = 0; i < size; ++i) {
            createThread(name);
        }
    }

    public MonitoredThread createThread(String name) {
        MonitoredThread thread = new MonitoredThread((name + "-" + this.latestThread++).toLowerCase(), this.threadTimeout, this.threadTimeoutUnit);
        this.threads.add(thread);
        return thread;
    }

    public void execute(Runnable runnable) {
        execute(runnable, false);
    }

    public void execute(Runnable runnable, boolean nonDisruptable) {
        MonitoredThread thread = getHealthyThread();
        if (thread != null)
            thread.execute(runnable, nonDisruptable);
    }

    public ScheduledFuture<?> schedule(Runnable runnable, long delay, TimeUnit timeUnit) {
        MonitoredThread thread = getHealthyThread();
        return thread == null ? null : thread.schedule(runnable, delay, timeUnit);
    }

    public ScheduledFuture<?> delay(Runnable runnable, long delay, TimeUnit timeUnit) {
        return schedule(runnable, delay, timeUnit);
    }

    private MonitoredThread getHealthyThread() {
        for (MonitoredThread thread : this.threads) {
            if (!thread.isRunning()) {
                return thread;
            } else if (thread.isStalled()) {
                thread.start();
                onStall();
                return thread;
            }
        }
        return null;
    }

    public int getRunningThreads() {
        int runningThreads = 0;
        for (MonitoredThread thread : this.threads) {
            if (thread.isRunning()) {
                runningThreads++;
            }
        }
        return runningThreads;
    }

    public void onStall() {
        // Must be overridden if you want to do something when a thread stalls
    }
}
