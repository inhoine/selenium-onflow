package com.example.seleniumtestng.clients;

import com.example.seleniumtestng.config.ConfigReader;
import com.example.seleniumtestng.models.CampaignAwbList;
import com.example.seleniumtestng.models.CampaignAwbTracking;
import com.example.seleniumtestng.models.OrderAwbInfo;
import com.example.seleniumtestng.models.PackingOrder;
import com.example.seleniumtestng.models.PickupItem;
import com.example.seleniumtestng.utils.Log;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Client for WMS Packing, Campaign & Order REST API interactions:
 * - Query packing orders and items
 * - Fetch Campaign AWB lists
 * - Inspect order details and tracking status
 * - Discover available pickups for packing
 * - Execute order handover to couriers
 */
public class PackingApiClient extends BaseApiClient {

    private final PickingApiClient pickingClient;

    public PackingApiClient() {
        super();
        this.pickingClient = new PickingApiClient();
    }

    public List<PackingOrder> getPickupPackingOrders(String pickupId, String token) {
        JsonNode json = request("GET", "/v1/pickup/detail/" + pickupId, token, null, true);
        List<PackingOrder> orders = new ArrayList<>();
        Map<String, List<String>> serialCodeCache = new HashMap<>();
        Map<String, Integer> serialCodeCursor = new HashMap<>();
        for (JsonNode order : json.path("data").path("pickup_orders")) {
            String trackingCode = order.path("tracking_code").asText(null);
            List<PickupItem> items = new ArrayList<>();
            for (JsonNode item : order.path("list_items")) {
                PickupItem pickupItem = pickingClient.toPickupItem(trackingCode, item);
                if (pickupItem.isSerialCode()) {
                    pickupItem = pickingClient.withAllocatedSerialCodes(pickupItem, token, serialCodeCache,
                            serialCodeCursor);
                }
                items.add(pickupItem);
            }
            orders.add(new PackingOrder(trackingCode, items));
        }
        return orders;
    }

    public CampaignAwbList getCampaignAwbList(String pickupCode, String token) {
        JsonNode json = request("GET", "/v1/campaign/awb/list/" + encode(pickupCode), token, null, true);
        JsonNode data = json.path("data");
        List<String> awbUrls = readStringList(data.path("list_awb"));
        List<CampaignAwbTracking> trackings = new ArrayList<>();
        for (JsonNode tracking : data.path("list_trackings")) {
            String trackingCode = textOrNull(tracking, "tracking_code", "trackingCode");
            if (trackingCode == null || trackingCode.isBlank()) {
                continue;
            }
            trackings.add(new CampaignAwbTracking(
                    trackingCode,
                    firstBoolean(tracking, "is_packing_done", "isPackingDone"),
                    firstBoolean(tracking, "is_awb_print", "isAwbPrint")));
        }
        System.out.println("Campaign AWB API: pickup="
                + pickupCode
                + ", pdfs="
                + awbUrls.size()
                + ", trackings="
                + trackings.size());
        return new CampaignAwbList(awbUrls, trackings);
    }

    public OrderAwbInfo getOrderAwbInfo(String trackingCode, String token) {
        JsonNode json = request("GET", "/v1/order/detail/" + encode(trackingCode), token, null, true);
        JsonNode order = firstPresent(json.path("data").path("order_id"), json.path("data"), json);
        return new OrderAwbInfo(
                firstNonBlank(textOrNull(order, "tracking_code", "trackingCode"), trackingCode),
                textOrNull(order, "external_tracking_code", "externalTrackingCode"),
                textOrNull(order, "carrier_tracking_code", "carrierTrackingCode", "awb_code", "awbCode"));
    }

    public int getOrderStatusId(String trackingCode, String token) {
        try {
            JsonNode json = request("GET", "/v1/order/detail/" + encode(trackingCode), token, null, true);
            JsonNode order = firstPresent(json.path("data").path("order_id"), json.path("data"), json);
            return firstInt(order, "status_id", "statusId", "status", "order_status_id");
        } catch (Exception e) {
            Log.warn("Failed to get order status for tracking {}: {}", trackingCode, e.getMessage());
            return 0;
        }
    }

