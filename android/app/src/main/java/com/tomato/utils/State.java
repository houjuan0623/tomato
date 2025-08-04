package com.tomato.utils;

public class State {
    private static final State instance = new State();
    private String novelNameToSearch;

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
}
