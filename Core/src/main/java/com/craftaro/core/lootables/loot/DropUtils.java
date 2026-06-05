package com.craftaro.core.lootables.loot;

import com.craftaro.core.compatibility.folia.SchedulerRunnable;
import com.craftaro.core.compatibility.folia.SchedulerUtils;
import com.craftaro.core.SongodaCore;
import com.craftaro.ultimatestacker.api.UltimateStackerApi;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class DropUtils {
    public static void processStackedDrop(LivingEntity entity, List<Drop> drops, EntityDeathEvent event) {
        int xpToDrop = event.getDroppedExp();
        List<ItemStack> items = new ArrayList<>();
        List<String> commands = new ArrayList<>();
        List<Integer> xp = new ArrayList<>();

        for (Drop drop : drops) {
            if (drop == null) continue;

            ItemStack droppedItem = drop.getItemStack();
            if (droppedItem != null) {
                droppedItem = droppedItem.clone();
                boolean success = false;

                for (ItemStack item : items) {
                    if (item.getType() != droppedItem.getType()
                            || item.getDurability() != droppedItem.getDurability()
                            || item.getAmount() + droppedItem.getAmount() > droppedItem.getMaxStackSize()) continue;
                    item.setAmount(item.getAmount() + droppedItem.getAmount());
                    success = true;

                    break;
                }

                if (!success) {
                    items.add(droppedItem);
                }
            }

            if (drop.getCommand() != null) {
                commands.add(drop.getCommand());
            }

            if (drop.getXp() != 0) {
                xp.add(drop.getXp());
            }
        }

        event.getDrops().clear();

        if (!items.isEmpty()) {
            dropItems(items, event);
        } else if (!commands.isEmpty()) {
            runCommands(entity, commands);
        }

        for (int x : xp) {
            xpToDrop += x;
        }

        event.setDroppedExp(xpToDrop);
    }

    private static void dropItems(List<ItemStack> items, EntityDeathEvent event) {
        if (Bukkit.getPluginManager().isPluginEnabled("UltimateStacker")) {
            List<StackedItem> stacks = new ArrayList<>();
            int maxSize = UltimateStackerApi.getSettings().getMaxItemStackSize() - 64;
            for (ItemStack item : items) {
                StackedItem stack = stacks.stream().filter(stackedItem -> stackedItem.getItem().getType() == item.getType()).filter(stackedItem -> stackedItem.getAmount() < Integer.MAX_VALUE/2).findFirst().orElse(null);
                if (stack == null) {
                    stacks.add(new StackedItem(item, item.getAmount()));
                    continue;
                }
                BigDecimal newAmount = BigDecimal.valueOf(stack.getAmount() + item.getAmount());
                //newAmount > maxSize
                while (newAmount.compareTo(BigDecimal.valueOf(maxSize)) > 0) {
                    //newAmount -= maxSize;
                    newAmount = newAmount.subtract(BigDecimal.valueOf(maxSize));
                    stacks.add(new StackedItem(item, maxSize));
                }
                stack.setAmount(newAmount.intValue());
            }
            SchedulerUtils.runLocationTask(SongodaCore.getHijackedPlugin(), event.getEntity().getLocation(), new SchedulerRunnable() {
                @Override
                public void run() {
                    for (StackedItem stack : stacks) {
                        UltimateStackerApi.getStackedItemManager().createStack(stack.getItemToDrop(), event.getEntity().getLocation(), stack.getAmount());
                    }
                }
            });
            return;
        }
        event.getDrops().addAll(items);
    }

    private static void runCommands(LivingEntity entity, List<String> commands) {
        SchedulerUtils.runTask(SongodaCore.getHijackedPlugin(), new SchedulerRunnable() {
            @Override
            public void run() {
                for (String command : commands) {
                    if (entity.getKiller() != null) {
                        command = command.replace("%player%", entity.getKiller().getName()
                                .replace("%x%", String.valueOf((int) entity.getLocation().getX()))
                                .replace("%y%", String.valueOf((int) entity.getLocation().getY()))
                                .replace("%z%", String.valueOf((int) entity.getLocation().getZ())));
                    }

                    if (!command.contains("%player%")) {
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
                    }
                }
            }
        });
    }

    private static class StackedItem {
        private final ItemStack item;
        private int amount;

        public StackedItem(ItemStack item, int amount) {
            this.item = item;
            this.amount = amount;
        }

        public Material getMaterial() {
            return this.item.getType();
        }

        public ItemStack getItem() {
            return this.item;
        }

        public ItemStack getItemToDrop() {
            this.item.setAmount(32);
            return this.item;
        }

        public int getAmount() {
            return this.amount;
        }

        /**
         * @deprecated Use {@link #setAmount(int)} instead.
         */
        @Deprecated
        public void setamount(int amount) {
            this.amount = amount;
        }

        public void setAmount(int amount) {
            this.amount = amount;
        }
    }
}
