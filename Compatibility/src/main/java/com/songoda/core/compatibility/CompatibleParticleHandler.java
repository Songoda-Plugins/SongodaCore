package com.songoda.core.compatibility;

import org.bukkit.Color;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

public class CompatibleParticleHandler {
    public enum ParticleType {
        EXPLOSION_NORMAL,
        EXPLOSION_LARGE,
        EXPLOSION_HUGE,
        FIREWORKS_SPARK,
        WATER_BUBBLE,
        WATER_SPLASH,
        WATER_WAKE,
        SUSPENDED,
        SUSPENDED_DEPTH,
        CRIT,
        CRIT_MAGIC,
        SMOKE_NORMAL,
        SMOKE_LARGE,
        SPELL,
        SPELL_INSTANT,
        SPELL_MOB,
        SPELL_MOB_AMBIENT,
        SPELL_WITCH,
        DRIP_WATER,
        DRIP_LAVA,
        VILLAGER_ANGRY,
        VILLAGER_HAPPY,
        TOWN_AURA,
        NOTE,
        PORTAL,
        ENCHANTMENT_TABLE,
        FLAME,
        LAVA,
        CLOUD,
        REDSTONE(), //DustOptions
        SNOWBALL,
        SNOW_SHOVEL,
        SLIME,
        HEART,
        BARRIER,
        ITEM_CRACK(), // ItemStack
        BLOCK_CRACK(), // BlockData
        BLOCK_DUST(), // BlockData
        WATER_DROP,
        // 1.8-1.12 included ITEM_TAKE
        MOB_APPEARANCE,
        /// End 1.8 particles ///
        DRAGON_BREATH(ServerVersion.V1_9, "SPELL_MOB_AMBIENT"),
        END_ROD(ServerVersion.V1_9, "ENCHANTMENT_TABLE"),
        DAMAGE_INDICATOR(ServerVersion.V1_9, "VILLAGER_ANGRY"),
        SWEEP_ATTACK(ServerVersion.V1_9, "CRIT"),
        /// End 1.9 particles ///
        FALLING_DUST(ServerVersion.V1_10, "BLOCK_DUST"), // BlockData
        /// End 1.10 ///
        TOTEM(ServerVersion.V1_11, "VILLAGER_HAPPY"),
        SPIT(ServerVersion.V1_11, "REDSTONE"),
        /// End 1.11-1.12 ///
        SQUID_INK(ServerVersion.V1_13, "CRIT"),
        BUBBLE_POP(ServerVersion.V1_13, "CRIT"),
        CURRENT_DOWN(ServerVersion.V1_13, "CRIT"),
        BUBBLE_COLUMN_UP(ServerVersion.V1_13, "CRIT"),
        NAUTILUS(ServerVersion.V1_13, "ENCHANTMENT_TABLE"),
        DOLPHIN(ServerVersion.V1_13, "TOWN_AURA"),
        /// End 1.13 ///
        SNEEZE(ServerVersion.V1_14, "REDSTONE"),
        CAMPFIRE_COSY_SMOKE(ServerVersion.V1_14, "SMOKE_NORMAL"),
        CAMPFIRE_SIGNAL_SMOKE(ServerVersion.V1_14, "SMOKE_LARGE"),
        COMPOSTER(ServerVersion.V1_14, "CRIT"),
        FLASH(ServerVersion.V1_14, "EXPLOSION_NORMAL"), // idk
        FALLING_LAVA(ServerVersion.V1_14, "DRIP_LAVA"),
        LANDING_LAVA(ServerVersion.V1_14, "LAVA"),
        FALLING_WATER(ServerVersion.V1_14, "DRIP_WATER"),
        /// End 1.14 ///
        DRIPPING_HONEY(ServerVersion.V1_15, "DRIP_WATER"),
        FALLING_HONEY(ServerVersion.V1_15, "DRIP_WATER"),
        FALLING_NECTAR(ServerVersion.V1_15, "DRIP_WATER"),
        LANDING_HONEY(ServerVersion.V1_15, "DRIP_WATER"),
        /// End 1.15 ///
        // ToDo: Someone needs to make better compatible fall backs.
        SOUL_FIRE_FLAME(ServerVersion.V1_16, "DRIP_WATER"),
        ASH(ServerVersion.V1_16, "DRIP_WATER"),
        CRIMSON_SPORE(ServerVersion.V1_16, "DRIP_WATER"),
        WARPED_SPORE(ServerVersion.V1_16, "DRIP_WATER"),
        SOUL(ServerVersion.V1_16, "DRIP_WATER"),
        DRIPPING_OBSIDIAN_TEAR(ServerVersion.V1_16, "DRIP_WATER"),
        FALLING_OBSIDIAN_TEAR(ServerVersion.V1_16, "DRIP_WATER"),
        LANDING_OBSIDIAN_TEAR(ServerVersion.V1_16, "DRIP_WATER"),
        REVERSE_PORTAL(ServerVersion.V1_16, "DRIP_WATER"),
        WHITE_ASH(ServerVersion.V1_16, "DRIP_WATER"),
        /// End 1.16 ///
        // ToDo: Someone needs to make better compatible fall backs.
        LIGHT(ServerVersion.V1_17, "DRIP_WATER"),
        DUST_COLOR_TRANSITION(ServerVersion.V1_17, "DRIP_WATER"),
        VIBRATION(ServerVersion.V1_17, "DRIP_WATER"),
        FALLING_SPORE_BLOSSOM(ServerVersion.V1_17, "DRIP_WATER"),
        SPORE_BLOSSOM_AIR(ServerVersion.V1_17, "DRIP_WATER"),
        SMALL_FLAME(ServerVersion.V1_17, "DRIP_WATER"),
        SNOWFLAKE(ServerVersion.V1_17, "DRIP_WATER"),
        DRIPPING_DRIPSTONE_LAVA(ServerVersion.V1_17, "DRIP_WATER"),
        FALLING_DRIPSTONE_LAVA(ServerVersion.V1_17, "DRIP_WATER"),
        DRIPPING_DRIPSTONE_WATER(ServerVersion.V1_17, "DRIP_WATER"),
        FALLING_DRIPSTONE_WATER(ServerVersion.V1_17, "DRIP_WATER"),
        GLOW_SQUID_INK(ServerVersion.V1_17, "DRIP_WATER"),
        GLOW(ServerVersion.V1_17, "DRIP_WATER"),
        WAX_ON(ServerVersion.V1_17, "DRIP_WATER"),
        WAX_OFF(ServerVersion.V1_17, "DRIP_WATER"),
        ELECTRIC_SPARK(ServerVersion.V1_17, "DRIP_WATER"),
        SCRAPE(ServerVersion.V1_17, "DRIP_WATER"),
        /// End 1.17 ///
        ;

