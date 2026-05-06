package org.auto.lucky_block_server_mod;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.MinecraftServer;

public class server_init {
    public static MinecraftServer currentServer;

    public static void server_initer() {
        ServerLifecycleEvents.SERVER_STARTING.register(server -> {
            currentServer = server;
            System.out.println("[AntiXray] Server instance captured early!");
        });

        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            currentServer = null;
        });
    }
}
