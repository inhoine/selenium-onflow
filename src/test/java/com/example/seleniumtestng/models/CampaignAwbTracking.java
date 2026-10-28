package com.example.seleniumtestng.models;

public class CampaignAwbTracking {
    private final String trackingCode;
    private final boolean packingDone;
    private final boolean awbPrint;

    public CampaignAwbTracking(String trackingCode, boolean packingDone, boolean awbPrint) {
        this.trackingCode = trackingCode;
        this.packingDone = packingDone;
        this.awbPrint = awbPrint;
    }

    public String trackingCode() {
        return trackingCode;
    }

    public boolean packingDone() {
        return packingDone;
    }

    public boolean awbPrint() {
        return awbPrint;
    }
}
