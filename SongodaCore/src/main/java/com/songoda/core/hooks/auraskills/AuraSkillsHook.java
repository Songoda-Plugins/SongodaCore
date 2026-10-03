package com.songoda.core.hooks.auraskills;

import dev.aurelium.auraskills.api.AuraSkillsApi;
import dev.aurelium.auraskills.api.registry.NamespacedId;
import dev.aurelium.auraskills.api.skill.Skill;
import dev.aurelium.auraskills.api.user.SkillsUser;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Operations for awarding AuraSkills experience.
 */
public final class AuraSkillsHook {

    private AuraSkillsHook() {
    }

    public static boolean addSkillExperience(@NotNull Player player, @NotNull Skill skill, double amount) {
        if (!Double.isFinite(amount) || amount <= 0) {
            return false;
        }

        SkillsUser user = AuraSkillsApi.get().getUser(player.getUniqueId());
        if (user == null || !user.isLoaded()) {
            return false;
        }

        user.addSkillXp(skill, amount);
        return true;
    }

    /**
     * Resolves a configured skill ID and awards experience when that skill exists.
     *
     * @param player player receiving experience
     * @param skillId AuraSkills skill ID, with an optional namespace
     * @param amount experience amount
     * @return true when experience was awarded
     */
    public static boolean addSkillExperience(@NotNull Player player, @NotNull String skillId, double amount) {
        if (skillId.isBlank()) {
            return false;
        }

        try {
            Skill skill = AuraSkillsApi.get().getGlobalRegistry().getSkill(NamespacedId.fromDefault(skillId));
            return skill != null && addSkillExperience(player, skill, amount);
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }
}