        final boolean compatibilityMode;
        final LegacyParticleEffects.Type compatibleEffect;
        final Object particle;
        static final Map<String, ParticleType> map = new HashMap<>();

        static {
            for (ParticleType t : values()) {
                map.put(t.name(), t);
            }
        }

        ParticleType() {
            if (ServerVersion.isServerVersionBelow(ServerVersion.V1_9)) {
                this.compatibilityMode = true;
                this.particle = null;
                this.compatibleEffect = LegacyParticleEffects.Type.valueOf(name());
            } else {
                this.compatibleEffect = null;
                // does this particle exist in our version?
                Particle check = Stream.of(Particle.values()).filter(p -> p.name().equals(name())).findFirst().orElse(null);
                if (check != null) {
                    this.particle = check;
                    this.compatibilityMode = false;
                } else {
                    // this shouldn't happen, really
                    this.particle = Particle.END_ROD;
                    this.compatibilityMode = true;
                }
            }
        }

        ParticleType(ServerVersion minVersion, String compatible) {
            // Particle class doesn't exist in 1.8
            if (ServerVersion.isServerVersionBelow(ServerVersion.V1_9)) {
                this.compatibilityMode = true;
                this.compatibleEffect = LegacyParticleEffects.Type.valueOf(compatible);
                this.particle = null;
            } else if (ServerVersion.isServerVersionBelow(minVersion)) {
                this.compatibilityMode = true;
                this.compatibleEffect = null;
                this.particle = Particle.valueOf(compatible);
            } else {
                this.compatibleEffect = null;
                // does this particle exist in our version?
                Particle check = Stream.of(Particle.values()).filter(p -> p.name().equals(name())).findFirst().orElse(null);
                if (check != null) {
                    this.particle = check;
                    this.compatibilityMode = false;
                } else {
                    // this shouldn't happen, really
                    this.particle = Particle.END_ROD;
                    this.compatibilityMode = true;
                }
            }
        }

