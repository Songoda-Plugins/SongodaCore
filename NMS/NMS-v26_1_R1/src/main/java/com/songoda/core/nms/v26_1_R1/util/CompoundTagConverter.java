package com.songoda.core.nms.v26_1_R1.util;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.ValueInput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
