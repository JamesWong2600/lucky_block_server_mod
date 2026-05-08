package org.auto.lucky_block_server_mod.config;

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
        public String id = "server-01";
        public int expire_seconds = 5;
    }
}