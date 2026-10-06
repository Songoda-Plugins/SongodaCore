package com.songoda.core.utils;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.reflect.accessors.Accessors;
import com.comphenix.protocol.reflect.accessors.FieldAccessor;
import com.comphenix.protocol.utility.MinecraftReflection;
import com.comphenix.protocol.wrappers.BukkitConverters;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import lombok.experimental.UtilityClass;
import com.songoda.core.SongodaPlugin;
import com.songoda.core.compatibility.KnownServerVersions;
import com.songoda.core.compatibility.ServerVersion;
import com.songoda.core.text.AdventureUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Utility class for sending fake advancement toasts to players via ProtocolLib.
 * Compatible with Minecraft 1.18.2 to 1.21.1.
 */
@UtilityClass
public class AdvancementUtil {

    private static final ProtocolManager protocolManager = ProtocolLibrary.getProtocolManager();
    private static final boolean IS_1_20_2_PLUS = ServerVersion.isAtLeastVersion(KnownServerVersions.V1_20_2);
    private static Constructor<?> displayInfoConstructor;
    private static Constructor<?> advancementConstructor;
    private static Constructor<?> advancementHolderConstructor;
    private static Constructor<?> advancementRequirementsConstructor;
    private static Constructor<?> resourceLocationConstructor;
    private static Method resourceLocationFactory;
    private static Object emptyRewards;
    private static Constructor<?> advancementProgressConstructor;
    private static Class<?> criterionClass;
    private static Class<?> criterionProgressClass;
    private static Method criterionProgressGrantMethod;
    private static Object impossibleCriterion;
    private static FieldAccessor criteriaFieldAccessor;
    private static Object frameTypeTask;
    private static Object frameTypeGoal;
    private static Object frameTypeChallenge;
    private static boolean isInitialized = false;

