package com.songoda.core.nms.v1_19_R1.server;

import com.songoda.core.nms.server.NmsServer;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v1_19_R1.CraftServer;

public class NmsServerImpl implements NmsServer {
    @Override
    public double[] getRecentTps() {
        return ((CraftServer) Bukkit.getServer()).getServer().recentTps;
    }
}
