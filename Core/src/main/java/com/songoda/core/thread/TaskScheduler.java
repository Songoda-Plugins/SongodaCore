package com.songoda.core.thread;

import com.songoda.core.SongodaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public abstract class TaskScheduler {

    private final SongodaPlugin plugin;
    private final Map<TaskWrapper, Long> tasks = new ConcurrentHashMap<>();
    private BukkitRunnable runnable;
    private long delay;
    private long period;

    /**
     * Constructor for TaskScheduler with default delay and period.
     * @param plugin The plugin instance.
     */
    public TaskScheduler(SongodaPlugin plugin) {
        this.plugin = plugin;
        this.delay = 20L;
        this.period = 20L;
    }

    /**
     * Constructor for TaskScheduler with a custom delay and period.
     * @param plugin The plugin instance.
     * @param delay The delay in ticks.
     * @param period The period in ticks.
     */
    public TaskScheduler(SongodaPlugin plugin, long delay, long period) {
        this.plugin = plugin;
        this.delay = delay;
        this.period = period;
    }

    private void startScheduler() {
        if (this.runnable == null || this.runnable.isCancelled()) {
            this.runnable = new BukkitRunnable() {
                @Override
                public void run() {
                    executeTasks();
                }
            };
            this.runnable.runTaskTimerAsynchronously(this.plugin, this.delay, this.period);
        }
    }

    private void stopScheduler() {
        if (this.runnable != null && !this.runnable.isCancelled()) {
            this.runnable.cancel();
        }
    }

    private void executeTasks() {
        if (this.tasks.isEmpty()) {
            stopScheduler();
            return;
        }

        long currentTime = System.currentTimeMillis();
        Iterator<Map.Entry<TaskWrapper, Long>> iterator = this.tasks.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<TaskWrapper, Long> entry = iterator.next();
            if (entry.getValue() <= currentTime) {
                TaskWrapper taskWrapper = entry.getKey();
                if (taskWrapper.isAsync()) {
                    // Run the task asynchronously
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            taskWrapper.getTask().run();
                        }
                    }.runTaskAsynchronously(this.plugin);
                } else {
                    // Run the task synchronously
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            taskWrapper.getTask().run();
                        }
                    }.runTask(this.plugin);
                }
                iterator.remove();
            }
        }
    }

    public synchronized void addTask(Runnable task, long delay, boolean async) {
        this.tasks.put(new TaskWrapper(task, async), System.currentTimeMillis() + delay);
        startScheduler();
    }

    public synchronized void addTask(Runnable task, long delay) {
        addTask(task, delay, false);
    }

    private static class TaskWrapper {
        private final Runnable task;
        private final boolean async;

        public TaskWrapper(Runnable task, boolean async) {
            this.task = task;
            this.async = async;
        }

        public Runnable getTask() {
            return this.task;
        }

        public boolean isAsync() {
            return this.async;
        }
    }
}
