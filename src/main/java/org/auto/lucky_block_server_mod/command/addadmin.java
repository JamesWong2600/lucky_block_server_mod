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
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.auto.lucky_block_server_mod.cache.DataMap.adminCache;
import static org.auto.lucky_block_server_mod.cache.DataMap.flushAdminDataToMongo;

public class addadmin {

    public static DataMap dataMap;

    public static void addadmin_command_register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(CommandManager.literal("addadmin")
                    .requires(source -> source.hasPermissionLevel(2))
                    .then(CommandManager.argument("name", StringArgumentType.string())
                            .executes(context -> {
                                ServerCommandSource source = context.getSource();
                                String name = StringArgumentType.getString(context, "name");
                                MinecraftServer server = source.getServer();

                                // 統一呼叫這一個方法即可
                                promotePlayer(server, source, name);

                                return 1;
                            })
                    )
            );
        });
    }

    public static void promotePlayer(MinecraftServer server, ServerCommandSource source, String name) {
        if (server.isOnlineMode()) {
            getUUIDFromMojang(name).thenAccept(uuid -> {
                if (uuid == null) {
                    source.sendError(Text.literal("§c[錯誤] 無法取得 UUID。"));
                } else {
                    executePromotion(source, uuid, name);
                }
            });
        } else {
            // 離線模式 UUID 生成
            UUID uuid = UUID.nameUUIDFromBytes(("OfflinePlayer:" + name).getBytes(java.nio.charset.StandardCharsets.UTF_8));
            executePromotion(source, uuid, name);
        }
    }

    private static void handleAdminPromotion(ServerCommandSource source, UUID uuid, String name) {
        source.getServer().execute(() -> {
            String rawPassword = UUID.randomUUID().toString().replace("-", "").substring(0, 16); // 取前16位比較好記

            // 1. 同步寫入資料庫
            dataMap.flushAdminDataToMongo(uuid, 99, rawPassword);

            // 2. 更新記憶體快取
            adminCache.add(uuid);

            source.sendFeedback(() -> Text.literal("§a[管理] 已為離線玩家 " + name + " 指派 Admin。"), true);
            source.sendFeedback(() -> Text.literal("§e密碼: §b" + rawPassword), false);
        });
    }

    public void promotePlayerToAdmin(MinecraftServer server, ServerCommandSource source, String name) {
        UUID targetUuid;

        // 判斷線上/離線模式生成 UUID
        if (server.isOnlineMode()) {
            getUUIDFromMojang(name).thenAccept(uuid -> {
                if (uuid == null) {
                    source.sendError(Text.literal("§c[錯誤] 無法取得 UUID。"));
                } else {
                    executePromotion(source, uuid, name);
                }
            });
            return;
        } else {
            // 離線模式 UUID 生成算法
            targetUuid = UUID.nameUUIDFromBytes(("OfflinePlayer:" + name).getBytes(StandardCharsets.UTF_8));
            executePromotion(source, targetUuid, name);
        }
    }

    private static void executePromotion(ServerCommandSource source, UUID uuid, String name) {
        String rawPassword = UUID.randomUUID().toString().replace("-", "").substring(0, 16);

        // 1. 同步寫入資料庫
        flushAdminDataToMongo(uuid, 99, rawPassword);

        // 2. 更新記憶體快取 (確保 adminCache 已載入)
        adminCache.add(uuid);

        source.sendFeedback(() -> Text.literal("§a[管理] 已為玩家 " + name + " 指派 Admin。"), true);
        source.sendFeedback(() -> Text.literal("§e密碼: §b" + rawPassword), false);
    }

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