    static {
        List<String> missingClasses = new ArrayList<>();
        try {
            Class<?> displayInfoClass = getNMSClass("net.minecraft.advancements.DisplayInfo", "advancements.DisplayInfo");
            if (displayInfoClass == null) missingClasses.add("DisplayInfo");

            Class<?> advancementClass = getNMSClass("net.minecraft.advancements.Advancement", "advancements.Advancement");
            if (advancementClass == null) missingClasses.add("Advancement");

            Class<?> frameTypeClass = getNMSClass(
                    "net.minecraft.advancements.AdvancementType",
                    "net.minecraft.advancements.FrameType",
                    "advancements.AdvancementType",
                    "advancements.FrameType"
            );

            // Smart Fallback for FrameType/AdvancementType: Inspect DisplayInfo constructor
            if (frameTypeClass == null && displayInfoClass != null) {
                for (Constructor<?> c : displayInfoClass.getDeclaredConstructors()) {
                    for (Class<?> p : c.getParameterTypes()) {
                        if (p.isEnum() && (p.getSimpleName().contains("Type") || p.getSimpleName().contains("Frame"))) {
                            frameTypeClass = p;
                            break;
                        }
                    }
                    if (frameTypeClass != null) break;
                }
            }

            if (frameTypeClass == null) missingClasses.add("FrameType/AdvancementType");

            Class<?> componentClass = getNMSClass("net.minecraft.network.chat.Component", "network.chat.Component");
            if (componentClass == null) missingClasses.add("Component");

            Class<?> itemStackClass = getNMSClass("net.minecraft.world.item.ItemStack", "world.item.ItemStack");
            if (itemStackClass == null) missingClasses.add("ItemStack");

            Class<?> resourceLocationClass = getNMSClass(
                    "net.minecraft.resources.ResourceLocation",
                    "resources.ResourceLocation",
                    "net.minecraft.resources.Identifier",
                    "resources.Identifier"
            );
            if (resourceLocationClass == null) missingClasses.add("ResourceLocation");

            Class<?> advancementRewardsClass = getNMSClass("net.minecraft.advancements.AdvancementRewards", "advancements.AdvancementRewards");
            if (advancementRewardsClass == null) missingClasses.add("AdvancementRewards");

            if (!missingClasses.isEmpty()) {
                throw new ClassNotFoundException("Failed to find NMS classes: " + String.join(", ", missingClasses));
            }
            try {
                resourceLocationConstructor = resourceLocationClass.getConstructor(String.class, String.class);
            } catch (NoSuchMethodException e) {
                // In 1.21+, ResourceLocation is a record and uses static factory methods
                for (Method m : resourceLocationClass.getDeclaredMethods()) {
                    if (java.lang.reflect.Modifier.isStatic(m.getModifiers()) &&
                            m.getReturnType() == resourceLocationClass &&
                            m.getParameterCount() == 2 &&
                            m.getParameterTypes()[0] == String.class &&
                            m.getParameterTypes()[1] == String.class) {
                        resourceLocationFactory = m;
                        break;
                    }
                }
                if (resourceLocationFactory == null) {
                    logDebugInfo(resourceLocationClass, "ResourceLocation constructor/factory");
                    throw e;
                }
            }

            // 2. Resolve Constructors based on version
            if (IS_1_20_2_PLUS) {
                Class<?> advancementHolderClass = getNMSClass("net.minecraft.advancements.AdvancementHolder");
                Class<?> advancementRequirementsClass = getNMSClass("net.minecraft.advancements.AdvancementRequirements");

                if (advancementHolderClass != null) {
                    advancementHolderConstructor = advancementHolderClass.getConstructor(resourceLocationClass, advancementClass);
                } else {
                    missingClasses.add("AdvancementHolder");
                }

                if (advancementRequirementsClass != null) {
                    advancementRequirementsConstructor = advancementRequirementsClass.getConstructor(List.class);
                } else {
                    missingClasses.add("AdvancementRequirements");
                }

                if (!missingClasses.isEmpty()) {
                    throw new ClassNotFoundException("Failed to find 1.20.2+ NMS classes: " + String.join(", ", missingClasses));
                }

                // DisplayInfo Record (1.20.2+): background is Optional
                try {
                    displayInfoConstructor = displayInfoClass.getConstructor(
                            itemStackClass, componentClass, componentClass, Optional.class, frameTypeClass, boolean.class, boolean.class, boolean.class
                    );
                } catch (NoSuchMethodException e) {
                    logDebugInfo(displayInfoClass, "DisplayInfo constructor (1.20.2+)");
                    throw e;
                }

                // Advancement Record (1.20.2+): parent and display are Optional
                try {
                    // Try 6 params: parent, display, rewards, criteria, requirements, telemetry
                    advancementConstructor = advancementClass.getConstructor(
                            Optional.class, Optional.class, advancementRewardsClass, Map.class, advancementRequirementsClass, boolean.class
                    );
                } catch (NoSuchMethodException e) {
                    try {
                        // Try 7 params for 1.20.5+ (adds Optional name)
                        advancementConstructor = advancementClass.getConstructor(
                                Optional.class, Optional.class, advancementRewardsClass, Map.class, advancementRequirementsClass, boolean.class, Optional.class
                        );
                    } catch (NoSuchMethodException ex) {
                        logDebugInfo(advancementClass, "Advancement constructor (1.20.2+)");
                        throw ex;
                    }
                }

                // Try to find EMPTY rewards
                try {
                    emptyRewards = advancementRewardsClass.getField("EMPTY").get(null);
                } catch (Exception e) {
                    try {
                        emptyRewards = advancementRewardsClass.getField("a").get(null);
                    } catch (Exception ignored) {
                    }
                }

                Class<?> progressClass = getNMSClass("net.minecraft.advancements.AdvancementProgress", "advancements.AdvancementProgress");
                if (progressClass != null) {
                    try {
                        advancementProgressConstructor = progressClass.getConstructor();
                        criteriaFieldAccessor = Accessors.getFieldAccessor(progressClass, Map.class, true);
                    } catch (Exception ignored) {
                    }
                }

                criterionClass = getNMSClass("net.minecraft.advancements.Criterion", "advancements.Criterion");
                criterionProgressClass = getNMSClass("net.minecraft.advancements.CriterionProgress", "advancements.CriterionProgress");
                if (criterionProgressClass != null) {
                    try {
                        criterionProgressGrantMethod = criterionProgressClass.getMethod("grant");
                    } catch (NoSuchMethodException e) {
                        try {
                            criterionProgressGrantMethod = criterionProgressClass.getMethod("b"); // Possible obfuscated name
                        } catch (Exception ignored) {
                        }
                    }
                }

                // Try to find a way to create an "impossible" criterion
                if (criterionClass != null) {
                    try {
                        Class<?> triggersClass = getNMSClass("net.minecraft.advancements.CriteriaTriggers", "advancements.CriteriaTriggers");
                        Class<?> impossibleTriggerClass = getNMSClass("net.minecraft.advancements.critereon.ImpossibleTrigger", "advancements.critereon.ImpossibleTrigger");

                        if (triggersClass != null && impossibleTriggerClass != null) {
                            // Find IMPOSSIBLE field
                            Object trig = null;
                            try {
                                trig = triggersClass.getField("IMPOSSIBLE").get(null);
                            } catch (Exception e) {
                                try {
                                    trig = triggersClass.getField("a").get(null);
                                } catch (Exception ignored) {
                                }
                            }

                            if (trig != null) {
                                Constructor<?> critCons = null;
                                for (Constructor<?> c : criterionClass.getConstructors()) {
                                    if (c.getParameterCount() == 2) {
                                        critCons = c;
                                        break;
                                    }
                                }

                                if (critCons != null) {
                                    // Need an instance of ImpossibleTrigger.TriggerInstance
                                    // In many versions it's a nested class 'a' or similar.
                                    Object inst = null;
                                    for (Class<?> inner : impossibleTriggerClass.getDeclaredClasses()) {
                                        if (inner.getSimpleName().contains("Instance") || inner.getSimpleName().equals("a")) {
                                            try {
                                                inst = inner.getConstructor().newInstance();
                                                break;
                                            } catch (Exception ignored) {
                                            }
                                        }
                                    }

                                    if (inst != null) {
                                        impossibleCriterion = critCons.newInstance(trig, inst);
                                    }
                                }
                            }
                        }
                    } catch (Exception ignored) {
                    }
                }
            } else {
                // Legacy DisplayInfo
                try {
                    displayInfoConstructor = displayInfoClass.getConstructor(
                            itemStackClass, componentClass, componentClass, resourceLocationClass, frameTypeClass, boolean.class, boolean.class, boolean.class
                    );
                } catch (NoSuchMethodException e) {
                    logDebugInfo(displayInfoClass, "Legacy DisplayInfo constructor");
                    throw e;
                }

                // Legacy Advancement
                try {
                    advancementConstructor = advancementClass.getConstructor(
                            resourceLocationClass, advancementClass, displayInfoClass, advancementRewardsClass, Map.class, String[][].class
                    );
                } catch (NoSuchMethodException e) {
                    logDebugInfo(advancementClass, "Legacy Advancement constructor");
                    throw e;
                }
            }

            // Resolve FrameTypes
            try {
                frameTypeTask = Enum.valueOf((Class<Enum>) frameTypeClass, "TASK");
                frameTypeGoal = Enum.valueOf((Class<Enum>) frameTypeClass, "GOAL");
                frameTypeChallenge = Enum.valueOf((Class<Enum>) frameTypeClass, "CHALLENGE");
            } catch (Exception e) {
                Object[] constants = frameTypeClass.getEnumConstants();
                if (constants != null && constants.length >= 3) {
                    frameTypeTask = constants[0];
                    frameTypeGoal = constants[1];
                    frameTypeChallenge = constants[2];
                } else if (constants != null && constants.length > 0) {
                    frameTypeTask = constants[0];
                    frameTypeGoal = constants[0];
                    frameTypeChallenge = constants[0];
                }
            }

            isInitialized = true;

        } catch (Exception e) {
            if (SongodaPlugin.getInstance() != null) {
                SongodaPlugin.getInstance().getLogger().severe("[AdvancementUtil] Failed to initialize reflection: " + e.getMessage());
                if (!(e instanceof ClassNotFoundException)) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static boolean isAvailable() {
        return isInitialized;
    }

    private static void logDebugInfo(Class<?> clazz, String context) {
        if (SongodaPlugin.getInstance() == null) return;
        var logger = SongodaPlugin.getInstance().getLogger();
        logger.severe("[AdvancementUtil] DEBUG: Failed to find " + context + " in " + clazz.getName());
        logger.severe("Available Constructors:");
        for (Constructor<?> c : clazz.getDeclaredConstructors()) {
            logger.severe(" - " + c.toString());
        }
        logger.severe("Available Methods:");
        for (Method m : clazz.getDeclaredMethods()) {
            StringBuilder params = new StringBuilder();
            for (Class<?> p : m.getParameterTypes()) params.append(p.getSimpleName()).append(", ");
            logger.severe(" - " + m.getReturnType().getSimpleName() + " " + m.getName() + "(" + (params.length() > 2 ? params.substring(0, params.length() - 2) : "") + ")");
        }
    }

    private static Class<?> getNMSClass(String... names) {
        for (String name : names) {
            try {
                Class<?> clazz = MinecraftReflection.getNullableNMS(name);
                if (clazz != null) return clazz;
                // Fallback to manual Class.forName
                return Class.forName(name);
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    /**
     * Sends a one-time fake advancement toast to a player with a player head icon.
     */
    public static void sendPlayerHeadToast(Player player, String title, String desc, Player headOwner) {
        sendPlayerHeadToast(player, title, desc, headOwner, ToastType.TASK, ToastStyle.SIMPLE);
    }

    public static void sendPlayerHeadToast(Player player, String title, String desc, Player headOwner, ToastType type) {
        sendPlayerHeadToast(player, title, desc, headOwner, type, ToastStyle.SIMPLE);
    }

    public static void sendPlayerHeadToast(Player player, String title, String desc, Player headOwner, ToastType type, ToastStyle style) {
        sendPlayerHeadToast(player, AdventureUtils.formatComponent(title), AdventureUtils.formatComponent(desc), headOwner, type, style);
    }

    /**
     * Sends a one-time fake advancement toast to a player with a player head icon.
     */
    public static void sendPlayerHeadToast(Player player, net.kyori.adventure.text.Component title, net.kyori.adventure.text.Component desc, Player headOwner) {
        sendPlayerHeadToast(player, title, desc, headOwner, ToastType.TASK, ToastStyle.SIMPLE);
    }

    /**
     * Sends a one-time fake advancement toast to a player with a player head icon.
     */
    public static void sendPlayerHeadToast(Player player, net.kyori.adventure.text.Component title, net.kyori.adventure.text.Component desc, Player headOwner, ToastType type) {
        sendPlayerHeadToast(player, title, desc, headOwner, type, ToastStyle.SIMPLE);
    }

    /**
     * Sends a one-time fake advancement toast to a player with a player head icon.
     */
    public static void sendPlayerHeadToast(Player player, net.kyori.adventure.text.Component title, net.kyori.adventure.text.Component desc, Player headOwner, ToastType type, ToastStyle style) {
        if (!isInitialized) return;

        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        if (meta != null) {
            meta.setOwningPlayer(headOwner);
            head.setItemMeta(meta);
        }
        sendToast(player, head, title, desc, type, style);
    }

    /**
     * Sends a one-time fake advancement toast to a player with any item icon.
     */
    public static void sendToast(Player player, ItemStack icon, String title, String description) {
        sendToast(player, icon, title, description, ToastType.TASK, ToastStyle.SIMPLE);
    }

    public static void sendToast(Player player, ItemStack icon, String title, String description, ToastType type) {
        sendToast(player, icon, title, description, type, ToastStyle.SIMPLE);
    }

    public static void sendToast(Player player, ItemStack icon, String title, String description, ToastType type, ToastStyle style) {
        sendToast(player, icon, AdventureUtils.formatComponent(title), AdventureUtils.formatComponent(description), type, style);
    }

    /**
     * Sends a one-time fake advancement toast to a player with any item icon.
     */
    public static void sendToast(Player player, ItemStack icon, net.kyori.adventure.text.Component title, net.kyori.adventure.text.Component description) {
        sendToast(player, icon, title, description, ToastType.TASK, ToastStyle.SIMPLE);
    }

    /**
     * Sends a one-time fake advancement toast to a player with any item icon.
     */
    public static void sendToast(Player player, ItemStack icon, net.kyori.adventure.text.Component title, net.kyori.adventure.text.Component description, ToastType type) {
        sendToast(player, icon, title, description, type, ToastStyle.SIMPLE);
    }

    /**
     * Sends a one-time fake advancement toast to a player with any item icon.
     */
    public static void sendToast(Player player, ItemStack icon, net.kyori.adventure.text.Component title, net.kyori.adventure.text.Component description, ToastType type, ToastStyle style) {
        if (!isInitialized) return;

        try {
            String randomId = "vortex_" + UUID.randomUUID().toString().substring(0, 8);
            Object key;
            if (resourceLocationConstructor != null) {
                key = resourceLocationConstructor.newInstance("vortex", randomId);
            } else {
                key = resourceLocationFactory.invoke(null, "vortex", randomId);
            }

            Object nmsIcon = BukkitConverters.getItemStackConverter().getGeneric(icon);

            net.kyori.adventure.text.Component finalTitle = title;
            if (style == ToastStyle.DETAILED && description != null && !AdventureUtils.clear(description).isEmpty()) {
                finalTitle = title.append(net.kyori.adventure.text.Component.newline()).append(description);
            }

            Object nmsTitle = WrappedChatComponent.fromJson(AdventureUtils.convertToJson(finalTitle)).getHandle();
            Object nmsDesc = WrappedChatComponent.fromJson(AdventureUtils.convertToJson(description)).getHandle();

            Object nmsFrameType = frameTypeTask;
            if (type == ToastType.GOAL) nmsFrameType = frameTypeGoal;
            else if (type == ToastType.CHALLENGE) nmsFrameType = frameTypeChallenge;

            Object displayInfo;
            Object advancement;

            if (IS_1_20_2_PLUS) {
                try {
                    displayInfo = displayInfoConstructor.newInstance(
                            nmsIcon, nmsTitle, nmsDesc, Optional.empty(), nmsFrameType, true, false, false
                    );
                } catch (IllegalArgumentException e) {
                    // Try without Optional for background (some versions/mappings might vary)
                    displayInfo = displayInfoConstructor.newInstance(
                            nmsIcon, nmsTitle, nmsDesc, null, nmsFrameType, true, false, false
                    );
                }

                Object requirements = null;
                Map<String, Object> criteriaMap = new HashMap<>();

                // Add a dummy criterion to make it "earnable"
                if (impossibleCriterion != null) {
                    criteriaMap.put("c", impossibleCriterion);
                } else if (criterionClass != null) {
                    try {
                        // Fallback: try to create a blank one if impossibleCriterion failed
                        Constructor<?> critCons = null;
                        for (Constructor<?> c : criterionClass.getConstructors()) {
                            if (c.getParameterCount() == 2) {
                                critCons = c;
                                break;
                            }
                        }
                        if (critCons != null) {
                            criteriaMap.put("c", critCons.newInstance(null, null));
                        }
                    } catch (Exception ignored) {
                    }
                }

                if (advancementRequirementsConstructor != null) {
                    try {
                        // requirements: List<List<String>>
                        // We want one requirement for our dummy criterion "c"
                        requirements = advancementRequirementsConstructor.newInstance(Collections.singletonList(Collections.singletonList("c")));
                    } catch (Exception e) {
                        try {
                            requirements = advancementRequirementsConstructor.newInstance(Collections.emptyList());
                        } catch (Exception ignored) {
                        }
                    }
                }

                if (advancementConstructor.getParameterCount() == 6) {
                    advancement = advancementConstructor.newInstance(
                            Optional.empty(), Optional.of(displayInfo), emptyRewards, criteriaMap, requirements, false
                    );
                } else {
                    advancement = advancementConstructor.newInstance(
                            Optional.empty(), Optional.of(displayInfo), emptyRewards, criteriaMap, requirements, false, Optional.empty()
                    );
                }

                Object advancementHolder = advancementHolderConstructor.newInstance(key, advancement);

                PacketContainer packet = protocolManager.createPacket(PacketType.Play.Server.ADVANCEMENTS);
                packet.getBooleans().write(0, false); // reset
                if (packet.getBooleans().size() > 1) {
                    packet.getBooleans().write(1, true); // show toast (1.21.11+)
                }

                // 1.20.2+ Fields: List<AdvancementHolder> added, Set<ResourceLocation> removed, Map<ResourceLocation, AdvancementProgress> progress
                packet.getModifier().withType(List.class).write(0, Collections.singletonList(advancementHolder));
                packet.getModifier().withType(Set.class).write(0, new HashSet<>());

                Map<Object, Object> progressMap = new HashMap<>();
                if (advancementProgressConstructor != null) {
                    try {
                        Object progress = advancementProgressConstructor.newInstance();

                        if (criteriaFieldAccessor != null) {
                            Map<String, Object> progressCriteria = (Map<String, Object>) criteriaFieldAccessor.get(progress);

                            if (criterionProgressClass != null) {
                                Object cp = criterionProgressClass.getConstructor().newInstance();
                                if (criterionProgressGrantMethod != null) {
                                    criterionProgressGrantMethod.invoke(cp);
                                }
                                progressCriteria.put("c", cp);
                            }
                        } else {
                        }

                        progressMap.put(key, progress);
                    } catch (Exception e) {
                        // Fallback: minimal progress
                        try {
                            progressMap.put(key, advancementProgressConstructor.newInstance());
                        } catch (Exception ignored) {
                        }
                    }
                }
                packet.getModifier().withType(Map.class).write(0, progressMap);

                protocolManager.sendServerPacket(player, packet);

            } else {
                displayInfo = displayInfoConstructor.newInstance(
                        nmsIcon, nmsTitle, nmsDesc, null, nmsFrameType, true, false, false
                );

                advancement = advancementConstructor.newInstance(
                        key, null, displayInfo, null, new HashMap<>(), new String[0][0]
                );

                PacketContainer packet = protocolManager.createPacket(PacketType.Play.Server.ADVANCEMENTS);
                packet.getBooleans().write(0, false); // reset

                Map<Object, Object> advancementMap = new HashMap<>();
                advancementMap.put(key, advancement);
                packet.getModifier().withType(Map.class).write(0, advancementMap);

                // Also need to send progress in legacy versions to show toast
                Map<Object, Object> progressMap = new HashMap<>();
                // Simplified legacy progress could be added here if needed
                packet.getModifier().withType(Map.class).write(1, progressMap); // Progress is usually the second Map

                protocolManager.sendServerPacket(player, packet);
            }

            // Remove after 10 seconds to keep the advancement tab clean
            Bukkit.getScheduler().runTaskLater(SongodaPlugin.getInstance(), () -> {
                try {
                    PacketContainer remove = protocolManager.createPacket(PacketType.Play.Server.ADVANCEMENTS);
                    remove.getBooleans().write(0, false);
                    remove.getModifier().withType(java.util.Set.class).write(0, Collections.singleton(key));
                    protocolManager.sendServerPacket(player, remove);
                } catch (Exception ignored) {
                }
            }, 200L);

        } catch (Exception e) {
            SongodaPlugin.getInstance().getLogger().warning("Failed to send toast to " + player.getName() + ": " + e.getMessage());
            if (e instanceof IllegalArgumentException || e.getCause() instanceof IllegalArgumentException) {
                logDetailedError(e, player);
            }
        }
    }

    private static void logDetailedError(Exception e, Player player) {
        var logger = SongodaPlugin.getInstance().getLogger();
        logger.severe("[AdvancementUtil] Fatal error sending toast to " + player.getName());
        logger.severe("Exception: " + e.getClass().getName() + " - " + e.getMessage());
        if (e.getCause() != null) {
            logger.severe("Cause: " + e.getCause().getClass().getName() + " - " + e.getCause().getMessage());
        }
    }

    public enum ToastType {
        TASK,
        GOAL,
        CHALLENGE
    }

    public enum ToastStyle {
        SIMPLE,
        DETAILED
    }
}