        public static ParticleType getParticle(String name) {
            return map.get(name);
        }
    }

    public static void spawnParticles(String type, Location location) {
        spawnParticles(type, location, 0);
    }

    public static void spawnParticles(String type, Location location, int count) {
        ParticleType pt;
        if (type != null && (pt = ParticleType.getParticle(type.toUpperCase())) != null) {
            spawnParticles(pt, location, count);
        }
    }

    public static void spawnParticles(String type, Location location, int count, double offsetX, double offsetY, double offsetZ) {
        ParticleType pt;
        if (type != null && (pt = ParticleType.getParticle(type.toUpperCase())) != null) {
            spawnParticles(pt, location, count, offsetX, offsetY, offsetZ);
        }
    }

    public static void spawnParticles(ParticleType type, Location location) {
        if (ServerVersion.isServerVersionBelow(ServerVersion.V1_9)) {
            LegacyParticleEffects.createParticle(location, type.compatibleEffect);
        } else {
            location.getWorld().spawnParticle((Particle) type.particle, location, 0);
        }
    }

    public static void spawnParticles(ParticleType type, Location location, int count) {
        if (ServerVersion.isServerVersionBelow(ServerVersion.V1_9)) {
            for (int i = 0; i < count; ++i) {
                float xx = (float) (1 * (Math.random() - Math.random()));
                float yy = (float) (1 * (Math.random() - Math.random()));
                float zz = (float) (1 * (Math.random() - Math.random()));

                Location at = location.clone().add(xx, yy, zz);
                LegacyParticleEffects.createParticle(at, type.compatibleEffect);
            }
        } else {
            location.getWorld().spawnParticle((Particle) type.particle, location, count);
        }
    }

    public static void spawnParticles(ParticleType type, Location location, int count, double offsetX, double offsetY, double offsetZ) {
        if (ServerVersion.isServerVersionBelow(ServerVersion.V1_9)) {
            for (int i = 0; i < count; ++i) {
                float xx = (float) (offsetX * (Math.random() - Math.random()));
                float yy = (float) (offsetY * (Math.random() - Math.random()));
                float zz = (float) (offsetZ * (Math.random() - Math.random()));
                Location at = location.clone().add(xx, yy, zz);
                LegacyParticleEffects.createParticle(at, type.compatibleEffect);
            }
        } else {
            location.getWorld().spawnParticle((Particle) type.particle, location, count, offsetX, offsetY, offsetZ);
        }
    }

    public static void spawnParticles(ParticleType type, Location location, int count, double offsetX, double offsetY, double offsetZ, double extra) {
        spawnParticles(type, location, count, offsetX, offsetY, offsetZ, extra, null);
    }

    public static void spawnParticles(ParticleType type, Location location, int count, double offsetX, double offsetY, double offsetZ, double extra, Player receiver) {
        if (ServerVersion.isServerVersionBelow(ServerVersion.V1_9)) {
            for (int i = 0; i < count; i++) {
                float xx = (float) (offsetX * (Math.random() - Math.random()));
                float yy = (float) (offsetY * (Math.random() - Math.random()));
                float zz = (float) (offsetZ * (Math.random() - Math.random()));
                Location at = location.clone().add(xx, yy, zz);
                LegacyParticleEffects.createParticle(at, type.compatibleEffect, 0F, 0F, 0F, (float) extra, 0, receiver != null ? Collections.singletonList(receiver) : null);
            }
        } else {
            if (receiver == null) {
                location.getWorld().spawnParticle((Particle) type.particle, location, count, offsetX, offsetY, offsetZ, extra);
            } else {
                receiver.spawnParticle((Particle) type.particle, location, count, offsetX, offsetY, offsetZ, extra);
            }
        }
    }

