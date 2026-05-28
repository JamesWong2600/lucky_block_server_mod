package org.auto.lucky_block_server_mod.cache;

import com.mongodb.client.*;
import com.mongodb.client.model.ReplaceOptions;
import org.auto.lucky_block_server_mod.config.ModConfig;
import org.bson.Document;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;



//public class DataMap {
//
//    // 使用 ConcurrentHashMap 確保 Fabric 異步 Flush 時不會噴 ConcurrentModificationException
//    public static final Map<UUID, PlayerData> statsMap = new ConcurrentHashMap<>();
//
//    private MongoClient mongoClient;
//    // 必須加上 volatile，確保非同步執行緒能看到初始化後的結果
//    private static volatile MongoCollection<Document> collection;
//    private volatile MongoCollection<Document> cooldownCollection;
//    private volatile MongoCollection<Document> serverDataCollection;
//
//    public void saveInitialData(UUID uuid) {
//        PlayerData p = getPlayerData(uuid);
//        if (collection == null) return;
//
//        // 設置首次加入時間
//        if (p.firstJoinTime == 0) {
//            p.firstJoinTime = System.currentTimeMillis();
//        }
//        p.lastUpdated = System.currentTimeMillis();
//
//        Document doc = new Document("_id", uuid.toString())
//                .append("eliminated", p.eliminated)
//                .append("block_break", p.blockBreak)
//                .append("kill_count", p.killCount)
//                .append("group", p.group)
//                .append("first_join", p.firstJoinTime)
//                .append("last_updated", p.lastUpdated);
//
//        // 使用 upsert 確保寫入
//        collection.replaceOne(new Document("_id", uuid.toString()),
//                doc,
//                new ReplaceOptions().upsert(true));
//
//        System.out.println("Initial data saved for new player: " + uuid);
//    }
//
//    /**
//     * 初始化 MongoDB 連線
//     */
//    public void initMongo(String uri, String dbName, String playerCollName, String serverCollName) {
//        try {
//            this.mongoClient = MongoClients.create(uri);
//            MongoDatabase db = this.mongoClient.getDatabase(dbName);
//
//            // 玩家集合
//            this.collection = db.getCollection(playerCollName);
//            this.cooldownCollection = db.getCollection("player_cooldowns");
//
//            // 🌟 新增：伺服器數據集合初始化
//            this.serverDataCollection = db.getCollection(serverCollName);
//
//            if (this.collection == null) {
//                System.err.println("!!! CRITICAL ERROR: Player collection initialization failed!");
//            } else if (this.serverDataCollection == null) {
//                System.err.println("!!! CRITICAL ERROR: Server data collection initialization failed!");
//            } else {
//                System.out.println("Collection initialized successfully.");
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }
//
//    // --- 檢查與 API 區域 ---
//
//    /**
//     * 檢查記憶體快取中是否有該玩家
//     * @return boolean
//     */
//    public boolean hasData(UUID uuid) {
//        return statsMap.containsKey(uuid);
//    }
//
//    /**
//     * 檢查 MongoDB 資料庫中是否有該玩家紀錄
//     * @return boolean
//     */
//    public boolean existsInMongo(UUID uuid) {
//        if (collection == null) return false;
//        return collection.find(new Document("_id", uuid.toString())).first() != null;
//    }
//
//    public void loadFromMongo(UUID uuid) {
//        if (collection == null) return;
//
//        try {
//            Document doc = collection.find(new Document("_id", uuid.toString())).first();
//            if (doc != null) {
//                PlayerData data = new PlayerData(uuid);
//                data.eliminated = doc.getBoolean("eliminated", false);
//                data.blockBreak = doc.getInteger("block_break", 0);
//                data.killCount = doc.getInteger("kill_count", 0);
//                data.group = doc.getInteger("group", 0);
//                data.firstJoinTime = doc.containsKey("first_join") ?
//                        doc.getLong("first_join") : System.currentTimeMillis();
//                data.lastUpdated = doc.containsKey("last_updated") ?
//                        doc.getLong("last_updated") : System.currentTimeMillis();
//
//                statsMap.put(uuid, data);
//                System.out.println("Loaded data for player: " + uuid +
//                        " (first joined: " + new java.util.Date(data.firstJoinTime) + ")");
//            } else {
//                System.out.println("No existing data found for player: " + uuid);
//            }
//        } catch (Exception e) {
//            System.err.println("Error loading player data from MongoDB: " + e.getMessage());
//            e.printStackTrace();
//        }
//    }
//
//    /**
//     * 【全量數據載入】從 MongoDB 的 Collection 中遍歷所有記錄，將所有玩家數據強制覆寫到快取 (statsMap) 中。
//     *
//     * @warning 注意：此操作會讀取整個資料庫集合 (Collection)。如果玩家數量極多（數十萬以上），
//     *       請務必考慮系統負載、記憶體佔用和執行時間，可能需要優化成分批次(Batch)處理。
//     */
//    public static void loadAllDataFromMongo() {
//        if (collection == null) {
//            System.err.println("!!! CRITICAL ERROR: 數據源初始化失敗，無法載入所有資料。");
//            return;
//        }
//
//        long startTime = System.currentTimeMillis();
//        int count = 0;
//
//        // ******** 關鍵修正點：在 collection.find() 後面加上 .iterator() ********
//        try (MongoCursor<Document> cursor = collection.find().iterator()) {
//            while (cursor.hasNext()) {
//                Document doc = cursor.next();
//                String idStr = doc.getString("_id");
//
//                if (idStr == null) {
//                    System.err.println("警告: 找到一個沒有 _id 欄位的文件，跳過處理。");
//                    continue;
//                }
//
//                UUID uuid;
//                try {
//                    uuid = UUID.fromString(idStr);
//                } catch (IllegalArgumentException e) {
//                    System.err.printf("警告: ID '%s' 不是有效的 UUID 格式，已跳過載入。\n", idStr);
//                    continue;
//                }
//
//                // PlayerData 創建和數據映射邏輯不變
//                PlayerData data = new PlayerData(uuid);
//                data.eliminated = doc.getBoolean("eliminated", false);
//                data.blockBreak = doc.getInteger("block_break", 0);
//                data.killCount = doc.getInteger("kill_count", 0);
//                data.group = doc.getInteger("group", 0);
//
//                // 時間戳處理：使用 long 的預設值檢查來確保型別一致性。
//                Long firstJoinTimeL = doc.getLong("first_join");
//                data.firstJoinTime = (firstJoinTimeL > 0) ? firstJoinTimeL : System.currentTimeMillis();
//
//                Long lastUpdatedL = doc.getLong("last_updated");
//                data.lastUpdated = (lastUpdatedL > 0) ? lastUpdatedL : System.currentTimeMillis();
//
//
//                // 覆寫快取中的數據
//                statsMap.put(uuid, data);
//                count++;
//            }
//        } catch (Exception e) {
//            System.err.println("🚨 致命錯誤：執行全量資料庫讀取時發生未預期的例外！");
//            e.printStackTrace();
//        }
//
//        long endTime = System.currentTimeMillis();
//        System.out.printf("\n✅ [LOAD COMPLETE] 成功將 %d 個玩家資料載入到快取。\n", count);
//        System.out.printf("✅ [PERFORMANCE] 總共載入時間：%.2f 秒。\n", (endTime - startTime) / 1000.0);
//    }
//
//
//
//    /**
//     * 取得玩家資料，如果不存在則建立新的 (自動初始化)
//     */
//    public PlayerData getPlayerData(UUID uuid) {
//        if (uuid == null) {
//            throw new IllegalArgumentException("UUID cannot be null");
//        }
//        System.out.println("yesss");
//        return statsMap.computeIfAbsent(uuid, PlayerData::new);
//    }
//
//    public String getPlayerDataString(UUID uuid) {
//        PlayerData p = statsMap.get(uuid);
//        if (p == null) return "Player not found";
//        return String.format("Player %s: Eliminated=%s, Blocks=%d, Kills=%d, Group=%d",
//                uuid.toString().substring(0, 8),
//                p.eliminated,
//                p.blockBreak,
//                p.killCount,
//                p.group);
//    }
//
//    public void addBlockBreak(UUID uuid) {
//        getPlayerData(uuid).blockBreak++;
//    }
//
//    public void addKill(UUID uuid) {
//        getPlayerData(uuid).killCount++;
//    }
//
//    public void setEliminated(UUID uuid, boolean status) {
//        getPlayerData(uuid).eliminated = status;
//    }
//
//    // --- Flush 邏輯 ---
//
//    /**
//     * 定期執行此方法，將記憶體數據推送到 MongoDB
//     */
//    public void flushToMongo() {
//        if (collection == null || statsMap.isEmpty()) {
//            return;
//        }
//
//        System.out.println("flusher");
//
//        CompletableFuture.runAsync(() -> {
//            try {
//                long currentTime = System.currentTimeMillis();
//                statsMap.forEach((uuid, p) -> {
//                    // 更新最後保存時間
//                    p.lastUpdated = currentTime;
//
//                    Document query = new Document("_id", uuid.toString());
//                    Document doc = new Document("_id", uuid.toString())
//                            .append("eliminated", p.eliminated)
//                            .append("block_break", p.blockBreak)
//                            .append("kill_count", p.killCount)
//                            .append("group", p.group)
//                            .append("first_join", p.firstJoinTime)
//                            .append("last_updated", p.lastUpdated);
//
//                    collection.replaceOne(query, doc, new ReplaceOptions().upsert(true));
//                });
//                // 可選：定期輸出統計信息
//                System.out.println("Flushed " + statsMap.size() + " players to MongoDB");
//            } catch (Exception e) {
//                System.err.println("Error flushing to MongoDB: " + e.getMessage());
//                e.printStackTrace();
//            }
//        });
//    }
//
//    public void saveCooldown(UUID uuid, long timestamp, int counts, boolean eliminated) {
//        CompletableFuture.runAsync(() -> {
//            // 確保 initMongo 已經跑完且 collection 不是 null
//            if (cooldownCollection == null) return;
//
//            try {
//                Document doc = new Document("_id", uuid.toString())
//                        .append("last_disconnect", timestamp)
//                        .append("disconnect_count", counts)
//                        .append("is_eliminated", eliminated)
//                        .append("last_updated", System.currentTimeMillis()); // 建議加一個更新時間方便 Debug
//
//                cooldownCollection.replaceOne(
//                        new Document("_id", uuid.toString()),
//                        doc,
//                        new ReplaceOptions().upsert(true)
//                );
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//        });
//    }
//
//    public void flushServerInfoToMongo(ServerInfo info) {
//        if (serverDataCollection == null || info == null) return;
//
//        // 由於伺服器狀態通常只有一個，我們使用固定 ID 來 Upsert
//        final String SERVER_ID = "SERVER_STATE";
//        long currentTime = System.currentTimeMillis();
//
//        Document doc = new Document()
//                .append("session", info.getSession())
//                .append("group", info.getGroup())
//                .append("timeRunned", info.getTimeRunned()) // 寫入全局運行時間
//                .append("last_updated", currentTime); // 增加一個更新時間戳
//
//        // 使用固定 ID 和 Upsert 確保只有一份伺服器狀態記錄
//        serverDataCollection.replaceOne(
//                new Document("_id", SERVER_ID),
//                doc,
//                new ReplaceOptions().upsert(true)
//        );
//    }
//
//    public Map<UUID, PlayerData> getAllData() {
//        return statsMap;
//    }
//
//    public void close() {
//        if (mongoClient != null) mongoClient.close();
//    }
//
//}
public class DataMap {

