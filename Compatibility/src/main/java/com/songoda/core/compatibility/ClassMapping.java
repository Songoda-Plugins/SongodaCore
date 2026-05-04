package com.songoda.core.compatibility;

/**
 * @deprecated Usage is highly discouraged – use com.songoda.core.nms.Nms#getImplementations() instead
 */
@Deprecated
public enum ClassMapping {
    BIOME_BASE("world.level.biome", "BiomeBase"),
    BIOME_STORAGE("world.level.chunk", "BiomeStorage"),
    BLOCK("world.level.block", "Block"),
    BLOCK_BASE("world.level.block.state", "BlockBase"),
    BLOCK_BUTTON_ABSTRACT("world.level.block", "BlockButtonAbstract"),
    BLOCK_PRESSURE_PLATE_ABSTRACT("world.level.block", "BlockPressurePlateAbstract"),
    BLOCK_LEVER("world.level.block", "BlockLever"),
    BLOCKS("world.level.block", "Blocks"),
    BLOCK_POSITION("core", "BlockPosition"),
    BLOCK_POSITION_26_1("core", "BlockPos"), // Added in 26.1
    CHAT_MESSAGE_TYPE("network.chat", "ChatMessageType"),
    CHUNK("world.level.chunk", "Chunk"),
    CLIENTBOUND_INITIALIZE_BORDER_PACKET("network.protocol.game", "ClientboundInitializeBorderPacket"), // Added in 1.17
    ENCHANTMENT_MANAGER("world.item.enchantment", "EnchantmentManager"),
    ENTITY("world.entity", "Entity"),
    ENTITY_INSENTIENT("world.entity", "EntityInsentient"),
    ENTITY_PLAYER("server.level", "EntityPlayer"),
    I_BLOCK_DATA("world.level.block.state", "IBlockData"),
    I_CHAT_BASE_COMPONENT("network.chat", "IChatBaseComponent"),
    I_REGISTRY("core", "IRegistry"),
    ITEM("world.item", "Item"),
    ITEM_STACK("world.item", "ItemStack"),
    LEVEL_ENTITY_GETTER("level.entity", "LevelEntityGetter"),
    MINECRAFT_SERVER("server", "MinecraftServer"),
    NBT_COMPRESSED_STREAM_TOOLS("nbt", "NBTCompressedStreamTools"),
    NBT_TAG_COMPOUND("nbt", "NBTTagCompound"),
    NBT_TAG_LIST("nbt", "NBTTagList"),
    NBT_BASE("nbt", "NBTBase"),
    PERSISTENT_ENTITY_SECTION_MANAGER("world.level.entity", "PersistentEntitySectionManager"),
    PACKET("network.protocol", "Packet"),
    PACKET_PLAY_OUT_CHAT("network.protocol.game", "PacketPlayOutChat"),
    /* 1.19 Packet */ CLIENTBOUND_SYSTEM_CHAT("network.protocol.game", "ClientboundSystemChatPacket"),
    PACKET_PLAY_OUT_WORLD_BORDER("PacketPlayOutWorldBorder"), // Removed in 1.17
    PLAYER_CONNECTION("server.network", "PlayerConnection"),
    WORLD("world.level", "World"),
    WORLD_BORDER("world.level.border", "WorldBorder"),
    WORLD_SERVER("server.level", "WorldServer"),

    CRAFT_BLOCK("block", "CraftBlock"),
    CRAFT_BLOCK_DATA("block.data", "CraftBlockData"),
    CRAFT_CHUNK("CraftChunk"),
    CRAFT_ENTITY("entity", "CraftEntity"),
    CRAFT_ITEM_STACK("inventory", "CraftItemStack"),
    CRAFT_MAGIC_NUMBERS("util", "CraftMagicNumbers"),
    CRAFT_PLAYER("entity", "CraftPlayer"),
    CRAFT_WORLD("CraftWorld"),

    SINGLE_THREADED_RANDOM_SOURCE("world.level.levelgen", "SingleThreadedRandomSource"),
    RANDOM_SOURCE("util", "RandomSource"),
    MOJANGSON_PARSER("nbt", "MojangsonParser");

    private final String packageName;
    private final String className;

    ClassMapping(String className) {
        this(null, className);
    }

    ClassMapping(String packageName, String className) {
        this.packageName = packageName;
        this.className = className;
    }

    public Class<?> getClazz() {
        return getClazz(null);
    }

    public Class<?> getClazz(String sub) {
        String name = sub == null ? this.className : this.className + "$" + sub;

        // BlockPos hack
        if (name.equals("BlockPosition") && ServerVersion.isServerVersionAtLeast(ServerVersion.V26_1)) {
            name = "BlockPos";
        }

        // NBTTagCompound hack
        if (name.equals("NBTTagCompound") && ServerVersion.isServerVersionAtLeast(ServerVersion.V26_1)) {
            name = "CompoundTag";
        }

        // NBTBase hack
        if (name.equals("NBTBase") && ServerVersion.isServerVersionAtLeast(ServerVersion.V26_1)) {
            name = "Tag";
        }

        // NBTTagList hack
        if (name.equals("NBTTagList") && ServerVersion.isServerVersionAtLeast(ServerVersion.V26_1)) {
            name = "ListTag";
        }

        if (ServerProject.isServer(ServerProject.PAPER) && (ServerVersion.isServerVersionAtLeast(ServerVersion.V1_20_5))) {
            //We don't have 1_20_R4 like packages in paper, so skip that part
            try {
                if (this.className.startsWith("Craft")) {
                    return Class.forName("org.bukkit.craftbukkit" + (this.packageName == null ? "" : "." + this.packageName) + "." + name);
                }

                return Class.forName("net.minecraft." + (this.packageName != null ? this.packageName : "server.") + "." + name);
            } catch (ClassNotFoundException ex) {
                ex.printStackTrace();
            }
        } else {
            try {
                if (this.className.startsWith("Craft")) {
                    return Class.forName("org.bukkit.craftbukkit." + ServerVersion.getServerVersionString()
                            + (this.packageName == null ? "" : "." + this.packageName) + "." + name);
                }

                return Class.forName("net.minecraft." + (
                        ServerVersion.isServerVersionAtLeast(ServerVersion.V1_17) && this.packageName != null
                                ? this.packageName : "server." + ServerVersion.getServerVersionString()) + "." + name);
            } catch (ClassNotFoundException ex) {
                ex.printStackTrace();
            }
        }

        return null;
    }
}
