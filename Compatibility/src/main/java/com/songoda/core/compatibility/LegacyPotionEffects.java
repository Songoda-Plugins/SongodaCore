package com.songoda.core.compatibility;

import org.bukkit.potion.PotionEffectType;

import java.util.HashMap;

public class LegacyPotionEffects {
    private LegacyPotionEffects() {
    }

    private static final HashMap<Integer, String> potionEffectNames = new HashMap<Integer, String>() {
        {
            put(PotionEffectType.SPEED.getId(), "Speed");
            put(PotionEffectType.SLOW.getId(), "Slowness");
            put(PotionEffectType.FAST_DIGGING.getId(), "Haste");
            put(PotionEffectType.SLOW_DIGGING.getId(), "Mining Fatigue");
            put(PotionEffectType.INCREASE_DAMAGE.getId(), "Strength");
            put(PotionEffectType.WEAKNESS.getId(), "Weakness");
            put(PotionEffectType.HEAL.getId(), "Instant Health");
            put(PotionEffectType.HARM.getId(), "Instant Damage");
            put(PotionEffectType.JUMP.getId(), "Jump Boost");
            put(PotionEffectType.CONFUSION.getId(), "Nausea");
            put(PotionEffectType.REGENERATION.getId(), "Regeneration");
            put(PotionEffectType.DAMAGE_RESISTANCE.getId(), "Resistance");
            put(PotionEffectType.FIRE_RESISTANCE.getId(), "Fire Resistance");
            put(PotionEffectType.WATER_BREATHING.getId(), "Water Breathing");
            put(PotionEffectType.INVISIBILITY.getId(), "Invisibility");
            put(PotionEffectType.BLINDNESS.getId(), "Blindness");
            put(PotionEffectType.NIGHT_VISION.getId(), "Night Vision");
            put(PotionEffectType.HUNGER.getId(), "Hunger");
            put(PotionEffectType.POISON.getId(), "Poison");
            put(PotionEffectType.WITHER.getId(), "Wither");
            put(PotionEffectType.HEALTH_BOOST.getId(), "Health Boost");
            put(PotionEffectType.ABSORPTION.getId(), "Absorption");
            put(PotionEffectType.SATURATION.getId(), "Saturation");
        }
    };

    public static String getEffectName(PotionEffectType e) {
        if (e == null) {
            return "null";
        }
        final String n = potionEffectNames.get(e.getId());
        return n == null ? e.getName() : n;
    }
}