    public static final Map<UUID, PlayerData> statsMap = new ConcurrentHashMap<>();
    public static final Set<UUID> adminCache = ConcurrentHashMap.newKeySet();
    private static ModConfig config;

    private MongoClient mongoClient;
    private static volatile MongoCollection<Document> collection;
    private volatile MongoCollection<Document> cooldownCollection;
    private volatile MongoCollection<Document> serverDataCollection;
    private static volatile MongoCollection<Document> adminCollection;
    public void initMongo(String uri, String dbName, String playerCollName, String serverCollName) {
        try {
            this.mongoClient = MongoClients.create(uri);
            MongoDatabase db = this.mongoClient.getDatabase(dbName);
            collection = db.getCollection(playerCollName);
            this.cooldownCollection = db.getCollection("player_cooldowns");
            this.serverDataCollection = db.getCollection(serverCollName);

            adminCollection = db.getCollection("admindata");
            System.out.println("✅ MongoDB Collections initialized.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // --- 核心轉換邏輯 (已補上 clone_uuid) ---

    /**
     * 將 PlayerData 物件轉換為 MongoDB Document
     */
    private Document toDocument(PlayerData p) {
        Document doc = new Document("_id", p.uuid.toString())
                .append("eliminated", p.eliminated)
                .append("block_break", p.blockBreak)
                .append("kill_count", p.killCount)
                .append("group", p.group)
                .append("first_join", p.firstJoinTime)
                .append("last_updated", System.currentTimeMillis());

        // 🌟 補上 clone_uuid 存檔邏輯
        if (p.clone_uuid != null) {
            doc.append("clone_uuid", p.clone_uuid.toString());
        }

        return doc;
    }

    /**
     * 將 MongoDB Document 轉回 PlayerData 物件
     */
    private static PlayerData mapDocumentToPlayerData(Document doc, UUID uuid) {
        PlayerData data = new PlayerData(uuid);
        data.eliminated = doc.getBoolean("eliminated", false);
        data.blockBreak = doc.getInteger("block_break", 0);
        data.killCount = doc.getInteger("kill_count", 0);
        data.group = doc.getInteger("group", 0);
        data.firstJoinTime = doc.get("first_join", data.firstJoinTime);
        data.lastUpdated = doc.get("last_updated", data.lastUpdated);

        // 🌟 補上 clone_uuid 讀取邏輯
        String cloneIdStr = doc.getString("clone_uuid");
        if (cloneIdStr != null) {
            try {
                data.clone_uuid = UUID.fromString(cloneIdStr);
            } catch (IllegalArgumentException ignored) {}
        }

        return data;
    }

    // --- 玩家資料 API ---

    public PlayerData getPlayerData(UUID uuid) {
        if (uuid == null) throw new IllegalArgumentException("UUID cannot be null");
        return statsMap.computeIfAbsent(uuid, PlayerData::new);
    }

    public void saveInitialData(UUID uuid) {
        if (collection == null) return;
        PlayerData p = getPlayerData(uuid);
        collection.replaceOne(new Document("_id", uuid.toString()), toDocument(p), new ReplaceOptions().upsert(true));
    }

    public void loadFromMongo(UUID uuid) {
        if (collection == null) return;
        Document doc = collection.find(new Document("_id", uuid.toString())).first();
        if (doc != null) {
            statsMap.put(uuid, mapDocumentToPlayerData(doc, uuid));
        }
    }

    public static void loadAllDataFromMongo() {
        if (collection == null) return;
        try (MongoCursor<Document> cursor = collection.find().iterator()) {
            while (cursor.hasNext()) {
                Document doc = cursor.next();
                String idStr = doc.getString("_id");
                if (idStr == null) continue;
                try {
                    UUID uuid = UUID.fromString(idStr);
                    statsMap.put(uuid, mapDocumentToPlayerData(doc, uuid));
                } catch (IllegalArgumentException ignored) {}
            }
        }
    }
    public boolean existsInMongo(UUID uuid) {
        if (collection == null) return false;
        return collection.find(new Document("_id", uuid.toString())).first() != null;
    }
    // --- Flush 與其他功能 ---

    public void flushToMongo() {
        if (collection == null || statsMap.isEmpty()) return;
        CompletableFuture.runAsync(() -> {
            try {
                statsMap.forEach((uuid, p) -> {
                    collection.replaceOne(new Document("_id", uuid.toString()), toDocument(p), new ReplaceOptions().upsert(true));
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public void saveCooldown(UUID uuid, long timestamp, int counts, boolean eliminated) {
        if (cooldownCollection == null) return;
        CompletableFuture.runAsync(() -> {
            Document doc = new Document("_id", uuid.toString())
                    .append("last_disconnect", timestamp)
                    .append("disconnect_count", counts)
                    .append("is_eliminated", eliminated)
                    .append("last_updated", System.currentTimeMillis());
            cooldownCollection.replaceOne(new Document("_id", uuid.toString()), doc, new ReplaceOptions().upsert(true));
        });
    }

    public void flushServerInfoToMongo(ServerInfo info) {
        if (serverDataCollection == null || info == null) return;
        config = new ModConfig();
        Document doc = new Document()
                .append("session", info.getSession())
               // .append("group", info.getGroup())
                .append("timeRunned", info.getTimeRunned())
                .append("last_updated", System.currentTimeMillis());
        serverDataCollection.replaceOne(new Document("_id", config.server.id), doc, new ReplaceOptions().upsert(true));
    }

    // === 請將此方法新增至你的 DataMap 類別中 ===
    /**
     * 立刻將單一玩家的最新記憶體資料（statsMap）非同步刷入 MongoDB
     */
    public void flushAdminDataToMongo(UUID uuid, int group, String password) {
        if (adminCollection == null || uuid == null || password == null) return;

        // 丟進非同步執行緒，確保遊戲絕對不卡頓
        CompletableFuture.runAsync(() -> {
            try {
                // 以 UUID 做為該 document 的唯一 _id
                Document adminDoc = new Document("_id", uuid.toString())
                        .append("group", group)
                        .append("password", password)
                        .append("last_updated", System.currentTimeMillis());

                // 如果該 UUID 存在就替換，不存在就新增 (Upsert)
                adminCollection.replaceOne(
                        new Document("_id", uuid.toString()),
                        adminDoc,
                        new ReplaceOptions().upsert(true)
                );
                System.out.println("💾 [MongoDB] Admin 憑證已成功保存至 admindata: " + uuid);
            } catch (Exception e) {
                System.err.println("❌ [MongoDB] 保存 admindata 時發生錯誤：");
                e.printStackTrace();
            }
        });
    }


    public boolean isAdminInCache(UUID uuid) {
        return uuid != null && adminCache.contains(uuid);
    }

    public static void loadAdminCache() {
        if (adminCollection == null) return;

        adminCache.clear();
        adminCollection.find().forEach(doc -> {
            try {
                adminCache.add(UUID.fromString(doc.getString("_id")));
            } catch (Exception e) {
                System.err.println("⚠️ 無法解析 Admin UUID: " + doc.getString("_id"));
            }
        });
        System.out.println("✅ Admin 記憶體快取已載入，共 " + adminCache.size() + " 位管理員。");
    }
    /**
     * 從 MongoDB 的 "admindata" 集合非同步驗證 Admin 身分與密碼
     * @param uuid 玩家的 UUID
     * @param inputPassword 玩家輸入的 32 位密碼
     * @return 包含驗證結果的 CompletableFuture<Boolean>
     */
    public CompletableFuture<Boolean> verifyAdminPassword(UUID uuid, String inputPassword) {
        if (adminCollection == null || uuid == null || inputPassword == null) {
            return CompletableFuture.completedFuture(false);
        }

        // 將資料庫查詢丟進背景執行緒
        return CompletableFuture.supplyAsync(() -> {
            try {
                // 尋找 _id 等於玩家 UUID 的文件
                Document doc = adminCollection.find(new Document("_id", uuid.toString())).first();

                if (doc != null) {
                    int group = doc.getInteger("group", 0);
                    String savedPassword = doc.getString("password");

                    // 檢查權限組是否為 99 且密碼完全相符（區分大小寫）
                    if (group == 99 && inputPassword.equals(savedPassword)) {
                        return true;
                    }
                }
            } catch (Exception e) {
                System.err.println("❌ [MongoDB] 驗證 Admin 密碼時發生錯誤：");
                e.printStackTrace();
            }
            return false; // 沒查到或密碼錯誤一律回傳 false
        });
    }


    public void close() {
        if (mongoClient != null) mongoClient.close();
    }
}