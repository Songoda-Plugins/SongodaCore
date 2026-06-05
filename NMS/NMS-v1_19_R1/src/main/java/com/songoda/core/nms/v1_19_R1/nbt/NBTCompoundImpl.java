package com.songoda.core.nms.v1_19_R1.nbt;

import com.songoda.core.nms.nbt.NBTCompound;
import com.songoda.core.nms.nbt.NBTObject;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Set;
import java.util.UUID;

public class NBTCompoundImpl implements NBTCompound {
    protected CompoundTag compound;

    protected NBTCompoundImpl(CompoundTag compound) {
        this.compound = compound;
    }

    public NBTCompoundImpl() {
        this.compound = new CompoundTag();
    }

    @Override
    public NBTCompound set(String tag, String s) {
        compound.putString(tag, s);
        return this;
    }

    @Override
    public NBTCompound set(String tag, boolean b) {
        compound.putBoolean(tag, b);
        return this;
    }

    @Override
    public NBTCompound set(String tag, int i) {
        compound.putInt(tag, i);
        return this;
    }

    @Override
    public NBTCompound set(String tag, double i) {
        compound.putDouble(tag, i);
        return this;
    }

    @Override
    public NBTCompound set(String tag, long l) {
        compound.putLong(tag, l);
        return this;
    }

    @Override
    public NBTCompound set(String tag, short s) {
        compound.putShort(tag, s);
        return this;
    }

    @Override
    public NBTCompound set(String tag, byte b) {
        compound.putByte(tag, b);
        return this;
    }

    @Override
    public NBTCompound set(String tag, int[] i) {
        compound.putIntArray(tag, i);
        return this;
    }

    @Override
    public NBTCompound set(String tag, byte[] b) {
        compound.putByteArray(tag, b);
        return this;
    }

    @Override
    public NBTCompound set(String tag, UUID u) {
        compound.putUUID(tag, u);
        return this;
    }

    @Override
    public NBTCompound remove(String tag) {
        compound.remove(tag);
        return this;
    }

    @Override
    public boolean has(String tag) {
        return compound.contains(tag);
    }

    @Override
    public NBTObject getNBTObject(String tag) {
        return new NBTObjectImpl(compound, tag);
    }

    @Override
    public String getString(String tag) {
        return getNBTObject(tag).asString();
    }

    @Override
    public boolean getBoolean(String tag) {
        return getNBTObject(tag).asBoolean();
    }

    @Override
    public int getInt(String tag) {
        return getNBTObject(tag).asInt();
    }

    @Override
    public double getDouble(String tag) {
        return getNBTObject(tag).asDouble();
    }

    @Override
    public long getLong(String tag) {
        return getNBTObject(tag).asLong();
    }

    @Override
    public short getShort(String tag) {
        return getNBTObject(tag).asShort();
    }

    @Override
    public byte getByte(String tag) {
        return getNBTObject(tag).asByte();
    }

    @Override
    public int[] getIntArray(String tag) {
        return getNBTObject(tag).asIntArray();
    }

    @Override
    public byte[] getByteArray(String tag) {
        return getNBTObject(tag).asByteArray();
    }

    @Override
    public NBTCompound getCompound(String tag) {
        if (has(tag)) {
            return getNBTObject(tag).asCompound();
        }

        CompoundTag newCompound = new CompoundTag();
        compound.put(tag, newCompound);
        return new NBTCompoundImpl(newCompound);
    }

    @Override
    public Set<String> getKeys() {
        return compound.getAllKeys();
    }

    @Override
    public Set<String> getKeys(String tag) {
        return compound.getCompound(tag).getAllKeys();
    }

    @Override
    public byte[] serialize(String... exclusions) {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
             ObjectOutputStream dataOutput = new ObjectOutputStream(outputStream)) {
            addExtras();
            CompoundTag compound = this.compound.copy();

            for (String exclusion : exclusions) {
                compound.remove(exclusion);
            }

            NbtIo.writeCompressed(compound, dataOutput);

            return outputStream.toByteArray();
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return null;
    }

    @Override
    public void deSerialize(byte[] serialized) {
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(serialized);
             ObjectInputStream dataInput = new ObjectInputStream(inputStream)) {
            compound = NbtIo.readCompressed(dataInput);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public void addExtras() {
        // None
    }

    @Override
    public String toString() {
        return compound.toString();
    }
}
