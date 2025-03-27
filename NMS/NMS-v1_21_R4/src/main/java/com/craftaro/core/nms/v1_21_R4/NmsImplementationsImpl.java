package com.craftaro.core.nms.v1_21_R4;

import com.craftaro.core.nms.NmsImplementations;
import com.craftaro.core.nms.entity.NMSPlayer;
import com.craftaro.core.nms.entity.NmsEntity;
import com.craftaro.core.nms.item.NmsItem;
import com.craftaro.core.nms.nbt.NBTCore;
import com.craftaro.core.nms.v1_21_R4.anvil.AnvilCore;
import com.craftaro.core.nms.v1_21_R4.entity.NMSPlayerImpl;
import com.craftaro.core.nms.v1_21_R4.entity.NmsEntityImpl;
import com.craftaro.core.nms.v1_21_R4.item.NmsItemImpl;
import com.craftaro.core.nms.v1_21_R4.nbt.NBTCoreImpl;
import com.craftaro.core.nms.v1_21_R4.world.NmsWorldBorderImpl;
import com.craftaro.core.nms.v1_21_R4.world.WorldCoreImpl;
import com.craftaro.core.nms.world.NmsWorldBorder;
import com.craftaro.core.nms.world.WorldCore;
import org.jetbrains.annotations.NotNull;

public class NmsImplementationsImpl implements NmsImplementations {
    private final NmsEntity entity;
    private final NMSPlayer player;
    private final WorldCore world;
    private final NmsWorldBorder worldBorder;
    private final com.craftaro.core.nms.anvil.AnvilCore anvil;
    private final NBTCore nbt;
    private final NmsItem item;

    public NmsImplementationsImpl() {
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
    public @NotNull com.craftaro.core.nms.anvil.AnvilCore getAnvil() {
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
