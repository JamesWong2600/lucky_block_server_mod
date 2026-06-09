package org.auto.lucky_block_server_mod.flow;

import net.minecraft.server.MinecraftServer;

public class flow_controller {
    public static int flow = 1;

    public static void StartGameFlow() {
        flow = 2;
    }

    public static void BattleGameFlow() {
        flow = 3;
    }

    public static void EndGameFlow() {
        flow = 4;
    }

    //public static void EndGameFlow() {flow = 5;}

    public static int GetGameFlow() {
        return flow;
    }
}
