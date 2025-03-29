package com.songoda.core.hooks;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

public abstract class HookRegistry<T extends Hook> {
    public abstract Optional<T> getActive();

    public abstract void setActive(@Nullable T hook);

    public abstract @NotNull List<String> getAllNames();

    public abstract void register(@NotNull T hook);

    public abstract void unregister(@NotNull T hook);

    public abstract void clear();

    @ApiStatus.Internal
    public abstract @Nullable T get(String name);

    @ApiStatus.Internal
    public abstract @NotNull List<T> getAll();
}
