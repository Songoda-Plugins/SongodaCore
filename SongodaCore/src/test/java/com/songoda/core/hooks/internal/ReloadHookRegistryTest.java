package com.songoda.core.hooks.internal;

import com.songoda.core.vinject.annotation.RegisterReloadHook;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class ReloadHookRegistryTest {

    @Test
    public void reloadsConfigurationsBeforeServicesRegardlessOfPriority() {
        ReloadHookRegistry registry = new ReloadHookRegistry();
        ReloadHook service = new HighPriorityService(new ArrayList<>());
        ConfigReloadHook config = new ConfigReloadHook(LowPriorityConfig.class);
        registry.register(service);
        registry.register(config);
        assertEquals(List.of(config, service), registry.orderedHooks());
    }

    @Test
    public void usesPriorityThenClassNameAndDeduplicatesRegisteredServices() {
        List<String> calls = new ArrayList<>();
        ReloadHookRegistry registry = new ReloadHookRegistry();
        ReloadHook beta = new BetaService(calls);
        registry.register(beta);
        registry.register(new AlphaService(calls));
        registry.register(new HighPriorityService(calls));
        registry.register(beta);
        registry.reload();
        assertEquals(List.of("high", "alpha", "beta"), calls);
    }

    @RegisterReloadHook(priority = -100)
    private static class LowPriorityConfig {
    }

    @RegisterReloadHook(priority = 100)
    private record HighPriorityService(List<String> calls) implements ReloadHook {
        public void onReload() {
            calls.add("high");
        }
    }

    private record AlphaService(List<String> calls) implements ReloadHook {
        public void onReload() {
            calls.add("alpha");
        }
    }

    private record BetaService(List<String> calls) implements ReloadHook {
        public void onReload() {
            calls.add("beta");
        }
    }
}
