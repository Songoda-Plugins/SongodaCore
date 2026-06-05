package com.songoda.core.compatibility;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Particle effects for servers 1.8 and below
 * TODO: Needs a recode, this class should not have any advanced logic like NMS magic
 */
public class LegacyParticleEffects {
    public enum Type {
        EXPLOSION_NORMAL("explode"),
        EXPLOSION_LARGE("largeexplode"),
        EXPLOSION_HUGE("hugeexplosion"),
        FIREWORKS_SPARK("fireworksSpark"),
        WATER_BUBBLE("bubble"),
        WATER_SPLASH("splash"),
        WATER_WAKE("wake", ServerVersion.V1_8),
        SUSPENDED("suspended"),
        SUSPENDED_DEPTH("depthsuspend"),
        CRIT("crit"),
        CRIT_MAGIC("magicCrit"),
        SMOKE_NORMAL("smoke"),
        SMOKE_LARGE("largesmoke"),
        SPELL("spell"),
        SPELL_INSTANT("instantSpell"),
        SPELL_MOB("mobSpell"),
        SPELL_MOB_AMBIENT("mobSpellAmbient"),
        SPELL_WITCH("witchMagic"),
        DRIP_WATER("dripWater"),
        DRIP_LAVA("dripLava"),
        VILLAGER_ANGRY("angryVillager"),
        VILLAGER_HAPPY("happyVillager"),
        TOWN_AURA("townaura"),
        NOTE("note"),
        PORTAL("portal"),
        ENCHANTMENT_TABLE("enchantmenttable"),
        FLAME("flame"),
        LAVA("lava"),
        FOOTSTEP("footstep"),
        CLOUD("cloud"),
        REDSTONE("reddust"),
        SNOWBALL("snowballpoof"),
        SNOW_SHOVEL("snowshovel"),
        SLIME("slime"),
        HEART("heart"),
        BARRIER("barrier", ServerVersion.V1_8),
        /**
         * Used when a block is broken
         */
        ITEM_CRACK("iconcrack_"),
        BLOCK_CRACK("blockcrack_", ServerVersion.V1_8),
        BLOCK_DUST("blockdust_", ServerVersion.V1_8),
        WATER_DROP("droplet", ServerVersion.V1_8),
        ITEM_TAKE("take", ServerVersion.V1_8),
        MOB_APPEARANCE("mobappearance", ServerVersion.V1_8),
        TOOL_BREAK("tilecrack_", ServerVersion.UNKNOWN, ServerVersion.V1_7);

        public final String name;
        public final ServerVersion minVersion;
        public final ServerVersion maxVersion;

        Type(String name) {
            this.name = name;
            this.minVersion = ServerVersion.UNKNOWN;
            this.maxVersion = null;
        }

        Type(String name, ServerVersion minVersion) {
            this.name = name;
            this.minVersion = minVersion;
            this.maxVersion = null;
        }

        Type(String name, ServerVersion minVersion, ServerVersion maxVersion) {
            this.name = name;
            this.minVersion = minVersion;
            this.maxVersion = maxVersion;
        }

        public static Type getById(String id) {
            for (Type t : Type.values()) {
                if (t.name.equalsIgnoreCase(id) || t.name().equalsIgnoreCase(id)) {
                    return t;
                }
            }
            return null;
        }
    }

    private static final String version = getNMSVersion();
    private static boolean enabled = true;
    private static Class<?> mc_packetPlayOutWorldParticlesClazz;
    private static Class<?> cb_craftPlayerClazz;
    private static Method cb_craftPlayer_getHandle;
    private static Class<?> mc_entityPlayerClazz;
    private static Class<?> mc_playerConnectionClazz;
    private static Field mc_entityPlayer_playerConnection;
    private static Class<?> mc_PacketInterface;
    private static Method mc_playerConnection_sendPacket;
    private static Class<?> mc_EnumParticle;
    private static Method mc_EnumParticle_valueOf;

    static {
        try {
            // lower versions use "Packet63WorldParticles"
            if (!version.startsWith("v1_7") && !version.startsWith("v1_8")) {
                mc_packetPlayOutWorldParticlesClazz = Class.forName("net.minecraft.server." + version + ".Packet63WorldParticles");
            } else {
                mc_packetPlayOutWorldParticlesClazz = Class.forName("net.minecraft.server." + version + ".PacketPlayOutWorldParticles");
            }
            cb_craftPlayerClazz = Class.forName("org.bukkit.craftbukkit." + version + ".entity.CraftPlayer");
            cb_craftPlayer_getHandle = cb_craftPlayerClazz.getDeclaredMethod("getHandle");
            mc_entityPlayerClazz = Class.forName("net.minecraft.server." + version + ".EntityPlayer");
            mc_entityPlayer_playerConnection = mc_entityPlayerClazz.getDeclaredField("playerConnection");
            mc_playerConnectionClazz = Class.forName("net.minecraft.server." + version + ".PlayerConnection");
            mc_PacketInterface = Class.forName("net.minecraft.server." + version + ".Packet");
            mc_playerConnection_sendPacket = mc_playerConnectionClazz.getDeclaredMethod(ServerVersion.isServerVersion(ServerVersion.V1_18) ? "a" : "sendPacket", mc_PacketInterface); // FIXME: Doesn't work cross version, use NMSManager#getPlayer() instead
            if (version.startsWith("v1_8")) {
                // Aren't worrying about anything after 1.8 in this class here
                mc_EnumParticle = Class.forName("net.minecraft.server." + version + ".EnumParticle");
                mc_EnumParticle_valueOf = mc_EnumParticle.getDeclaredMethod("valueOf", String.class);
            }
        } catch (Throwable ex) {
            Logger.getAnonymousLogger().log(Level.WARNING, "Problem preparing particle packets (disabling further packets)", ex);
            enabled = false;
        }
    }

