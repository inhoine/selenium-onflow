package com.example.seleniumtestng.models;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PickupDetail {
    private final String pickupId;
    private final String pickupCode;
    private final String pickupType;
    private final int totalOrder;
    private final List<String> basketCodes;

    public PickupDetail(String pickupId, String pickupCode, String pickupType, int totalOrder, List<String> basketCodes) {
        this.pickupId = pickupId;
        this.pickupCode = pickupCode;
        this.pickupType = pickupType;
        this.totalOrder = totalOrder;
        this.basketCodes = basketCodes == null
                ? Collections.emptyList()
                : Collections.unmodifiableList(new ArrayList<>(basketCodes));
    }

    public String pickupId() {
        return pickupId;
    }

    public String pickupCode() {
        return pickupCode;
    }

    public String pickupType() {
        return pickupType;
    }

    public int totalOrder() {
        return totalOrder;
    }

    public List<String> basketCodes() {
        return basketCodes;
    }
}
