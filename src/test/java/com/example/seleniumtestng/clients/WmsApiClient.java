package com.example.seleniumtestng.clients;

import com.example.seleniumtestng.models.CampaignAwbList;
import com.example.seleniumtestng.models.OrderAwbInfo;
import com.example.seleniumtestng.models.POSku;
import com.example.seleniumtestng.models.PackingOrder;
import com.example.seleniumtestng.models.PickOrderBasket;
import com.example.seleniumtestng.models.PickupDetail;
import com.example.seleniumtestng.models.PickupItem;
import com.example.seleniumtestng.models.RmaReturnOrder;
import java.util.List;

/**
 * Unified Facade client for all WMS REST API interactions.
 * Delegates domain-specific API calls to dedicated clients:
 * - {@link InboundApiClient}: PO receiving, box querying, putaway
 * - {@link PickingApiClient}: Pickup details, trolley/tote management, picking execution
 * - {@link PackingApiClient}: Packing orders, campaign AWBs, pickup discovery, handover
 * - {@link RmaApiClient}: RMA returns, tracking codes, serial numbers
 *
 * Provides 100% backward compatibility for existing tests and flows while
 * exposing clean domain-scoped accessors via inbound(), picking(), packing(), rma().
 */
public class WmsApiClient {

    private final InboundApiClient inboundClient;
    private final PickingApiClient pickingClient;
    private final PackingApiClient packingClient;
    private final RmaApiClient rmaClient;

    public WmsApiClient() {
        this.inboundClient = new InboundApiClient();
        this.pickingClient = new PickingApiClient();
        this.packingClient = new PackingApiClient();
        this.rmaClient = new RmaApiClient();
    }

    // --- Domain Sub-Client Accessors ---

    public InboundApiClient inbound() {
        return inboundClient;
    }

    public PickingApiClient picking() {
        return pickingClient;
    }

    public PackingApiClient packing() {
        return packingClient;
    }

    public RmaApiClient rma() {
        return rmaClient;
    }

    // --- Inbound API Delegates ---

    public void receivedPoAtWarehouse(String inboundCode, String token) {
        inboundClient.receivedPoAtWarehouse(inboundCode, token);
    }

    public List<POSku> getPoSkus(String poCode, String token) {
        return inboundClient.getPoSkus(poCode, token);
    }

    public List<String> getPoBoxes(String poCode, String token) {
        return inboundClient.getPoBoxes(poCode, token);
    }

    public List<String> getPendingPoBoxes(String poCode, String token) {
        return inboundClient.getPendingPoBoxes(poCode, token);
    }

    public List<String> getPendingPoBoxes(String poCode, String token, int maxPendingBoxes) {
        return inboundClient.getPendingPoBoxes(poCode, token, maxPendingBoxes);
    }

    public List<POSku> findInspectionProducts(String poCode, String boxCode, String token) {
        return inboundClient.findInspectionProducts(poCode, boxCode, token);
    }

    public int updatePutaway(String inboundCode, String token) {
        return inboundClient.updatePutaway(inboundCode, token);
    }

    // --- Picking & Trolley API Delegates ---

    public List<PickupItem> getPickupDetail(String pickupId, String token) {
        return pickingClient.getPickupDetail(pickupId, token);
    }

    public PickupDetail getPickupDetailInfo(String pickupCode, String token) {
        return pickingClient.getPickupDetailInfo(pickupCode, token);
    }

    public String getPickingTrolleyId(String pickupCode, String token) {
        return pickingClient.getPickingTrolleyId(pickupCode, token);
    }

    public void assignBasketsToPickOrder(String trolleyId, List<String> toteCodes, String token) {
        pickingClient.assignBasketsToPickOrder(trolleyId, toteCodes, token);
    }

    public List<PickOrderBasket> getPickOrderBaskets(String pickupCode, String token) {
        return pickingClient.getPickOrderBaskets(pickupCode, token);
    }

    public void mapTrolleyPicking(String pickupCode, String trolleyCode, String token) {
        pickingClient.mapTrolleyPicking(pickupCode, trolleyCode, token);
    }

    public void pickAllProductsInPickup(String pickupCode, String token) {
        pickingClient.pickAllProductsInPickup(pickupCode, token);
    }

    public void commitPickingPickup(String pickupCode, String trolleyCode, String token) {
        pickingClient.commitPickingPickup(pickupCode, trolleyCode, token);
    }

    // --- Packing, Campaign & Order API Delegates ---

    public List<PackingOrder> getPickupPackingOrders(String pickupId, String token) {
        return packingClient.getPickupPackingOrders(pickupId, token);
    }

    public CampaignAwbList getCampaignAwbList(String pickupCode, String token) {
        return packingClient.getCampaignAwbList(pickupCode, token);
    }

    public OrderAwbInfo getOrderAwbInfo(String trackingCode, String token) {
        return packingClient.getOrderAwbInfo(trackingCode, token);
    }

    public int getOrderStatusId(String trackingCode, String token) {
        return packingClient.getOrderStatusId(trackingCode, token);
    }

    public List<String> getOrderMainMaterialCodes(String trackingCode, String token) {
        return packingClient.getOrderMainMaterialCodes(trackingCode, token);
    }

    public List<String> resolveMaterialScanCodes(String trackingCode, String token) {
        return packingClient.resolveMaterialScanCodes(trackingCode, token);
    }

    public String resolveMaterialScanCode(String trackingCode, String token) {
        return packingClient.resolveMaterialScanCode(trackingCode, token);
    }

    public boolean isPickupReadyForPacking(String pickupCode, String token) {
        return packingClient.isPickupReadyForPacking(pickupCode, token);
    }

    public String findAvailablePackingPickup(String token) {
        return packingClient.findAvailablePackingPickup(token);
    }

    public String findAvailablePackingPickup(String token, int lookbackDays) {
        return packingClient.findAvailablePackingPickup(token, lookbackDays);
    }

    public String getOrderPickupCode(String trackingCode, String token) {
        return packingClient.getOrderPickupCode(trackingCode, token);
    }

    public String handoverPackedOrders(
            List<PackingOrder> packingOrders,
            String courierCode,
            String driverName,
            String driverPhone,
            String driverLicenseNumber,
            String token) {
        return packingClient.handoverPackedOrders(
                packingOrders,
                courierCode,
                driverName,
                driverPhone,
                driverLicenseNumber,
                token);
    }

    // --- RMA Return API Delegates ---

    public List<RmaReturnOrder> getRmaReturnOrders(String rmaCode, String token) {
        return rmaClient.getRmaReturnOrders(rmaCode, token);
    }

    public List<String> getRmaTrackingCodes(String rmaCode, String token) {
        return rmaClient.getRmaTrackingCodes(rmaCode, token);
    }
}
