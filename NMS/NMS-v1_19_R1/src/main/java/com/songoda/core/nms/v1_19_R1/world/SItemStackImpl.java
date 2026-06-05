package com.songoda.core.nms.v1_19_R1.world;

import com.songoda.core.nms.world.SItemStack;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import org.bukkit.craftbukkit.v1_19_R1.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_19_R1.inventory.CraftItemStack;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class SItemStackImpl implements SItemStack {
    private final ItemStack item;

    public SItemStackImpl(ItemStack item) {
        this.item = item;
    }

    @Override
    public void breakItem(Player player, int amount) {
        ServerPlayer entityPlayer = ((CraftPlayer) player).getHandle();

        for (int i = 0; i < amount; ++i) {
            Vec3 vec3d = new Vec3(((double) random.nextFloat() - 0.5D) * 0.1D, Math.random() * 0.1D + 0.1D, 0.0D);
            vec3d = vec3d.xRot(-entityPlayer.getXRot() * 0.017453292F);
            vec3d = vec3d.yRot(-entityPlayer.getYRot() * 0.017453292F);

            double d0 = (double) (-random.nextFloat()) * 0.6D - 0.3D;

            Vec3 vec3d1 = new Vec3(((double) random.nextFloat() - 0.5D) * 0.3D, d0, 0.6D);
            vec3d1 = vec3d1.xRot(-entityPlayer.getXRot() * 0.017453292F);
            vec3d1 = vec3d1.yRot(-entityPlayer.getYRot() * 0.017453292F);
            vec3d1 = vec3d1.add(entityPlayer.getX(), entityPlayer.getEyeY(), entityPlayer.getZ());

            entityPlayer.level.addParticle(new ItemParticleOption(ParticleTypes.ITEM, CraftItemStack.asNMSCopy(item)), vec3d1.x, vec3d1.y, vec3d1.z, vec3d.x, vec3d.y + 0.05D, vec3d.z);
        }
    }
}
