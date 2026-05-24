package org.auto.lucky_block_server_mod.mixins;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;
import net.minecraft.world.TeleportTarget;
import org.auto.lucky_block_server_mod.Lucky_block_server_mod;
import org.auto.lucky_block_server_mod.cache.PlayerData;
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
    @Inject(method = "onDeath", at = @At("HEAD"))
    private void onEntityDeath(DamageSource damageSource, CallbackInfo ci) {
        // 將 this 轉型為當前的 LivingEntity 實例
        LivingEntity killedEntity = (LivingEntity) (Object) this;

        // 如果是在伺服器端運行
        if (!killedEntity.getWorld().isClient()) {
            // 從傷害來源中獲取攻擊者
            Entity attacker = damageSource.getAttacker();

            if (attacker != null) {
                // 這裡拿到的 attacker 就是擊殺者
                PlayerData playerData = Lucky_block_server_mod.DATA_MANAGER.getPlayerData(attacker.getUuid());

                playerData.incrementKillCount();
                System.out.println(attacker.getName().getString() + " 擊殺了 " + killedEntity.getName().getString());
            }
        }
    }

    @Inject(
            method = "onDeath",
            at = @At("TAIL"),
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
        }else{
            player.sendMessage(Text.literal("你已死亡，進入旁觀模式"), false);

            // 執行切換模式
            // 注意：在死亡瞬間直接切換可能會被重生邏輯覆蓋，
            // 但在 1.21.4 中，對 ServerPlayerEntity 設置遊戲模式通常是穩定的。
            player.changeGameMode(GameMode.SPECTATOR);
            PlayerData playerData = Lucky_block_server_mod.DATA_MANAGER.getPlayerData(player.getUuid());

            playerData.setEliminated(true);
            // 如果你想順便給他夜視（回應你之前的需求），可以加在這裡
            player.addStatusEffect(new StatusEffectInstance(
                    StatusEffects.NIGHT_VISION,
                    StatusEffectInstance.INFINITE, // 1.21.4 推薦使用常量
                    0,
                    false,
                    false
            ));
        }
    }
}