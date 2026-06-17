package com.example.seleniumtestng.models;

import java.util.List;

public class PickupItem {
    private final String trackingCode;
    private final String partnerCode;
    private final String goodsCode;
    private final List<String> barcodes;
    private final int quantitySold;
    private final int quantityPick;

    public PickupItem(String trackingCode, String partnerCode, String goodsCode, List<String> barcodes, int quantitySold, int quantityPick) {
        this.trackingCode = trackingCode;
        this.partnerCode = partnerCode;
        this.goodsCode = goodsCode;
        this.barcodes = barcodes;
        this.quantitySold = quantitySold;
        this.quantityPick = quantityPick;
    }

    public String trackingCode() {
        return trackingCode;
    }

    public String partnerCode() {
        return partnerCode;
    }

    public String goodsCode() {
        return goodsCode;
    }

    public List<String> barcodes() {
        return barcodes;
    }

    public int quantitySold() {
        return quantitySold;
    }

    public int quantityPick() {
        return quantityPick;
    }
}
