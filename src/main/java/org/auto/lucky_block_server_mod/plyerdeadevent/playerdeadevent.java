package org.auto.lucky_block_server_mod.plyerdeadevent;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.world.GameMode;

public class playerdeadevent {

    public static void death_to_specttor(){
    ServerLivingEntityEvents.AFTER_DEATH.register((entity, damageSource) -> {
        // 判斷死亡的是不是玩家
        if (entity instanceof ServerPlayerEntity player) {
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
