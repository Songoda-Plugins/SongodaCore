package com.songoda.core.nms.v1_16_R2.world;

import com.songoda.core.nms.world.SItemStack;
import net.minecraft.server.v1_16_R2.EntityPlayer;
import net.minecraft.server.v1_16_R2.ParticleParamItem;
import net.minecraft.server.v1_16_R2.Particles;
import net.minecraft.server.v1_16_R2.Vec3D;
import org.bukkit.craftbukkit.v1_16_R2.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_16_R2.inventory.CraftItemStack;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class SItemStackImpl implements SItemStack {
    private final ItemStack item;

    public SItemStackImpl(ItemStack item) {
        this.item = item;
    }

    @Override
    public void breakItem(Player player, int amount) {
        EntityPlayer entityPlayer = ((CraftPlayer) player).getHandle();

        for (int i = 0; i < amount; ++i) {
            Vec3D vec3d = new Vec3D(((double) this.random.nextFloat() - 0.5D) * 0.1D, Math.random() * 0.1D + 0.1D, 0.0D);
            vec3d = vec3d.a(-entityPlayer.pitch * 0.017453292F);
            vec3d = vec3d.b(-entityPlayer.yaw * 0.017453292F);

            double d0 = (double) (-this.random.nextFloat()) * 0.6D - 0.3D;

            Vec3D vec3d1 = new Vec3D(((double) this.random.nextFloat() - 0.5D) * 0.3D, d0, 0.6D);
            vec3d1 = vec3d1.a(-entityPlayer.pitch * 0.017453292F);
            vec3d1 = vec3d1.b(-entityPlayer.yaw * 0.017453292F);
            vec3d1 = vec3d1.add(entityPlayer.locX(), entityPlayer.getHeadY(), entityPlayer.locZ());

            entityPlayer.world.addParticle(new ParticleParamItem(Particles.ITEM, CraftItemStack.asNMSCopy(item)), vec3d1.x, vec3d1.y, vec3d1.z, vec3d.x, vec3d.y + 0.05D, vec3d.z);
        }
    }
}
