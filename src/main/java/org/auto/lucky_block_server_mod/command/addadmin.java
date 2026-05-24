package org.auto.lucky_block_server_mod.command;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.Text;
import org.auto.lucky_block_server_mod.cache.DataMap;
import org.auto.lucky_block_server_mod.cache.PlayerData;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class addadmin {

    public static DataMap dataMap;

    public static void addadmin_command_register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(CommandManager.literal("addadmin")
                    // 只有 OP 2 以上或控制台能指派 Admin
                    .requires(source -> source.hasPermissionLevel(2))
                    .then(CommandManager.argument("target", EntityArgumentType.player())
                            .executes(context -> {
                                ServerCommandSource source = context.getSource();
                                ServerPlayerEntity targetPlayer = EntityArgumentType.getPlayer(context, "target");

                                UUID uuid = targetPlayer.getUuid();
                                String name = targetPlayer.getName().getString();

                                if (dataMap == null) {
                                    source.sendError(Text.literal("§c[錯誤] DataMap 尚未初始化，無法執行指令！"));
                                    return 0;
                                }

                                // 1. 產生 32 位不含橫槓的隨機密碼
                                String rawPassword = UUID.randomUUID().toString().replace("-", "");
                                int adminGroupCode = 99; // 管理組代號

                                // 2. 更新遊戲內記憶體狀態
                                PlayerData pData = dataMap.getPlayerData(uuid);
                                pData.group = adminGroupCode;

                                // 3. 提示執行者並附帶「點擊複製密碼」功能
                                source.sendFeedback(() -> Text.literal("§a[管理] 已將 " + name + " 指派為 Admin，權限已寫入 admindata 表。"), true);

                                Text passwordText = Text.literal("§e生成 32 位安全密碼為: §b" + rawPassword + " §7[點擊複製]")
                                        .styled(style -> style
                                                .withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, rawPassword))
                                                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.literal("點擊複製密碼"))));
                                source.sendFeedback(() -> passwordText, false);

                                // 4. 通知目標玩家（不透露密碼）
                                if (source.getPlayer() != targetPlayer) {
                                    targetPlayer.sendMessage(Text.literal("§6你已被提升為 Admin 身分組！請聯絡後台索取你的管理驗證密碼。"), false);
                                }

                                // 5. 🌟 立馬非同步 Flush 到獨立的 "admindata" Collection
                                dataMap.flushAdminDataToMongo(uuid, adminGroupCode, rawPassword);

                                return 1;
                            })
                    )
            );
        });
    }
}
