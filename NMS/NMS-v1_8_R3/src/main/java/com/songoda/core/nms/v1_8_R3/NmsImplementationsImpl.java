package com.songoda.core.nms.v1_8_R3;

import com.songoda.core.nms.NmsImplementations;
import com.songoda.core.nms.anvil.AnvilCore;
import com.songoda.core.nms.entity.NMSPlayer;
import com.songoda.core.nms.entity.NmsEntity;
import com.songoda.core.nms.item.NmsItem;
import com.songoda.core.nms.nbt.NBTCore;
import com.songoda.core.nms.v1_8_R3.entity.NMSPlayerImpl;
import com.songoda.core.nms.v1_8_R3.entity.NmsEntityImpl;
import com.songoda.core.nms.v1_8_R3.item.NmsItemImpl;
import com.songoda.core.nms.v1_8_R3.nbt.NBTCoreImpl;
import com.songoda.core.nms.v1_8_R3.world.NmsWorldBorderImpl;
import com.songoda.core.nms.v1_8_R3.world.WorldCoreImpl;
import com.songoda.core.nms.world.NmsWorldBorder;
import com.songoda.core.nms.world.WorldCore;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("unused")
public class NmsImplementationsImpl implements NmsImplementations {
    private final NmsEntity entity;
    private final NMSPlayer player;
    private final WorldCore world;
    private final NmsWorldBorder worldBorder;
    private final AnvilCore anvil;
    private final NBTCore nbt;
    private final NmsItem item;

    public NmsImplementationsImpl() {
        this.entity = new NmsEntityImpl();
        this.player = new NMSPlayerImpl();
        this.world = new WorldCoreImpl();
        this.worldBorder = new NmsWorldBorderImpl();
        this.anvil = new com.songoda.core.nms.v1_8_R3.anvil.AnvilCore();
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
    public @NotNull AnvilCore getAnvil() {
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
