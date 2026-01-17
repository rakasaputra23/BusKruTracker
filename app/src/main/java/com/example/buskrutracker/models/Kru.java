package com.example.buskrutracker.models;

import com.google.gson.annotations.SerializedName;

public class Kru {

    @SerializedName("id")
    private int id;

    @SerializedName("driver")
    private String driver;

    @SerializedName("username")
    private String username;

    @SerializedName("status")
    private String status;

    @SerializedName("token")
    private String token;

    // Constructor
    public Kru() {}

    public Kru(int id, String driver, String username, String status) {
        this.id = id;
        this.driver = driver;
        this.username = username;
        this.status = status;
    }

    // Getters & Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getDriver() {
        return driver;
    }

    public void setDriver(String driver) {
        this.driver = driver;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }
}