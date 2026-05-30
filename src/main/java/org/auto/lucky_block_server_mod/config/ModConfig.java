package org.auto.lucky_block_server_mod.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.moandjiezana.toml.Toml;
import com.moandjiezana.toml.TomlWriter;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.MinecraftServer;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import static org.auto.lucky_block_server_mod.Lucky_block_server_mod.CONFIG;
import static org.auto.lucky_block_server_mod.server_init.currentServer;

public class ModConfig {
    public DatabaseSection database = new DatabaseSection();
    public RedisSection redis = new RedisSection();
    public ServerSection server = new ServerSection();

    public static class DatabaseSection {
        public String uri = "mongodb://admin:password@127.0.0.1:27017";
        public String db_name = "playerdataset";
        public String collection_name = "playerdataset";
        public String server_data_collection = "serverdata";
    }

    public static class RedisSection {
        public String host = "127.0.0.1";
        public int port = 6379;
        public String password = "";
    }

    public static class ServerSection {
        public String id = "";
        public int expire_seconds = 5;

        // 加入 MinecraftServer 參數
        public void initId(net.minecraft.server.MinecraftServer server) {
            if (this.id == null || this.id.isEmpty()) {
                try {
                    String ip = java.net.InetAddress.getLocalHost().getHostAddress();
                    int port = server.getServerPort(); // 正確呼叫
                    this.id = ip + ":" + port;
                } catch (Exception e) {
                    this.id = "unknown-server";
                }
            }
        }
    }

    public static ModConfig loadConfigFromFile() {
        File configFile = new File(FabricLoader.getInstance().getConfigDir().toFile(), "lucky_block_config.toml");
        Toml toml = new Toml();

        if (configFile.exists()) {
            return toml.read(configFile).to(ModConfig.class);
        }

        // 建立預設檔案
        ModConfig defaultConfig = new ModConfig();
        try {
            new TomlWriter().write(defaultConfig, configFile);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return defaultConfig;
    }

    public static void saveConfigToFile() {
        File configFile = new File(FabricLoader.getInstance().getConfigDir().toFile(), "lucky_block_config.toml");
        try {
            new TomlWriter().write(CONFIG, configFile);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}