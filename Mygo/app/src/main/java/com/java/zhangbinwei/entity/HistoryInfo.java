package com.java.zhangbinwei.entity;

public class HistoryInfo {
    private int history_id;
    private String uniquekey;
    private String username;
    private String new_json;

    public HistoryInfo(int history_id, String uniquekey, String username, String new_json) {
        this.history_id = history_id;
        this.uniquekey = uniquekey;
        this.username = username;
        this.new_json = new_json;
    }

    public int getHistory_id() {
        return history_id;
    }

    public void setHistory_id(int history_id) {
        this.history_id = history_id;
    }

    public String getUniquekey() {
        return uniquekey;
    }

    public void setUniquekey(String uniquekey) {
        this.uniquekey = uniquekey;
    }
    public String getUsername() {
        return username;
    }
    public void setUsername(String username) {
        this.username = username;
    }
    public String getNew_json() {
        return new_json;
    }
    public void setNew_json(String new_json) {
        this.new_json = new_json;
    }
}
