package com.craftaro.core.compatibility.folia;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.scheduler.BukkitTask;

public class SchedulerTask {

    private final Object task;

    public SchedulerTask(Object task) {
        if (task == null) throw new IllegalArgumentException("Task cannot be null");
        switch (task.getClass().getSimpleName()) {
            case "BukkitTask":
            case "SchedulerTask":
            case "AsyncScheduledTask":
            case "EntityScheduledTask":
            case "GlobalScheduledTask":
            case "LocationScheduledTask":
                this.task = task;
                break;
            default:
                throw new IllegalArgumentException("Task: " + task.getClass().getSimpleName() + " is not a BukkitTask or ScheduledTask");
        }
    }

    public Object getTask() {
        return task;
    }

    public ScheduledTask getAsFoliaTask() {
        return (ScheduledTask) task;
    }

    public BukkitTask getAsBukkitTask() {
        return (BukkitTask) task;
    }

    public int getAsBukkitTaskId() {
        return getAsBukkitTask().getTaskId();
    }
}
