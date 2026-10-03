package com.songoda.core.hooks.jobs;

import com.gamingmesh.jobs.Jobs;
import com.gamingmesh.jobs.actions.BlockActionInfo;
import com.gamingmesh.jobs.container.ActionType;
import com.gamingmesh.jobs.container.JobsPlayer;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Jobs Reborn operations supported by SongodaCore integrations.
 */
public final class JobsHook {

    private JobsHook() {
    }

    public static void breakBlock(@NotNull Player player, @NotNull Block block) {
        JobsPlayer jobsPlayer = Jobs.getPlayerManager().getJobsPlayer(player);
        if (jobsPlayer == null) {
            return;
        }

        Jobs.action(jobsPlayer, new BlockActionInfo(block, ActionType.BREAK), block);
    }
}
