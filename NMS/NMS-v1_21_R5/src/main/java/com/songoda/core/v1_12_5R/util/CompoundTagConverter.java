package com.songoda.core.v1_12_5R.util;

import com.mojang.serialization.DynamicOps;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.dedicated.DedicatedServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v1_21_R5.CraftServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

public class CompoundTagConverter {

    private static final ProblemReporter problemReporter = new ProblemReporter.ScopedCollector(LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME));

    public static ValueInput toInput(CompoundTag compoundTag, ServerLevel level) {
        return TagValueInput.create(
                problemReporter,
                level.registryAccess(),
                compoundTag
        );
    }
}
