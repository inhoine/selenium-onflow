package com.example.seleniumtestng.clients;

import com.example.seleniumtestng.models.RmaInspectionItem;
import com.example.seleniumtestng.models.RmaReturnOrder;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Client for WMS RMA (Return Merchandise Authorization) REST API interactions:
 * - Query RMA return orders and line items
 * - Discover tracking codes and returned products
 * - Query and allocate serial codes for RMA inspection
 */
public class RmaApiClient extends BaseApiClient {

    public List<RmaReturnOrder> getRmaReturnOrders(String rmaCode, String token) {
        JsonNode json = request("GET", "/v1/rma/detail/" + encode(rmaCode), token, null, true);
        List<RmaReturnOrder> orders = new ArrayList<>();
        collectRmaReturnOrders(json.path("data"), orders);
        if (orders.isEmpty()) {
            collectRmaReturnOrders(json, orders);
        }
        if (orders.isEmpty()) {
            throw new IllegalStateException("No tracking_code found in RMA detail: " + rmaCode);
        }
        orders = withRmaOrderDetailItems(orders, token);
        System.out.println("RMA return orders from API: rma=" + rmaCode
                + ", orders=" + orders.size()
                + ", trackingCodes=" + rmaTrackingSummary(orders));
        return orders;
    }

    public List<String> getRmaTrackingCodes(String rmaCode, String token) {
        List<String> trackingCodes = new ArrayList<>();
        for (RmaReturnOrder order : getRmaReturnOrders(rmaCode, token)) {
            trackingCodes.add(order.trackingCode());
        }
        return trackingCodes;
    }

