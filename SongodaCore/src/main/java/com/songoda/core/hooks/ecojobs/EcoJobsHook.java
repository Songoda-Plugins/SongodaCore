package com.songoda.core.hooks.ecojobs;

import com.willfp.ecojobs.api.EcoJobsAPI;
import com.willfp.ecojobs.jobs.Job;
import com.willfp.ecojobs.jobs.Jobs;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Operations for awarding EcoJobs experience.
 */
public final class EcoJobsHook {

    private EcoJobsHook() {
    }

    public static boolean addJobExperience(@NotNull Player player, @NotNull Job job, double amount) {
        if (!Double.isFinite(amount) || amount <= 0 || !EcoJobsAPI.hasJobActive(player, job)) {
            return false;
        }

        EcoJobsAPI.giveJobExperience(player, job, amount);
        return true;
    }

    /**
     * Resolves a configured job ID and awards experience when that job exists and is active.
     *
     * @param player player receiving experience
     * @param jobId EcoJobs job ID
     * @param amount experience amount
     * @return true when experience was awarded
     */
    public static boolean addJobExperience(@NotNull Player player, @NotNull String jobId, double amount) {
        if (jobId.isBlank()) {
            return false;
        }

        Job job = Jobs.getByID(jobId);
        return job != null && addJobExperience(player, job, amount);
    }
}
