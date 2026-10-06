package com.songoda.core.vinject.handler;

import net.vortexdevelopment.vinject.annotation.component.Registry;
import net.vortexdevelopment.vinject.di.DependencyContainer;
import net.vortexdevelopment.vinject.di.DependencyRepository;
import net.vortexdevelopment.vinject.di.registry.AnnotationHandler;
import com.songoda.core.vinject.annotation.Api;
import org.jetbrains.annotations.Nullable;

/**
 * This class will handle Api load event emissions.
 */
@Registry(annotation = Api.class)
public class ApiHandler extends AnnotationHandler {

    @Override
    public void handle(Class<?> aClass, @Nullable Object instance, DependencyContainer dependencyContainer) {
        if (instance == null) {
            dependencyContainer.newInstance(aClass);
        }
        DependencyRepository.getInstance().getEventManager().emitEvent("songodacore.api.load");
    }
}
