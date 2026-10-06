package com.songoda.core.compatibility.entity;

import lombok.Getter;
import net.vortexdevelopment.vinject.annotation.component.Component;
import net.vortexdevelopment.vinject.annotation.lifecycle.PostConstruct;
import com.songoda.core.compatibility.VersionDependent;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class EntityUtils {

    @Getter
    private static EntityUtils INSTANCE;
    private final Map<CompatibleEntity, VersionDependent> COMPATIBLE_ENTITIES = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        if (INSTANCE != null) {
            throw new IllegalStateException("EntityUtils instance already exists!");
        }
        INSTANCE = this;
    }
}
