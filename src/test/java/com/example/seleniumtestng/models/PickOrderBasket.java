package com.example.seleniumtestng.models;

public class PickOrderBasket {
    private final String code;
    private final int statusId;
    private final String statusName;
    private final String trackingCode;

    public PickOrderBasket(String code, int statusId, String statusName, String trackingCode) {
        this.code = code;
        this.statusId = statusId;
        this.statusName = statusName;
        this.trackingCode = trackingCode;
    }

    public String code() {
        return code;
    }

    public int statusId() {
        return statusId;
    }

    public String statusName() {
        return statusName;
    }

    public String trackingCode() {
        return trackingCode;
    }
}