    public static void redstoneParticles(Location location, int red, int green, int blue) {
        redstoneParticles(location, red, green, blue, 1F, 1, 0, null);
    }

    public static void redstoneParticles(Location location, int red, int green, int blue, float size, int count, float radius) {
        redstoneParticles(location, red, green, blue, size, count, radius, null);
    }

    /**
     * Spawn colored redstone particles
     *
     * @param location area to spawn the particle in
     * @param red      red value 0-255
     * @param green    green value 0-255
     * @param blue     blue value 0-255
     * @param size     (1.13+) size of the particles
     * @param count    how many particles to spawn
     * @param radius   how far to spread out the particles from location
     */
    public static void redstoneParticles(Location location, int red, int green, int blue, float size, int count, float radius, Player player) {
        if (ServerVersion.isServerVersionAtLeast(ServerVersion.V1_13)) {
            float xx = (float) (radius * (Math.random() - Math.random()));
            float yy = (float) (radius * (Math.random() - Math.random()));
            float zz = (float) (radius * (Math.random() - Math.random()));
            if (player == null) {
                location.getWorld().spawnParticle(Particle.REDSTONE, location, count, xx, yy, zz, 1, new Particle.DustOptions(Color.fromBGR(blue, green, red), size));
            } else {
                player.spawnParticle(Particle.REDSTONE, location, count, xx, yy, zz, 1, new Particle.DustOptions(Color.fromBGR(blue, green, red), size));
            }
        } else if (ServerVersion.isServerVersionAtLeast(ServerVersion.V1_9)) {
            for (int i = 0; i < count; ++i) {
                float xx = (float) (radius * (Math.random() - Math.random()));
                float yy = (float) (radius * (Math.random() - Math.random()));
                float zz = (float) (radius * (Math.random() - Math.random()));
                Location at = location.clone().add(xx, yy, zz);
                if (player == null)
                    location.getWorld().spawnParticle(Particle.REDSTONE, at, 0, red / 255F, green / 255F, blue / 255F, size); // particle, location, count, red, green, blue, extra data
                else
                    player.spawnParticle(Particle.REDSTONE, at, 0, red / 255F, green / 255F, blue / 255F, size); // particle, location, count, red, green, blue, extra data
            }
        } else {
            // WE NEED MAGIC!
            for (int i = 0; i < count; ++i) {
                float xx = (float) (radius * (Math.random() - Math.random()));
                float yy = (float) (radius * (Math.random() - Math.random()));
                float zz = (float) (radius * (Math.random() - Math.random()));

                Location at = location.clone().add(xx, yy, zz);
                LegacyParticleEffects.createParticle(at, LegacyParticleEffects.Type.REDSTONE,
                        red / 255F, green / 255F, blue / 255F, 1F,
                        0, player == null ? null : Collections.singletonList(player));
            }
        }
    }

    public static void bonemealSmoke(Location loc) {
        World world = loc.getWorld();
        assert world != null;

        world.playEffect(loc, Effect.SMOKE, BlockFace.SOUTH_EAST);
        world.playEffect(loc, Effect.SMOKE, BlockFace.SOUTH);
        world.playEffect(loc, Effect.SMOKE, BlockFace.SOUTH_WEST);
        world.playEffect(loc, Effect.SMOKE, BlockFace.EAST);
        world.playEffect(loc, Effect.SMOKE, BlockFace.SELF);
        world.playEffect(loc, Effect.SMOKE, BlockFace.WEST);
        world.playEffect(loc, Effect.SMOKE, BlockFace.NORTH_EAST);
        world.playEffect(loc, Effect.SMOKE, BlockFace.NORTH);
        world.playEffect(loc, Effect.SMOKE, BlockFace.NORTH_WEST);
    }
}
