package com.songoda.core.nms.v1_21_R3.server;

import com.songoda.core.nms.server.NmsServer;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v1_21_R3.CraftServer;

public class NmsServerImpl implements NmsServer {
    @Override
    public double[] getRecentTps() {
        return ((CraftServer) Bukkit.getServer()).getServer().recentTps;
    }
}
