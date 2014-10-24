package com.madgeeklabs.shakeit.models;

/**
 * Created by goofyahead on 10/24/14.
 */
public class User {
    private String username;
    private String operation;
    private double ammount;

    public User(String username, String operation, double ammount) {
        this.username = username;
        this.operation = operation;
        this.ammount = ammount;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getOperation() {
        return operation;
    }

    public void setOperation(String operation) {
        this.operation = operation;
    }

    public double getAmmount() {
        return ammount;
    }

    public void setAmmount(double ammount) {
        this.ammount = ammount;
    }
}
