package com.example.diglet.Configuration;

import java.math.BigDecimal;
import java.text.NumberFormat;

public class Expense {
    private int id;
    private int userId;
    private int categoryId;
    private BigDecimal cost;
    private String description;
    private String day;
    private String month;
    private String year;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userID) {
        this.userId = userID;
    }

    public int getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(int categoryID) {
        this.categoryId = categoryID;
    }

    public BigDecimal getCost() {
        return cost;
    }

    public void setCost(BigDecimal cost) {
        this.cost = cost;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDay() {
        return day;
    }

    public void setDay(String day) {
        this.day = day;
    }

    public String getMonth() {
        return month;
    }

    public void setMonth(String month) {
        this.month = month;
    }

    public String getYear() {
        return year;
    }

    public void setYear(String year) {
        this.year = year;
    }

    @Override
    public String toString() {
        return NumberFormat.getCurrencyInstance().format(cost) + " " + description;
    }
}
