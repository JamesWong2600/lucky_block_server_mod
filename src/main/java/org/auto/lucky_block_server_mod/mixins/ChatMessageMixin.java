package org.auto.lucky_block_server_mod.mixins;

import net.minecraft.network.packet.c2s.play.ChatMessageC2SPacket;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import org.auto.lucky_block_server_mod.message.cross_server_msg;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

@Mixin(ServerPlayNetworkHandler.class)
public class ChatMessageMixin {

    @Shadow
    public ServerPlayerEntity player; // 獲取當前發送訊息的玩家物件

    @Inject(
            method = "onChatMessage",
            at = @At("HEAD"),
            cancellable = true // 允許我們中斷、取消這個方法
    )
    private void onPlayerChat(ChatMessageC2SPacket packet, CallbackInfo ci) {
        // 1. 取得玩家輸入的原始字串
        String rawMessage = packet.chatMessage();

        // 2. 獲取玩家的資訊
        UUID playerUuid = this.player.getUuid();
        String playerName = this.player.getName().getString();

        // 3. 用我們之前的 RedisManager 異步推送到外部
        cross_server_msg.publishEvent(playerUuid, playerName, rawMessage);

        // 4. 【核心控制點】
        // 如果你只想偷偷把訊息傳到 Redis，不希望這句話出現在遊戲聊天室裡：
        ci.cancel();

        // 注意：如果你希望遊戲內聊天室照常顯示，只是順便備份到 Redis，
        // 把上面那行 `ci.cancel();` 註解掉或刪除即可！
    }
}
