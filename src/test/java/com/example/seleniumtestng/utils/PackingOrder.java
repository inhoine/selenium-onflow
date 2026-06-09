package com.example.seleniumtestng.utils;

import java.util.List;

public class PackingOrder {
    private final String trackingCode;
    private final List<PickupItem> items;

    public PackingOrder(String trackingCode, List<PickupItem> items) {
        this.trackingCode = trackingCode;
        this.items = items;
    }

    public String trackingCode() {
        return trackingCode;
    }

    public List<PickupItem> items() {
        return items;
    }
}
