package org.auto.lucky_block_server_mod.scoreboard;

import net.minecraft.scoreboard.*;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.*;

public class EventScoreboard {
    private static final String OBJECTIVE_NAME = "lucky_event";

    private static final Map<UUID, List<String>> playerLines = new HashMap<>();

    public static void updateScoreboard(ServerPlayerEntity player, String stage, int time, int brokenCount, int borderSize, String group) {
        Scoreboard scoreboard = player.getScoreboard();
        ScoreboardObjective objective = scoreboard.getNullableObjective(OBJECTIVE_NAME);

        if (objective == null) {
            objective = scoreboard.addObjective(
                    OBJECTIVE_NAME,
                    ScoreboardCriterion.DUMMY,
                    Text.literal("§6§l《幸運方塊活動》"),
                    ScoreboardCriterion.RenderType.INTEGER,
                    true,
                    null
            );
            scoreboard.setObjectiveSlot(ScoreboardDisplaySlot.SIDEBAR, objective);
        }

        // 1. 先清除該玩家之前的舊行（防止文字變動時留下殘影）
        if (playerLines.containsKey(player.getUuid())) {
            for (String oldLine : playerLines.get(player.getUuid())) {
                scoreboard.removeScore(ScoreHolder.fromName(oldLine), objective);
            }
        }

        // 2. 定義新的內容
        List<String> newLines = new ArrayList<>();
        newLines.add("§f "); // 分數 6
        newLines.add("§7現階段: §e" + stage); // 分數 5
        newLines.add("§7遊戲時間: §a" + time + "s"); // 分數 4
        newLines.add("§7破壞數量: §d" + brokenCount + "個"); // 分數 3
        newLines.add("§7邊界大小: §b" + borderSize + "x" + borderSize); // 分數 2
        newLines.add("§7當前分組: §f" + group); // 分數 1
        newLines.add("§8 "); // 分數 0

        // 3. 設定每一行（使用 ScoreHolder）
        for (int i = 0; i < newLines.size(); i++) {
            String lineText = newLines.get(i);
            int scoreValue = newLines.size() - 1 - i; // 計算分數，讓第一筆在最上面

            // 關鍵：使用文字內容作為 ScoreHolder，而不是 player 物件
            scoreboard.getOrCreateScore(ScoreHolder.fromName(lineText), objective).setScore(scoreValue);
        }

        // 4. 更新暫存，下次更新時清理
        playerLines.put(player.getUuid(), newLines);
    }

    private static void setLine(ServerPlayerEntity player, ScoreboardObjective obj, String text, int score) {
        player.getScoreboard().getOrCreateScore(player, obj).setScore(score);
        // 如果要動態更新文字且不閃爍，通常會搭配 Team 的 Prefix/Suffix
        // 這裡先提供基礎寫法
    }
}
