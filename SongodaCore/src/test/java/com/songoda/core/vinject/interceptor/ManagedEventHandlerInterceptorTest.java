package com.songoda.core.vinject.interceptor;

import org.bukkit.event.Event;
import org.bukkit.event.EventException;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.EventExecutor;
import org.junit.Test;

import java.lang.reflect.Method;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class ManagedEventHandlerInterceptorTest {

    @Test
    public void discoversAndInvokesPrivateEventHandler() throws EventException {
        TestComponent component = new TestComponent();
        List<Method> methods = ManagedEventHandlerInterceptor.findEventHandlerMethods(TestComponent.class);

        assertEquals(1, methods.size());

        EventExecutor executor = ManagedEventHandlerInterceptor.createExecutor(component, methods.get(0));
        executor.execute(null, new TestEvent());

        assertEquals(1, component.invocations);
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsHandlerWithoutEventParameter() {
        ManagedEventHandlerInterceptor.findEventHandlerMethods(InvalidComponent.class);
    }

    private static final class TestComponent {

        private int invocations;

        @EventHandler
        private void onTest(TestEvent event) {
            invocations++;
        }
    }

    private static final class InvalidComponent {

        @EventHandler
        private void onTest(String value) {
        }
    }

    private static final class TestEvent extends Event {

        private static final HandlerList HANDLERS = new HandlerList();

        public static HandlerList getHandlerList() {
            return HANDLERS;
        }

        @Override
        public HandlerList getHandlers() {
            return HANDLERS;
        }
    }
}
