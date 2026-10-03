package com.songoda.core.hooks.mcmmo;

import com.gmail.nossr50.api.AbilityAPI;
import com.gmail.nossr50.api.ExperienceAPI;
import com.gmail.nossr50.datatypes.skills.PrimarySkillType;
import com.gmail.nossr50.datatypes.skills.SubSkillType;
import com.gmail.nossr50.util.Permissions;
import com.gmail.nossr50.util.player.UserManager;
import com.gmail.nossr50.util.random.ProbabilityUtil;
import com.gmail.nossr50.util.skills.RankUtils;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;

/**
 * mcMMO operations supported by SongodaCore integrations.
 */
public final class McMMOHook {

    private McMMOHook() {
    }

    public static void addWoodcutting(@NotNull Player player, @NotNull Collection<Block> blocks) {
        if (blocks.isEmpty()) {
            return;
        }

        ArrayList<BlockState> blockStates = new ArrayList<>(blocks.stream().map(Block::getState).toList());
        ExperienceAPI.addXpFromBlocksBySkill(blockStates, UserManager.getPlayer(player), PrimarySkillType.WOODCUTTING);
    }

    public static boolean hasWoodcuttingDoubleDrops(@NotNull Player player) {
        return !PrimarySkillType.WOODCUTTING.getDoubleDropsDisabled()
                && Permissions.isSubSkillEnabled(player, SubSkillType.WOODCUTTING_HARVEST_LUMBER)
                && RankUtils.hasReachedRank(1, player, SubSkillType.WOODCUTTING_HARVEST_LUMBER)
                && ProbabilityUtil.isSkillRNGSuccessful(SubSkillType.WOODCUTTING_HARVEST_LUMBER, player);
    }

    public static boolean isUsingTreeFeller(@NotNull Player player) {
        return AbilityAPI.treeFellerEnabled(player);
    }
}
