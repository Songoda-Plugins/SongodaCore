package com.songoda.core.data;

import com.songoda.core.utils.ItemSerializer;
import org.bukkit.inventory.ItemStack;

import java.time.Instant;
import java.util.UUID;

public class StoredData {
    private Object object;

    public StoredData(Object object) {
        this.object = object;
    }

    public Object getObject() {
        return this.object;
    }

    public String asString() {
        if (this.object == null) {
            return null;
        }
        return this.object.toString();
    }

    public int asInt() {
        if (this.object == null) {
            return 0;
        }
        return Integer.parseInt(asString());
    }

    public double asDouble() {
        return Double.parseDouble(asString());
    }

    public long asLong() {
        String string = asString();
        if (string == null) {
            return 0;
        }
        return Long.parseLong(string);
    }

    public float asFloat() {
        return Float.parseFloat(asString());
    }

    public Instant asInstant() {
        return Instant.ofEpochMilli(asLong());
    }

    public boolean asBoolean() {
        if (this.object instanceof Integer) {
            return (int) this.object == 1;
        }
        return Boolean.parseBoolean(asString());
    }

    public void swap(Object object) {
        this.object = object;
    }

    public boolean isNull() {
        return this.object == null;
    }

    // get longblob
    public byte[] asBytes() {
        return (byte[]) this.object;
    }

    public UUID asUniqueID() {
        return UUID.fromString(asString());
    }

    public ItemStack asItemStack() {
        return ItemSerializer.deserializeItem(asBytes());
    }
}
