package com.example.diglet.Configuration;

import java.io.Serializable;

public class Category implements Serializable {
    private int id;
    private int userId;
    private String category;

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public int getUserID() {
        return userId;
    }

    public void setUserId(int userID) {
        this.userId = userID;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    @Override
    public String toString() {
        return category;
    }
}
