package com.example.seleniumtestng.models;

public class OrderAwbInfo {
    private final String trackingCode;
    private final String externalTrackingCode;
    private final String carrierTrackingCode;

    public OrderAwbInfo(String trackingCode, String externalTrackingCode, String carrierTrackingCode) {
        this.trackingCode = trackingCode;
        this.externalTrackingCode = externalTrackingCode;
        this.carrierTrackingCode = carrierTrackingCode;
    }

    public String trackingCode() {
        return trackingCode;
    }

    public String externalTrackingCode() {
        return externalTrackingCode;
    }

    public String carrierTrackingCode() {
        return carrierTrackingCode;
    }
}
