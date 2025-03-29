package com.songoda.core.thread;


import java.time.Instant;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class MonitoredThread {
    private final String name;
    private final int timeout;
    private final TimeUnit timeUnit;
    private ScheduledExecutorService executor;
    private Instant started = null;
    private StackTraceElement[] trace = null;
    private boolean nonDisruptable = false;

    public MonitoredThread(String name, int timeout, TimeUnit timeUnit) {
        this.name = name;
        this.timeout = timeout;
        this.timeUnit = timeUnit;

        System.out.println("Thread '" + name + "' was started...");
        start();
    }

    public void execute(Runnable runnable, boolean nonDisruptable) {
        this.nonDisruptable = nonDisruptable;
        StackTraceElement[] trace = Thread.currentThread().getStackTrace();
        this.executor.execute(() -> {
            this.started = Instant.now();
            this.trace = trace;
            try {
                runnable.run();
            } catch (Exception ex) {
                StackTraceElement[] newTrace = new StackTraceElement[ex.getStackTrace().length + trace.length];
                System.arraycopy(ex.getStackTrace(), 0, newTrace, 0, ex.getStackTrace().length);
                System.arraycopy(trace, 0, newTrace, ex.getStackTrace().length, trace.length);
                ex.setStackTrace(newTrace);
                System.out.println("Thread '" + this.name + "' failed with exception: " + ex.getMessage());
                ex.printStackTrace();
            }
            this.started = null;
        });
    }

    public MonitoredThread start() {
        if (this.executor != null) {
            this.executor.shutdown();
            System.out.println("Thread '" + this.name + "' was restarted due to a stall. Stack trace:");
            for (StackTraceElement element : this.trace) {
                System.out.println("    " + element.toString());
            }
        }
        this.executor = Executors.newSingleThreadScheduledExecutor(r -> new Thread(r, this.name));
        return this;
    }

    public String getName() {
        return this.name;
    }

    public boolean isStalled() {
        return !this.nonDisruptable && this.started != null && this.started.plusMillis(this.timeUnit.toMillis(this.timeout)).isBefore(Instant.now())
                || this.started != null && this.started.plusMillis(TimeUnit.HOURS.toMillis(1)).isBefore(Instant.now());
    }

    public boolean isRunning() {
        return this.started != null;
    }

    public Instant getStarted() {
        return this.started;
    }

    public ScheduledFuture<?> schedule(Runnable runnable, long delay, TimeUnit timeUnit) {
        return this.executor.schedule(runnable, delay, timeUnit);
    }

    public void destroy() {
        this.executor.shutdownNow();
    }
}
