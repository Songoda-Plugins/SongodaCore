package com.songoda.core.nms.v1_13_R2.nbt;

import com.songoda.core.nms.nbt.NBTCompound;
import com.songoda.core.nms.nbt.NBTObject;
import net.minecraft.server.v1_13_R2.NBTCompressedStreamTools;
import net.minecraft.server.v1_13_R2.NBTTagCompound;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;

public class NBTCompoundImpl implements NBTCompound {
    protected NBTTagCompound compound;

    protected NBTCompoundImpl(NBTTagCompound compound) {
        this.compound = compound;
    }

    public NBTCompoundImpl() {
        this.compound = new NBTTagCompound();
    }

    @Override
    public NBTCompound set(String tag, String s) {
        compound.setString(tag, s);
        return this;
    }

    @Override
    public NBTCompound set(String tag, boolean b) {
        compound.setBoolean(tag, b);
        return this;
    }

    @Override
    public NBTCompound set(String tag, int i) {
        compound.setInt(tag, i);
        return this;
    }

    @Override
    public NBTCompound set(String tag, double i) {
        compound.setDouble(tag, i);
        return this;
    }

    @Override
    public NBTCompound set(String tag, long l) {
        compound.setLong(tag, l);
        return this;
    }

    @Override
    public NBTCompound set(String tag, short s) {
        compound.setShort(tag, s);
        return this;
    }

    @Override
    public NBTCompound set(String tag, byte b) {
        compound.setByte(tag, b);
        return this;
    }

    @Override
    public NBTCompound set(String tag, int[] i) {
        compound.setIntArray(tag, i);
        return this;
    }

    @Override
    public NBTCompound set(String tag, byte[] b) {
        compound.setByteArray(tag, b);
        return this;
    }

    @Override
    public NBTCompound set(String tag, UUID u) {
        compound.a(tag, u);
        return this;
    }

    @Override
    public NBTCompound remove(String tag) {
        compound.remove(tag);
        return this;
    }

    @Override
    public boolean has(String tag) {
        return compound.hasKey(tag);
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
        return new byte[0];
    }

    @Override
    public NBTCompound getCompound(String tag) {
        if (has(tag)) {
            return getNBTObject(tag).asCompound();
        }

        NBTTagCompound newCompound = new NBTTagCompound();
        compound.set(tag, newCompound);
        return new NBTCompoundImpl(newCompound);
    }

    @Override
    public Set<String> getKeys() {
        return compound.getKeys();
    }

    @Override
    public Set<String> getKeys(String tag) {
        return getNBTObject(tag).getKeys();
    }

    @Override
    public byte[] serialize(String... exclusions) {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
             ObjectOutputStream dataOutput = new ObjectOutputStream(outputStream)) {
            addExtras();
            NBTTagCompound compound = this.compound.clone();

            for (String exclusion : exclusions)
                compound.remove(exclusion);

            NBTCompressedStreamTools.a(compound, (OutputStream) dataOutput);

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
            compound = NBTCompressedStreamTools.a(dataInput);
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
