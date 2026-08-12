package com.example.seleniumtestng.utils;

import com.example.seleniumtestng.models.InboundPackageData;
import com.example.seleniumtestng.models.InboundProductData;
import com.example.seleniumtestng.models.OrderProductData;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class TestDataReader {
    private static final String PRODUCT_DATA_RESOURCE = "testdata/products.json";
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final JsonNode PRODUCT_DATA = loadProductData();

    private TestDataReader() {
    }

    public static List<InboundProductData> inboundProducts() {
        List<InboundProductData> products = MAPPER.convertValue(
                requiredSection("inboundProducts"),
                new TypeReference<List<InboundProductData>>() {
                });
        if (products == null || products.isEmpty()) {
            throw new IllegalStateException("Inbound product test data must not be empty");
        }
        validateInboundProducts(products);
        return List.copyOf(products);
    }

    public static List<InboundPackageData> inboundPackages() {
        JsonNode packagesSection = PRODUCT_DATA.path("inboundPackages");
        if (!packagesSection.isMissingNode() && packagesSection.isArray() && !packagesSection.isEmpty()) {
            List<InboundPackageData> packages = MAPPER.convertValue(
                    packagesSection,
                    new TypeReference<List<InboundPackageData>>() {
                    });
            validateInboundPackages(packages);
            return List.copyOf(packages);
        }
        return List.of(new InboundPackageData(inboundProducts()));
    }

    public static List<InboundProductData> flattenInboundProducts(List<InboundPackageData> packages) {
        List<InboundProductData> products = new ArrayList<>();
        for (InboundPackageData inboundPackage : packages) {
            if (inboundPackage != null && inboundPackage.getProducts() != null) {
                products.addAll(inboundPackage.getProducts());
            }
        }
        return List.copyOf(products);
    }

    public static List<OrderProductData> orderProducts() {
        List<OrderProductData> products = MAPPER.convertValue(
                requiredSection("orderProducts"),
                new TypeReference<List<OrderProductData>>() {
                });
        validateOrderProducts(products);
        return List.copyOf(products);
    }

    private static JsonNode loadProductData() {
        try (InputStream stream = TestDataReader.class.getClassLoader()
                .getResourceAsStream(PRODUCT_DATA_RESOURCE)) {
            if (stream == null) {
                throw new IllegalStateException("Test data resource not found: " + PRODUCT_DATA_RESOURCE);
            }
            return MAPPER.readTree(stream);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to read product test data", e);
        }
    }

    private static JsonNode requiredSection(String name) {
        JsonNode section = PRODUCT_DATA.path(name);
        if (!section.isArray()) {
            throw new IllegalStateException("Product test data section must be an array: " + name);
        }
        return section;
    }

    private static void validateInboundProducts(List<InboundProductData> products) {
        Set<String> uniqueSkus = new HashSet<>();
        for (int index = 0; index < products.size(); index++) {
            InboundProductData product = products.get(index);
            if (product == null) {
                throw new IllegalStateException("Inbound product is null at index " + index);
            }
            validateProduct(product.getSku(), product.getQuantity(), "Inbound", index, uniqueSkus);
        }
    }

    private static void validateInboundPackages(List<InboundPackageData> packages) {
        if (packages == null || packages.isEmpty()) {
            throw new IllegalStateException("Inbound package test data must not be empty");
        }
        for (int packageIndex = 0; packageIndex < packages.size(); packageIndex++) {
            InboundPackageData inboundPackage = packages.get(packageIndex);
            if (inboundPackage == null) {
                throw new IllegalStateException("Inbound package is null at index " + packageIndex);
            }
            List<InboundProductData> products = inboundPackage.getProducts();
            if (products == null || products.isEmpty()) {
                throw new IllegalStateException("Inbound package products must not be empty at index " + packageIndex);
            }
            Set<String> uniqueSkus = new HashSet<>();
            for (int productIndex = 0; productIndex < products.size(); productIndex++) {
                InboundProductData product = products.get(productIndex);
                if (product == null) {
                    throw new IllegalStateException("Inbound package product is null at package "
                            + packageIndex
                            + ", product "
                            + productIndex);
                }
                validateProduct(
                        product.getSku(),
                        product.getQuantity(),
                        "Inbound package " + packageIndex,
                        productIndex,
                        uniqueSkus);
            }
        }
    }

    private static void validateOrderProducts(List<OrderProductData> products) {
        if (products == null || products.isEmpty()) {
            throw new IllegalStateException("Order product test data must not be empty");
        }
        Set<String> uniqueSkus = new HashSet<>();
        for (int index = 0; index < products.size(); index++) {
            OrderProductData product = products.get(index);
            if (product == null) {
                throw new IllegalStateException("Order product is null at index " + index);
            }
            validateProduct(product.getSku(), product.getQuantity(), "Order", index, uniqueSkus);
        }
    }

    private static void validateProduct(
            String sku,
            int quantity,
            String context,
            int index,
            Set<String> uniqueSkus) {
        if (sku == null || sku.isBlank()) {
            throw new IllegalStateException(context + " product SKU is blank at index " + index);
        }
        if (quantity <= 0) {
            throw new IllegalStateException(context + " product quantity must be positive at index " + index);
        }
        String normalizedSku = sku.toLowerCase(Locale.ROOT);
        if (!uniqueSkus.add(normalizedSku)) {
            throw new IllegalStateException("Duplicate " + context.toLowerCase(Locale.ROOT) + " product SKU: " + sku);
        }
    }
}
