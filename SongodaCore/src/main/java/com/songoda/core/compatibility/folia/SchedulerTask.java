package com.songoda.core.compatibility.folia;

import org.bukkit.scheduler.BukkitTask;

public class SchedulerTask {

    private final Object task;

    public SchedulerTask(Object task) {
        if (task == null) {
            throw new IllegalArgumentException("Task cannot be null");
        }
        if (task instanceof BukkitTask || SchedulerUtils.isFolia()) {
            this.task = task;
        } else {
            throw new IllegalArgumentException("Task: " + task.getClass().getName() + " is not a BukkitTask or ScheduledTask");
        }
    }

    public Object getTask() {
        return task;
    }

    public Object getAsFoliaTask() {
        return task;
    }

    public BukkitTask getAsBukkitTask() {
        return (BukkitTask) task;
    }

    public int getAsBukkitTaskId() {
        return getAsBukkitTask().getTaskId();
    }
}