    private void collectRmaReturnOrders(JsonNode node, List<RmaReturnOrder> orders) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return;
        }
        if (node.isArray()) {
            for (JsonNode item : node) {
                collectRmaReturnOrders(item, orders);
            }
            return;
        }
        if (!node.isObject()) {
            return;
        }

        String trackingCode = textOrNull(
                node,
                "tracking_code",
                "trackingCode",
                "partner_tracking_code",
                "partnerTrackingCode");
        if (trackingCode != null && !trackingCode.isBlank() && !containsRmaTracking(orders, trackingCode)) {
            List<RmaInspectionItem> items = new ArrayList<>();
            collectRmaInspectionItems(node, items, new LinkedHashSet<>());
            orders.add(new RmaReturnOrder(trackingCode, items));
        }

        for (JsonNode child : node) {
            collectRmaReturnOrders(child, orders);
        }
    }

    private void collectRmaInspectionItems(
            JsonNode node,
            List<RmaInspectionItem> items,
            Set<String> seenItemKeys) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return;
        }
        if (node.isArray()) {
            for (JsonNode item : node) {
                collectRmaInspectionItems(item, items, seenItemKeys);
            }
            return;
        }
        if (!node.isObject()) {
            return;
        }

        RmaInspectionItem inspectionItem = toRmaInspectionItem(node);
        if (inspectionItem != null) {
            String key = inspectionItem.partnerCode()
                    + "|" + inspectionItem.goodsCode()
                    + "|" + inspectionItem.barcodes()
                    + "|" + inspectionItem.quantity()
                    + "|" + inspectionItem.serialCodes();
            if (seenItemKeys.add(key)) {
                items.add(inspectionItem);
            }
        }

        for (JsonNode child : node) {
            collectRmaInspectionItems(child, items, seenItemKeys);
        }
    }

    private RmaInspectionItem toRmaInspectionItem(JsonNode node) {
        JsonNode goods = firstPresent(
                node.path("goods_id"),
                node.path("goods"),
                node.path("product"),
                node.path("goods_info"),
                node.path("goodsInfo"),
                node.path("product_info"),
                node.path("productInfo"),
                node.path("sku"));
        String partnerCode = textOrNull(node, "partner_code", "partnerCode", "sku", "goods_code", "goodsCode");
        String goodsCode = textOrNull(goods, "goods_code", "goodsCode", "code");

        List<String> barcodes = readStringList(node.path("barcodes"));
        if (barcodes.isEmpty()) {
            barcodes = readStringList(goods.path("barcodes"));
        }
        if (barcodes.isEmpty()) {
            String barcode = textOrNull(node, "barcode", "bar_code", "barCode");
            if (barcode == null || barcode.isBlank()) {
                barcode = textOrNull(goods, "barcode", "bar_code", "barCode");
            }
            if (barcode != null && !barcode.isBlank()) {
                barcodes = Collections.singletonList(barcode);
            }
        }

        String fallbackScanCode = firstNonBlank(goodsCode, partnerCode);
        if (barcodes.isEmpty() && fallbackScanCode != null) {
            barcodes = Collections.singletonList(fallbackScanCode);
        }
        if ((partnerCode == null || partnerCode.isBlank()) && (barcodes == null || barcodes.isEmpty())) {
            return null;
        }
        List<String> serialCodes = rmaSerialCodes(node, goods);

        int quantity = firstInt(
                node,
                "quantity_return",
                "quantityReturn",
                "return_quantity",
                "returnQuantity",
                "quantity_rma",
                "quantityRma",
                "quantity_need_inspection",
                "quantityNeedInspection",
                "quantity",
                "qty",
                "quantity_sold");
        if (quantity <= 0) {
            quantity = 1;
        }
        return new RmaInspectionItem(partnerCode, goodsCode, barcodes, quantity, serialCodes);
    }

    private List<String> rmaSerialCodes(JsonNode node, JsonNode goods) {
        Set<String> serialCodes = new LinkedHashSet<>();
        collectRmaSerialCodes(node, serialCodes);
        collectRmaSerialCodes(goods, serialCodes);
        return new ArrayList<>(serialCodes);
    }

    private void collectRmaSerialCodes(JsonNode node, Set<String> serialCodes) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return;
        }
        if (node.isArray()) {
            for (JsonNode item : node) {
                collectRmaSerialCodes(item, serialCodes);
            }
            return;
        }
        if (!node.isObject()) {
            return;
        }

        Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> field = fields.next();
            String fieldName = field.getKey().toLowerCase();
            JsonNode value = field.getValue();
            if (fieldName.contains("serial")) {
                collectTextValues(value, serialCodes);
            }
            collectRmaSerialCodes(value, serialCodes);
        }
    }

    private void collectTextValues(JsonNode node, Set<String> values) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return;
        }
        if (node.isArray()) {
            for (JsonNode item : node) {
                collectTextValues(item, values);
            }
            return;
        }
        if (node.isObject()) {
            String serialCode = textOrNull(node, "serial_code", "serialCode", "code", "value");
            if (serialCode != null && isUsefulSerialCode(serialCode)) {
                values.add(serialCode.trim());
            }
            for (JsonNode child : node) {
                collectTextValues(child, values);
            }
            return;
        }
        String value = node.asText(null);
        if (value != null && isUsefulSerialCode(value)) {
            values.add(value.trim());
        }
    }

    private boolean isUsefulSerialCode(String value) {
        String normalized = value == null ? "" : value.trim();
        return !normalized.isBlank()
                && !"true".equalsIgnoreCase(normalized)
                && !"false".equalsIgnoreCase(normalized)
                && !"0".equals(normalized)
                && !"1".equals(normalized);
    }

    private boolean containsRmaTracking(List<RmaReturnOrder> orders, String trackingCode) {
        for (RmaReturnOrder order : orders) {
            if (trackingCode.equals(order.trackingCode())) {
                return true;
            }
        }
        return false;
    }

    private List<RmaReturnOrder> withRmaOrderDetailItems(List<RmaReturnOrder> orders, String token) {
        Map<String, List<String>> serialCodeCache = new HashMap<>();
        Map<String, Integer> serialCodeCursor = new HashMap<>();
        List<RmaReturnOrder> enrichedOrders = new ArrayList<>();
        for (RmaReturnOrder order : orders) {
            List<RmaInspectionItem> orderDetailItems = getOrderDetailRmaItems(
                    order.trackingCode(),
                    token,
                    serialCodeCache,
                    serialCodeCursor);
            if (orderDetailItems.isEmpty()) {
                enrichedOrders.add(order);
            } else {
                enrichedOrders.add(new RmaReturnOrder(order.trackingCode(), orderDetailItems));
            }
        }
        return enrichedOrders;
    }

    private List<RmaInspectionItem> getOrderDetailRmaItems(
            String trackingCode,
            String token,
            Map<String, List<String>> serialCodeCache,
            Map<String, Integer> serialCodeCursor) {
        JsonNode json = request("GET", "/v1/order/detail/" + encode(trackingCode), token, null, true);
        List<RmaInspectionItem> items = new ArrayList<>();
        collectOrderDetailRmaItems(json.path("data"), items, new LinkedHashSet<>(), token, serialCodeCache,
                serialCodeCursor);
        if (items.isEmpty()) {
            collectOrderDetailRmaItems(json, items, new LinkedHashSet<>(), token, serialCodeCache, serialCodeCursor);
        }
        System.out.println("Order detail items for RMA: tracking="
                + trackingCode
                + ", items="
                + rmaItemSummary(items));
        return items;
    }

    private void collectOrderDetailRmaItems(
            JsonNode node,
            List<RmaInspectionItem> items,
            Set<String> seenItemKeys,
            String token,
            Map<String, List<String>> serialCodeCache,
            Map<String, Integer> serialCodeCursor) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return;
        }
        if (node.isArray()) {
            for (JsonNode item : node) {
                collectOrderDetailRmaItems(item, items, seenItemKeys, token, serialCodeCache, serialCodeCursor);
            }
            return;
        }
        if (!node.isObject()) {
            return;
        }

        RmaInspectionItem item = toOrderDetailRmaItem(node, token, serialCodeCache, serialCodeCursor);
        if (item != null) {
            String key = item.partnerCode()
                    + "|"
                    + item.goodsCode()
                    + "|"
                    + item.quantity();
            if (seenItemKeys.add(key)) {
                items.add(item);
            }
        }

        for (JsonNode child : node) {
            collectOrderDetailRmaItems(child, items, seenItemKeys, token, serialCodeCache, serialCodeCursor);
        }
    }

    private RmaInspectionItem toOrderDetailRmaItem(
            JsonNode node,
            String token,
            Map<String, List<String>> serialCodeCache,
            Map<String, Integer> serialCodeCursor) {
        JsonNode goods = firstPresent(
                node.path("goods_id"),
                node.path("goods"),
                node.path("product"),
                node.path("sku"));
        String partnerSku = firstNonBlank(
                textOrNull(node, "partner_sku", "partnerSku"),
                textOrNull(goods, "partner_sku", "partnerSku"),
                textOrNull(node, "partner_code", "partnerCode", "sku"),
                textOrNull(goods, "partner_code", "partnerCode", "sku"));
        String goodsPartnerCode = textOrNull(goods, "partner_code", "partnerCode", "sku");
        if (partnerSku == null || partnerSku.isBlank()) {
            return null;
        }

        int quantity = firstInt(
                node,
                "quantity",
                "qty",
                "quantity_sold",
                "quantitySold",
                "quantity_order",
                "quantityOrder");
        if (quantity <= 0) {
            return null;
        }

        String goodsCode = firstNonBlank(
                textOrNull(goods, "goods_code", "goodsCode", "code"),
                textOrNull(node, "goods_code", "goodsCode", "product_code", "productCode", "goods_id", "product_id"));
        List<String> barcodes = readStringList(node.path("barcodes"));
        if (barcodes.isEmpty()) {
            barcodes = readStringList(goods.path("barcodes"));
        }
        if (barcodes.isEmpty()) {
            barcodes = new ArrayList<>();
        } else {
            barcodes = new ArrayList<>(barcodes);
        }
        if (goodsPartnerCode != null && !goodsPartnerCode.isBlank() && !barcodes.contains(goodsPartnerCode)) {
            barcodes.add(0, goodsPartnerCode);
        }
        if (!barcodes.contains(partnerSku)) {
            barcodes.add(partnerSku);
        }
        if (goodsCode != null && !goodsCode.isBlank() && !barcodes.contains(goodsCode)) {
            barcodes.add(goodsCode);
        }

        boolean serialProduct = firstBoolean(node, "is_serial_code", "isSerialCode")
                || firstBoolean(goods, "is_serial_code", "isSerialCode");
        List<String> serialCodes = serialProduct && goodsCode != null && !goodsCode.isBlank()
                ? allocatedProductSerialCodes(goodsCode, quantity, token, serialCodeCache, serialCodeCursor)
                : Collections.emptyList();
        return new RmaInspectionItem(partnerSku, goodsCode, barcodes, quantity, serialCodes);
    }

    private List<String> allocatedProductSerialCodes(
            String productCode,
            int quantity,
            String token,
            Map<String, List<String>> serialCodeCache,
            Map<String, Integer> serialCodeCursor) {
        List<String> productSerialCodes = serialCodeCache.computeIfAbsent(
                productCode,
                code -> getProductSerialCodesWithStatus(code, token, 202));
        int cursor = serialCodeCursor.getOrDefault(productCode, 0);
        int endIndex = Math.min(productSerialCodes.size(), cursor + Math.max(1, quantity));
        List<String> allocatedSerials = cursor >= endIndex
                ? Collections.emptyList()
                : new ArrayList<>(productSerialCodes.subList(cursor, endIndex));
        serialCodeCursor.put(productCode, endIndex);
        return allocatedSerials;
    }

    private List<String> getProductSerialCodesWithStatus(String productCode, String token, int statusId) {
        JsonNode json = request("GET", "/v1/inventory/list-serial/" + encode(productCode), token, null, true);
        Set<String> serialCodes = new LinkedHashSet<>();
        collectProductSerialCodesWithStatus(json.path("data"), serialCodes, statusId);
        if (serialCodes.isEmpty()) {
            collectProductSerialCodesWithStatus(json, serialCodes, statusId);
        }
        System.out.println("Product serials from API: product="
                + productCode
                + ", status_id="
                + statusId
                + ", serials="
                + serialCodes.size());
        return new ArrayList<>(serialCodes);
    }

    private void collectProductSerialCodesWithStatus(JsonNode node, Set<String> serialCodes, int expectedStatusId) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return;
        }
        if (node.isArray()) {
            for (JsonNode item : node) {
                collectProductSerialCodesWithStatus(item, serialCodes, expectedStatusId);
            }
            return;
        }
        if (!node.isObject()) {
            return;
        }
        String serialCode = textOrNull(node, "serial_code", "serialCode");
        if (serialCode != null && isUsefulSerialCode(serialCode) && readSerialStatusId(node) == expectedStatusId) {
            serialCodes.add(serialCode.trim());
        }
        for (JsonNode child : node) {
            collectProductSerialCodesWithStatus(child, serialCodes, expectedStatusId);
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

    private String rmaTrackingSummary(List<RmaReturnOrder> orders) {
        List<String> trackingCodes = new ArrayList<>();
        for (RmaReturnOrder order : orders) {
            trackingCodes.add(order.trackingCode());
        }
        return trackingCodes.toString();
    }

    private String rmaItemSummary(List<RmaInspectionItem> items) {
        List<String> parts = new ArrayList<>();
        for (RmaInspectionItem item : items) {
            parts.add(item.partnerCode()
                    + ":qty="
                    + item.quantity()
                    + ":goods="
                    + item.goodsCode()
                    + ":serials="
                    + (item.serialCodes() == null ? 0 : item.serialCodes().size()));
        }
        return parts.toString();
    }
}

