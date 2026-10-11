package com.songoda.core.hooks.plugin.stacker;

import com.songoda.core.hooks.types.StackerHook;
import net.vortexdevelopment.vinject.annotation.component.Element;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigInteger;

/**
 * SongodaCore's reflection-based integration with UltimateStacker.
 *
 * <p>Reflection keeps SongodaCore independent of the optional plugin's API artifact.
 * Calls are made through UltimateStacker's public API entrypoint and manager
 * interfaces, with its public block creation method used where that operation is not
 * yet exposed by the API interface.</p>
 */
@Element
public final class UltimateStackerHook extends StackerHook {

    private Object itemManager;
    private Object spawnerManager;
    private Object blockManager;
    private Object entityManager;

    @Override
    public @NotNull BigInteger getRealAmount(@NotNull Item item) {
        Object stackedItem = invoke(itemManager(), "getStackedItem", item);
        if (stackedItem == null) {
            return BigInteger.valueOf(item.getItemStack().getAmount());
        }
        return amountOf(stackedItem);
    }

    @Override
    public void createStackedItem(@NotNull Item item, @NotNull BigInteger amount) {
        invoke(itemManager(), "createStackedItem", item, amount);
    }

    @Override
    public void createStackedItem(@NotNull Location location, @NotNull ItemStack itemStack, @NotNull BigInteger amount) {
        Object result = invoke(itemManager(), "createStackedItem", itemStack, location, amount);
        if (result == null) {
            throw new IllegalStateException("UltimateStacker could not create the stacked item");
        }
    }

    @Override
    public boolean canStack(@NotNull EntityType entityType) {
        if (entityManager == null) {
            return false;
        }
        try {
            return Boolean.TRUE.equals(invoke(entityManager(), "isEntityTypeEnabledForStacking", entityType));
        } catch (RuntimeException exception) {
            return false;
        }
    }

    @Override
    public boolean canStack(@NotNull LivingEntity entity) {
        if (entityManager == null) {
            return false;
        }
        try {
            return Boolean.TRUE.equals(invoke(entityManager(), "canStack", entity));
        } catch (RuntimeException exception) {
            return false;
        }
    }

    @Override
    public @NotNull BigInteger getEntityStackAmount(@NotNull LivingEntity entity) {
        if (entityManager == null) {
            return BigInteger.ONE;
        }
        try {
            Object stack = invoke(entityManager(), "getStackedEntity", entity);
            return stack == null ? BigInteger.ONE : amountOf(stack);
        } catch (RuntimeException exception) {
            return BigInteger.ONE;
        }
    }

    @Override
    public void createStackedEntity(@NotNull LivingEntity entity, @NotNull BigInteger amount) {
        if (!canStack(entity)) {
            throw new IllegalStateException("UltimateStacker cannot stack this entity");
        }
        invoke(entityManager(), "createStack", entity, amount);
    }

    @Override
    public @Nullable BigInteger getSpawnerStackAmount(@NotNull Location location) {
        Object stack = invoke(spawnerManager(), "getStackedSpawner", location);
        return stack == null ? null : amountOf(stack);
    }

    @Override
    public void createStackedSpawner(@NotNull BlockPlaceEvent event) {
        invoke(spawnerManager(), "createStackedSpawner", event);
    }

    @Override
    public @Nullable BigInteger getBlockStackAmount(@NotNull Location location) {
        Object stack = invoke(blockManager(), "getStackedBlock", location);
        return stack == null ? null : amountOf(stack);
    }

    @Override
    public void createStackedBlock(@NotNull Location location, @NotNull Material material, @NotNull BigInteger amount) {
        invoke(blockManager(), "createStack", location, material, amount);
    }

    @Override
    public @NotNull String getRequiredPlugin() {
        return "UltimateStacker";
    }

    @Override
    public void onEnable() {
        if (!canEnable()) {
            clearManagers();
            return;
        }

        try {
            ClassLoader pluginClassLoader = Bukkit.getPluginManager().getPlugin(getRequiredPlugin()).getClass().getClassLoader();
            Class<?> apiClass = Class.forName("com.songoda.ultimatestacker.api.UltimateStackerApi", true, pluginClassLoader);
            this.itemManager = invokeStatic(apiClass, "getItemManager");
            this.spawnerManager = invokeStatic(apiClass, "getSpawnerManager");
            this.blockManager = invokeStatic(apiClass, "getBlockManager");
            this.entityManager = invokeStatic(apiClass, "getEntityManager");
        } catch (ReflectiveOperationException | RuntimeException exception) {
            clearManagers();
            Bukkit.getLogger().warning("[SongodaCore] Could not connect to the UltimateStacker API: " + exception.getMessage());
        }
    }

    @Override
    public void onDisable() {
        clearManagers();
    }

    private void clearManagers() {
        this.itemManager = null;
        this.spawnerManager = null;
        this.blockManager = null;
        this.entityManager = null;
    }

    private Object itemManager() {
        return requireManager(itemManager, "item");
    }

    private Object spawnerManager() {
        return requireManager(spawnerManager, "spawner");
    }

    private Object blockManager() {
        return requireManager(blockManager, "block");
    }

    private Object entityManager() {
        return requireManager(entityManager, "entity");
    }

    private Object requireManager(Object manager, String type) {
        if (manager == null) {
            throw new IllegalStateException("UltimateStacker " + type + " manager is unavailable");
        }
        return manager;
    }

    private static Object invokeStatic(Class<?> type, String methodName) throws ReflectiveOperationException {
        return findMethod(type, methodName).invoke(null);
    }

    private static Object invoke(Object target, String methodName, Object... arguments) {
        try {
            return findMethod(target.getClass(), methodName, arguments).invoke(target, arguments);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Could not call UltimateStacker API method " + methodName, unwrap(exception));
        }
    }

    private static Method findMethod(Class<?> type, String methodName, Object... arguments) throws NoSuchMethodException {
        for (Method method : type.getMethods()) {
            if (!method.getName().equals(methodName) || method.getParameterCount() != arguments.length) {
                continue;
            }

            Class<?>[] parameterTypes = method.getParameterTypes();
            boolean matches = true;
            for (int index = 0; index < parameterTypes.length; index++) {
                if (arguments[index] != null && !parameterTypes[index].isInstance(arguments[index])) {
                    matches = false;
                    break;
                }
            }
            if (matches) {
                return method;
            }
        }
        throw new NoSuchMethodException(type.getName() + "." + methodName);
    }

    private static BigInteger amountOf(Object stack) {
        Object amount = invoke(stack, "getAmount");
        if (amount instanceof BigInteger bigInteger) {
            return bigInteger;
        }
        if (amount instanceof Number number) {
            return BigInteger.valueOf(number.longValue());
        }
        throw new IllegalStateException("UltimateStacker returned an unsupported stack amount type");
    }

    private static Throwable unwrap(ReflectiveOperationException exception) {
        if (exception instanceof InvocationTargetException invocation && invocation.getCause() != null) {
            return invocation.getCause();
        }
        return exception;
    }
}
