package com.craftaro.core.compatibility;

import org.apache.commons.lang3.ArrayUtils;
import org.bukkit.Bukkit;

public enum ServerProject {
    UNKNOWN, CRAFTBUKKIT, SPIGOT, PAPER, FOLIA, TACO, GLOWSTONE, MOCK_BUKKIT;
    private static final ServerProject serverProject = checkProject();

    private static ServerProject checkProject() {
        String serverPath = Bukkit.getServer().getClass().getName();

        if (serverPath.equals("be.seeseemelk.mockbukkit.ServerMock")) {
            return MOCK_BUKKIT;
        }

        if (serverPath.contains("glowstone")) {
            return GLOWSTONE;
        }

        // taco is pretty easy to check. it uses paper stuff, though, so should be checked first
        try {
            Class.forName("net.techcable.tacospigot.TacoSpigotConfig");
            return TACO;
        } catch (ClassNotFoundException ignore) {
        }

        // paper used to be called "paperclip"
        try {
            Class.forName("com.destroystokyo.paperclip.Paperclip");
            return PAPER;
        } catch (ClassNotFoundException ignore) {
        }

        try {
            Class.forName("com.destroystokyo.paper.PaperConfig");
            return PAPER;
        } catch (ClassNotFoundException ignore) {
        }

        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            return FOLIA;
        } catch (ClassNotFoundException ignored) {
        }

        // spigot is the fork that pretty much all builds are based on anymore
        try {
            Class.forName("org.spigotmc.SpigotConfig");
            return SPIGOT;
        } catch (ClassNotFoundException ignore) {
        }

        return serverPath.contains("craftbukkit") ? CRAFTBUKKIT : UNKNOWN;
    }

    public static ServerProject getServerVersion() {
        return serverProject;
    }

    public static boolean isServer(ServerProject version) {
        return serverProject == version;
    }

    public static boolean isServer(ServerProject... versions) {
        return ArrayUtils.contains(versions, serverProject);
    }
}
