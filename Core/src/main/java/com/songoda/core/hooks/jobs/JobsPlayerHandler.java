package com.songoda.core.hooks.jobs;

import com.songoda.core.SongodaPlugin;
import com.gamingmesh.jobs.CMILib.CMIEnchantment;
import com.gamingmesh.jobs.Jobs;
import com.gamingmesh.jobs.actions.BlockActionInfo;
import com.gamingmesh.jobs.actions.CustomKillInfo;
import com.gamingmesh.jobs.actions.EnchantActionInfo;
import com.gamingmesh.jobs.actions.EntityActionInfo;
import com.gamingmesh.jobs.actions.ItemActionInfo;
import com.gamingmesh.jobs.container.ActionType;
import com.gamingmesh.jobs.container.CurrencyType;
import com.gamingmesh.jobs.container.Job;
import com.gamingmesh.jobs.container.JobProgression;
import com.gamingmesh.jobs.container.JobsPlayer;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @deprecated This class is part of the old hook system and will be deleted very soon – See {@link SongodaPlugin#getHookManager()}
 */
@Deprecated
public class JobsPlayerHandler {
    protected final JobsPlayer jPlayer;

    protected JobsPlayerHandler(JobsPlayer jPlayer) {
        this.jPlayer = jPlayer;
    }

    public static JobsPlayerHandler loadPlayer(Player player) {
        JobsPlayer jPlayer = Jobs.getPlayerManager().getJobsPlayer(player);

        return jPlayer != null ? new JobsPlayerHandler(jPlayer) : null;
    }

    public double getBoostExp(String job) {
        return jPlayer.getBoost(job, CurrencyType.EXP);
    }

    public double getBoostMoney(String job) {
        return jPlayer.getBoost(job, CurrencyType.MONEY);
    }

    public double getBoostPoints(String job) {
        return jPlayer.getBoost(job, CurrencyType.POINTS);
    }

    public void promoteJob(String jobName) {
        Job job = Jobs.getJob(jobName);

        if (job != null) {
            jPlayer.promoteJob(job, 1);
        }
    }

    public void promoteJob(String jobName, int levels) {
        Job job = Jobs.getJob(jobName);

        if (job != null) {
            jPlayer.promoteJob(job, levels);
        }
    }

    public void demoteJob(String jobName) {
        Job job = Jobs.getJob(jobName);

        if (job != null) {
            jPlayer.demoteJob(job, 1);
        }
    }

    public void demoteJob(String jobName, int levels) {
        Job job = Jobs.getJob(jobName);

        if (job != null) {
            jPlayer.demoteJob(job, levels);
        }
    }

    public void joinJob(String jobName) {
        Job job = Jobs.getJob(jobName);

        if (job != null) {
            jPlayer.joinJob(job);
        }
    }

    public void leaveAllJobs() {
        jPlayer.leaveAllJobs();
    }

    public void leaveJob(String jobName) {
        Job job = Jobs.getJob(jobName);

        if (job != null) {
            jPlayer.leaveJob(job);
        }
    }

    public int getTotalLevels() {
        return jPlayer.getTotalLevels();
    }

    public int getMaxBrewingStandsAllowed() {
        return jPlayer.getMaxBrewingStandsAllowed();
    }

    public int getMaxFurnacesAllowed() {
        return jPlayer.getMaxFurnacesAllowed();
    }

    public List<String> getJobs() {
        return jPlayer.getJobProgression().stream().map(p -> p.getJob().getName()).collect(Collectors.toList());
    }

    public void eatItem(ItemStack item) {
        Jobs.action(jPlayer, new ItemActionInfo(item, ActionType.EAT));
    }

    public void breakBlock(Block block) {
        Jobs.action(jPlayer, new BlockActionInfo(block, ActionType.BREAK), block);
    }

    public void tntBreakBlock(Block block) {
        Jobs.action(jPlayer, new BlockActionInfo(block, ActionType.TNTBREAK), block);
    }

    public void placeBlock(Block block) {
        Jobs.action(jPlayer, new BlockActionInfo(block, ActionType.PLACE), block);
    }

    public void placeEntity(Entity block) {
        Jobs.action(jPlayer, new EntityActionInfo(block, ActionType.PLACE));
    }

    public void breakEntity(Entity block) {
        Jobs.action(jPlayer, new EntityActionInfo(block, ActionType.BREAK));
    }

    public void breedEntity(LivingEntity entity) {
        Jobs.action(jPlayer, new EntityActionInfo(entity, ActionType.BREED));
    }

    public void killEntity(LivingEntity entity) {
        killEntity(entity, jPlayer.getPlayer());
    }

    public void tameEntity(LivingEntity entity) {
        Jobs.action(jPlayer, new EntityActionInfo(entity, ActionType.TAME));
    }

    public void catchFish(ItemStack items) {
        Jobs.action(jPlayer, new ItemActionInfo(items, ActionType.FISH));
    }

    public void killEntity(LivingEntity entity, Entity damageSource) {
        Jobs.action(jPlayer, new EntityActionInfo(entity, ActionType.KILL), damageSource, entity);
        if (entity instanceof Player && !entity.hasMetadata("NPC")) {
            JobsPlayer jVictim = Jobs.getPlayerManager().getJobsPlayer((Player) entity);

            if (jVictim == null) {
                return;
            }

            List<JobProgression> jobs = jVictim.getJobProgression();
            if (jobs == null) {
                return;
            }

            for (JobProgression job : jobs) {
                Jobs.action(jPlayer, new CustomKillInfo(job.getJob().getName(), ActionType.CUSTOMKILL), damageSource, entity);
            }
        }
    }

    public void itemEnchanted(ItemStack resultStack) {
        Map<Enchantment, Integer> enchants = resultStack.getEnchantments();

        for (Map.Entry<Enchantment, Integer> oneEnchant : enchants.entrySet()) {
            CMIEnchantment e;
            String enchantName;
            Integer level2;
            Enchantment enchant = oneEnchant.getKey();

            if (enchant == null || (enchantName = (e = CMIEnchantment.get(enchant)) == null ? null : e.toString()) == null || (level2 = oneEnchant.getValue()) == null) {
                continue;
            }

            Jobs.action(jPlayer, new EnchantActionInfo(enchantName, level2, ActionType.ENCHANT));
        }
    }
}
