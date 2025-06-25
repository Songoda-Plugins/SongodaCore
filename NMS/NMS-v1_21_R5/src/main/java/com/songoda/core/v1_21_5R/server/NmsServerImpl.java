package com.songoda.core.v1_21_5R.server;

import com.songoda.core.nms.server.NmsServer;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v1_21_R5.CraftServer;

public class NmsServerImpl implements NmsServer {
    @Override
    public double[] getRecentTps() {
        return ((CraftServer) Bukkit.getServer()).getServer().recentTps;
    }
}
