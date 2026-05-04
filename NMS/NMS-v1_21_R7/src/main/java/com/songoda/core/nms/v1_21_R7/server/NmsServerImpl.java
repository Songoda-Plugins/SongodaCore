package com.songoda.core.nms.v1_21_R7.server;

import com.songoda.core.nms.server.NmsServer;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v1_21_R7.CraftServer;

import java.lang.reflect.Method;

public class NmsServerImpl implements NmsServer {
    @Override
    public double[] getRecentTps() {
        try {
            return ((CraftServer) Bukkit.getServer()).getServer().recentTps;
        } catch (Throwable e) {
            // Paper removed recentTps field, try Bukkit.getServer().getTPS()
            try {
                Object server = Bukkit.getServer();
                Method getTpsMethod = server.getClass().getMethod("getTPS");
                return (double[]) getTpsMethod.invoke(server);
            } catch (Throwable ex) {
                throw new RuntimeException("Unable to get recent TPS", ex);
            }
        }
    }
}
