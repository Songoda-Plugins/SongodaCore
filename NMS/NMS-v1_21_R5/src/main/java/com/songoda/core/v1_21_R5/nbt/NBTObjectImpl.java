package com.songoda.core.v1_21_R5.nbt;

import com.songoda.core.nms.nbt.NBTCompound;
import com.songoda.core.nms.nbt.NBTObject;
import net.minecraft.nbt.CompoundTag;

import java.util.Set;

public class NBTObjectImpl implements NBTObject {
    private final CompoundTag compound;
    private final String tag;

    public NBTObjectImpl(CompoundTag compound, String tag) {
        this.compound = compound;
        this.tag = tag;
    }

    @Override
    public String asString() {
        return this.compound.getString(this.tag).get();
    }

    @Override
    public boolean asBoolean() {
        return this.compound.getBoolean(this.tag).get();
    }

    @Override
    public int asInt() {
        return this.compound.getInt(this.tag).get();
    }

    @Override
    public double asDouble() {
        return this.compound.getDouble(this.tag).get();
    }

    @Override
    public long asLong() {
        return this.compound.getLong(this.tag).get();
    }

    @Override
    public short asShort() {
        return this.compound.getShort(this.tag).get();
    }

    @Override
    public byte asByte() {
        return this.compound.getByte(this.tag).get();
    }

    @Override
    public int[] asIntArray() {
        return this.compound.getIntArray(this.tag).get();
    }

    @Override
    public byte[] asByteArray() {
        return this.compound.getByteArray(this.tag).get();
    }

    @Override
    public NBTCompound asCompound() {
        return new NBTCompoundImpl(this.compound.getCompound(this.tag).get());
    }

    @Override
    public Set<String> getKeys() {
        return this.compound.keySet();
    }
}
