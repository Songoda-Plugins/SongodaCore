package com.songoda.core.hooks;

import com.songoda.core.SongodaPlugin;
import com.songoda.core.hooks.jobs.JobsHandler;
import com.songoda.core.hooks.jobs.JobsPlayerHandler;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Collections;
import java.util.List;

/**
 * @deprecated This class is part of the old hook system and will be deleted very soon – See {@link SongodaPlugin#getHookManager()}
 */
@Deprecated
public class JobsHook {
    static boolean canHook;

    static {
        try {
            // if this class exists, we're good to use Jobs classes
            Class.forName("com.gamingmesh.jobs.Jobs");
            canHook = true;
        } catch (ClassNotFoundException ignore) {
        }
    }

    public static JobsPlayerHandler getPlayer(Player player) {
        if (canHook) {
            return JobsPlayerHandler.loadPlayer(player);
        }

        return null;
    }

    public static boolean isEnabled() {
        return canHook;
    }

    public static List<String> getAllJobs() {
        if (canHook) {
            return JobsHandler.getJobs();
        }

        return Collections.emptyList();
    }

    public static double getBoostExp(Player player, String job) {
        JobsPlayerHandler jPlayer = getPlayer(player);

        if (jPlayer != null) {
            return jPlayer.getBoostExp(job);
        }

        return -1;
    }

    public static double getBoostMoney(Player player, String job) {
        JobsPlayerHandler jPlayer = getPlayer(player);

        if (jPlayer != null) {
            return jPlayer.getBoostMoney(job);
        }

        return -1;
    }

    public static double getBoostPoints(Player player, String job) {
        JobsPlayerHandler jPlayer = getPlayer(player);

        if (jPlayer != null) {
            return jPlayer.getBoostPoints(job);
        }

        return -1;
    }

    public static void promoteJob(Player player, String job) {
        JobsPlayerHandler jPlayer = getPlayer(player);

        if (jPlayer != null) {
            jPlayer.promoteJob(job);
        }
    }

    public static void promoteJob(Player player, String job, int levels) {
        JobsPlayerHandler jPlayer = getPlayer(player);

        if (jPlayer != null) {
            jPlayer.promoteJob(job, levels);
        }
    }

    public static void demoteJob(Player player, String job) {
        JobsPlayerHandler jPlayer = getPlayer(player);

        if (jPlayer != null) {
            jPlayer.demoteJob(job);
        }
    }

    public static void demoteJob(Player player, String job, int levels) {
        JobsPlayerHandler jPlayer = getPlayer(player);

        if (jPlayer != null) {
            jPlayer.demoteJob(job, levels);
        }
    }

    public static void joinJob(Player player, String job) {
        JobsPlayerHandler jPlayer = getPlayer(player);

        if (jPlayer != null) {
            jPlayer.joinJob(job);
        }
    }

    public static void leaveAllJobs(Player player) {
        JobsPlayerHandler jPlayer = getPlayer(player);

        if (jPlayer != null) {
            jPlayer.leaveAllJobs();
        }
    }

    public static void leaveJob(Player player, String job) {
        JobsPlayerHandler jPlayer = getPlayer(player);

        if (jPlayer != null) {
            jPlayer.leaveJob(job);
        }
    }

    public static int getTotalLevels(Player player) {
        JobsPlayerHandler jPlayer = getPlayer(player);

        if (jPlayer != null) {
            return jPlayer.getTotalLevels();
        }

        return -1;
    }

    public static int getMaxBrewingStandsAllowed(Player player) {
        JobsPlayerHandler jPlayer = getPlayer(player);

        if (jPlayer != null) {
            return jPlayer.getMaxBrewingStandsAllowed();
        }

        return -1;
    }

    public static int getMaxFurnacesAllowed(Player player) {
        JobsPlayerHandler jPlayer = getPlayer(player);

        if (jPlayer != null) {
            return jPlayer.getMaxFurnacesAllowed();
        }

        return -1;
    }

    public static List<String> getJobs(Player player) {
        JobsPlayerHandler jPlayer = getPlayer(player);

        if (jPlayer != null) {
            return jPlayer.getJobs();
        }

        return Collections.emptyList();
    }

    public static void eatItem(Player player, ItemStack item) {
        JobsPlayerHandler jPlayer = getPlayer(player);

        if (jPlayer != null) {
            jPlayer.eatItem(item);
        }
    }

    public static void breakBlock(Player player, Block block) {
        JobsPlayerHandler jPlayer = getPlayer(player);

        if (jPlayer != null) {
            jPlayer.breakBlock(block);
        }
    }

    public static void tntBreakBlock(Player player, Block block) {
        JobsPlayerHandler jPlayer = getPlayer(player);

        if (jPlayer != null) {
            jPlayer.tntBreakBlock(block);
        }
    }

    public static void placeBlock(Player player, Block block) {
        JobsPlayerHandler jPlayer = getPlayer(player);

        if (jPlayer != null) {
            jPlayer.placeBlock(block);
        }
    }

    public static void placeEntity(Player player, Entity block) {
        JobsPlayerHandler jPlayer = getPlayer(player);

        if (jPlayer != null) {
            jPlayer.placeEntity(block);
        }
    }

    public static void breakEntity(Player player, Entity block) {
        JobsPlayerHandler jPlayer = getPlayer(player);

        if (jPlayer != null) {
            jPlayer.breakEntity(block);
        }
    }

    public static void breedEntity(Player player, LivingEntity entity) {
        JobsPlayerHandler jPlayer = getPlayer(player);

        if (jPlayer != null) {
            jPlayer.breedEntity(entity);
        }
    }

    public static void killEntity(Player player, LivingEntity entity) {
        JobsPlayerHandler jPlayer = getPlayer(player);

        if (jPlayer != null) {
            jPlayer.killEntity(entity);
        }
    }

    public static void tameEntity(Player player, LivingEntity entity) {
        JobsPlayerHandler jPlayer = getPlayer(player);

        if (jPlayer != null) {
            jPlayer.tameEntity(entity);
        }
    }

    public static void catchFish(Player player, ItemStack items) {
        JobsPlayerHandler jPlayer = getPlayer(player);

        if (jPlayer != null) {
            jPlayer.catchFish(items);
        }
    }

    public static void killEntity(Player player, LivingEntity entity, Entity damageSource) {
        JobsPlayerHandler jPlayer = getPlayer(player);

        if (jPlayer != null) {
            jPlayer.killEntity(entity, damageSource);
        }
    }

    public static void itemEnchanted(Player player, ItemStack resultStack) {
        JobsPlayerHandler jPlayer = getPlayer(player);

        if (jPlayer != null) {
            jPlayer.itemEnchanted(resultStack);
        }
    }
}
