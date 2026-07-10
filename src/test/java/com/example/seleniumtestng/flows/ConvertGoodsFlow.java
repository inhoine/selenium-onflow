package com.example.seleniumtestng.flows;

import com.example.seleniumtestng.models.ConvertGoodsRequestData;
import com.example.seleniumtestng.pages.ConvertGoodsPage;
import org.openqa.selenium.WebDriver;

public class ConvertGoodsFlow {
    private final WebDriver driver;

    public ConvertGoodsFlow(WebDriver driver) {
        this.driver = driver;
    }

    public void createConvertGoodsRequest(ConvertGoodsRequestData data) {
        ConvertGoodsPage convertGoodsPage = new ConvertGoodsPage(driver);
        convertGoodsPage.open();
        if (convertGoodsPage.openExistingRequestDetailIfPresent(data.productCode())) {
            convertGoodsPage.approveRequest(data.approvalReason());
            convertGoodsPage.putawayApprovedGoods(data.putawayLocation());
            return;
        }
        convertGoodsPage.createRequest(data);
        convertGoodsPage.openCreatedRequestDetail(data.productCode());
        convertGoodsPage.approveRequest(data.approvalReason());
        convertGoodsPage.putawayApprovedGoods(data.putawayLocation());
    }
}
