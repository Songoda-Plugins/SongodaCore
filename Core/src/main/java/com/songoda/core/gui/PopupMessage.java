package com.songoda.core.gui;

import com.songoda.core.compatibility.ServerVersion;
import com.cryptomorin.xseries.XMaterial;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.chat.ComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.advancement.Advancement;
import org.bukkit.advancement.AdvancementProgress;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.HashSet;
import java.util.UUID;

/**
 * Instance of a popup message that can be sent to a player <br>
 * Popup toast messages only work on Minecraft 1.12+ <br>
 * Calling this class on anything below 1.12 will cause ClassLoader Exceptions!
 */
class PopupMessage {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final HashSet<UUID> REGISTERED_MESSAGES = new HashSet<>();

    final UUID id = UUID.randomUUID();
    private final NamespacedKey key;
    private final TextComponent title;
    XMaterial icon;
    TriggerType trigger = TriggerType.IMPOSSIBLE;
    FrameType frame = FrameType.GOAL; // TASK is the default
    BackgroundType background = BackgroundType.ADVENTURE;

    PopupMessage(Plugin source, XMaterial icon, String title) {
        this.key = new NamespacedKey(source, "popup/" + this.id);
        this.title = new TextComponent(title.length() < 74 ? title : (title.substring(0, 72) + "..."));
        this.icon = icon;
    }

    PopupMessage(Plugin source, XMaterial icon, String title, BackgroundType background) {
        this.key = new NamespacedKey(source, "popup/" + this.id);
        this.title = new TextComponent(title.length() < 74 ? title : (title.substring(0, 72) + "..."));
        this.icon = icon;
        this.background = background;
    }

    private String getJSON() {
        JsonObject json = new JsonObject();
        JsonObject advDisplay = new JsonObject();

        if (this.icon != null) {
            JsonObject displayIcon = new JsonObject();
            displayIcon.addProperty("item", "minecraft:" + this.icon.parseMaterial().name().toLowerCase());

            if (this.icon.getData() != 0) {
                displayIcon.addProperty("data", this.icon.getData());
            }

            advDisplay.add("icon", displayIcon);
        }

        advDisplay.add("title", GSON.fromJson(ComponentSerializer.toString(this.title), JsonElement.class));
        advDisplay.addProperty("background", this.background.key);
        advDisplay.addProperty("description", "");
        advDisplay.addProperty("frame", this.frame.id);
        advDisplay.addProperty("announce_to_chat", false);
        advDisplay.addProperty("show_toast", true);
        advDisplay.addProperty("hidden", true);
        json.add("display", advDisplay);

        JsonObject advCriteria = new JsonObject();
        json.add("criteria", advCriteria);

        JsonObject advTrigger = new JsonObject();
        advTrigger.addProperty("trigger", this.trigger.getKey());
        /*if() {
            JsonObject advConditions = new JsonObject();
            // can add items to this list with [item,amount,data]
            advTrigger.add("conditions", advConditions);
        }*/
        advCriteria.add("mentioned", advTrigger);

        return GSON.toJson(json);
    }

    protected void grant(final Player pl) {
        final Advancement adv = getAdvancement();
        final AdvancementProgress progress = pl.getAdvancementProgress(adv);

        if (!progress.isDone()) {
            for (String s : progress.getRemainingCriteria()) {
                progress.awardCriteria(s);
            }
        }
    }

    protected void revoke(final Player pl) {
        final Advancement adv = getAdvancement();
        final AdvancementProgress prog = pl.getAdvancementProgress(adv);

        if (prog.isDone()) {
            for (String s : prog.getAwardedCriteria()) {
                prog.revokeCriteria(s);
            }
        }
    }

    protected void add() {
        if (!REGISTERED_MESSAGES.contains(this.id)) {
            REGISTERED_MESSAGES.add(this.id);

            try {
                Bukkit.getUnsafe().loadAdvancement(this.key, getJSON());
            } catch (IllegalArgumentException ex) {
                Bukkit.getLogger().warning("Failed to create popup advancement!");
            }
        }
    }

    protected void remove() {
        if (REGISTERED_MESSAGES.contains(this.id)) {
            REGISTERED_MESSAGES.remove(this.id);
            Bukkit.getUnsafe().removeAdvancement(this.key);
        }
    }

    public Advancement getAdvancement() {
        return Bukkit.getAdvancement(this.key);
    }

    public enum FrameType {
        TASK,
        CHALLENGE,
        GOAL;

        final String id;

        FrameType() {
            this.id = name().toLowerCase();
        }
    }

    public enum TriggerType {
        ARBITRARY_PLAYER_TICK(ServerVersion.V1_13, "TICK"),
        BRED_ANIMALS,
        BREWED_POTION,
        CHANGED_DIMENSION,
        CONSTRUCT_BEACON,
        CONSUME_ITEM,
        CURED_ZOMBIE_VILLAGER,
        EFFECTS_CHANGED,
        ENCHANTED_ITEM,
        ENTER_BLOCK,
        ENTITY_HURT_PLAYER,
        ENTITY_KILLED_PLAYER,
        IMPOSSIBLE,
        INVENTORY_CHANGED,
        ITEM_DURABILITY_CHANGED,
        LEVITATION,
        LOCATION,
        NETHER_TRAVEL,
        PLACED_BLOCK,
        PLAYER_HURT_ENTITY,
        PLAYER_KILL_ENTITY,
        RECIPE_UNLOCKED,
        SLEPT_IN_BED,
        SUMMONED_ENTITY,
        TAME_ANIMAL,
        TICK,
        USED_ENDER_EYE,
        USED_TOTEM,
        VILLAGER_TRADE;

        final ServerVersion minVersion;
        final String compatible;
        final String key;

        TriggerType() {
            this.minVersion = ServerVersion.UNKNOWN;
            this.compatible = "";
            this.key = "minecraft:" + name().toLowerCase();
        }

        TriggerType(ServerVersion minVersion, String compatible) {
            this.minVersion = minVersion;
            this.compatible = compatible;
            this.key = "minecraft:" + (ServerVersion.isServerVersionAtLeast(minVersion) ? name() : compatible).toLowerCase();
        }

        public String getKey() {
            return this.key;
        }
    }
}
