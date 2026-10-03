package com.songoda.core.hooks.ecoskills;

import com.willfp.ecoskills.api.EcoSkillsAPI;
import com.willfp.ecoskills.skills.Skill;
import com.willfp.ecoskills.skills.Skills;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

/**
 * Operations for awarding EcoSkills experience.
 */
public final class EcoSkillsHook {

    private EcoSkillsHook() {
    }

    public static boolean addSkillExperience(@NotNull OfflinePlayer player, @NotNull Skill skill, double amount) {
        if (!Double.isFinite(amount) || amount <= 0) {
            return false;
        }

        EcoSkillsAPI.giveSkillXP(player, skill, amount);
        return true;
    }

    /**
     * Resolves a configured skill ID and awards experience when that skill exists.
     *
     * @param player player receiving experience
     * @param skillId EcoSkills skill ID
     * @param amount experience amount
     * @return true when experience was awarded
     */
    public static boolean addSkillExperience(@NotNull OfflinePlayer player, @NotNull String skillId, double amount) {
        if (skillId.isBlank()) {
            return false;
        }

        Skill skill = Skills.INSTANCE.getByID(skillId);
        return skill != null && addSkillExperience(player, skill, amount);
    }
}
