package org.auto.lucky_block_server_mod.scoreboard;

import net.minecraft.network.packet.s2c.play.ScoreboardDisplayS2CPacket;
import net.minecraft.network.packet.s2c.play.ScoreboardObjectiveUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.ScoreboardScoreResetS2CPacket;
import net.minecraft.network.packet.s2c.play.ScoreboardScoreUpdateS2CPacket;
import net.minecraft.scoreboard.*;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.auto.lucky_block_server_mod.Lucky_block_server_mod;
import org.auto.lucky_block_server_mod.cache.PlayerData;
import org.auto.lucky_block_server_mod.clone_player_entity.ClonePlayerEntity;
import org.jetbrains.annotations.Nullable;

import java.util.*;

import static org.auto.lucky_block_server_mod.Lucky_block_server_mod.currentMspt;
import static org.auto.lucky_block_server_mod.performance_stat.NetWorkStuff.getPlayerPing;

public class EventScoreboard {
    private static final String OBJECTIVE_NAME = "lucky_block_objective";  // 固定名稱，所有玩家共用

    private static final Map<UUID, List<String>> playerLineKeys = new HashMap<>();  // 儲存每個玩家的行 key

    private static final Set<UUID> initializedPlayers = new HashSet<>();

    private static final Map<UUID, List<String>> playerLastLines = new HashMap<>();

    public static void resetPlayer(UUID uuid) {
        initializedPlayers.remove(uuid);
        playerLastLines.remove(uuid); // 確保這行也有被清理
    }

    public static void updateScoreboard(ServerPlayerEntity player, String stage, Integer time,
                                        Integer brokenCount, Integer borderSize,
                                        Integer GlobalPlayerAmount, Integer GroupPlayerAmount,
                                        @Nullable String group) {

        // 維持你的 16 位 UUID 前綴邏輯
        String pObjName = "lb_" + player.getUuidAsString().substring(0, 16);


        if (!initializedPlayers.contains(player.getUuid())) {
            Scoreboard scoreboard = player.getScoreboard();
            ScoreboardObjective fakeObjective = new ScoreboardObjective(
                    scoreboard, pObjName, ScoreboardCriterion.DUMMY,
                    Text.literal("§6§l《幸運方塊活動》"), ScoreboardCriterion.RenderType.INTEGER,
                    true, null
            );
            player.networkHandler.sendPacket(new ScoreboardObjectiveUpdateS2CPacket(fakeObjective, 0));
            player.networkHandler.sendPacket(new ScoreboardDisplayS2CPacket(ScoreboardDisplaySlot.SIDEBAR, fakeObjective));
            initializedPlayers.add(player.getUuid());
        }

        // 3. 構建內容
        List<String> lines = new ArrayList<>();
        lines.add("§f ");
        lines.add("§f現階段: §a" + stage);
        if (time != null) lines.add("§f遊戲時間: §a" + time + "s");
        if (brokenCount != null) lines.add("§f破壞數量: §a" + brokenCount + "個");

        // 提醒：此處建議修正變數，不要重複使用 brokenCount
        if (brokenCount != null) lines.add("§f殺敵數量: §a" + brokenCount + "個");

        if (borderSize != null) lines.add("§f邊界大小: §a" + borderSize + "x" + borderSize);
        if (GlobalPlayerAmount != null) lines.add("§f全域人數: §a" + GlobalPlayerAmount);
        if (currentMspt != -1) lines.add("§fMSPT: §a" + (int) currentMspt);
        lines.add("§f網路延遲: §a" + getPlayerPing(player) + "ms");
        lines.add("§8 ");

        // 4. 發送分數更新封包 (關鍵修復點)
        List<String> lastKeys = playerLastLines.get(player.getUuid());
        if (lastKeys != null) {
            for (String oldKey : lastKeys) {
                // 在 1.21.4 中，發送一個 score 為空或特定的 Reset 封包來刪除行
                // 使用 ScoreboardScoreResetS2CPacket (如果你的 Yarn 版本有這個類)
                // 或者發送一個空的 ScoreboardScoreUpdateS2CPacket (某些版本支援)
                player.networkHandler.sendPacket(new ScoreboardScoreResetS2CPacket(oldKey, pObjName));
            }
        }

        // 4. 發送新分數並記錄 Key
        List<String> newKeys = new ArrayList<>();
        for (int i = 0; i < lines.size(); i++) {
            String displayText = lines.get(i);
            String finalKey = displayText + " ".repeat(i); // 確保每行唯一

            newKeys.add(finalKey);
            player.networkHandler.sendPacket(new ScoreboardScoreUpdateS2CPacket(
                    finalKey,
                    pObjName,
                    lines.size() - i,
                    Optional.empty(),
                    Optional.empty()
            ));
        }

        // 5. 更新快取
        playerLastLines.put(player.getUuid(), newKeys);
    }

    private static void setLine(ServerPlayerEntity player, ScoreboardObjective obj, String text, int score) {
        // 在文字中加入玩家 UUID 後綴，但顯示時用 §r 隱藏
        String uniqueText = text + "§" + player.getUuid().toString().substring(0, 1);
        player.getScoreboard().getOrCreateScore(ScoreHolder.fromName(uniqueText), obj).setScore(score);
    }
}
