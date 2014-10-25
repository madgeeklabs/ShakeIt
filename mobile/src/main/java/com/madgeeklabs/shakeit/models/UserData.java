package com.madgeeklabs.shakeit.models;

/**
 * Created by goofyahead on 10/25/14.
 */
public class UserData {
    private String username;
    private String operation;
    private double ammount;
    private String timeStamp;
    private String image;

    public UserData(String username, String operation, double ammount, String timeStamp) {
        this.username = username;
        this.operation = operation;
        this.ammount = ammount;
        this.timeStamp = timeStamp;
    }

    public String getUsername() {
        return username;
    }

    public String getImage() {
        return image;
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

    public String getTimeStamp() {
        return timeStamp;
    }

    public void setTimeStamp(String timeStamp) {
        this.timeStamp = timeStamp;
    }
}
