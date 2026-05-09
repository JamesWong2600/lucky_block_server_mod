package org.auto.lucky_block_server_mod.scoreboard;

import net.minecraft.scoreboard.*;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.auto.lucky_block_server_mod.Lucky_block_server_mod;
import org.auto.lucky_block_server_mod.cache.PlayerData;
import org.jetbrains.annotations.Nullable;

import java.util.*;

import static org.auto.lucky_block_server_mod.Lucky_block_server_mod.currentMspt;
import static org.auto.lucky_block_server_mod.performance_stat.NetWorkStuff.getPlayerPing;

public class EventScoreboard {
    private static final String OBJECTIVE_NAME = "lucky_block_objective";

    private static final Map<UUID, List<String>> playerLines = new HashMap<>();

    public static void updateScoreboard(ServerPlayerEntity player, String stage, Integer time, Integer brokenCount, Integer borderSize, Integer GlobalPlayerAmount, Integer GroupPlayerAmount, @Nullable String group) {
        Scoreboard scoreboard = player.getScoreboard();
        ScoreboardObjective objective = scoreboard.getNullableObjective(OBJECTIVE_NAME);
        final ScoreboardObjective finalObjective = objective;
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

        // --- 修正 1：獲取該 Objective 下的所有分數並清除 ---
        // 在 1.21.4 中，使用 getScoreboardEntries(objective) 來獲取
        // 並透過方法刪除舊的內容
        scoreboard.getScoreboardEntries(objective).forEach(entry -> {
            // 使用 entry.holder() 獲取 ScoreHolder
            scoreboard.removeScore(ScoreHolder.fromName(entry.owner()), finalObjective);
        });

        // 構建新內容
        List<String> lines = new ArrayList<>();
        lines.add("§f ");
        lines.add("§f現階段: §a" + stage);
        if (time != null) lines.add("§f遊戲時間: §a" + time + "s");
        if (brokenCount != null) lines.add("§f破壞數量: §a" + brokenCount + "個");
        if (brokenCount != null) lines.add("§f殺敵數量: §a" + brokenCount + "個");
        if (borderSize != null) lines.add("§f邊界大小: §a" + borderSize + "x" + borderSize);
        if (group != null) lines.add("§f當前分組: §a" + group);
        if (GlobalPlayerAmount != null) lines.add("§f全域人數: §a" + GlobalPlayerAmount);
        if (GroupPlayerAmount != null) lines.add("§f分組人數: §a" + GroupPlayerAmount);
        if (currentMspt != -1) lines.add("§fMSPT: §a" + (int) currentMspt);
        lines.add("§f網路延遲: §a" + getPlayerPing(player) + "ms");
        lines.add("§8 ");

        // --- 修正 2：寫入新行 ---
        for (int i = 0; i < lines.size(); i++) {
            int scoreValue = lines.size() - i;
            setLine(player, objective, lines.get(i), scoreValue);
        }
    }

    private static void setLine(ServerPlayerEntity player, ScoreboardObjective obj, String text, int score) {
        // 1.21.4 使用 ScoreHolder.fromName(text) 來建立或獲取分數
        player.getScoreboard().getOrCreateScore(ScoreHolder.fromName(text), obj).setScore(score);
    }
}
