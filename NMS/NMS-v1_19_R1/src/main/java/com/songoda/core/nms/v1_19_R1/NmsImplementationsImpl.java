package com.songoda.core.nms.v1_19_R1;

import com.songoda.core.nms.NmsImplementations;
import com.songoda.core.nms.entity.NMSPlayer;
import com.songoda.core.nms.entity.NmsEntity;
import com.songoda.core.nms.item.NmsItem;
import com.songoda.core.nms.nbt.NBTCore;
import com.songoda.core.nms.v1_19_R1.anvil.AnvilCore;
import com.songoda.core.nms.v1_19_R1.entity.NMSPlayerImpl;
import com.songoda.core.nms.v1_19_R1.entity.NmsEntityImpl;
import com.songoda.core.nms.v1_19_R1.item.NmsItemImpl;
import com.songoda.core.nms.v1_19_R1.nbt.NBTCoreImpl;
import com.songoda.core.nms.v1_19_R1.world.NmsWorldBorderImpl;
import com.songoda.core.nms.v1_19_R1.world.WorldCoreImpl;
import com.songoda.core.nms.world.NmsWorldBorder;
import com.songoda.core.nms.world.WorldCore;
import org.bukkit.craftbukkit.v1_19_R1.util.CraftMagicNumbers;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("unused")
public class NmsImplementationsImpl implements NmsImplementations {
    private final NmsEntity entity;
    private final NMSPlayer player;
    private final WorldCore world;
    private final NmsWorldBorder worldBorder;
    private final com.songoda.core.nms.anvil.AnvilCore anvil;
    private final NBTCore nbt;
    private final NmsItem item;

    public NmsImplementationsImpl() {
        if (((CraftMagicNumbers) CraftMagicNumbers.INSTANCE).getMappingsVersion().equals("7b9de0da1357e5b251eddde9aa762916")) {
            var nmsMc1_19_0 = new com.songoda.core.nms.v1_19_0.NmsImplementationsImpl();

            this.entity = nmsMc1_19_0.getEntity();
            this.player = nmsMc1_19_0.getPlayer();
            this.world = nmsMc1_19_0.getWorld();
            this.worldBorder = nmsMc1_19_0.getWorldBorder();
            this.anvil = nmsMc1_19_0.getAnvil();
            this.nbt = nmsMc1_19_0.getNbt();
            this.item = nmsMc1_19_0.getItem();

            return;
        }

        this.entity = new NmsEntityImpl();
        this.player = new NMSPlayerImpl();
        this.world = new WorldCoreImpl();
        this.worldBorder = new NmsWorldBorderImpl();
        this.anvil = new AnvilCore();
        this.nbt = new NBTCoreImpl();
        this.item = new NmsItemImpl();
    }

    @Override
    public @NotNull NmsEntity getEntity() {
        return this.entity;
    }

    @Override
    public @NotNull NMSPlayer getPlayer() {
        return this.player;
    }

    @Override
    public @NotNull WorldCore getWorld() {
        return this.world;
    }

    @Override
    public @NotNull NmsWorldBorder getWorldBorder() {
        return this.worldBorder;
    }

    @Override
    public @NotNull com.songoda.core.nms.anvil.AnvilCore getAnvil() {
        return this.anvil;
    }

    @Override
    public @NotNull NBTCore getNbt() {
        return this.nbt;
    }

    @Override
    public @NotNull NmsItem getItem() {
        return this.item;
    }
}
