package com.songoda.core.services;

import net.vortexdevelopment.vinject.annotation.Bean;
import net.vortexdevelopment.vinject.annotation.component.Service;
import com.songoda.core.SongodaCore;
import org.bukkit.plugin.Plugin;

@Service
public class PluginBeanService {

    @Bean
    public Plugin registerPluginBean() {
        return SongodaCore.getPlugin();
    }
}
