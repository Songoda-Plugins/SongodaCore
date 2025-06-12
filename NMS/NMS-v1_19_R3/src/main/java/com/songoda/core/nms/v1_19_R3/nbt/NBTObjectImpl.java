package com.songoda.core.nms.v1_19_R3.nbt;

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
        return this.compound.getString(this.tag);
    }

    @Override
    public boolean asBoolean() {
        return this.compound.getBoolean(this.tag);
    }

    @Override
    public int asInt() {
        return this.compound.getInt(this.tag);
    }

    @Override
    public double asDouble() {
        return this.compound.getDouble(this.tag);
    }

    @Override
    public long asLong() {
        return this.compound.getLong(this.tag);
    }

    @Override
    public short asShort() {
        return this.compound.getShort(this.tag);
    }

    @Override
    public byte asByte() {
        return this.compound.getByte(this.tag);
    }

    @Override
    public int[] asIntArray() {
        return this.compound.getIntArray(this.tag);
    }

    @Override
    public byte[] asByteArray() {
        return this.compound.getByteArray(this.tag);
    }

    @Override
    public NBTCompound asCompound() {
        return new NBTCompoundImpl(this.compound.getCompound(this.tag));
    }

    @Override
    public Set<String> getKeys() {
        return this.compound.getAllKeys();
    }
}
