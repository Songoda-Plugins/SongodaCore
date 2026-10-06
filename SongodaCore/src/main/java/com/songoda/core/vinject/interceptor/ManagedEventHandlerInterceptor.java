package com.songoda.core.vinject.interceptor;

import net.vortexdevelopment.vinject.di.ComponentInterceptor;
import net.vortexdevelopment.vinject.di.DependencyContainer;
import com.songoda.core.vinject.annotation.RegisterListener;
import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.event.EventException;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.EventExecutor;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Registers Bukkit {@link EventHandler} methods declared directly on VInject-managed
 * instances, allowing services and managers to handle an occasional event without
 * implementing {@link Listener}.
 * <p>
 * Classes registered through {@link RegisterListener} remain on Bukkit's normal
 * listener path and are ignored by this interceptor to avoid duplicate invocations.
 */
public final class ManagedEventHandlerInterceptor implements ComponentInterceptor {

    private final List<PendingComponent> pendingComponents = new ArrayList<>();
    private Plugin activePlugin;

    private static void registerComponent(Plugin plugin, PendingComponent pendingComponent) {
        Listener listener = new ManagedComponentListener();
        for (Method method : pendingComponent.handlers()) {
            registerEventHandler(plugin, listener, pendingComponent.instance(), method);
        }
    }

    static List<Method> findEventHandlerMethods(Class<?> clazz) {
        return Arrays.stream(clazz.getDeclaredMethods())
                .filter(method -> method.isAnnotationPresent(EventHandler.class))
                .peek(ManagedEventHandlerInterceptor::validateEventHandler)
                .toList();
    }

    static EventExecutor createExecutor(Object instance, Method method) {
        method.setAccessible(true);
        Class<?> eventClass = method.getParameterTypes()[0];
        return (listener, event) -> {
            if (!eventClass.isInstance(event)) {
                return;
            }
            try {
                method.invoke(instance, event);
            } catch (InvocationTargetException exception) {
                Throwable cause = exception.getCause();
                throw new EventException(cause == null ? exception : cause);
            } catch (ReflectiveOperationException | RuntimeException exception) {
                throw new EventException(exception);
            }
        };
    }

    private static boolean usesRegisteredListenerPath(Class<?> clazz) {
        return Listener.class.isAssignableFrom(clazz);
    }

    private static void registerEventHandler(Plugin plugin, Listener listener, Object instance, Method method) {
        EventHandler annotation = method.getAnnotation(EventHandler.class);
        Class<? extends Event> eventType = method.getParameterTypes()[0].asSubclass(Event.class);

        Bukkit.getPluginManager().registerEvent(
                eventType,
                listener,
                annotation.priority(),
                createExecutor(instance, method),
                plugin,
                annotation.ignoreCancelled()
        );
    }

    private static void validateEventHandler(Method method) {
        String location = method.getDeclaringClass().getName() + "#" + method.getName();

        if (Modifier.isStatic(method.getModifiers())) {
            throw new IllegalArgumentException("Managed @EventHandler method must not be static: " + location);
        }
        if (method.getReturnType() != void.class) {
            throw new IllegalArgumentException("Managed @EventHandler method must return void: " + location);
        }
        if (method.getParameterCount() != 1 || !Event.class.isAssignableFrom(method.getParameterTypes()[0])) {
            throw new IllegalArgumentException(
                    "Managed @EventHandler method must declare exactly one Bukkit Event parameter: " + location
            );
        }
    }

    @Override
    public synchronized void onComponentRegistered(Class<?> clazz, Object instance, DependencyContainer container) {
        if (usesRegisteredListenerPath(clazz)) {
            return;
        }

        List<Method> handlers = findEventHandlerMethods(clazz);
        if (handlers.isEmpty()) {
            return;
        }

        PendingComponent pendingComponent = new PendingComponent(instance, handlers);
        if (activePlugin == null) {
            pendingComponents.add(pendingComponent);
            return;
        }

        registerComponent(activePlugin, pendingComponent);
    }

    /**
     * Activates all event handlers collected while the dependency container was
     * constructing and injecting managed instances.
     *
     * @param plugin owning Bukkit plugin
     */
    public synchronized void activate(Plugin plugin) {
        if (activePlugin != null) {
            if (activePlugin != plugin) {
                throw new IllegalStateException("Managed event handlers are already active for another plugin");
            }
            return;
        }

        activePlugin = plugin;
        for (PendingComponent pendingComponent : pendingComponents) {
            registerComponent(plugin, pendingComponent);
        }
        pendingComponents.clear();
    }

    private static final class ManagedComponentListener implements Listener {
    }

    private record PendingComponent(Object instance, List<Method> handlers) {
    }
}
