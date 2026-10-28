package com.example.seleniumtestng.models;

import java.util.List;

public class CampaignAwbList {
    private final List<String> awbUrls;
    private final List<CampaignAwbTracking> trackings;

    public CampaignAwbList(List<String> awbUrls, List<CampaignAwbTracking> trackings) {
        this.awbUrls = awbUrls;
        this.trackings = trackings;
    }

    public List<String> awbUrls() {
        return awbUrls;
    }

    public List<CampaignAwbTracking> trackings() {
        return trackings;
    }
}
