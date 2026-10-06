package com.songoda.core.compatibility.entity.impl;

import com.songoda.core.compatibility.KnownServerVersions;
import com.songoda.core.compatibility.VersionDependent;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Frog;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CompatibleFrog extends VersionDependent {

    private final Frog frog;

    public CompatibleFrog(Entity entity) {
        super(KnownServerVersions.V1_19);
        if (entity instanceof Frog) {
            this.frog = (Frog) entity;
        } else {
            throw new IllegalArgumentException("Entity must be a Frog");
        }
    }

    @Nullable Entity getTongueTarget() {
        return frog.getTongueTarget();
    }

    public void setTongueTarget(@Nullable Entity target) {
        frog.setTongueTarget(target);
    }

    public CompatibleFrogVariant getVariant() {
        return CompatibleFrogVariant.fromApiVariant(frog.getVariant());
    }

    public void setVariant(@NotNull CompatibleFrogVariant variant) {
        frog.setVariant(CompatibleFrogVariant.toApiVariant(variant));
    }

    public static enum CompatibleFrogVariant {
        TEMPERATE("temperate"),
        WARM("warm"),
        COLD("cold");

        private final String name;

        CompatibleFrogVariant(String name) {
            this.name = name;
        }

        private static CompatibleFrogVariant fromApiVariant(Frog.Variant apiVariant) {
            for (CompatibleFrogVariant variant : values()) {
                if (variant.getName().equalsIgnoreCase(apiVariant.getKey().getKey())) {
                    return variant;
                }
            }
            throw new IllegalArgumentException("Unknown frog variant: " + apiVariant);
        }

        private static Frog.Variant toApiVariant(CompatibleFrogVariant variant) {
            return switch (variant) {
                case TEMPERATE -> Frog.Variant.TEMPERATE;
                case WARM -> Frog.Variant.WARM;
                case COLD -> Frog.Variant.COLD;
            };
        }

        public String getName() {
            return name;
        }
    }
}
