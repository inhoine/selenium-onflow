package com.example.seleniumtestng.clients;

import com.example.seleniumtestng.config.ConfigReader;
import com.example.seleniumtestng.models.POSku;
import com.example.seleniumtestng.models.PackingOrder;
import com.example.seleniumtestng.models.PickOrderBasket;
import com.example.seleniumtestng.models.PickupDetail;
import com.example.seleniumtestng.models.PickupItem;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class WmsApiClient {
    private static final List<PutawayLocationGroup> PUTAWAY_LOCATION_GROUPS = Arrays.asList(
            new PutawayLocationGroup("A", Arrays.asList("A-01-02-005", "A-01-02-001", "CHU-A-1", "BIN-03-CHUA")),
            new PutawayLocationGroup("D1", Arrays.asList("NANO-02", "A26-NT-04", "HU-1-01", "THANH-CHUA-3", "FC1-DA-01-7-011", "FC1-DA-01-7-012", "FC1-DA-01-7-014")),
            new PutawayLocationGroup("D2", Arrays.asList(
                    "FC1-DA-01-3-200",
                    "FC1-DA-01-3-201",
                    "FC1-DA-01-3-202",
                    "FC1-DA-01-3-203")),
            new PutawayLocationGroup("D3", Arrays.asList(
                    "FC1-DA-01-3-300",
                    "FC1-DA-01-3-301",
                    "FC1-DA-01-3-302",
                    "FC1-DA-01-3-303")));

    private final HttpClient client = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.ALWAYS)
            .build();
    private final ObjectMapper mapper = new ObjectMapper();
    private final String baseUrl = ConfigReader.required("WMS_BASE_URL");

    public void receivedPoAtWarehouse(String inboundCode, String token) {
        String body = "{"
                + "\"status_id\":101,"
                + "\"shipment_images\":["
                + "{\"image_urls\":\"https://nhl.sgp1.cdn.digitaloceanspaces.com/ts/uploads/2026/05/19/cabd764512824dca82e47f9c5045fba6.jpg\"},"
                + "{\"image_urls\":\"https://nhl.sgp1.cdn.digitaloceanspaces.com/ts/uploads/2026/05/19/662a0f7066a7431fb1e7b704cd1ff363.jpg\"},"
                + "{\"image_urls\":\"https://nhl.sgp1.cdn.digitaloceanspaces.com/ts/uploads/2026/05/19/6390648a8c654cdb844355f7ebec9d40.jpg\"}"
                + "],"
                + "\"reason_for_refusal\":\"\","
                + "\"delivery_drive_name\":\"thuhn\","
                + "\"delivery_drive_phone\":\"2525\","
                + "\"delivery_drive_license_number\":\"uimoo\""
                + "}";
        request("PUT", "/v1/po/received-po-at-warehouse/" + inboundCode + "/?", token, body, false);
    }

    public List<POSku> getPoSkus(String poCode, String token) {
        int page = 1;
        int pageSize = 100;
        List<POSku> result = new ArrayList<>();

        while (true) {
            JsonNode json = request(
                    "GET",
                    "/v1/po/detail/" + poCode + "/skus?page=" + page + "&page_size=" + pageSize,
                    token,
                    null,
                    true);
            JsonNode poSkus = skuArray(json);
            if (!poSkus.isArray() || poSkus.isEmpty()) {
                break;
            }

            for (JsonNode sku : poSkus) {
                JsonNode goods = firstPresent(sku.path("goods_id"), sku.path("goods"));
                result.add(new POSku(
                        textOrNull(sku, "box_code", "boxCode", "package_code"),
                        firstInt(sku, "quantity_inbound", "quantityInbound", "quantity", "qty"),
                        textOrNull(sku, "partner_code", "sku", "goods_code", "goodsCode"),
                        numberOrNull(firstPresent(goods.path("goods_w"), sku.path("goods_w"))),
                        numberOrNull(firstPresent(goods.path("goods_d"), sku.path("goods_d"))),
                        numberOrNull(firstPresent(goods.path("goods_h"), sku.path("goods_h"))),
                        numberOrNull(firstPresent(goods.path("goods_weight"), sku.path("goods_weight")))));
            }

            if (poSkus.size() < pageSize) {
                break;
            }
            page++;
        }

        if (result.isEmpty()) {
            throw new IllegalStateException("No SKU found in PO: " + poCode);
        }
        return result;
    }

    public List<String> getPoBoxes(String poCode, String token) {
        return readPoBoxes(poCode, token, 0).boxCodes;
    }

    public List<String> getPendingPoBoxes(String poCode, String token) {
        BoxReadResult result = readPoBoxes(poCode, token, 0);
        System.out.println("Pending PO boxes from API: po="
                + poCode
                + ", pending=" + result.pendingBoxCodes.size()
                + ", total=" + result.boxCodes.size());
        return result.pendingBoxCodes;
    }

    public List<String> getPendingPoBoxes(String poCode, String token, int maxPendingBoxes) {
        BoxReadResult result = readPoBoxes(poCode, token, maxPendingBoxes);
        List<String> pendingBoxCodes = result.pendingBoxCodes;
        if (maxPendingBoxes > 0 && pendingBoxCodes.size() > maxPendingBoxes) {
            pendingBoxCodes = new ArrayList<>(pendingBoxCodes.subList(0, maxPendingBoxes));
        }
        System.out.println("Pending PO boxes from API: po="
                + poCode
                + ", pending=" + pendingBoxCodes.size()
                + ", total_read=" + result.boxCodes.size()
                + ", max_pending=" + maxPendingBoxes);
        return pendingBoxCodes;
    }

    private BoxReadResult readPoBoxes(String poCode, String token, int maxPendingBoxes) {
        int page = 1;
        int pageSize = Integer.parseInt(ConfigReader.getOrDefault("INBOUND_BOX_PAGE_SIZE", "50"));
        Set<String> boxCodes = new LinkedHashSet<>();
        Set<String> pendingBoxCodes = new LinkedHashSet<>();

        while (true) {
            JsonNode json;
            try {
                json = request(
                        "GET",
                        "/v1/po/detail/" + poCode + "/boxes?page=" + page + "&page_size=" + pageSize,
                        token,
                        null,
                        true);
            } catch (IllegalStateException e) {
                if (isInvalidPage(e) && !boxCodes.isEmpty()) {
                    System.out.println("Reached final PO box page before page " + page + " for " + poCode);
                    break;
                }
                throw e;
            }
            JsonNode boxes = itemArray(json);
            if (!boxes.isArray() || boxes.isEmpty()) {
                break;
            }

            for (JsonNode box : boxes) {
                String boxCode = textOrNull(box, "box_code", "boxCode", "package_code", "code");
                if (boxCode != null && !boxCode.isBlank()) {
                    boxCodes.add(boxCode);
                    if (isPendingBox(box)) {
                        pendingBoxCodes.add(boxCode);
                    }
                }
            }

            if (maxPendingBoxes > 0 && pendingBoxCodes.size() >= maxPendingBoxes) {
                break;
            }

            if (boxes.size() < pageSize) {
                break;
            }
            page++;
            if (page > 1000) {
                throw new IllegalStateException("Too many PO box pages for " + poCode);
            }
        }

        System.out.println("Read PO boxes from API pages: po=" + poCode + ", boxes=" + boxCodes.size() + ", page_size=" + pageSize);
        return new BoxReadResult(new ArrayList<>(boxCodes), new ArrayList<>(pendingBoxCodes));
    }

    public List<POSku> findInspectionProducts(String poCode, String boxCode, String token) {
        JsonNode json = request(
                "GET",
                "/v1/inspection/find-po?shipment_po=" + encode(poCode) + "&code=" + encode(boxCode),
                token,
                null,
                true);
        List<POSku> products = new ArrayList<>();
        collectInspectionProducts(json.path("data"), boxCode, products);
        if (products.isEmpty()) {
            collectInspectionProducts(json, boxCode, products);
        }
        return products;
    }

    public int updatePutaway(String inboundCode, String token) {
        int updated = 0;
        for (PutawayLocationGroup locationGroup : PUTAWAY_LOCATION_GROUPS) {
            String stockLevel = locationGroup.stockLevel;
            JsonNode todo = request("GET", "/v1/putaway/todo?page_size=100&page=1&stock_level=" + stockLevel, token, null, false);
            for (JsonNode task : todo.path("data")) {
                if (!inboundCode.equals(task.path("shipment_id").path("shipment_po").asText())) {
                    continue;
                }
                putawayTaskWithFallbackLocations(task, locationGroup.locationCodes, token);
                updated++;
            }
        }
        if (updated == 0) {
            throw new IllegalStateException("No putaway task found for PO " + inboundCode);
        }
        return updated;
    }

    private void putawayTaskWithFallbackLocations(JsonNode task, List<String> locationCodes, String token) {
        RuntimeException lastFailure = null;
        for (String locationCode : locationCodes) {
            String body = "{"
                    + "\"dr_id\":" + task.path("dr_id").asLong() + ","
                    + "\"location_code\":\"" + locationCode + "\","
                    + "\"quantity_putaway\":" + task.path("quantity").asInt()
                    + "}";
            try {
                request("POST", "/v1/putaway/update?", token, body, false);
                return;
            } catch (RuntimeException e) {
                lastFailure = e;
                System.out.println("Putaway failed for dr_id="
                        + task.path("dr_id").asLong()
                        + ", stock_level=" + task.path("stock_level").asText()
                        + ", location_code=" + locationCode
                        + ". Trying next location if available. Error: " + e.getMessage());
            }
        }

        throw new IllegalStateException("Unable to putaway dr_id="
                + task.path("dr_id").asLong()
                + ", stock_level=" + task.path("stock_level").asText()
                + " to any configured location: " + locationCodes, lastFailure);
    }

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
        throw new IllegalStateException("No picking trolley found for pickup " + pickupCode + ": " + String.join(" | ", attempts));
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

    public List<PackingOrder> getPickupPackingOrders(String pickupId, String token) {
        JsonNode json = request("GET", "/v1/pickup/detail/" + pickupId, token, null, true);
        List<PackingOrder> orders = new ArrayList<>();
        for (JsonNode order : json.path("data").path("pickup_orders")) {
            String trackingCode = order.path("tracking_code").asText(null);
            List<PickupItem> items = new ArrayList<>();
            for (JsonNode item : order.path("list_items")) {
                items.add(toPickupItem(trackingCode, item));
            }
            orders.add(new PackingOrder(trackingCode, items));
        }
        return orders;
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
                    throw new IllegalStateException("No barcode for product " + product.path("goods_id").path("partner_code").asText());
                }

                int quantityNeedPick = product.path("quantity_sold").asInt() - product.path("quantity_pick").asInt();
                if (quantityNeedPick <= 0) {
                    continue;
                }
                updateTrolleyDetail(pickupCode, product.path("bin_code").asText(), barcodes.get(0), quantityNeedPick, token);
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

    private JsonNode request(String method, String path, String token, String body, boolean bearer) {
        int attempts = Integer.parseInt(ConfigReader.getOrDefault("WMS_API_RETRY_ATTEMPTS", "3"));
        IOException lastIoError = null;
        IllegalStateException lastHttpError = null;

        for (int attempt = 1; attempt <= attempts; attempt++) {
            try {
                HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(baseUrl + path))
                        .header("Accept", "Application/json")
                        .header("Content-Type", "Application/json")
                        .header("x-client-platform", "mobile")
                        .header("accept-language", "vi");
                if (token != null && !token.isBlank()) {
                    builder.header("Authorization", authorizationHeader(token, bearer));
                }
                if ("GET".equalsIgnoreCase(method)) {
                    builder.GET();
                } else {
                    builder.method(method, HttpRequest.BodyPublishers.ofString(body == null ? "" : body));
                }

                HttpResponse<String> response = client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
                JsonNode json = response.body() == null || response.body().isBlank()
                        ? mapper.createObjectNode()
                        : mapper.readTree(response.body());
                if (response.statusCode() < 200 || response.statusCode() >= 300) {
                    lastHttpError = new IllegalStateException("WMS API failed " + response.statusCode()
                            + " " + method + " " + path
                            + (body == null ? "" : " body=" + body)
                            + ": " + json);
                    if (attempt < attempts && isRetryableStatus(response.statusCode())) {
                        sleepBeforeRetry(attempt, method, path, lastHttpError);
                        continue;
                    }
                    throw lastHttpError;
                }
                return json;
            } catch (IOException e) {
                lastIoError = e;
                if (attempt < attempts) {
                    sleepBeforeRetry(attempt, method, path, e);
                    continue;
                }
                throw new IllegalStateException("Unable to call WMS API", e);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Interrupted while calling WMS API", e);
            }
        }

        if (lastHttpError != null) {
            throw lastHttpError;
        }
        throw new IllegalStateException("Unable to call WMS API", lastIoError);
    }

    private boolean isRetryableStatus(int statusCode) {
        return statusCode == 429 || statusCode >= 500;
    }

    private void sleepBeforeRetry(int attempt, String method, String path, Exception error) {
        long delayMillis = 500L * attempt;
        System.out.println("Retry WMS API " + method + " " + path + " attempt=" + (attempt + 1)
                + " after " + error.getClass().getSimpleName());
        try {
            Thread.sleep(delayMillis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted before retrying WMS API", e);
        }
    }

    private Number numberOrNull(JsonNode node) {
        return node == null || node.isMissingNode() || node.isNull() ? null : node.numberValue();
    }

    private JsonNode skuArray(JsonNode json) {
        JsonNode data = json.path("data");
        if (data.isArray()) {
            return data;
        }
        for (String field : Arrays.asList("po_skus", "skus", "items", "list", "results")) {
            JsonNode node = data.path(field);
            if (node.isArray()) {
                return node;
            }
        }
        return mapper.createArrayNode();
    }

    private JsonNode itemArray(JsonNode json) {
        JsonNode data = json.path("data");
        if (data.isArray()) {
            return data;
        }
        for (String field : Arrays.asList("items", "list", "results", "boxes", "baskets", "records", "rows", "data")) {
            JsonNode node = data.path(field);
            if (node.isArray()) {
                return node;
            }
        }
        return mapper.createArrayNode();
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
                + "&q=" + encode(pickupCode)
                + "&page_size=10"
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

    private boolean isInvalidPage(IllegalStateException e) {
        String message = e.getMessage();
        return message != null && message.toLowerCase().contains("invalid page");
    }

    private boolean isPendingBox(JsonNode box) {
        int quantityInbound = firstInt(box, "quantity_inbound", "quantityInbound", "quantity");
        int quantityReceived = firstInt(box, "quantity_received", "quantityReceived", "quantity_goods_normal");
        int quantityDamaged = firstInt(box, "quantity_damaged", "quantityDamaged", "quantity_goods_damaged");
        int quantityLost = firstInt(
                box,
                "quantity_lost",
                "quantityLost",
                "quantity_not_received",
                "quantityNotReceived",
                "quantity_refused",
                "quantityRefused");
        int quantityInspected = quantityReceived + quantityDamaged + quantityLost;
        return quantityInbound > 0 && quantityInspected < quantityInbound;
    }

    private void collectInspectionProducts(JsonNode node, String fallbackBoxCode, List<POSku> products) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return;
        }
        if (node.isArray()) {
            for (JsonNode item : node) {
                collectInspectionProducts(item, fallbackBoxCode, products);
            }
            return;
        }
        if (!node.isObject()) {
            return;
        }

        POSku sku = toInspectionSku(node, fallbackBoxCode);
        if (sku != null && !containsSku(products, sku.partnerCode())) {
            products.add(sku);
        }
        for (JsonNode child : node) {
            collectInspectionProducts(child, fallbackBoxCode, products);
        }
    }

    private POSku toInspectionSku(JsonNode node, String fallbackBoxCode) {
        JsonNode goods = firstPresent(node.path("goods_id"), node.path("goods"));
        String partnerCode = textOrNull(node, "partner_code", "sku", "goods_code", "goodsCode");
        if (partnerCode == null || partnerCode.isBlank()) {
            partnerCode = textOrNull(goods, "partner_code", "sku", "goods_code", "goodsCode");
        }
        if (partnerCode == null || partnerCode.isBlank()) {
            return null;
        }

        int quantity = firstInt(
                node,
                "quantity_need_inspection",
                "quantityNeedInspection",
                "quantity_remaining",
                "remaining_quantity",
                "quantity_pending",
                "quantity_inbound",
                "quantityInbound",
                "quantity",
                "qty",
                "quantity_inspection",
                "quantityInspection");
        if (quantity <= 0) {
            return null;
        }

        String boxCode = textOrNull(node, "box_code", "boxCode", "package_code", "code");
        if (boxCode == null || boxCode.isBlank()) {
            boxCode = fallbackBoxCode;
        }
        return new POSku(
                boxCode,
                quantity,
                partnerCode,
                numberOrNull(firstPresent(goods.path("goods_w"), node.path("goods_w"))),
                numberOrNull(firstPresent(goods.path("goods_d"), node.path("goods_d"))),
                numberOrNull(firstPresent(goods.path("goods_h"), node.path("goods_h"))),
                numberOrNull(firstPresent(goods.path("goods_weight"), node.path("goods_weight"))));
    }

    private boolean containsSku(List<POSku> products, String sku) {
        String normalizedSku = normalized(sku);
        return products.stream().anyMatch(product -> normalized(product.partnerCode()).equals(normalizedSku));
    }

    private String normalized(String value) {
        return value == null ? "" : value.replaceAll("[^A-Za-z0-9]", "").toLowerCase();
    }

    private JsonNode firstPresent(JsonNode... nodes) {
        for (JsonNode node : nodes) {
            if (node != null && !node.isMissingNode() && !node.isNull()) {
                return node;
            }
        }
        return mapper.nullNode();
    }

    private String textOrNull(JsonNode node, String... fields) {
        for (String field : fields) {
            JsonNode value = node.path(field);
            if (!value.isMissingNode() && !value.isNull() && !value.asText().isBlank()) {
                return value.asText();
            }
        }
        JsonNode goods = firstPresent(node.path("goods_id"), node.path("goods"));
        for (String field : Arrays.asList("partner_code", "sku", "goods_code", "goodsCode")) {
            JsonNode value = goods.path(field);
            if (!value.isMissingNode() && !value.isNull() && !value.asText().isBlank()) {
                return value.asText();
            }
        }
        return null;
    }

    private int firstInt(JsonNode node, String... fields) {
        for (String field : fields) {
            JsonNode value = node.path(field);
            if (value.canConvertToInt()) {
                return value.asInt();
            }
        }
        return 0;
    }

    private String authorizationHeader(String token, boolean bearer) {
        String normalized = token.trim();
        if ((normalized.startsWith("\"") && normalized.endsWith("\"")) || (normalized.startsWith("'") && normalized.endsWith("'"))) {
            normalized = normalized.substring(1, normalized.length() - 1).trim();
        }
        if (bearer && !normalized.regionMatches(true, 0, "Bearer ", 0, "Bearer ".length())) {
            return "Bearer " + normalized;
        }
        return normalized;
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private PickupItem toPickupItem(String trackingCode, JsonNode item) {
        JsonNode goods = item.path("goods_id");
        return new PickupItem(
                trackingCode,
                goods.path("partner_code").asText(null),
                goods.path("goods_code").asText(null),
                readStringList(goods.path("barcodes")),
                item.path("quantity_sold").asInt(),
                item.path("quantity_pick").asInt());
    }

    private List<JsonNode> getPickupBinsets(String pickupCode, String token) {
        JsonNode json = request("GET", "/v1/trolley/binset/" + pickupCode + "?is_issue=0&picking_mode=pattern", token, null, true);
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

    private List<String> readStringList(JsonNode node) {
        if (node == null || !node.isArray()) {
            return Collections.emptyList();
        }
        List<String> values = new ArrayList<>();
        for (JsonNode item : node) {
            values.add(item.asText());
        }
        return values;
    }

    private List<String> readPickupBasketCodes(JsonNode data) {
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

    private List<JsonNode> iterableToList(JsonNode arrayNode) {
        List<JsonNode> values = new ArrayList<>();
        for (JsonNode item : arrayNode) {
            values.add(item);
        }
        return values;
    }

    private static final class PutawayLocationGroup {
        private final String stockLevel;
        private final List<String> locationCodes;

        private PutawayLocationGroup(String stockLevel, List<String> locationCodes) {
            this.stockLevel = stockLevel;
            this.locationCodes = locationCodes;
        }
    }

    private static final class BoxReadResult {
        private final List<String> boxCodes;
        private final List<String> pendingBoxCodes;

        private BoxReadResult(List<String> boxCodes, List<String> pendingBoxCodes) {
            this.boxCodes = boxCodes;
            this.pendingBoxCodes = pendingBoxCodes;
        }
    }
}
