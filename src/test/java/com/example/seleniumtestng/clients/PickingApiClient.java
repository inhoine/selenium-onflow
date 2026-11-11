package com.example.seleniumtestng.clients;

import com.example.seleniumtestng.config.ConfigReader;
import com.example.seleniumtestng.models.PickOrderBasket;
import com.example.seleniumtestng.models.PickupDetail;
import com.example.seleniumtestng.models.PickupItem;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Client for WMS Picking & Trolley REST API interactions:
 * - Query pickup details and basket assignments
 * - Discover and assign picking trolleys/totes
 * - Execute picking workflows and trolley item scans via API
 * - Commit picking pickup sessions
 */
public class PickingApiClient extends BaseApiClient {

    public List<PickupItem> getPickupDetail(String pickupId, String token) {
        JsonNode json = request("GET", "/v1/pickup/detail/" + pickupId, token, null, true);
        List<PickupItem> items = new ArrayList<>();
        for (JsonNode order : json.path("data").path("pickup_orders")) {
            String trackingCode = order.path("tracking_code").asText(null);
            for (JsonNode item : order.path("list_items")) {
                items.add(toPickupItem(trackingCode, item));
            }
        }
        return items;
    }

    public PickupDetail getPickupDetailInfo(String pickupCode, String token) {
        JsonNode json = request("GET", "/v1/pickup/detail/" + pickupCode, token, null, true);
        JsonNode pickupList = json.path("data").path("pickup_list");
        return new PickupDetail(
                pickupList.path("pickup_id").asText(null),
                pickupList.path("pickup_code").asText(pickupCode),
                pickupList.path("pickup_type").asText(null),
                pickupList.path("total_order").asInt(0),
                readPickupBasketCodes(json.path("data")));
    }

    public String getPickingTrolleyId(String pickupCode, String token) {
        long now = Instant.now().getEpochSecond();
        long fromTime = now - Long.parseLong(ConfigReader.getOrDefault("PICKPACK_TROLLEY_LOOKBACK_SECONDS", "2592000"));
        long toTime = now + Long.parseLong(ConfigReader.getOrDefault("PICKPACK_TROLLEY_LOOKAHEAD_SECONDS", "86400"));
        List<String> attempts = new ArrayList<>();

        for (String statusId : pickingTrolleyStatusIds()) {
            String path = trolleyListPath(pickupCode, fromTime, toTime, statusId);
            JsonNode json = request("GET", path, token, null, true);
            String trolleyId = firstMatchingTrolleyId(json, pickupCode);
            if (trolleyId != null) {
                System.out.println("Found picking trolley: pickup=" + pickupCode
                        + ", status_id=" + (statusId.isBlank() ? "<none>" : statusId)
                        + ", trolleyId=" + trolleyId);
                return trolleyId;
            }
            attempts.add("status_id=" + (statusId.isBlank() ? "<none>" : statusId) + " response=" + json);
        }
        throw new IllegalStateException(
                "No picking trolley found for pickup " + pickupCode + ": " + String.join(" | ", attempts));
    }

    public void assignBasketsToPickOrder(String trolleyId, List<String> toteCodes, String token) {
        if (trolleyId == null || trolleyId.isBlank()) {
            throw new IllegalStateException("Missing trolley id for assign basket");
        }
        if (toteCodes == null || toteCodes.isEmpty()) {
            throw new IllegalStateException("No tote codes provided for trolley " + trolleyId);
        }

        ObjectNode body = mapper.createObjectNode();
        ArrayNode toteCodeArray = body.putArray("tote_codes");
        for (String toteCode : toteCodes) {
            if (toteCode != null && !toteCode.isBlank()) {
                toteCodeArray.add(toteCode.trim());
            }
        }
        if (toteCodeArray.isEmpty()) {
            throw new IllegalStateException("No valid tote codes provided for trolley " + trolleyId);
        }
        request("POST", "/v2/pick-order/" + trolleyId + "/assign-basket", token, body.toString(), true);
    }

    public List<PickOrderBasket> getPickOrderBaskets(String pickupCode, String token) {
        JsonNode json = request("GET", "/v2/pick-order/" + pickupCode + "/baskets", token, null, true);
        List<PickOrderBasket> baskets = new ArrayList<>();
        for (JsonNode basket : basketArray(json)) {
            String code = readBasketCode(basket);
            if (code == null || code.isBlank()) {
                continue;
            }
            int statusId = readBasketStatusId(basket);
            String statusName = readBasketStatusName(basket);
            String trackingCode = readBasketTrackingCode(basket);
            baskets.add(new PickOrderBasket(code, statusId, statusName, trackingCode));
        }
        System.out.println("Pick order baskets from API: pickup=" + pickupCode
                + ", baskets=" + baskets.size()
                + ", detail=" + basketSummary(baskets));
        return baskets;
    }