    /**
     * Calls GET /v1/packing/material/{trackingCode} to fetch packaging materials
     * for the order.
     * Extracts all unique material codes defined in
     * packaging_standard.main_material of each product.
     */
    /**
     * Retrieves packaging standard & material data for an order from WMS APIs.
     * Tries /v1/packing/material/{trackingCode} first, then
     * /v1/packaging/orders/{trackingCode}/standards.
     */
    public JsonNode getOrderPackagingData(String trackingCode, String token) {
        // 1. Try /v1/packing/material/{trackingCode}
        try {
            JsonNode json = request("GET", "/v1/packing/material/" + encode(trackingCode), token, null, true);
            JsonNode data = json.path("data");
            if (!data.isMissingNode() && !data.isNull()) {
                JsonNode items = data.path("items");
                JsonNode products = data.path("products");
                if ((items.isArray() && items.size() > 0) || (products.isArray() && products.size() > 0)) {
                    Log.info("Retrieved packaging data from /v1/packing/material/{}", trackingCode);
                    return json;
                }
            }
        } catch (Exception e) {
            Log.warn("Query /v1/packing/material/{} failed: {}", trackingCode, e.getMessage());
        }

        // 2. Try /v1/packaging/orders/{trackingCode}/standards
        try {
            JsonNode json = request("GET", "/v1/packaging/orders/" + encode(trackingCode) + "/standards", token, null,
                    true);
            JsonNode data = json.path("data");
            if (!data.isMissingNode() && !data.isNull()) {
                Log.info("Retrieved packaging data from /v1/packaging/orders/{}/standards", trackingCode);
                return json;
            }
        } catch (Exception e) {
            Log.warn("Query /v1/packaging/orders/{}/standards failed: {}", trackingCode, e.getMessage());
        }

        // 3. Fallback to /v1/packing/material response even if items/products are empty
        try {
            return request("GET", "/v1/packing/material/" + encode(trackingCode), token, null, true);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Extracts all unique active material codes from packaging standard or products.
     */
    public List<String> getOrderMainMaterialCodes(String trackingCode, String token) {
        JsonNode json = getOrderPackagingData(trackingCode, token);
        if (json == null) {
            return Collections.emptyList();
        }
        List<String> codes = new ArrayList<>();
        JsonNode data = json.path("data");

        // From items[]
        JsonNode items = data.path("items");
        if (items.isArray()) {
            for (JsonNode item : items) {
                codes.addAll(extractMainMaterialCodes(item.path("main_material")));
            }
        }

        // From products[]
        JsonNode products = data.path("products");
        if (products.isArray()) {
            for (JsonNode product : products) {
                codes.addAll(extractMainMaterialCodes(product.path("packaging_standard").path("main_material")));
            }
        }

        // Fallback to data.material_code
        if (codes.isEmpty()) {
            JsonNode materialCodeList = data.path("material_code");
            if (materialCodeList.isArray()) {
                for (JsonNode codeNode : materialCodeList) {
                    String code = codeNode.asText(null);
                    if (code != null && !code.isBlank() && !codes.contains(code)) {
                        codes.add(code);
                    }
                }
            }
        }
        return codes;
    }

    /**
     * Resolves the list of material codes to scan for an order:
     * - Case 1: Standard API trả ra 1 id tiêu chuẩn đóng gói (ví dụ items.size() ==
     * 1 hoặc products.size() == 1):
     * Scan TOÀN BỘ code thuộc main_material của tiêu chuẩn đó (nếu có 1 scan 1, nếu
     * có 2 scan 2).
     * - Case 2: Nhiều tiêu chuẩn đóng gói (ví dụ 2 goods_id hoặc 2 items):
     * Chỉ cần chọn 1 trong các code thuộc main_material để scan.
     */
    public List<String> resolveMaterialScanCodes(String trackingCode, String token) {
        JsonNode json = getOrderPackagingData(trackingCode, token);
        if (json == null) {
            Log.warn("No packaging data found for trackingCode {}. Falling back to default.", trackingCode);
            return List.of(ConfigReader.getOrDefault("DEFAULT_PACKING_MATERIAL_CODE", "40x20x20"));
        }

        JsonNode data = json.path("data");

        // ── 1. Kiểm tra cấu trúc data.items (API packaging standards) ──
        JsonNode items = data.path("items");
        if (items.isArray() && items.size() > 0) {
            int standardCount = items.size();
            Log.info("Order {} packaging standards API returned {} item(s)", trackingCode, standardCount);

            if (standardCount == 1) {
                // Chỉ trả ra 1 id -> scan toàn bộ code thuộc main_material có auto_scan == false (nếu có 1 scan 1, nếu có 2 scan 2)
                JsonNode singleItem = items.get(0);
                int standardId = singleItem.path("id").asInt(0);
                String standardName = singleItem.path("name").asText("");
                List<String> codes = extractMainMaterialCodes(singleItem.path("main_material"));
                Log.info("Order {} has 1 packaging standard [id={}, name='{}']. Required scan codes (auto_scan=false): {}",
                        trackingCode, standardId, standardName, codes);
                return codes;
            } else {
                // Có nhiều hơn 1 tiêu chuẩn đóng gói -> chỉ cần chọn 1 trong các code thuộc main_material có auto_scan == false
                List<String> allCodes = new ArrayList<>();
                for (JsonNode item : items) {
                    allCodes.addAll(extractMainMaterialCodes(item.path("main_material")));
                }
                if (!allCodes.isEmpty()) {
                    String selected = allCodes.get(0);
                    Log.info("Order {} has {} packaging standards. Selected 1 code to scan (auto_scan=false): {}",
                            trackingCode, standardCount, selected);
                    return List.of(selected);
                } else {
                    Log.info("Order {} has {} packaging standards, but all materials have auto_scan=true -> không cần scan NVL",
                            trackingCode, standardCount);
                    return Collections.emptyList();
                }
            }
        }

        // ── 2. Kiểm tra cấu trúc data.products (API packing material với products[]) ──
        JsonNode products = data.path("products");
        if (products.isArray() && products.size() > 0) {
            int productCount = products.size();
            Log.info("Order {} packing material API returned {} product(s)/goods_id", trackingCode, productCount);

            if (productCount == 1) {
                // Chỉ có 1 goods_id (1 tiêu chuẩn) -> scan toàn bộ code thuộc main_material có auto_scan == false
                JsonNode singleProduct = products.get(0);
                JsonNode standard = singleProduct.path("packaging_standard");
                int standardId = standard.path("id").asInt(0);
                String standardName = standard.path("name").asText("");
                List<String> codes = extractMainMaterialCodes(standard.path("main_material"));
                Log.info("Order {} has 1 product [standard id={}, name='{}']. Required scan codes (auto_scan=false): {}",
                        trackingCode, standardId, standardName, codes);
                return codes;
            } else {
                // Có 2 (hoặc nhiều hơn) goods_id -> chỉ cần chọn 1 trong các code thuộc main_material có auto_scan == false
                List<String> allCodes = new ArrayList<>();
                for (JsonNode product : products) {
                    JsonNode standard = product.path("packaging_standard");
                    allCodes.addAll(extractMainMaterialCodes(standard.path("main_material")));
                }
                if (!allCodes.isEmpty()) {
                    String selected = allCodes.get(0);
                    Log.info("Order {} has multiple ({}) goods_id. Selected 1 code to scan (auto_scan=false): {}",
                            trackingCode, productCount, selected);
                    return List.of(selected);
                } else {
                    Log.info("Order {} has multiple ({}) goods_id, but all materials have auto_scan=true -> không cần scan NVL",
                            trackingCode, productCount);
                    return Collections.emptyList();
                }
            }
        }

        // ── 3. Fallback: data.material_code ──
        JsonNode materialCodeList = data.path("material_code");
        if (materialCodeList.isArray() && materialCodeList.size() > 0) {
            List<String> codes = new ArrayList<>();
            for (JsonNode codeNode : materialCodeList) {
                String code = codeNode.asText(null);
                if (code != null && !code.isBlank() && !codes.contains(code)) {
                    codes.add(code);
                }
            }
            if (!codes.isEmpty()) {
                Log.info("Order {} fallback to data.material_code: {}", trackingCode, codes);
                return codes;
            }
        }

        // ── 4. Fallback cuối cùng: default config ──
        String defaultCode = ConfigReader.getOrDefault("DEFAULT_PACKING_MATERIAL_CODE", "40x20x20");
        Log.warn("No main_material codes found for trackingCode {}. Falling back to default: {}", trackingCode,
                defaultCode);
        return List.of(defaultCode);
    }

    /**
     * Resolves a single material code to scan for an order (backward compatibility).
     */
    public String resolveMaterialScanCode(String trackingCode, String token) {
        List<String> codes = resolveMaterialScanCodes(trackingCode, token);
        return codes.isEmpty() ? ConfigReader.getOrDefault("DEFAULT_PACKING_MATERIAL_CODE", "40x20x20") : codes.get(0);
    }

    private List<String> extractMainMaterialCodes(JsonNode mainMaterials) {
        List<String> codes = new ArrayList<>();
        if (mainMaterials != null && mainMaterials.isArray()) {
            for (JsonNode material : mainMaterials) {
                boolean isActive = material.path("is_active").asBoolean(true);
                if (!isActive) {
                    continue;
                }
                boolean autoScan = material.path("auto_scan").asBoolean(false);
                String code = textOrNull(material, "code");
                if (autoScan) {
                    Log.info("Material [code={}, name='{}'] has auto_scan=true -> bỏ qua không cần scan",
                            code, material.path("name").asText(""));
                    continue;
                }
                if (code != null && !code.isBlank() && !codes.contains(code)) {
                    codes.add(code);
                }
            }
        }
        return codes;
    }

    public boolean isPickupReadyForPacking(String pickupCode, String token) {
        if (pickupCode == null || pickupCode.isBlank()) {
            return false;
        }
        try {
            JsonNode json = request("GET", "/v1/pickup/detail/" + encode(pickupCode), token, null, true);
            JsonNode data = json.path("data");
            if (data.isMissingNode() || data.isNull()) {
                Log.warn("Pickup {} detail returned empty data", pickupCode);
                return false;
            }
            JsonNode pickupList = data.path("pickup_list");
            int statusId = firstInt(pickupList, "status_id", "statusId", "status", "pickup_status_id");
            if (statusId == 0) {
                statusId = firstInt(data, "status_id", "statusId", "status", "pickup_status_id");
            }
            if (statusId == 0) {
                statusId = firstInt(json, "status_id", "statusId", "status", "pickup_status_id");
            }
            Log.info("Checking pickup {} status_id={}", pickupCode, statusId);

            if (statusId >= 605) {
                Log.info("Pickup {} status_id={} (>= 605: Đã hoàn tất đóng gói) -> bỏ qua", pickupCode, statusId);
                return false;
            }

            if (statusId >= 600 && statusId <= 604) {
                Log.info("Pickup {} status_id={} ({}) -> hợp lệ để thực thi pick/pack", pickupCode, statusId,
                        pickupStatusName(statusId));
                return true;
            }

            if (statusId == 0) {
                Log.info("Pickup {} detail statusId could not be determined. Considering ready", pickupCode);
                return true;
            }

            return false;
        } catch (Exception e) {
            Log.warn("Failed checking pickup readiness for {}: {}", pickupCode, e.getMessage());
            return false;
        }
    }

    public String findAvailablePackingPickup(String token) {
        int lookbackDays = Integer.parseInt(ConfigReader.getOrDefault("PICKUP_SEARCH_LOOKBACK_DAYS", "1"));
        return findAvailablePackingPickup(token, lookbackDays);
    }

    public String findAvailablePackingPickup(String token, int lookbackDays) {
        ZoneId zoneId = ZoneId.of("Asia/Ho_Chi_Minh");
        ZonedDateTime now = ZonedDateTime.now(zoneId);
        long fromTime = now.minusDays(lookbackDays).toLocalDate().atStartOfDay(zoneId).toEpochSecond();
        long toTime = now.toLocalDate().atTime(23, 59, 59).atZone(zoneId).toEpochSecond();

        Log.info("Searching available packing pickup from {} (00:00:00) to {} (23:59:59) [from_time={}, to_time={}]",
                now.minusDays(lookbackDays).toLocalDate(), now.toLocalDate(), fromTime, toTime);

        List<String> targetStatuses = Arrays.asList("600", "603", "601", "602", "604");
        for (String statusId : targetStatuses) {
            String path = "/v1/pickup/list?status_id=" + statusId + "&from_time=" + fromTime + "&to_time=" + toTime
                    + "&page=1&page_size=50";
            try {
                JsonNode json = request("GET", path, token, null, true);
                JsonNode items = pickupArray(json);
                Log.info("Search /v1/pickup/list?status_id={} ({}) found {} items", statusId,
                        pickupStatusName(Integer.parseInt(statusId)), items.size());
                for (JsonNode item : items) {
                    String code = extractPickupCode(item);
                    int candidateStatus = firstInt(item, "status_id", "statusId", "status");
                    Log.info("Found pickup candidate: code={}, status_id={} ({}), raw_id={}", code,
                            candidateStatus, pickupStatusName(candidateStatus), item.path("pickup_id").asText(""));
                    if (code != null && !code.isBlank()) {
                        if (candidateStatus >= 605) {
                            Log.info("Candidate pickup {} status_id={} >= 605 -> skipping", code, candidateStatus);
                            continue;
                        }
                        if (isPickupReadyForPacking(code, token)) {
                            int finalStatus = candidateStatus > 0 ? candidateStatus : Integer.parseInt(statusId);
                            Log.info("Selected available pickup with status_id={} ({}): {}", finalStatus,
                                    pickupStatusName(finalStatus), code);
                            return code;
                        }
                    }
                }
            } catch (Exception e) {
                Log.warn("Query /v1/pickup/list?status_id={} failed: {}", statusId, e.getMessage());
            }
        }

        try {
            String generalPath = "/v1/pickup/list?from_time=" + fromTime + "&to_time=" + toTime
                    + "&page=1&page_size=50";
            JsonNode json = request("GET", generalPath, token, null, true);
            JsonNode items = pickupArray(json);
            Log.info("Search /v1/pickup/list general found {} items", items.size());
            for (JsonNode item : items) {
                int statusId = firstInt(item, "status_id", "statusId", "status");
                if (statusId == 0) {
                    statusId = firstInt(item.path("pickup_list"), "status_id", "statusId", "status");
                }
                if (statusId >= 600 && statusId < 605) {
                    String code = extractPickupCode(item);
                    if (code != null && !code.isBlank() && isPickupReadyForPacking(code, token)) {
                        Log.info("Selected available pickup with status_id={} ({}): {}", statusId,
                                pickupStatusName(statusId), code);
                        return code;
                    }
                }
            }
        } catch (Exception e) {
            Log.warn("Query /v1/pickup/list general failed: {}", e.getMessage());
        }

        return null;
    }

    public String getOrderPickupCode(String trackingCode, String token) {
        try {
            JsonNode json = request("GET", "/v1/order/detail/" + encode(trackingCode), token, null, true);
            JsonNode data = json.path("data");
            JsonNode order = firstPresent(data.path("order_id"), data, json);
            String pickupCode = textOrNull(order, "pickup_code", "pickupCode", "pickup_id", "pickupId");
            if (pickupCode == null || pickupCode.isBlank()) {
                JsonNode pickup = order.path("pickup");
                if (!pickup.isMissingNode()) {
                    pickupCode = textOrNull(pickup, "code", "pickup_code", "id", "pickup_id");
                }
            }
            if (pickupCode == null || pickupCode.isBlank()) {
                JsonNode pickupList = order.path("pickup_list");
                if (!pickupList.isMissingNode()) {
                    pickupCode = textOrNull(pickupList, "code", "pickup_code", "id", "pickup_id");
                }
            }
            return pickupCode;
        } catch (Exception e) {
            Log.warn("Failed to get pickup code for trackingCode {}: {}", trackingCode, e.getMessage());
            return null;
        }
    }

    public String handoverPackedOrders(
            List<PackingOrder> packingOrders,
            String courierCode,
            String driverName,
            String driverPhone,
            String driverLicenseNumber,
            String token) {
        List<String> trackingCodes = uniqueTrackingCodes(packingOrders);
        if (trackingCodes.isEmpty()) {
            throw new IllegalStateException("No tracking code found for handover");
        }

        String firstTrackingCode = trackingCodes.get(0);
        String handoverCode = createHandoverSession(firstTrackingCode, courierCode, token);
        String lastTrackingCode = firstTrackingCode;
        for (int index = 1; index < trackingCodes.size(); index++) {
            String trackingCode = trackingCodes.get(index);
            addHandoverOrder(trackingCode, courierCode, handoverCode, token);
            lastTrackingCode = trackingCode;
            System.out.println("Added order to handover: tracking="
                    + trackingCode
                    + ", handoverCode="
                    + handoverCode);
        }

        approveHandover(
                handoverCode,
                lastTrackingCode,
                driverName,
                driverPhone,
                driverLicenseNumber,
                token);
        System.out.println("Approved handover " + handoverCode + " with " + trackingCodes.size() + " orders");
        return handoverCode;
    }

    private String createHandoverSession(String trackingCode, String courierCode, String token) {
        String handoverCode = addHandoverOrder(trackingCode, courierCode, null, token);
        System.out.println("Created handover session: handoverCode="
                + handoverCode
                + ", firstTracking="
                + trackingCode);
        return handoverCode;
    }

    private String addHandoverOrder(
            String trackingCode,
            String courierCode,
            String handoverCode,
            String token) {
        ObjectNode body = mapper.createObjectNode();
        body.put("tracking_code", trackingCode);
        body.put("courier_code", courierCode);
        if (handoverCode == null || handoverCode.isBlank()) {
            body.putNull("handover_code");
        } else {
            body.put("handover_code", handoverCode);
        }

        JsonNode json = request("POST", "/v1/handover/add", token, body.toString(), true);
        String responseHandoverCode = extractHandoverCode(json);
        if (responseHandoverCode != null && !responseHandoverCode.isBlank()) {
            return responseHandoverCode;
        }
        if (handoverCode != null && !handoverCode.isBlank()) {
            return handoverCode;
        }
        throw new IllegalStateException("Handover add response did not include handover_code. tracking="
                + trackingCode
                + ", response="
                + json);
    }

    private void approveHandover(
            String handoverCode,
            String trackingCode,
            String driverName,
            String driverPhone,
            String driverLicenseNumber,
            String token) {
        if (handoverCode == null || handoverCode.isBlank()) {
            throw new IllegalStateException("Cannot approve handover without handover_code");
        }

        ObjectNode body = mapper.createObjectNode();
        body.put("tracking_code", trackingCode);
        body.put("is_update_drive", false);
        body.put("delivery_drive_name", driverName);
        body.put("delivery_drive_phone", driverPhone);
        body.put("delivery_drive_license_number", driverLicenseNumber);
        request("PUT", "/v1/handover/approved/" + encode(handoverCode), token, body.toString(), true);
    }

    private String pickupStatusName(int statusId) {
        return switch (statusId) {
            case 600 -> "Mới tạo";
            case 601 -> "Chờ lấy hàng";
            case 602 -> "Đang lấy hàng";
            case 603 -> "Chờ đóng gói";
            case 604 -> "Đang đóng gói";
            case 605 -> "Đã hoàn tất đóng gói";
            default -> "Status " + statusId;
        };
    }

    private List<String> uniqueTrackingCodes(List<PackingOrder> packingOrders) {
        Set<String> trackingCodes = new LinkedHashSet<>();
        for (PackingOrder order : packingOrders) {
            if (order.trackingCode() != null && !order.trackingCode().isBlank()) {
                trackingCodes.add(order.trackingCode());
            }
        }
        return new ArrayList<>(trackingCodes);
    }

    private String extractHandoverCode(JsonNode json) {
        String handoverCode = firstTextRecursive(json, "handover_code", "handoverCode");
        if (handoverCode != null && !handoverCode.isBlank()) {
            return handoverCode;
        }
        handoverCode = firstTextRecursive(json, "code");
        if (handoverCode != null && handoverCode.toUpperCase().startsWith("HD-")) {
            return handoverCode;
        }
        if (json != null && json.isTextual() && json.asText("").toUpperCase().startsWith("HD-")) {
            return json.asText();
        }
        return null;
    }

    private String extractPickupCode(JsonNode item) {
        String code = textOrNull(item, "pickup_code", "pickupCode", "pickup_id", "pickupId", "code", "id");
        if (code != null && !code.isBlank()) {
            return code;
        }
        JsonNode nested = firstPresent(item.path("pickup_list"), item.path("pickup"), item.path("pickup_order"));
        return textOrNull(nested, "pickup_code", "pickupCode", "pickup_id", "pickupId", "code", "id");
    }

    private JsonNode pickupArray(JsonNode json) {
        JsonNode data = json.path("data");
        if (data.isArray()) {
            return data;
        }
        for (String field : Arrays.asList("pickup_list", "pickups", "pickup_orders", "items", "list", "results",
                "records", "rows", "data")) {
            JsonNode node = data.path(field);
            if (node.isArray()) {
                return node;
            }
        }
        for (String field : Arrays.asList("pickup_list", "pickups", "items", "list", "results", "data")) {
            JsonNode node = json.path(field);
            if (node.isArray()) {
                return node;
            }
        }
        return mapper.createArrayNode();
    }
}
