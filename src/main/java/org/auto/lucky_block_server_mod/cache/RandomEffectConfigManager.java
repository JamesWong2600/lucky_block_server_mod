package org.auto.lucky_block_server_mod.cache;

import com.google.gson.Gson;
import net.minecraft.server.MinecraftServer;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class RandomEffectConfigManager {
    private static final Gson GSON = new Gson();
    private static RandomEffectConfigData config;

    public static void loadConfig(MinecraftServer server) {
        try {
            // 從 resources 讀取檔案
            InputStream stream = RandomEffectConfigManager.class.getResourceAsStream("/assets/lucky/random_event.json");
            if (stream == null) {
                System.err.println("[Mod] 找不到 random_event.json 檔案！");
                return;
            }
            InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8);
            config = GSON.fromJson(reader, RandomEffectConfigData.class);
            reader.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static RandomEffectConfigData getConfig() {
        return config;
    }
}
