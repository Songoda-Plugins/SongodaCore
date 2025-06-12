package com.songoda.core.world;

import com.songoda.core.compatibility.CompatibleHand;
import com.songoda.core.compatibility.ServerVersion;
import com.songoda.core.nms.Nms;
import com.cryptomorin.xseries.XSound;
import de.tr7zw.changeme.nbtapi.NBT;
import org.bukkit.Bukkit;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerItemBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;

public class SItemStack {
    protected final com.songoda.core.nms.world.SItemStack sItem;
    protected final ItemStack item;

    public SItemStack(ItemStack item) {
        this.item = item;
        this.sItem = Nms.getImplementations().getWorld().getItemStack(item);
    }

    public SItemStack(CompatibleHand hand, Player player) {
        this.item = hand.getItem(player);
        this.sItem = Nms.getImplementations().getWorld().getItemStack(this.item);
    }

    public ItemStack addDamage(Player player, int damage) {
        return addDamage(player, damage, false);
    }

    /**
     * Damage the selected item
     *
     * @param player the player whose item you want to damage
     * @param damage the amount of damage to apply to the item
     */
    public ItemStack addDamage(Player player, int damage, boolean respectVanillaUnbreakingEnchantments) {
        if (this.item == null) {
            return null;
        }

        if (this.item.getItemMeta() == null) {
            return this.item;
        }

        int maxDurability = this.item.getType().getMaxDurability();
        int durability;

        if (ServerVersion.isServerVersionBelow(ServerVersion.V1_11)
                ? NBT.readNbt(this.item).hasTag("Unbreakable")
                : this.item.getItemMeta().isUnbreakable()) {
            return this.item;
        } else if (ServerVersion.isServerVersionAtLeast(ServerVersion.V1_13)) {
            // ItemStack.setDurability(short) still works in 1.13-1.14, but use these methods now
            ItemMeta meta = this.item.getItemMeta();
            if (meta instanceof Damageable) {
                Damageable damageable = ((Damageable) meta);

                if (respectVanillaUnbreakingEnchantments) {
                    damage = shouldApplyDamage(meta.getEnchantLevel(Enchantment.DURABILITY), damage);
                }

                damageable.setDamage(((Damageable) meta).getDamage() + damage);
                this.item.setItemMeta(meta);
                durability = damageable.getDamage();
            } else {
                return this.item;
            }
        } else {
            if (respectVanillaUnbreakingEnchantments) {
                damage = shouldApplyDamage(this.item.getEnchantmentLevel(Enchantment.DURABILITY), damage);
            }

            this.item.setDurability((short) Math.max(0, this.item.getDurability() + damage));
            durability = this.item.getDurability();
        }

        if (durability >= maxDurability && player != null) {
            destroy(player);
        }

        return this.item;
    }

    public void destroy(Player player) {
        destroy(player, 1);
    }

    public void destroy(Player player, int amount) {
        PlayerItemBreakEvent breakEvent = new PlayerItemBreakEvent(player, this.item);
        Bukkit.getServer().getPluginManager().callEvent(breakEvent);

        this.sItem.breakItem(player, amount);
        XSound.ENTITY_ITEM_BREAK.play(player);
    }

    public ItemStack getItem() {
        return this.item;
    }

    private static int shouldApplyDamage(int unbreakingEnchantLevel, int damageAmount) {
        int result = 0;

        for (int i = 0; i < damageAmount; ++i) {
            if (shouldApplyDamage(unbreakingEnchantLevel)) {
                ++result;
            }
        }

        return result;
    }

    private static boolean shouldApplyDamage(int unbreakingEnchantLevel) {
        if (unbreakingEnchantLevel <= 0) {
            return true;
        }

        return Math.random() <= 1.0 / (unbreakingEnchantLevel + 1);
    }
}
