package com.example.seleniumtestng.models;

public class POSku {
    private final String boxCode;
    private final int quantityInbound;
    private final String partnerCode;
    private final Number goodsW;
    private final Number goodsD;
    private final Number goodsH;
    private final Number goodsWeight;

    public POSku(String boxCode, int quantityInbound, String partnerCode, Number goodsW, Number goodsD, Number goodsH, Number goodsWeight) {
        this.boxCode = boxCode;
        this.quantityInbound = quantityInbound;
        this.partnerCode = partnerCode;
        this.goodsW = goodsW;
        this.goodsD = goodsD;
        this.goodsH = goodsH;
        this.goodsWeight = goodsWeight;
    }

    public String boxCode() {
        return boxCode;
    }

    public int quantityInbound() {
        return quantityInbound;
    }

    public String partnerCode() {
        return partnerCode;
    }

    public Number goodsW() {
        return goodsW;
    }

    public Number goodsD() {
        return goodsD;
    }

    public Number goodsH() {
        return goodsH;
    }

    public Number goodsWeight() {
        return goodsWeight;
    }
}
