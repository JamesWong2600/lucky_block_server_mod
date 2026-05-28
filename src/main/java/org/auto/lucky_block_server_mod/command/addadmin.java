package org.auto.lucky_block_server_mod.command;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.Text;
import org.auto.lucky_block_server_mod.cache.DataMap;
import org.auto.lucky_block_server_mod.cache.PlayerData;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.auto.lucky_block_server_mod.cache.DataMap.adminCache;

public class addadmin {

    public static DataMap dataMap;

    public static void addadmin_command_register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(CommandManager.literal("addadmin")
                    .requires(source -> source.hasPermissionLevel(2))
                    .then(CommandManager.argument("name", StringArgumentType.string())
                            .executes(context -> {
                                dataMap = new DataMap();

                                ServerCommandSource source = context.getSource();
                                String name = StringArgumentType.getString(context, "name");
                                MinecraftServer server = source.getServer(); // 取得伺服器實體

                                getUUIDFromMojang(name).thenAccept(uuid -> {
                                    // 強制將執行緒切回主執行緒執行
                                    server.execute(() -> {
                                        if (uuid == null) {
                                            source.sendError(Text.literal("§c[錯誤] 無法取得 UUID，請確認名稱。"));
                                            return;
                                        }
                                        adminCache.put()
                                        String rawPassword = UUID.randomUUID().toString().replace("-", "");
                                        dataMap.flushAdminDataToMongo(uuid, 99, rawPassword);

                                        source.sendFeedback(() -> Text.literal("§a[管理] 已為 " + name + " 指派 Admin。"), true);
                                        source.sendFeedback(() -> Text.literal("§e密碼: §b" + rawPassword), false);
                                    });
                                });

                                return 1;
                            })
                    )
            );
        });
    }

//            dispatcher.register(CommandManager.literal("addadmin")
//                    // 只有 OP 2 以上或控制台能指派 Admin
//                    .requires(source -> source.hasPermissionLevel(2))
//                    .then(CommandManager.argument("target", EntityArgumentType.player())
//                            .executes(context -> {
//                                ServerCommandSource source = context.getSource();
//                                ServerPlayerEntity targetPlayer = EntityArgumentType.getPlayer(context, "target");
//
//                                UUID uuid = targetPlayer.getUuid();
//                                String name = targetPlayer.getName().getString();
//
//                                dataMap = new DataMap();
//
//                                if (dataMap == null) {
//
//                                    source.sendError(Text.literal("§c[錯誤] DataMap 尚未初始化，無法執行指令！"));
//                                    return 0;
//                                }
//
//                                // 1. 產生 32 位不含橫槓的隨機密碼
//                                String rawPassword = UUID.randomUUID().toString().replace("-", "");
//                                int adminGroupCode = 99; // 管理組代號
//
//                                // 2. 更新遊戲內記憶體狀態
//                                PlayerData pData = dataMap.getPlayerData(uuid);
//                                pData.group = adminGroupCode;
//
//                                // 3. 提示執行者並附帶「點擊複製密碼」功能
//                                source.sendFeedback(() -> Text.literal("§a[管理] 已將 " + name + " 指派為 Admin，權限已寫入 admindata 表。"), true);
//
//                                Text passwordText = Text.literal("§e生成 32 位安全密碼為: §b" + rawPassword + " §7[點擊複製]")
//                                        .styled(style -> style
//                                                .withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, rawPassword))
//                                                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.literal("點擊複製密碼"))));
//                                source.sendFeedback(() -> passwordText, false);
//
//                                // 4. 通知目標玩家（不透露密碼）
//                                if (source.getPlayer() != targetPlayer) {
//                                    targetPlayer.sendMessage(Text.literal("§6你已被提升為 Admin 身分組！請聯絡後台索取你的管理驗證密碼。"), false);
//                                }
//
//                                // 5. 🌟 立馬非同步 Flush 到獨立的 "admindata" Collection
//                                dataMap.flushAdminDataToMongo(uuid, adminGroupCode, rawPassword);
//
//                                return 1;
//                            })
//                    )
//            );
//        });


    public static CompletableFuture<UUID> getUUIDFromMojang(String username) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                HttpClient client = HttpClient.newHttpClient();
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create("https://api.mojang.com/users/profiles/minecraft/" + username))
                        .build();

                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 200) {
                    JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
                    String id = json.get("id").getAsString();
                    // API 回傳的 ID 是不含橫槓的，需手動轉換格式
                    return UUID.fromString(id.replaceFirst("(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})", "$1-$2-$3-$4-$5"));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        });
    }
}
