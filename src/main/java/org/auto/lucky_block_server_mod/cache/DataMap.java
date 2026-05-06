package org.auto.lucky_block_server_mod.cache;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.ReplaceOptions;
import org.bson.Document;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;


public class DataMap {

    // 使用 ConcurrentHashMap 確保 Fabric 異步 Flush 時不會噴 ConcurrentModificationException
    private final Map<UUID, PlayerData> statsMap = new ConcurrentHashMap<>();

    private MongoClient mongoClient;
    // 必須加上 volatile，確保非同步執行緒能看到初始化後的結果
    private volatile MongoCollection<Document> collection;
    private volatile MongoCollection<Document> cooldownCollection;

    public void saveInitialData(UUID uuid) {
        PlayerData p = getPlayerData(uuid);
        if (collection == null) return;

        // 設置首次加入時間
        if (p.firstJoinTime == 0) {
            p.firstJoinTime = System.currentTimeMillis();
        }
        p.lastUpdated = System.currentTimeMillis();

        Document doc = new Document("_id", uuid.toString())
                .append("eliminated", p.eliminated)
                .append("block_break", p.blockBreak)
                .append("kill_count", p.killCount)
                .append("group", p.group)
                .append("first_join", p.firstJoinTime)
                .append("last_updated", p.lastUpdated);

        // 使用 upsert 確保寫入
        collection.replaceOne(new Document("_id", uuid.toString()),
                doc,
                new ReplaceOptions().upsert(true));

        System.out.println("Initial data saved for new player: " + uuid);
    }

    /**
     * 初始化 MongoDB 連線
     */
    public void initMongo(String uri, String dbName, String collName) {
        try {
            this.mongoClient = MongoClients.create(uri);
            MongoDatabase db = this.mongoClient.getDatabase(dbName);

            this.collection = db.getCollection(collName);
            this.cooldownCollection = db.getCollection("player_cooldowns");

            if (this.collection == null) {
                System.err.println("!!! CRITICAL ERROR: collection initialization failed!");
            } else {
                System.out.println("Collection initialized: " + this.collection.getNamespace());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // --- 檢查與 API 區域 ---

    /**
     * 檢查記憶體快取中是否有該玩家
     * @return boolean
     */
    public boolean hasData(UUID uuid) {
        return statsMap.containsKey(uuid);
    }

    /**
     * 檢查 MongoDB 資料庫中是否有該玩家紀錄
     * @return boolean
     */
    public boolean existsInMongo(UUID uuid) {
        if (collection == null) return false;
        return collection.find(new Document("_id", uuid.toString())).first() != null;
    }

    public void loadFromMongo(UUID uuid) {
        if (collection == null) return;

        try {
            Document doc = collection.find(new Document("_id", uuid.toString())).first();
            if (doc != null) {
                PlayerData data = new PlayerData(uuid);
                data.eliminated = doc.getBoolean("eliminated", false);
                data.blockBreak = doc.getInteger("block_break", 0);
                data.killCount = doc.getInteger("kill_count", 0);
                data.group = doc.getInteger("group", 0);
                data.firstJoinTime = doc.containsKey("first_join") ?
                        doc.getLong("first_join") : System.currentTimeMillis();
                data.lastUpdated = doc.containsKey("last_updated") ?
                        doc.getLong("last_updated") : System.currentTimeMillis();

                statsMap.put(uuid, data);
                System.out.println("Loaded data for player: " + uuid +
                        " (first joined: " + new java.util.Date(data.firstJoinTime) + ")");
            } else {
                System.out.println("No existing data found for player: " + uuid);
            }
        } catch (Exception e) {
            System.err.println("Error loading player data from MongoDB: " + e.getMessage());
            e.printStackTrace();
        }
    }
    /**
     * 取得玩家資料，如果不存在則建立新的 (自動初始化)
     */
    public PlayerData getPlayerData(UUID uuid) {
        if (uuid == null) {
            throw new IllegalArgumentException("UUID cannot be null");
        }
        System.out.println("yesss");
        return statsMap.computeIfAbsent(uuid, PlayerData::new);
    }

    public String getPlayerDataString(UUID uuid) {
        PlayerData p = statsMap.get(uuid);
        if (p == null) return "Player not found";
        return String.format("Player %s: Eliminated=%s, Blocks=%d, Kills=%d, Group=%d",
                uuid.toString().substring(0, 8),
                p.eliminated,
                p.blockBreak,
                p.killCount,
                p.group);
    }

    public void addBlockBreak(UUID uuid) {
        getPlayerData(uuid).blockBreak++;
    }

    public void addKill(UUID uuid) {
        getPlayerData(uuid).killCount++;
    }

    public void setEliminated(UUID uuid, boolean status) {
        getPlayerData(uuid).eliminated = status;
    }

    // --- Flush 邏輯 ---

    /**
     * 定期執行此方法，將記憶體數據推送到 MongoDB
     */
    public void flushToMongo() {
        if (collection == null || statsMap.isEmpty()) {
            return;
        }

        CompletableFuture.runAsync(() -> {
            try {
                long currentTime = System.currentTimeMillis();
                statsMap.forEach((uuid, p) -> {
                    // 更新最後保存時間
                    p.lastUpdated = currentTime;

                    Document query = new Document("_id", uuid.toString());
                    Document doc = new Document("_id", uuid.toString())
                            .append("eliminated", p.eliminated)
                            .append("block_break", p.blockBreak)
                            .append("kill_count", p.killCount)
                            .append("group", p.group)
                            .append("first_join", p.firstJoinTime)
                            .append("last_updated", p.lastUpdated);

                    collection.replaceOne(query, doc, new ReplaceOptions().upsert(true));
                });
                // 可選：定期輸出統計信息
                System.out.println("Flushed " + statsMap.size() + " players to MongoDB");
            } catch (Exception e) {
                System.err.println("Error flushing to MongoDB: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    public void saveCooldown(UUID uuid, long timestamp, int counts, boolean eliminated) {
        CompletableFuture.runAsync(() -> {
            // 確保 initMongo 已經跑完且 collection 不是 null
            if (cooldownCollection == null) return;

            try {
                Document doc = new Document("_id", uuid.toString())
                        .append("last_disconnect", timestamp)
                        .append("disconnect_count", counts)
                        .append("is_eliminated", eliminated)
                        .append("last_updated", System.currentTimeMillis()); // 建議加一個更新時間方便 Debug

                cooldownCollection.replaceOne(
                        new Document("_id", uuid.toString()),
                        doc,
                        new ReplaceOptions().upsert(true)
                );
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public Map<UUID, PlayerData> getAllData() {
        return statsMap;
    }

    public void close() {
        if (mongoClient != null) mongoClient.close();
    }
}