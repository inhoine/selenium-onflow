package com.example.seleniumtestng.clients;

import com.example.seleniumtestng.config.ConfigReader;
import com.example.seleniumtestng.models.POSku;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Client for WMS Inbound REST API interactions:
 * - Mark PO received at warehouse
 * - Query PO SKUs, boxes, and pending boxes
 * - Query inspectable products for boxes
 * - Update Putaway tasks to storage locations
 */
public class InboundApiClient extends BaseApiClient {

    private static final List<PutawayLocationGroup> PUTAWAY_LOCATION_GROUPS = Arrays.asList(
            new PutawayLocationGroup("A", Arrays.asList("A-01-02-005", "A-01-02-001", "CHU-A-1", "BIN-03-CHUA")),
            new PutawayLocationGroup("D1",
                    Arrays.asList("NANO-02", "A26-NT-04", "HU-1-01", "THANH-CHUA-3", "FC1-DA-01-7-011",
                            "FC1-DA-01-7-012", "FC1-DA-01-7-014")),
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

        System.out.println("Read PO boxes from API pages: po=" + poCode + ", boxes=" + boxCodes.size() + ", page_size="
                + pageSize);
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
            JsonNode todo = request("GET", "/v1/putaway/todo?page_size=100&page=1&stock_level=" + stockLevel, token,
                    null, false);
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

