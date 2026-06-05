package com.songoda.core.hooks.mcmmo;

import com.songoda.core.SongodaPlugin;
import com.songoda.core.compatibility.ServerVersion;
import com.gmail.nossr50.api.AbilityAPI;
import com.gmail.nossr50.api.ExperienceAPI;
import com.gmail.nossr50.config.experience.ExperienceConfig;
import com.gmail.nossr50.datatypes.experience.XPGainReason;
import com.gmail.nossr50.datatypes.experience.XPGainSource;
import com.gmail.nossr50.datatypes.skills.PrimarySkillType;
import com.gmail.nossr50.datatypes.skills.SubSkillType;
import com.gmail.nossr50.util.Permissions;
import com.gmail.nossr50.util.player.UserManager;
import com.gmail.nossr50.util.random.ProbabilityUtil;
import com.gmail.nossr50.util.skills.RankUtils;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permissible;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * @deprecated This class is part of the old hook system and will be deleted very soon – See {@link SongodaPlugin#getHookManager()}
 */
@Deprecated
public class McMMOHandler {
    static boolean mcmmo_v2 = false;
    static boolean legacy_v13 = false;
    static boolean legacy_v12 = false;
    static boolean legacy_v8 = false;

    static Class mcmmo_SkillType;
    static Method mcmmo_SkillType_valueOf;
    static Method mcmmo_SkillType_getDoubleDropsDisabled;
    static Object mcmmo_ExperienceConfig_instance;
    static Method mcmmo_ExperienceConfig_getXp;
    static Method mcmmo_McMMOPlayer_getSkillLevel;
    static Class mcmmo_PerksUtils;
    static Method mcmmo_PerksUtils_handleLuckyPerks;
    static Class mcmmo_SecondaryAbility;
    static Method mcmmo_SecondaryAbility_valueOf;
    static Method mcmmo_Permissions_secondaryAbilityEnabled;
    static Method mcmmo_SkillUtils_activationSuccessful;

    static {
        try {
            Class.forName("com.gmail.nossr50.datatypes.skills.PrimarySkillType");
            mcmmo_v2 = true;
        } catch (ClassNotFoundException ex) {
            try {
                mcmmo_SkillType = Class.forName("com.gmail.nossr50.datatypes.skills.SkillType");
                mcmmo_SkillType_valueOf = mcmmo_SkillType.getDeclaredMethod("valueOf", String.class);
                mcmmo_SkillType_getDoubleDropsDisabled = mcmmo_SkillType.getDeclaredMethod("getDoubleDropsDisabled");
                mcmmo_ExperienceConfig_instance = ExperienceConfig.getInstance();
                mcmmo_McMMOPlayer_getSkillLevel = com.gmail.nossr50.datatypes.player.McMMOPlayer.class.getDeclaredMethod("getSkillLevel", mcmmo_SkillType);
                mcmmo_PerksUtils = Class.forName("com.gmail.nossr50.util.skills.PerksUtils");
                mcmmo_PerksUtils_handleLuckyPerks = mcmmo_PerksUtils.getDeclaredMethod("handleLuckyPerks", Player.class, mcmmo_SkillType);
                mcmmo_SecondaryAbility = Class.forName("com.gmail.nossr50.datatypes.skills.SecondaryAbility");
                mcmmo_SecondaryAbility_valueOf = mcmmo_SecondaryAbility.getDeclaredMethod("valueOf", String.class);
                mcmmo_Permissions_secondaryAbilityEnabled = com.gmail.nossr50.util.Permissions.class.getDeclaredMethod("secondaryAbilityEnabled", Permissible.class, mcmmo_SecondaryAbility);
                mcmmo_SkillUtils_activationSuccessful = com.gmail.nossr50.util.skills.SkillUtils.class.getDeclaredMethod("activationSuccessful", mcmmo_SecondaryAbility, Player.class, int.class, int.class);

                if (ServerVersion.isServerVersionAtLeast(ServerVersion.V1_13)) {
                    mcmmo_ExperienceConfig_getXp = mcmmo_ExperienceConfig_instance.getClass().getDeclaredMethod("getXp", mcmmo_SkillType, org.bukkit.block.data.BlockData.class);
                    legacy_v13 = true;
                } else if (ServerVersion.isServerVersionAtLeast(ServerVersion.V1_12)) {
                    mcmmo_ExperienceConfig_getXp = mcmmo_ExperienceConfig_instance.getClass().getDeclaredMethod("getXp", mcmmo_SkillType, org.bukkit.material.MaterialData.class);
                    legacy_v12 = true;
                } else {
                    mcmmo_ExperienceConfig_getXp = mcmmo_ExperienceConfig_instance.getClass().getDeclaredMethod("getXp", mcmmo_SkillType, org.bukkit.Material.class);
                    legacy_v8 = true;
                }
            } catch (Exception ex2) {
                Logger.getLogger(McMMOHandler.class.getName()).log(Level.SEVERE, "Failed to register McMMO Legacy Hook", ex2);
            }
        }
    }

    public static void addMining(Player player, Collection<Block> blocks) {
        if (player == null || blocks == null || blocks.isEmpty()) {
            return;
        }

        if (legacy_v13 || legacy_v12 || legacy_v8) {
            addBlockSkillLegacy(player, blocks, "mining");
            return;
        }

        ArrayList<BlockState> blockStates = blocks.stream().map(Block::getState).collect(Collectors.toCollection(ArrayList::new));
        ExperienceAPI.addXpFromBlocksBySkill(blockStates, UserManager.getPlayer(player), PrimarySkillType.MINING);
    }

    public static void addExcavation(Player player, Collection<Block> blocks) {
        if (player == null || blocks == null || blocks.isEmpty()) {
            return;
        }

        if (legacy_v13 || legacy_v12 || legacy_v8) {
            addBlockSkillLegacy(player, blocks, "excavation");
            return;
        }

        ArrayList<BlockState> blockStates = blocks.stream().map(Block::getState).collect(Collectors.toCollection(ArrayList::new));
        ExperienceAPI.addXpFromBlocksBySkill(blockStates, UserManager.getPlayer(player), PrimarySkillType.EXCAVATION);
    }

    public static void addHerbalism(Player player, Collection<Block> blocks) {
        if (player == null || blocks == null || blocks.isEmpty()) {
            return;
        }

        if (legacy_v13 || legacy_v12 || legacy_v8) {
            addBlockSkillLegacy(player, blocks, "herbalism");
            return;
        }

        ArrayList<BlockState> blockStates = blocks.stream().map(Block::getState).collect(Collectors.toCollection(ArrayList::new));
        ExperienceAPI.addXpFromBlocksBySkill(blockStates, UserManager.getPlayer(player), PrimarySkillType.HERBALISM);
    }

    public static void addWoodcutting(Player player, Collection<Block> blocks) {
        if (player == null || blocks == null || blocks.isEmpty()) {
            return;
        }

        if (legacy_v13 || legacy_v12 || legacy_v8) {
            addBlockSkillLegacy(player, blocks, "woodcutting");
            return;
        }

        ArrayList<BlockState> blockStates = blocks.stream().map(Block::getState).collect(Collectors.toCollection(ArrayList::new));
        ExperienceAPI.addXpFromBlocksBySkill(blockStates, UserManager.getPlayer(player), PrimarySkillType.WOODCUTTING);
    }

    public static int getAcrobaticsSkill(Player player) {
        if (player == null) {
            return -1;
        }

        if (legacy_v13 || legacy_v12 || legacy_v8) {
            return getSkillLegacy(player, "acrobatics");
        }

        return UserManager.getPlayer(player).getSkillLevel(PrimarySkillType.ACROBATICS);
    }

    public static int getAlchemySkill(Player player) {
        if (player == null) {
            return -1;
        }

        if (legacy_v13 || legacy_v12 || legacy_v8) {
            return getSkillLegacy(player, "alchemy");
        }

        return UserManager.getPlayer(player).getSkillLevel(PrimarySkillType.ALCHEMY);
    }

    public static int getArcherySkill(Player player) {
        if (player == null) {
            return -1;
        }

        if (legacy_v13 || legacy_v12 || legacy_v8) {
            return getSkillLegacy(player, "archery");
        }

        return UserManager.getPlayer(player).getSkillLevel(PrimarySkillType.ARCHERY);
    }

    public static int getAxesSkill(Player player) {
        if (player == null) {
            return -1;
        }

        if (legacy_v13 || legacy_v12 || legacy_v8) {
            return getSkillLegacy(player, "axes");
        }

        return UserManager.getPlayer(player).getSkillLevel(PrimarySkillType.AXES);
    }

    public static int getExcavationSkill(Player player) {
        if (player == null) {
            return -1;
        }

        if (legacy_v13 || legacy_v12 || legacy_v8) {
            return getSkillLegacy(player, "excavation");
        }

        return UserManager.getPlayer(player).getSkillLevel(PrimarySkillType.EXCAVATION);
    }

    public static int getFishingSkill(Player player) {
        if (player == null) {
            return -1;
        }

        if (legacy_v13 || legacy_v12 || legacy_v8) {
            return getSkillLegacy(player, "fishing");
        }

        return UserManager.getPlayer(player).getSkillLevel(PrimarySkillType.FISHING);
    }

    public static int getHerbalismSkill(Player player) {
        if (player == null) {
            return -1;
        }

        if (legacy_v13 || legacy_v12 || legacy_v8) {
            return getSkillLegacy(player, "herbalism");
        }

        return UserManager.getPlayer(player).getSkillLevel(PrimarySkillType.HERBALISM);
    }

    public static int getMiningSkill(Player player) {
        if (player == null) {
            return -1;
        }

        if (legacy_v13 || legacy_v12 || legacy_v8) {
            return getSkillLegacy(player, "mining");
        }

        return UserManager.getPlayer(player).getSkillLevel(PrimarySkillType.MINING);
    }

    public static int getRepairSkill(Player player) {
        if (player == null) {
            return -1;
        }

        if (legacy_v13 || legacy_v12 || legacy_v8) {
            return getSkillLegacy(player, "repair");
        }

        return UserManager.getPlayer(player).getSkillLevel(PrimarySkillType.REPAIR);
    }

    public static int getSmeltingSkill(Player player) {
        if (player == null) {
            return -1;
        }

        if (legacy_v13 || legacy_v12 || legacy_v8) {
            return getSkillLegacy(player, "smelting");
        }

        return UserManager.getPlayer(player).getSkillLevel(PrimarySkillType.SMELTING);
    }

    public static int getSwordsSkill(Player player) {
        if (player == null) {
            return -1;
        }

        if (legacy_v13 || legacy_v12 || legacy_v8) {
            return getSkillLegacy(player, "swords");
        }

        return UserManager.getPlayer(player).getSkillLevel(PrimarySkillType.SWORDS);
    }

    public static int getTamingSkill(Player player) {
        if (player == null) {
            return -1;
        }

        if (legacy_v13 || legacy_v12 || legacy_v8) {
            return getSkillLegacy(player, "taming");
        }

        return UserManager.getPlayer(player).getSkillLevel(PrimarySkillType.TAMING);
    }

    public static int getUnarmedSkill(Player player) {
        if (player == null) {
            return -1;
        }

        if (legacy_v13 || legacy_v12 || legacy_v8) {
            return getSkillLegacy(player, "unarmed");
        }

        return UserManager.getPlayer(player).getSkillLevel(PrimarySkillType.UNARMED);
    }

    public static int getWoodcuttingSkill(Player player) {
        if (player == null) {
            return -1;
        }

        if (legacy_v13 || legacy_v12 || legacy_v8) {
            return getSkillLegacy(player, "woodcutting");
        }

        return UserManager.getPlayer(player).getSkillLevel(PrimarySkillType.WOODCUTTING);
    }

    public static void addAcrobatics(Player player, int xp) {
        if (player == null) {
            return;
        }

        if (legacy_v13 || legacy_v12 || legacy_v8) {
            ExperienceAPI.addXP(player, "acrobatics", xp);
        }

        UserManager.getPlayer(player).beginXpGain(PrimarySkillType.ACROBATICS, xp, XPGainReason.UNKNOWN, XPGainSource.CUSTOM);
    }

    public static void addAlchemy(Player player, int xp) {
        if (player == null) {
            return;
        }

        if (legacy_v13 || legacy_v12 || legacy_v8) {
            ExperienceAPI.addXP(player, "alchemy", xp);
        }

        UserManager.getPlayer(player).beginXpGain(PrimarySkillType.ALCHEMY, xp, XPGainReason.UNKNOWN, XPGainSource.CUSTOM);
    }

    public static void addArchery(Player player, int xp) {
        if (player == null) {
            return;
        }

        if (legacy_v13 || legacy_v12 || legacy_v8) {
            ExperienceAPI.addXP(player, "archery", xp);
        }

        UserManager.getPlayer(player).beginXpGain(PrimarySkillType.ARCHERY, xp, XPGainReason.UNKNOWN, XPGainSource.CUSTOM);
    }

    public static void addAxes(Player player, int xp) {
        if (player == null) {
            return;
        }

        if (legacy_v13 || legacy_v12 || legacy_v8) {
            ExperienceAPI.addXP(player, "axes", xp);
        }

        UserManager.getPlayer(player).beginXpGain(PrimarySkillType.AXES, xp, XPGainReason.UNKNOWN, XPGainSource.CUSTOM);
    }

    public static void addExcavation(Player player, int xp) {
        if (player == null) {
            return;
        }

        if (legacy_v13 || legacy_v12 || legacy_v8) {
            ExperienceAPI.addXP(player, "excavation", xp);
        }

        UserManager.getPlayer(player).beginXpGain(PrimarySkillType.EXCAVATION, xp, XPGainReason.UNKNOWN, XPGainSource.CUSTOM);
    }

    public static void addFishing(Player player, int xp) {
        if (player == null) {
            return;
        }

        if (legacy_v13 || legacy_v12 || legacy_v8) {
            ExperienceAPI.addXP(player, "fishing", xp);
        }

        UserManager.getPlayer(player).beginXpGain(PrimarySkillType.FISHING, xp, XPGainReason.UNKNOWN, XPGainSource.CUSTOM);
    }

    public static void addHerbalism(Player player, int xp) {
        if (player == null) {
            return;
        }

        if (legacy_v13 || legacy_v12 || legacy_v8) {
            ExperienceAPI.addXP(player, "herbalism", xp);
        }

        UserManager.getPlayer(player).beginXpGain(PrimarySkillType.HERBALISM, xp, XPGainReason.UNKNOWN, XPGainSource.CUSTOM);
    }

    public static void addMining(Player player, int xp) {
        if (player == null) {
            return;
        }

        if (legacy_v13 || legacy_v12 || legacy_v8) {
            ExperienceAPI.addXP(player, "mining", xp);
        }

        UserManager.getPlayer(player).beginXpGain(PrimarySkillType.MINING, xp, XPGainReason.UNKNOWN, XPGainSource.CUSTOM);
    }

    public static void addRepair(Player player, int xp) {
        if (player == null) {
            return;
        }

        if (legacy_v13 || legacy_v12 || legacy_v8) {
            ExperienceAPI.addXP(player, "repair", xp);
        }

        UserManager.getPlayer(player).beginXpGain(PrimarySkillType.REPAIR, xp, XPGainReason.UNKNOWN, XPGainSource.CUSTOM);
    }

    public static void addSmelting(Player player, int xp) {
        if (player == null) {
            return;
        }

        if (legacy_v13 || legacy_v12 || legacy_v8) {
            ExperienceAPI.addXP(player, "smelting", xp);
        }

        UserManager.getPlayer(player).beginXpGain(PrimarySkillType.SMELTING, xp, XPGainReason.UNKNOWN, XPGainSource.CUSTOM);
    }

    public static void addSwords(Player player, int xp) {
        if (player == null) {
            return;
        }

        if (legacy_v13 || legacy_v12 || legacy_v8) {
            ExperienceAPI.addXP(player, "swords", xp);
        }

        UserManager.getPlayer(player).beginXpGain(PrimarySkillType.SWORDS, xp, XPGainReason.UNKNOWN, XPGainSource.CUSTOM);
    }

    public static void addTaming(Player player, int xp) {
        if (player == null) {
            return;
        }

        if (legacy_v13 || legacy_v12 || legacy_v8) {
            ExperienceAPI.addXP(player, "taming", xp);
        }

        UserManager.getPlayer(player).beginXpGain(PrimarySkillType.TAMING, xp, XPGainReason.UNKNOWN, XPGainSource.CUSTOM);
    }

    public static void addUnarmed(Player player, int xp) {
        if (player == null) {
            return;
        }

        if (legacy_v13 || legacy_v12 || legacy_v8) {
            ExperienceAPI.addXP(player, "unarmed", xp);
        }

        UserManager.getPlayer(player).beginXpGain(PrimarySkillType.UNARMED, xp, XPGainReason.UNKNOWN, XPGainSource.CUSTOM);
    }

    public static void addWoodcutting(Player player, int xp) {
        if (player == null) {
            return;
        }

        if (legacy_v13 || legacy_v12 || legacy_v8) {
            ExperienceAPI.addXP(player, "woodcutting", xp);
        }

        UserManager.getPlayer(player).beginXpGain(PrimarySkillType.WOODCUTTING, xp, XPGainReason.UNKNOWN, XPGainSource.CUSTOM);
    }

    public static boolean hasHerbalismDoubleDrops(Player player) {
        if (legacy_v13 || legacy_v12 || legacy_v8) {
            return hasBlockDoubleLegacy(player, "herbalism");
        }

        if (PrimarySkillType.HERBALISM.getDoubleDropsDisabled()) {
            return false;
        }

        return Permissions.isSubSkillEnabled(player, SubSkillType.HERBALISM_DOUBLE_DROPS)
                && RankUtils.hasReachedRank(1, player, SubSkillType.HERBALISM_DOUBLE_DROPS)
                && ProbabilityUtil.isSkillRNGSuccessful(SubSkillType.HERBALISM_DOUBLE_DROPS, player);
    }

    public static boolean hasMiningDoubleDrops(Player player) {
        if (legacy_v13 || legacy_v12 || legacy_v8) {
            return hasBlockDoubleLegacy(player, "mining");
        }

        if (PrimarySkillType.MINING.getDoubleDropsDisabled()) {
            return false;
        }

        return Permissions.isSubSkillEnabled(player, SubSkillType.MINING_DOUBLE_DROPS)
                && RankUtils.hasReachedRank(1, player, SubSkillType.MINING_DOUBLE_DROPS)
                && ProbabilityUtil.isSkillRNGSuccessful(SubSkillType.MINING_DOUBLE_DROPS, player);
    }

    public static boolean hasWoodcuttingDoubleDrops(Player player) {
        if (legacy_v13 || legacy_v12 || legacy_v8) {
            return hasBlockDoubleLegacy(player, "woodcutting");
        }

        if (PrimarySkillType.WOODCUTTING.getDoubleDropsDisabled()) {
            return false;
        }

        return Permissions.isSubSkillEnabled(player, SubSkillType.WOODCUTTING_HARVEST_LUMBER)
                && RankUtils.hasReachedRank(1, player, SubSkillType.WOODCUTTING_HARVEST_LUMBER)
                && ProbabilityUtil.isSkillRNGSuccessful(SubSkillType.WOODCUTTING_HARVEST_LUMBER, player);
    }

    public static boolean isUsingBerserk(Player player) {
        return AbilityAPI.berserkEnabled(player);
    }

    public static boolean isUsingGigaDrill(Player player) {
        return AbilityAPI.gigaDrillBreakerEnabled(player);
    }

    public static boolean isUsingGreenTerra(Player player) {
        return AbilityAPI.greenTerraEnabled(player);
    }

    public static boolean isUsingSerratedStrikes(Player player) {
        return AbilityAPI.serratedStrikesEnabled(player);
    }

    public static boolean isUsingSkullSplitter(Player player) {
        return AbilityAPI.skullSplitterEnabled(player);
    }

    public static boolean isUsingSuperBreaker(Player player) {
        return AbilityAPI.superBreakerEnabled(player);
    }

    public static boolean isUsingTreeFeller(Player player) {
        return AbilityAPI.treeFellerEnabled(player);
    }

    public static boolean isBleeding(LivingEntity victim) {
        return AbilityAPI.isBleeding(victim);
    }

    /**
     * woodcutting, mining, herbalism
     */
    protected static boolean hasBlockDoubleLegacy(Player player, String skill) {
        if (player.hasMetadata("mcMMO: Player Data")) {
            try {
                Object skillType = mcmmo_SkillType_valueOf.invoke(null, skill.toUpperCase());

                if ((boolean) mcmmo_SkillType_getDoubleDropsDisabled.invoke(skillType)) {
                    return false;
                }

                int skillLevel = (int) mcmmo_McMMOPlayer_getSkillLevel.invoke(UserManager.getPlayer(player), skillType);
                int activationChance = (int) mcmmo_PerksUtils_handleLuckyPerks.invoke(null, player, skillType);
                Object secondaryDouble = mcmmo_SecondaryAbility_valueOf.invoke(null, skill.toUpperCase() + "_DOUBLE_DROPS");

                if (!((boolean) mcmmo_Permissions_secondaryAbilityEnabled.invoke(null, player, secondaryDouble))) {
                    return false;
                }

                return (boolean) mcmmo_SkillUtils_activationSuccessful.invoke(null, secondaryDouble, player, skillLevel, activationChance);
            } catch (Exception ex) {
                Logger.getLogger(McMMOHandler.class.getName()).log(Level.SEVERE, "Failed to invoke McMMO Legacy Hook", ex);
            }
        }

        return false;
    }

    protected static void addBlockSkillLegacy(Player player, Collection<Block> blocks, String skill) {
        try {
            Object skillType = mcmmo_SkillType_valueOf.invoke(null, skill.toUpperCase());

            int xp = 0;
            for (Block block : blocks) {
                xp += (int) mcmmo_ExperienceConfig_getXp.invoke(mcmmo_ExperienceConfig_instance, skillType, legacy_getBlock(block));
            }

            ExperienceAPI.addXP(player, skill, xp);
        } catch (Exception ex) {
            Logger.getLogger(McMMOHandler.class.getName()).log(Level.SEVERE, "Failed to invoke McMMO Legacy Hook", ex);
        }
    }

    protected static Object legacy_getBlock(Block block) {
        if (legacy_v13) {
            return block.getBlockData();
        }

        if (legacy_v12) {
            return block.getState().getData();
        }

        return block.getType();
    }

    protected static int getSkillLegacy(Player player, String skill) {
        if (player.hasMetadata("mcMMO: Player Data")) {
            try {
                Object skillType = mcmmo_SkillType_valueOf.invoke(null, skill.toUpperCase());

                return (int) mcmmo_McMMOPlayer_getSkillLevel.invoke(UserManager.getPlayer(player), skillType);
            } catch (Exception ex) {
                Logger.getLogger(McMMOHandler.class.getName()).log(Level.SEVERE, "Failed to invoke McMMO Legacy Hook", ex);
            }
        }

        return 0;
    }
}
