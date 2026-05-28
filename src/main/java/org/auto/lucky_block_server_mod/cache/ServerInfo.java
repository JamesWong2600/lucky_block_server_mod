package org.auto.lucky_block_server_mod.cache;

public class ServerInfo {
    // timeRunned: 游戲已進行時間 (單位：毫秒)
    private long timeRunned = 0;

    // group: 當前可用的分組數目或當前使用的最大分組ID
    private int group = 0;

    // session: 當前遊戲會話的編號或唯一標識符
    private int session = 1; // 通常從 1 開始計數
    private boolean isBorderShrinkingStarted = false;

    public boolean isBorderShrinkingStarted() {
        return isBorderShrinkingStarted;
    }

    // Setter
    public void setBorderShrinkingStarted(boolean started) {
        this.isBorderShrinkingStarted = started;
    }

    public ServerInfo() {}

    /** @return 游戲已進行時間 (毫秒) */
    public long getTimeRunned() { return timeRunned; }
    public void setTimeRunned(long timeRunned) { this.timeRunned = timeRunned; }

    /** @return 當前分組數目 */
    public int getGroup() { return group; }
    public void setGroup(int group) { this.group = group; }

    /** @return 當前遊戲會話編號 */
    public int getSession() { return session; }
    public void setSession(int session) { this.session = session; }
}