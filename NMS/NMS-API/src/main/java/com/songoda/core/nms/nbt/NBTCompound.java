package com.songoda.core.nms.nbt;

import java.util.Set;
import java.util.UUID;

public interface NBTCompound {
    NBTCompound set(String tag, String s);

    NBTCompound set(String tag, boolean b);

    NBTCompound set(String tag, int i);

    NBTCompound set(String tag, double i);

    NBTCompound set(String tag, long l);

    NBTCompound set(String tag, short s);

    NBTCompound set(String tag, byte b);

    NBTCompound set(String tag, int[] i);

    NBTCompound set(String tag, byte[] b);

    NBTCompound set(String tag, UUID u);

    NBTCompound remove(String tag);

    boolean has(String tag);

    NBTObject getNBTObject(String tag);

    String getString(String tag);

    boolean getBoolean(String tag);

    int getInt(String tag);

    double getDouble(String tag);

    long getLong(String tag);

    short getShort(String tag);

    byte getByte(String tag);

    int[] getIntArray(String tag);

    byte[] getByteArray(String tag);

    NBTCompound getCompound(String tag);

    Set<String> getKeys();

    Set<String> getKeys(String tag);

    byte[] serialize(String... exclusions);

    void deSerialize(byte[] serialized);

    void addExtras();
}
