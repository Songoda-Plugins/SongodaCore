package com.songoda.core.hooks.betonquest;

import org.betonquest.betonquest.api.BetonQuestApi;
import org.betonquest.betonquest.api.BetonQuestApiService;
import org.betonquest.betonquest.api.QuestException;
import org.betonquest.betonquest.api.identifier.ActionIdentifier;
import org.betonquest.betonquest.api.config.quest.QuestPackage;
import org.betonquest.betonquest.api.profile.Profile;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

/**
 * Operations for running player actions through BetonQuest.
 */
public final class BetonQuestHook {

    private BetonQuestHook() {
    }

    public static @NotNull Optional<BetonQuestApi> getApi(@NotNull Plugin plugin) {
        BetonQuestApiService service = Bukkit.getServicesManager().load(BetonQuestApiService.class);
        return service == null ? Optional.empty() : Optional.of(service.api(plugin));
    }

    public static boolean runAction(@NotNull Plugin plugin,
                                    @NotNull Player player,
                                    @NotNull String packageName,
                                    @NotNull String actionName) {
        Optional<BetonQuestApi> apiOptional = getApi(plugin);
        if (apiOptional.isEmpty()) {
            return false;
        }

        BetonQuestApi api = apiOptional.get();
        QuestPackage questPackage = api.packages().getPackage(packageName);
        if (questPackage == null) {
            return false;
        }

        ActionIdentifier identifier;
        try {
            identifier = api.identifiers()
                    .getFactory(ActionIdentifier.class)
                    .parseIdentifier(questPackage, actionName);
        } catch (QuestException exception) {
            return false;
        }
        Profile profile = api.profiles().getProfile(player);
        return api.actions().manager().run(profile, identifier);
    }
}
