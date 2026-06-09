package com.example.seleniumtestng.utils;

import com.example.seleniumtestng.config.ConfigReader;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class WmsApiClient {
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
        JsonNode json = request("GET", "/v1/po/detail/" + poCode + "/", token, null, true);
        JsonNode poSkus = json.path("data").path("po_skus");
        if (!poSkus.isArray() || poSkus.isEmpty()) {
            throw new IllegalStateException("No SKU found in PO: " + poCode);
        }

        List<POSku> result = new ArrayList<>();
        for (JsonNode sku : poSkus) {
            JsonNode goods = sku.path("goods_id");
            result.add(new POSku(
                    sku.path("box_code").asText(),
                    sku.path("quantity_inbound").asInt(),
                    goods.path("partner_code").asText(null),
                    numberOrNull(goods.path("goods_w")),
                    numberOrNull(goods.path("goods_d")),
                    numberOrNull(goods.path("goods_h")),
                    numberOrNull(goods.path("goods_weight"))));
        }
        return result;
    }

    public void updatePutaway(String inboundCode, String token) {
        JsonNode todo = request("GET", "/v1/putaway/todo?page_size=100&page=1&stock_level=A", token, null, false);
        int updated = 0;
        for (JsonNode task : todo.path("data")) {
            if (!inboundCode.equals(task.path("shipment_id").path("shipment_po").asText())) {
                continue;
            }
            String body = "{"
                    + "\"dr_id\":" + task.path("dr_id").asLong() + ","
                    + "\"location_code\":\"A-01-02-006\","
                    + "\"quantity_putaway\":" + task.path("quantity").asInt()
                    + "}";
            request("POST", "/v1/putaway/update?", token, body, false);
            updated++;
        }
        if (updated == 0) {
            throw new IllegalStateException("No putaway task found for PO " + inboundCode);
        }
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
                throw new IllegalStateException("WMS API failed " + response.statusCode()
                        + " " + method + " " + path
                        + (body == null ? "" : " body=" + body)
                        + ": " + json);
            }
            return json;
        } catch (IOException e) {
            throw new IllegalStateException("Unable to call WMS API", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while calling WMS API", e);
        }
    }

    private Number numberOrNull(JsonNode node) {
        return node == null || node.isMissingNode() || node.isNull() ? null : node.numberValue();
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

    private List<JsonNode> iterableToList(JsonNode arrayNode) {
        List<JsonNode> values = new ArrayList<>();
        for (JsonNode item : arrayNode) {
            values.add(item);
        }
        return values;
    }
}
