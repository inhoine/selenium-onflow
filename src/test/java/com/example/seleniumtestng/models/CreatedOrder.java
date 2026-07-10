package com.example.seleniumtestng.models;

public final class CreatedOrder {
    private final String orderNumber;
    private final String trackingCode;

    public CreatedOrder(String orderNumber, String trackingCode) {
        this.orderNumber = orderNumber;
        this.trackingCode = trackingCode;
    }

    public String orderNumber() {
        return orderNumber;
    }

    public String trackingCode() {
        return trackingCode;
    }
}
