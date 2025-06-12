package com.songoda.core.nms.v1_17_R1.world;

import com.songoda.core.nms.world.SItemStack;
import net.minecraft.core.particles.ParticleParamItem;
import net.minecraft.core.particles.Particles;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.world.phys.Vec3D;
import org.bukkit.craftbukkit.v1_17_R1.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_17_R1.inventory.CraftItemStack;
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
            Vec3D vec3d = new Vec3D(((double) random.nextFloat() - 0.5D) * 0.1D, Math.random() * 0.1D + 0.1D, 0.0D);
            vec3d = vec3d.a(-entityPlayer.getXRot() * 0.017453292F);
            vec3d = vec3d.b(-entityPlayer.getYRot() * 0.017453292F);

            double d0 = (double) (-random.nextFloat()) * 0.6D - 0.3D;

            Vec3D vec3d1 = new Vec3D(((double) random.nextFloat() - 0.5D) * 0.3D, d0, 0.6D);
            vec3d1 = vec3d1.a(-entityPlayer.getXRot() * 0.017453292F);
            vec3d1 = vec3d1.b(-entityPlayer.getYRot() * 0.017453292F);
            vec3d1 = vec3d1.add(entityPlayer.locX(), entityPlayer.getHeadY(), entityPlayer.locZ());

            entityPlayer.t.addParticle(new ParticleParamItem(Particles.K, CraftItemStack.asNMSCopy(item)), vec3d1.b, vec3d1.c, vec3d1.d, vec3d.b, vec3d.c + 0.05D, vec3d.d);
        }
    }
}
