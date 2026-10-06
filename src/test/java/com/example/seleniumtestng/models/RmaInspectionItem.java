package com.example.seleniumtestng.models;

import java.util.List;

public record RmaInspectionItem(
        String partnerCode,
        String goodsCode,
        List<String> barcodes,
        int quantity,
        List<String> serialCodes) {
}
