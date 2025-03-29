package com.songoda.core.hooks;

import com.songoda.core.SongodaPlugin;
import com.songoda.core.hooks.mcmmo.McMMOHandler;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.Collection;

/**
 * @deprecated This class is part of the old hook system and will be deleted very soon – See {@link SongodaPlugin#getHookManager()}
 */
@Deprecated
public class McMMOHook {
    static boolean canHook = false;

    static {
        try {
            // if this class exists, we're good to use McMMO
            Class.forName("com.gmail.nossr50.api.AbilityAPI");
            canHook = true;
        } catch (ClassNotFoundException ignore) {
        }
    }

    public static void addMining(Player player, Collection<Block> blocks) {
        if (canHook) {
            McMMOHandler.addMining(player, blocks);
        }
    }

    public static void addExcavation(Player player, Collection<Block> blocks) {
        if (canHook) {
            McMMOHandler.addExcavation(player, blocks);
        }
    }

    public static void addHerbalism(Player player, Collection<Block> blocks) {
        if (canHook) {
            McMMOHandler.addHerbalism(player, blocks);
        }
    }

    public static void addWoodcutting(Player player, Collection<Block> blocks) {
        if (canHook) {
            McMMOHandler.addWoodcutting(player, blocks);
        }
    }

    public static int getAcrobaticsSkill(Player player) {
        return canHook ? McMMOHandler.getAcrobaticsSkill(player) : -1;
    }

    public static int getAlchemySkill(Player player) {
        return canHook ? McMMOHandler.getAlchemySkill(player) : -1;
    }

    public static int getArcherySkill(Player player) {
        return canHook ? McMMOHandler.getArcherySkill(player) : -1;
    }

    public static int getAxesSkill(Player player) {
        return canHook ? McMMOHandler.getAxesSkill(player) : -1;
    }

    public static int getExcavationSkill(Player player) {
        return canHook ? McMMOHandler.getExcavationSkill(player) : -1;
    }

    public static int getFishingSkill(Player player) {
        return canHook ? McMMOHandler.getFishingSkill(player) : -1;
    }

    public static int getHerbalismSkill(Player player) {
        return canHook ? McMMOHandler.getHerbalismSkill(player) : -1;
    }

    public static int getMiningSkill(Player player) {
        return canHook ? McMMOHandler.getMiningSkill(player) : -1;
    }

    public static int getRepairSkill(Player player) {
        return canHook ? McMMOHandler.getRepairSkill(player) : -1;
    }

    public static int getSmeltingSkill(Player player) {
        return canHook ? McMMOHandler.getSmeltingSkill(player) : -1;
    }

    public static int getSwordsSkill(Player player) {
        return canHook ? McMMOHandler.getSwordsSkill(player) : -1;
    }

    public static int getTamingSkill(Player player) {
        return canHook ? McMMOHandler.getTamingSkill(player) : -1;
    }

    public static int getUnarmedSkill(Player player) {
        return canHook ? McMMOHandler.getUnarmedSkill(player) : -1;
    }

    public static int getWoodcuttingSkill(Player player) {
        return canHook ? McMMOHandler.getWoodcuttingSkill(player) : -1;
    }

    public static void addAcrobatics(Player player, int xp) {
        if (canHook) {
            McMMOHandler.addAcrobatics(player, xp);
        }
    }

    public static void addAlchemy(Player player, int xp) {
        if (canHook) {
            McMMOHandler.addAlchemy(player, xp);
        }
    }

    public static void addArchery(Player player, int xp) {
        if (canHook) {
            McMMOHandler.addArchery(player, xp);
        }
    }

    public static void addAxes(Player player, int xp) {
        if (canHook) {
            McMMOHandler.addAxes(player, xp);
        }
    }

    public static void addExcavation(Player player, int xp) {
        if (canHook) {
            McMMOHandler.addExcavation(player, xp);
        }
    }

    public static void addFishing(Player player, int xp) {
        if (canHook) {
            McMMOHandler.addFishing(player, xp);
        }
    }

    public static void addHerbalism(Player player, int xp) {
        if (canHook) {
            McMMOHandler.addHerbalism(player, xp);
        }
    }

    public static void addMining(Player player, int xp) {
        if (canHook) {
            McMMOHandler.addMining(player, xp);
        }
    }

    public static void addRepair(Player player, int xp) {
        if (canHook) {
            McMMOHandler.addRepair(player, xp);
        }
    }

    public static void addSmelting(Player player, int xp) {
        if (canHook) {
            McMMOHandler.addSmelting(player, xp);
        }
    }

    public static void addSwords(Player player, int xp) {
        if (canHook) {
            McMMOHandler.addSwords(player, xp);
        }
    }

    public static void addTaming(Player player, int xp) {
        if (canHook) {
            McMMOHandler.addTaming(player, xp);
        }
    }

    public static void addUnarmed(Player player, int xp) {
        if (canHook) {
            McMMOHandler.addUnarmed(player, xp);
        }
    }

    public static void addWoodcutting(Player player, int xp) {
        if (canHook) {
            McMMOHandler.addWoodcutting(player, xp);
        }
    }

    public static boolean hasHerbalismDoubleDrops(Player player) {
        return canHook && McMMOHandler.hasHerbalismDoubleDrops(player);
    }

    public static boolean hasMiningDoubleDrops(Player player) {
        return canHook && McMMOHandler.hasMiningDoubleDrops(player);
    }

    public static boolean hasWoodcuttingDoubleDrops(Player player) {
        return canHook && McMMOHandler.hasWoodcuttingDoubleDrops(player);
    }

    public static boolean isUsingBerserk(Player player) {
        return canHook && McMMOHandler.isUsingBerserk(player);
    }

    public static boolean isUsingGigaDrill(Player player) {
        return canHook && McMMOHandler.isUsingGigaDrill(player);
    }

    public static boolean isUsingGreenTerra(Player player) {
        return canHook && McMMOHandler.isUsingGreenTerra(player);
    }

    public static boolean isUsingSerratedStrikes(Player player) {
        return canHook && McMMOHandler.isUsingSerratedStrikes(player);
    }

    public static boolean isUsingSkullSplitter(Player player) {
        return canHook && McMMOHandler.isUsingSkullSplitter(player);
    }

    public static boolean isUsingSuperBreaker(Player player) {
        return canHook && McMMOHandler.isUsingSuperBreaker(player);
    }

    public static boolean isUsingTreeFeller(Player player) {
        return canHook && McMMOHandler.isUsingTreeFeller(player);
    }

    public static boolean isBleeding(LivingEntity victim) {
        return canHook && McMMOHandler.isBleeding(victim);
    }
}
