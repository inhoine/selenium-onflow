package com.example.seleniumtestng.models;

import java.util.ArrayList;
import java.util.List;

public class InboundPackageData {
    private List<InboundProductData> products = new ArrayList<>();

    public InboundPackageData() {
    }

    public InboundPackageData(List<InboundProductData> products) {
        setProducts(products);
    }

    public List<InboundProductData> getProducts() {
        return products;
    }

    public void setProducts(List<InboundProductData> products) {
        this.products = products == null ? new ArrayList<>() : new ArrayList<>(products);
    }
}
