package com.songoda.core.compatibility;

import org.bukkit.entity.EntityType;

import java.util.HashMap;
import java.util.Map;

public class EntityNamespace {
    static final HashMap<String, EntityType> validTypes = new HashMap<>();
    static final HashMap<String, String> legacyToModernTypes = new HashMap<String, String>() {
        {
            put("xporb", "experience_orb");
            put("xp_orb", "experience_orb");
            put("leashknot", "leash_knot");
            put("smallfireball", "small_fireball");
            put("thrownenderpearl", "ender_pearl");
            put("eyeofendersignal", "eye_of_ender");
            put("eye_of_ender_signal", "eye_of_ender");
            put("thrownexpbottle", "experience_bottle");
            put("xp_bottle", "experience_bottle");
            put("itemframe", "item_frame");
            put("witherskull", "wither_skull");
            put("primedtnt", "tnt");
            put("fallingsand", "falling_block");
            put("fireworksrocketentity", "firework_rocket");
            put("fireworks_rocket", "firework_rocket");
            put("spectralarrow", "spectral_arrow");
            put("tippedarrow", "arrow");
            put("shulkerbullet", "shulker_bullet");
            put("dragonfireball", "dragon_fireball");
            put("armorstand", "armor_stand");
            put("minecartcommandblock", "command_block_minecart");
            put("commandblock_minecart", "command_block_minecart");
            put("minecartrideable", "minecart");
            put("minecartchest", "chest_minecart");
            put("minecartfurnace", "furnace_minecart");
            put("minecarttnt", "tnt_minecart");
            put("minecarthopper", "hopper_minecart");
            put("minecartmobspawner", "spawner_minecart");
            put("pigzombie", "zombie_pigman");
            put("cavespider", "cave_spider");
            put("lavaslime", "magma_cube");
            put("enderdragon", "ender_dragon");
            put("witherboss", "wither");
            put("mushroomcow", "mooshroom");
            put("snowman", "snow_golem");
            put("ozelot", "ocelot");
            put("villagergolem", "iron_golem");
            put("villager_golem", "iron_golem");
            put("entityhorse", "horse");
            put("endercrystal", "end_crystal");
            put("ender_crystal", "end_crystal");
        }
    };

    static {
        for (EntityType t : EntityType.values()) {
            if (t.getName() != null) {
                validTypes.put(t.getName().toLowerCase(), t);
            }
        }
    }

    public static EntityType minecraftToBukkit(String entity) {
        if (entity == null) {
            return null;
        }

        // first try to translate natively
        EntityType type = EntityType.fromName(entity);

        if (type == null) {
            // try legacy values
            type = EntityType.fromName(legacyToModernTypes.get(entity));

            // try converting modern to legacy
            if (type == null && legacyToModernTypes.containsValue(entity)) {
                for (Map.Entry<String, String> e : legacyToModernTypes.entrySet()) {
                    if (e.getValue().equals(entity) && (type = EntityType.fromName(legacyToModernTypes.get(e.getKey()))) != null) {
                        return type;
                    }
                }
            }
        }

        return type;
    }
}
