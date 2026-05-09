package org.auto.lucky_block_server_mod.mixins;

import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.TeleportTarget;
import org.auto.lucky_block_server_mod.Lucky_block_server_mod;
import org.auto.lucky_block_server_mod.cache.ServerInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static org.auto.lucky_block_server_mod.Lucky_block_server_mod.LOBBY_WORLD_KEY;

@Mixin(ServerPlayerEntity.class)
public abstract class Death {
    private static final double LOBBY_X = 0.5;
    private static final double LOBBY_Y = 10.0;
    private static final double LOBBY_Z = 0.5;



    @Inject(
            method = "onDeath",
            at = @At("HEAD"),
            cancellable = true
    )
    private void cancelDeathAndTeleport(DamageSource damageSource, CallbackInfo ci) {
        ServerPlayerEntity player = (ServerPlayerEntity) (Object) this;
        ServerInfo currentServerInfo = Lucky_block_server_mod.serverManager;

        if (currentServerInfo.getSession() == 1 || currentServerInfo.getSession() == 5) {
            ServerWorld lobbyWorld = player.getServer().getWorld(LOBBY_WORLD_KEY);

            if (lobbyWorld != null) {
                // 取消死亡事件

                // 手動執行傳送
                player.teleportTo(new TeleportTarget(
                        lobbyWorld,
                        new Vec3d(LOBBY_X, LOBBY_Y, LOBBY_Z),
                        Vec3d.ZERO,
                        0.0f,
                        90.0f,
                        TeleportTarget.NO_OP
                ));

                // 恢復生命值
                player.setHealth(player.getMaxHealth());

                // 清除狀態效果
                player.clearStatusEffects();

                ;
            }
        }
    }
}