    public void mapTrolleyPicking(String pickupCode, String trolleyCode, String token) {
        String body = "{"
                + "\"trolley_code\":\"" + trolleyCode + "\","
                + "\"skip_trolley_code\":false"
                + "}";
        request("PUT", "/v1/trolley/trolley-map-picking/" + pickupCode + "?", token, body, true);
    }

    public void pickAllProductsInPickup(String pickupCode, String token) {
        List<JsonNode> binsets = getPickupBinsets(pickupCode, token);
        if (binsets.isEmpty()) {
            throw new IllegalStateException("No binset found for pickup " + pickupCode);
        }

        for (JsonNode bin : binsets) {
            String binCode = bin.path("bin_code").asText();
            List<JsonNode> products = getPickingProductsByBin(pickupCode, binCode, token);
            for (JsonNode product : products) {
                List<String> barcodes = readStringList(product.path("barcodes"));
                if (barcodes.isEmpty()) {
                    throw new IllegalStateException(
                            "No barcode for product " + product.path("goods_id").path("partner_code").asText());
                }

                int quantityNeedPick = product.path("quantity_sold").asInt() - product.path("quantity_pick").asInt();
                if (quantityNeedPick <= 0) {
                    continue;
                }
                updateTrolleyDetail(pickupCode, product.path("bin_code").asText(), barcodes.get(0), quantityNeedPick,
                        token);
            }
        }
    }

    public void commitPickingPickup(String pickupCode, String trolleyCode, String token) {
        String body = "{"
                + "\"trolley_code\":\"" + trolleyCode + "\","
                + "\"step_count\":0"
                + "}";
        request("PUT", "/v1/trolley/commit-status/" + pickupCode + "?", token, body, true);
    }

    public PickupItem toPickupItem(String trackingCode, JsonNode item) {
        JsonNode goods = item.path("goods_id");
        return new PickupItem(
                trackingCode,
                goods.path("partner_code").asText(null),
                goods.path("goods_code").asText(null),
                readStringList(goods.path("barcodes")),
                firstBoolean(item, "is_serial_code", "isSerialCode")
                        || firstBoolean(goods, "is_serial_code", "isSerialCode"),
                Collections.emptyList(),
                item.path("quantity_sold").asInt(),
                item.path("quantity_pick").asInt());
    }

    public PickupItem withAllocatedSerialCodes(
            PickupItem item,
            String token,
            Map<String, List<String>> serialCodeCache,
            Map<String, Integer> serialCodeCursor) {
        int quantityNeedScan = Math.max(0, item.quantitySold() - item.quantityPick());
        if (quantityNeedScan == 0) {
            return item;
        }

        String productCode = serialProductCode(item);
        if (productCode == null || productCode.isBlank()) {
            throw new IllegalStateException("Serial item has no product code: tracking="
                    + item.trackingCode()
                    + ", partnerCode="
                    + item.partnerCode());
        }

        List<String> availableSerialCodes = serialCodeCache.computeIfAbsent(
                productCode,
                code -> getAvailableSerialCodes(code, token));
        int cursor = serialCodeCursor.getOrDefault(productCode, 0);
        if (cursor + quantityNeedScan > availableSerialCodes.size()) {
            throw new IllegalStateException("Not enough available serials with status_id=201 for product "
                    + productCode
                    + ". need="
                    + quantityNeedScan
                    + ", allocated="
                    + cursor
                    + ", available="
                    + availableSerialCodes.size());
        }

        List<String> allocatedSerialCodes = new ArrayList<>(
                availableSerialCodes.subList(cursor, cursor + quantityNeedScan));
        serialCodeCursor.put(productCode, cursor + quantityNeedScan);
        System.out.println("Allocated serials for tracking="
                + item.trackingCode()
                + ", product="
                + productCode
                + ": "
                + allocatedSerialCodes);
        return new PickupItem(
                item.trackingCode(),
                item.partnerCode(),
                item.goodsCode(),
                item.barcodes(),
                true,
                allocatedSerialCodes,
                item.quantitySold(),
                item.quantityPick());
    }

    public List<String> getAvailableSerialCodes(String productCode, String token) {
        JsonNode json = request("GET", "/v1/inventory/list-serial/" + encode(productCode), token, null, true);
        List<String> serialCodes = new ArrayList<>();
        collectAvailableSerialCodes(json.path("data"), serialCodes);
        if (serialCodes.isEmpty()) {
            collectAvailableSerialCodes(json, serialCodes);
        }
        if (serialCodes.isEmpty()) {
            throw new IllegalStateException("No serial_code with status_id=201 found for product " + productCode);
        }
        System.out.println("Available serials status_id=201 for product=" + productCode + ": " + serialCodes.size());
        return serialCodes;
    }

