package com.tomato.utils;

public class State {
    private static final State instance = new State();
    private String novelNameToSearch;
    private volatile boolean autoReading = true; // 新增：自动阅读状态标志，使用 volatile 保证线程可见性

    private State() {}

    public static State getInstance() {
        return instance;
    }

    public String getNovelNameToSearch() {
        return novelNameToSearch;
    }

    /**
     * 储存前段传入的字符串，用于后续的搜索
     * @param novelNameToSearch 输入的字符串
     */
    public void setNovelNameToSearch(String novelNameToSearch) {
        this.novelNameToSearch = novelNameToSearch;
    }

    /**
     * 检查当前是否处于自动阅读状态。
     * @return 如果是，返回 true。
     */
    public boolean isAutoReading() {
        return autoReading;
    }

    /**
     * 设置自动阅读的状态。
     * @param autoReading true 为开启，false 为关闭。
     */
    public void setAutoReading(boolean autoReading) {
        this.autoReading = autoReading;
    }
}
