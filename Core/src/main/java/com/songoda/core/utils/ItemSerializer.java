package com.songoda.core.utils;

import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;
import org.yaml.snakeyaml.external.biz.base64Coder.Base64Coder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Class based off of https://gist.github.com/graywolf336/8153678
 *
 * @deprecated Will be moved into a more appropriate package and refactored.
 */
@Deprecated
public class ItemSerializer {
    /**
     * A method to serialize an {@link ItemStack} list to Base64 String.
     *
     * @param items to turn into a Base64 String.
     *
     * @return Base64 string of the items.
     */
    public static String toBase64(List<ItemStack> items) {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            BukkitObjectOutputStream dataOutput = new BukkitObjectOutputStream(outputStream);

            // Write the size of the inventory
            dataOutput.writeInt(items.size());

            // Save every element in the list
            for (ItemStack item : items) {
                dataOutput.writeObject(item);
            }

            // Serialize that array
            dataOutput.close();
            return Base64Coder.encodeLines(outputStream.toByteArray());
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return null;
    }

    /**
     * Gets a list of ItemStacks from Base64 string.
     *
     * @param data Base64 string to convert to ItemStack list.
     *
     * @return ItemStack array created from the Base64 string.
     */
    public static List<ItemStack> fromBase64(String data) {
        try {
            ByteArrayInputStream inputStream = new ByteArrayInputStream(Base64Coder.decodeLines(data));
            BukkitObjectInputStream dataInput = new BukkitObjectInputStream(inputStream);
            int length = dataInput.readInt();
            List<ItemStack> items = new ArrayList<>();

            // Read the serialized itemstack list
            for (int i = 0; i < length; ++i) {
                items.add((ItemStack) dataInput.readObject());
            }

            dataInput.close();

            return items;
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return null;
    }

    /**
     * Deserialize a byte array into an ItemStack.
     *
     * @param data Data to deserialize.
     *
     * @return Deserialized ItemStack.
     */
    public static ItemStack deserializeItem(byte[] data) {
        ItemStack item = null;

        try (BukkitObjectInputStream stream = new BukkitObjectInputStream(new ByteArrayInputStream(data))) {
            item = (ItemStack) stream.readObject();
        } catch (IOException | ClassNotFoundException ex) {
            ex.printStackTrace();
        }

        return item;
    }

    /**
     * Serialize an ItemStack into a byte array.
     *
     * @param item Item to serialize.
     *
     * @return Serialized data.
     */
    public static byte[] serializeItem(ItemStack item) {
        try (ByteArrayOutputStream stream = new ByteArrayOutputStream(); BukkitObjectOutputStream bukkitStream = new BukkitObjectOutputStream(stream)) {
            bukkitStream.writeObject(item);

            return stream.toByteArray();
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        return null;
    }
}
