package com.example.seleniumtestng.clients;

import com.example.seleniumtestng.config.ConfigReader;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Base HTTP client providing resilient REST API execution, retries,
 * common JSON traversal and parsing helpers for WMS API interactions.
 */
public abstract class BaseApiClient {

    protected final HttpClient client;
    protected final ObjectMapper mapper;
    protected final String baseUrl;

    protected BaseApiClient() {
        this.client = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.ALWAYS)
                .build();
        this.mapper = new ObjectMapper();
        this.baseUrl = ConfigReader.required("WMS_BASE_URL");
    }

    protected JsonNode request(String method, String path, String token, String body, boolean bearer) {
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

    protected boolean isRetryableStatus(int statusCode) {
        return statusCode == 429 || statusCode >= 500;
    }

    protected void sleepBeforeRetry(int attempt, String method, String path, Exception error) {
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

    protected String authorizationHeader(String token, boolean bearer) {
        String normalized = token.trim();
        if ((normalized.startsWith("\"") && normalized.endsWith("\""))
                || (normalized.startsWith("'") && normalized.endsWith("'"))) {
            normalized = normalized.substring(1, normalized.length() - 1).trim();
        }
        if (bearer && !normalized.regionMatches(true, 0, "Bearer ", 0, "Bearer ".length())) {
            return "Bearer " + normalized;
        }
        return normalized;
    }

    protected String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    protected JsonNode firstPresent(JsonNode... nodes) {
        for (JsonNode node : nodes) {
            if (node != null && !node.isMissingNode() && !node.isNull()) {
                return node;
            }
        }
        return mapper.nullNode();
    }

    protected String textOrNull(JsonNode node, String... fields) {
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

    protected int firstInt(JsonNode node, String... fields) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return 0;
        }
        for (String field : fields) {
            JsonNode value = node.path(field);
            if (value.canConvertToInt()) {
                return value.asInt();
            }
            if (value.isTextual()) {
                try {
                    return Integer.parseInt(value.asText().trim());
                } catch (NumberFormatException ignored) {
                }
            }
            if (value.isObject()) {
                int nested = firstInt(value, "status_id", "statusId", "id", "value", field);
                if (nested != 0) {
                    return nested;
                }
            }
        }
        return 0;
    }

    protected boolean firstBoolean(JsonNode node, String... fields) {
        for (String field : fields) {
            JsonNode value = node.path(field);
            if (value.isBoolean()) {
                return value.asBoolean();
            }
            if (value.isNumber()) {
                return value.asInt() != 0;
            }
            if (value.isTextual()) {
                String text = value.asText("").trim();
                if ("true".equalsIgnoreCase(text) || "1".equals(text)) {
                    return true;
                }
                if ("false".equalsIgnoreCase(text) || "0".equals(text)) {
                    return false;
                }
            }
        }
        return false;
    }

    protected Number numberOrNull(JsonNode node) {
        return node == null || node.isMissingNode() || node.isNull() ? null : node.numberValue();
    }

    protected String firstTextRecursive(JsonNode node, String... fields) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }
        if (node.isObject()) {
            for (String field : fields) {
                JsonNode value = node.path(field);
                if (!value.isMissingNode() && !value.isNull() && !value.asText().isBlank()) {
                    return value.asText();
                }
            }
        }
        for (JsonNode child : node) {
            String value = firstTextRecursive(child, fields);
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    protected String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    protected String firstListValue(List<String> values) {
        if (values == null || values.isEmpty()) {
            return null;
        }
        return values.get(0);
    }

    protected List<String> readStringList(JsonNode node) {
        if (node == null || !node.isArray()) {
            return Collections.emptyList();
        }
        List<String> values = new ArrayList<>();
        for (JsonNode item : node) {
            values.add(item.asText());
        }
        return values;
    }

    protected List<JsonNode> iterableToList(JsonNode arrayNode) {
        List<JsonNode> values = new ArrayList<>();
        for (JsonNode item : arrayNode) {
            values.add(item);
        }
        return values;
    }

    protected String normalized(String value) {
        return value == null ? "" : value.replaceAll("[^A-Za-z0-9]", "").toLowerCase();
    }

    protected JsonNode itemArray(JsonNode json) {
        JsonNode data = json.path("data");
        if (data.isArray()) {
            return data;
        }
        for (String field : Arrays.asList("pickup_list", "pickups", "pickup_orders", "items", "list", "results",
                "boxes", "baskets", "records", "rows", "data")) {
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

    protected boolean isInvalidPage(IllegalStateException e) {
        String message = e.getMessage();
        return message != null && message.toLowerCase().contains("invalid page");
    }
}

