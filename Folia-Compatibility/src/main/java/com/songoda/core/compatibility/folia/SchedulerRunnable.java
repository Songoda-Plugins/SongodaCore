package com.songoda.core.compatibility.folia;

public abstract class SchedulerRunnable implements Runnable {

    private SchedulerTask task;

    @Override
    public void run() {

    }

    public void cancel() {
        SchedulerUtils.cancelTask(task);
    }

    void setTask(SchedulerTask task) {
        this.task = task;
    }
}
