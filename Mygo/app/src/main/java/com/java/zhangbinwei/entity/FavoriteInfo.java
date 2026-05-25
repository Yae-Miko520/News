package com.java.zhangbinwei.entity;

public class FavoriteInfo {
    private int favorite_id;
    private String uniquekey;
    private String username;
    private String new_json;

    public FavoriteInfo(int favorite_id, String uniquekey, String username, String new_json) {
        this.favorite_id = favorite_id;
        this.uniquekey = uniquekey;
        this.username = username;
        this.new_json = new_json;
    }

    // Getters and setters
    public int getFavorite_id() { return favorite_id; }
    public void setFavorite_id(int favorite_id) { this.favorite_id = favorite_id; }
    public String getUniquekey() { return uniquekey; }
    public void setUniquekey(String uniquekey) { this.uniquekey = uniquekey; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getNew_json() { return new_json; }
    public void setNew_json(String new_json) { this.new_json = new_json; }
}