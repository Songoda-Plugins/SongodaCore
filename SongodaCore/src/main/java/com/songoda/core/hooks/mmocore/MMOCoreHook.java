package com.songoda.core.hooks.mmocore;

import net.Indyuce.mmocore.api.player.PlayerData;
import net.Indyuce.mmocore.MMOCore;
import net.Indyuce.mmocore.experience.EXPSource;
import net.Indyuce.mmocore.experience.Profession;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Operations for awarding MMOCore class and profession experience.
 */
public final class MMOCoreHook {

    private MMOCoreHook() {
    }

    public static boolean addClassExperience(@NotNull Player player, double amount) {
        if (!Double.isFinite(amount) || amount <= 0 || !PlayerData.has(player)) {
            return false;
        }

        PlayerData playerData = PlayerData.get(player);
        if (playerData == null || !playerData.isFullyLoaded()) {
            return false;
        }

        playerData.giveExperience(amount, EXPSource.OTHER);
        return true;
    }

    public static boolean addProfessionExperience(@NotNull Player player,
                                                  @NotNull Profession profession,
                                                  double amount) {
        if (!Double.isFinite(amount) || amount <= 0 || !PlayerData.has(player)) {
            return false;
        }

        PlayerData playerData = PlayerData.get(player);
        if (playerData == null || !playerData.isFullyLoaded()) {
            return false;
        }

        playerData.getCollectionSkills().giveExperience(profession, amount, EXPSource.OTHER);
        return true;
    }

    /**
     * Resolves a configured profession ID and awards experience when that profession exists.
     *
     * @param player player receiving experience
     * @param professionId MMOCore profession ID
     * @param amount experience amount
     * @return true when experience was awarded
     */
    public static boolean addProfessionExperience(@NotNull Player player,
                                                  @NotNull String professionId,
                                                  double amount) {
        if (professionId.isBlank() || MMOCore.plugin == null) {
            return false;
        }

        Profession profession = MMOCore.plugin.professionManager.get(professionId);
        return profession != null && addProfessionExperience(player, profession, amount);
    }
}
