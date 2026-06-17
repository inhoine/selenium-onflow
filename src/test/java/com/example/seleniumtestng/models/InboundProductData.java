package com.example.seleniumtestng.models;

public class InboundProductData {
    private String sku;
    private int quantity;

    public InboundProductData() {
    }

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku == null ? null : sku.trim();
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }
}
