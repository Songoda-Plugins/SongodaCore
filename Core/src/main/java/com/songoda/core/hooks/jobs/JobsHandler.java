package com.songoda.core.hooks.jobs;

import com.songoda.core.SongodaPlugin;
import com.gamingmesh.jobs.Jobs;
import com.gamingmesh.jobs.container.Job;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @deprecated This class is part of the old hook system and will be deleted very soon – See {@link SongodaPlugin#getHookManager()}
 */
@Deprecated
public class JobsHandler {
    public static List<String> getJobs() {
        return Jobs.getJobs().stream().map(Job::getName).collect(Collectors.toList());
    }
}
