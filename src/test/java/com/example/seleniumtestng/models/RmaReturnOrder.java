package com.example.seleniumtestng.models;

import java.util.List;

public record RmaReturnOrder(
        String trackingCode,
        List<RmaInspectionItem> items) {
}