    private void collectAvailableSerialCodes(JsonNode node, List<String> serialCodes) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return;
        }
        if (node.isArray()) {
            for (JsonNode item : node) {
                collectAvailableSerialCodes(item, serialCodes);
            }
            return;
        }
        if (!node.isObject()) {
            return;
        }

        int statusId = readSerialStatusId(node);
        String serialCode = textOrNull(node, "serial_code", "serialCode");
        if (statusId == 201 && serialCode != null && !serialCode.isBlank()) {
            serialCodes.add(serialCode);
        }

        for (JsonNode child : node) {
            collectAvailableSerialCodes(child, serialCodes);
        }
    }

    private int readSerialStatusId(JsonNode serial) {
        int statusId = firstInt(serial, "status_id", "statusId");
        if (statusId > 0) {
            return statusId;
        }
        JsonNode status = firstPresent(
                serial.path("status"),
                serial.path("serial_status"),
                serial.path("serialStatus"),
                serial.path("status_id"),
                serial.path("statusId"));
        return firstInt(status, "id", "status_id", "statusId", "value");
    }

    private String serialProductCode(PickupItem item) {
        if (item.goodsCode() != null && !item.goodsCode().isBlank()) {
            return item.goodsCode();
        }
        return item.partnerCode();
    }

    private List<JsonNode> getPickupBinsets(String pickupCode, String token) {
        JsonNode json = request("GET", "/v1/trolley/binset/" + pickupCode + "?is_issue=0&picking_mode=pattern", token,
                null, true);
        return json.path("data").isArray() ? iterableToList(json.path("data")) : Collections.emptyList();
    }

    private List<JsonNode> getPickingProductsByBin(String pickupCode, String binCode, String token) {
        JsonNode json = request("GET", "/v1/trolley/picking/" + pickupCode + "?bin_code=" + binCode, token, null, true);
        return json.path("data").isArray() ? iterableToList(json.path("data")) : Collections.emptyList();
    }

    private void updateTrolleyDetail(String pickupCode, String binCode, String barcode, int quantity, String token) {
        String body = "{"
                + "\"bin_code\":\"" + binCode + "\","
                + "\"goods_code\":\"" + barcode + "\","
                + "\"quantity\":" + quantity
                + "}";
        request("PUT", "/v1/trolley/detail/" + pickupCode + "?", token, body, true);
    }

    public List<String> readPickupBasketCodes(JsonNode data) {
        Set<String> basketCodes = new LinkedHashSet<>();
        JsonNode pickupList = data.path("pickup_list");
        collectCodeValues(basketCodes, pickupList.path("basket_codes"));
        collectCodeValues(basketCodes, pickupList.path("basketCodes"));
        collectCodeValues(basketCodes, pickupList.path("tote_codes"));
        collectCodeValues(basketCodes, pickupList.path("toteCodes"));
        collectCodeValues(basketCodes, data.path("basket_codes"));
        collectCodeValues(basketCodes, data.path("basketCodes"));

        JsonNode pickupOrders = data.path("pickup_orders");
        if (pickupOrders.isArray()) {
            for (JsonNode order : pickupOrders) {
                collectCodeValues(basketCodes, order.path("basket_codes"));
                collectCodeValues(basketCodes, order.path("basketCodes"));
                collectCodeValues(basketCodes, order.path("tote_codes"));
                collectCodeValues(basketCodes, order.path("toteCodes"));
            }
        }
        return new ArrayList<>(basketCodes);
    }

    private void collectCodeValues(Set<String> codes, JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return;
        }
        if (node.isArray()) {
            for (JsonNode item : node) {
                collectCodeValues(codes, item);
            }
            return;
        }
        if (node.isObject()) {
            String code = textOrNull(
                    node,
                    "basket_code",
                    "basketCode",
                    "tote_code",
                    "toteCode",
                    "trolley_code",
                    "trolleyCode",
                    "code");
            if (code != null && !code.isBlank()) {
                codes.add(code.trim());
            }
            return;
        }
        String code = node.asText(null);
        if (code == null || code.isBlank()) {
            return;
        }
        for (String value : code.split(",")) {
            if (!value.isBlank()) {
                codes.add(value.trim());
            }
        }
    }

    private JsonNode basketArray(JsonNode json) {
        JsonNode data = json.path("data");
        if (data.isArray()) {
            return data;
        }
        for (String field : Arrays.asList("baskets", "items", "list", "results", "records", "rows", "data")) {
            JsonNode node = data.path(field);
            if (node.isArray()) {
                return node;
            }
        }
        return mapper.createArrayNode();
    }

    private String readBasketCode(JsonNode basket) {
        String code = textOrNull(
                basket,
                "basket_code",
                "basketCode",
                "tote_code",
                "toteCode",
                "trolley_code",
                "trolleyCode",
                "code");
        if (code != null && !code.isBlank()) {
            return code;
        }
        JsonNode nested = firstPresent(basket.path("basket"), basket.path("tote"), basket.path("trolley"));
        return textOrNull(
                nested,
                "basket_code",
                "basketCode",
                "tote_code",
                "toteCode",
                "trolley_code",
                "trolleyCode",
                "code");
    }

    private int readBasketStatusId(JsonNode basket) {
        int statusId = firstInt(basket, "status_id", "statusId");
        if (statusId > 0) {
            return statusId;
        }
        JsonNode status = firstPresent(basket.path("status"), basket.path("status_id"), basket.path("statusId"));
        return firstInt(status, "id", "status_id", "statusId");
    }

    private String readBasketStatusName(JsonNode basket) {
        String statusName = textOrNull(basket, "status_name", "statusName", "status_text", "statusText");
        if (statusName != null && !statusName.isBlank()) {
            return statusName;
        }
        JsonNode status = firstPresent(basket.path("status"), basket.path("status_id"), basket.path("statusId"));
        return textOrNull(status, "name", "status_name", "statusName", "status_text", "statusText");
    }

    private String readBasketTrackingCode(JsonNode basket) {
        List<String> fields = Arrays.asList(
                "tracking_code",
                "trackingCode",
                "order_code",
                "orderCode",
                "order_number",
                "orderNumber",
                "shipment_order",
                "shipmentOrder",
                "ecommerce_order_code",
                "ecommerceOrderCode");
        String trackingCode = textOrNull(basket, fields.toArray(new String[0]));
        if (trackingCode != null && !trackingCode.isBlank()) {
            return trackingCode;
        }

        for (String nestedField : Arrays.asList(
                "order",
                "order_id",
                "orderId",
                "pickup_order",
                "pickupOrder",
                "basket_order",
                "basketOrder",
                "shipment_order",
                "shipmentOrder")) {
            trackingCode = textOrNull(basket.path(nestedField), fields.toArray(new String[0]));
            if (trackingCode != null && !trackingCode.isBlank()) {
                return trackingCode;
            }
        }

        return findTextByFieldName(basket, fields);
    }

    private String findTextByFieldName(JsonNode node, List<String> fieldNames) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }
        if (node.isObject()) {
            Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                if (fieldNames.contains(field.getKey())) {
                    String value = field.getValue().asText(null);
                    if (value != null && !value.isBlank()) {
                        return value;
                    }
                }
                String nestedValue = findTextByFieldName(field.getValue(), fieldNames);
                if (nestedValue != null && !nestedValue.isBlank()) {
                    return nestedValue;
                }
            }
        }
        if (node.isArray()) {
            for (JsonNode item : node) {
                String nestedValue = findTextByFieldName(item, fieldNames);
                if (nestedValue != null && !nestedValue.isBlank()) {
                    return nestedValue;
                }
            }
        }
        return null;
    }

    private String basketSummary(List<PickOrderBasket> baskets) {
        List<String> parts = new ArrayList<>();
        for (PickOrderBasket basket : baskets) {
            parts.add(basket.code() + ":" + basket.statusId() + ":" + basket.trackingCode());
        }
        return parts.toString();
    }

    private List<String> pickingTrolleyStatusIds() {
        String configured = ConfigReader.getOrDefault("PICKPACK_TROLLEY_STATUS_IDS", "700,");
        Set<String> statusIds = new LinkedHashSet<>();
        for (String statusId : configured.split(",", -1)) {
            statusIds.add(statusId.trim());
        }
        statusIds.add("");
        return new ArrayList<>(statusIds);
    }

    private String trolleyListPath(String pickupCode, long fromTime, long toTime, String statusId) {
        String path = "/v1/trolley/list"
                + "?from_time=" + fromTime
                + "&to_time=" + toTime
                + (pickupCode != null && !pickupCode.isBlank() ? "&q=" + encode(pickupCode) : "")
                + "&page_size=20"
                + "&page=1";
        if (statusId != null && !statusId.isBlank()) {
            path += "&status_id=" + encode(statusId);
        }
        return path;
    }

    private String firstMatchingTrolleyId(JsonNode json, String pickupCode) {
        JsonNode trolleys = itemArray(json);
        for (JsonNode trolley : trolleys) {
            String pickup = textOrNull(trolley, "pickup_code", "pickupCode", "pickup_id", "pickupId");
            String trolleyId = textOrNull(trolley, "trolley_id", "trolleyId", "id");
            if (trolleyId != null && !trolleyId.isBlank()
                    && (pickup == null || pickup.isBlank() || pickup.contains(pickupCode))) {
                return trolleyId;
            }
        }
        return null;
    }
}

