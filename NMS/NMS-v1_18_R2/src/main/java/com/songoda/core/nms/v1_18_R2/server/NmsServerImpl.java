package com.songoda.core.nms.v1_18_R2.server;

import com.songoda.core.nms.server.NmsServer;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v1_18_R2.CraftServer;

public class NmsServerImpl implements NmsServer {
    @Override
    public double[] getRecentTps() {
        return ((CraftServer) Bukkit.getServer()).getServer().recentTps;
    }
}