    private static String getNMSVersion() {
        String ver = Bukkit.getServer().getClass().getPackage().getName();
        return ver.substring(ver.lastIndexOf('.') + 1);
    }

    public static void createParticle(Location l, Type e) {
        createParticle(l, e, 0F, 0F, 0F, 1, 0, null);
    }

    public static void createParticle(Location l, Type e, List<Player> localOnly) {
        createParticle(l, e, 0F, 0F, 0F, 1, 0, localOnly);
    }

    public static void createParticle(Location l, Type e, float effectSpeed, int amountOfParticles) {
        createParticle(l, e, 0F, 0F, 0F, effectSpeed, amountOfParticles, null);
    }

    /**
     * @param l                 exact location to spawn the particle
     * @param e                 particle effect type
     * @param xx                for notes, this is a value 0-1 for the color ([0-24]/24), for
     *                          redstone this is the red value 0-1 ([0-255]/255).
     *                          Otherwise, this is the distance for particles to fly on the x-axis.
     * @param yy                for redstone this is the green value 0-1 ([0-255]/255)
     *                          Otherwise this is the distance for particles to fly on the y-axis.
     * @param zz                for redstone this is the blue value 0-1 ([0-255]/255)
     *                          Otherwise this is the distance for particles to fly on the z-axis.
     * @param effectSpeed       particle effect speed
     * @param amountOfParticles extra particles to spawn (client-side)
     * @param localOnly         list of players to send the packets to, or null for all players
     */
    public static void createParticle(Location l, Type e, float xx, float yy, float zz, float effectSpeed, int amountOfParticles, List<Player> localOnly) {
        if (!enabled || e == null || effectSpeed < 0 || amountOfParticles < 0
                || !ServerVersion.isServerVersionAtLeast(e.minVersion)
                || (e.maxVersion != null && !ServerVersion.isServerVersionBelow(e.maxVersion))) {
            return;
        }
        final int rangeSquared = 256; // apparently there is no way to override this (unless to make smaller, of course)
        // collect a list of players to send this packet to
        List<Player> sendTo = new ArrayList<>();
        if (localOnly == null) {
            for (Player p : l.getWorld().getPlayers()) {
                if (p.getLocation().distanceSquared(l) <= rangeSquared) {
                    sendTo.add(p);
                }
            }
        } else {
            final World w = l.getWorld();
            for (Player p : localOnly) {
                if (p.getWorld() == w && p.getLocation().distanceSquared(l) <= rangeSquared) {
                    sendTo.add(p);
                }
            }
        }
        if (sendTo.isEmpty()) {
            return;
        }
        try {
            // Make an instance of the packet!
            Object sPacket = mc_packetPlayOutWorldParticlesClazz.newInstance();
            for (Field field : sPacket.getClass().getDeclaredFields()) {
                // Set those fields we need to be accessible!
                field.setAccessible(true);
                final String fieldName = field.getName();

                // Set them to what we want!
                if (fieldName.equals("a")) {
                    // we're just going to assume it's either 1.7 or 1.8
                    if (version.startsWith("v1_8")) {
                        // 1.8 uses an Enum
                        field.set(sPacket, mc_EnumParticle_valueOf.invoke(null, e.name()));
                    } else {
                        field.set(sPacket, e.name);
                    }
                } else if (fieldName.equals("b")) {
                    field.setFloat(sPacket, (float) l.getX()); // x
                } else if (fieldName.equals("c")) {
                    field.setFloat(sPacket, (float) l.getY()); // y
                } else if (fieldName.equals("d")) {
                    field.setFloat(sPacket, (float) l.getZ()); // z
                } else if (fieldName.equals("e")) {
                    field.setFloat(sPacket, xx); // xOffset
                } else if (fieldName.equals("f")) {
                    field.setFloat(sPacket, yy); // yOffset
                } else if (fieldName.equals("g")) {
                    field.setFloat(sPacket, zz); // zOffset
                } else if (fieldName.equals("h")) {
                    field.setFloat(sPacket, effectSpeed);
                } else if (fieldName.equals("i")) {
                    field.setInt(sPacket, amountOfParticles);
                }
                /*
                 1.8 also includes other data:
                 j = boolean, set if view distance is increased from 256 to 65536
                 k = int[] for packet data (like block type for ITEM_CRACK)
                 */
            }

            // send it on its way!
            for (Player p : sendTo) {
                sendPacket(sPacket, p);
            }
        } catch (Throwable ex) {
            Logger.getAnonymousLogger().log(Level.WARNING, "Problem preparing a particle packet (disabling further packets)", ex);
            enabled = false;
        }
    }

    // FIXME: Remove this method on next major release

    /**
     * @deprecated Doesn't work cross version, use NMSManager#getPlayer() instead
     */
    @Deprecated
    private static void sendPacket(Object packet, Player to) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        Object cbPlayer = cb_craftPlayer_getHandle.invoke(to);
        Object mcConnection = mc_entityPlayer_playerConnection.get(cbPlayer);

        mc_playerConnection_sendPacket.invoke(mcConnection, packet);
    }
}
