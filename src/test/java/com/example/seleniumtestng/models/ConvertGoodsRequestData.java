package com.example.seleniumtestng.models;

public class ConvertGoodsRequestData {
    private final String productCode;
    private final int quantity;
    private final String currentGoodsType;
    private final String targetGoodsType;
    private final String currentLocation;
    private final String pendingLocation;
    private final String reason;
    private final String approvalReason;
    private final String putawayLocation;

    public ConvertGoodsRequestData(
            String productCode,
            int quantity,
            String currentGoodsType,
            String targetGoodsType,
            String currentLocation,
            String pendingLocation,
            String reason,
            String approvalReason,
            String putawayLocation) {
        this.productCode = productCode;
        this.quantity = quantity;
        this.currentGoodsType = currentGoodsType;
        this.targetGoodsType = targetGoodsType;
        this.currentLocation = currentLocation;
        this.pendingLocation = pendingLocation;
        this.reason = reason;
        this.approvalReason = approvalReason;
        this.putawayLocation = putawayLocation;
    }

    public String productCode() {
        return productCode;
    }

    public int quantity() {
        return quantity;
    }

    public String currentGoodsType() {
        return currentGoodsType;
    }

    public String targetGoodsType() {
        return targetGoodsType;
    }

    public String currentLocation() {
        return currentLocation;
    }

    public String pendingLocation() {
        return pendingLocation;
    }

    public String reason() {
        return reason;
    }

    public String approvalReason() {
        return approvalReason;
    }

    public String putawayLocation() {
        return putawayLocation;
    }
}
