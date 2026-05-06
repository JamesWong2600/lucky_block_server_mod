package org.auto.lucky_block_server_mod.plyerdeadevent;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;
import net.minecraft.world.TeleportTarget;

import static org.auto.lucky_block_server_mod.Lucky_block_server_mod.LOBBY_WORLD_KEY;
import static org.auto.lucky_block_server_mod.flow.flow_controller.GetGameFlow;

public class playerdeadevent {
    private static final double LOBBY_X = 0.5;
    private static final double LOBBY_Y = 10.0;
    private static final double LOBBY_Z = 0.5;

    public static void death_to_specttor(){
    ServerLivingEntityEvents.AFTER_DEATH.register((entity, damageSource) -> {
        // 判斷死亡的是不是玩家
        if (entity instanceof ServerPlayerEntity player) {
            if(GetGameFlow() == 1 || GetGameFlow() == 5){
                ServerWorld world = (ServerWorld) entity.getWorld();
                if (world != null) {
                    TeleportTarget target = new TeleportTarget(
                            world,
                            new Vec3d(LOBBY_X, LOBBY_Y, LOBBY_Z),
                            Vec3d.ZERO, 0.0f, 90.0f, TeleportTarget.NO_OP

                    );
                    player.teleportTo(target);
               }
               return;
            }

            player.sendMessage(Text.literal("你已死亡，進入旁觀模式"), false);

            // 執行切換模式
            // 注意：在死亡瞬間直接切換可能會被重生邏輯覆蓋，
            // 但在 1.21.4 中，對 ServerPlayerEntity 設置遊戲模式通常是穩定的。
            player.changeGameMode(GameMode.SPECTATOR);

            // 如果你想順便給他夜視（回應你之前的需求），可以加在這裡
            player.addStatusEffect(new StatusEffectInstance(
                    StatusEffects.NIGHT_VISION,
                    StatusEffectInstance.INFINITE, // 1.21.4 推薦使用常量
                    0,
                    false,
                    false
            ));
        }
    });
}
}
