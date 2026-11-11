package com.example.seleniumtestng.flows;

import com.example.seleniumtestng.clients.WmsApiClient;
import com.example.seleniumtestng.config.ConfigReader;
import com.example.seleniumtestng.utils.Log;
import java.util.Arrays;
import java.util.List;
import org.openqa.selenium.WebDriver;

/**
 * Resolves an active Pickup Order ID for WMS Pick & Pack testing using a 3-tier
 * strategy:
 * - Tier 1: Configured DEFAULT_PICKUP_ID (if valid and not yet completed)
 * - Tier 2: Auto-discovery via WMS API (finds active pickup ready for packing)
 * - Tier 3: On-demand auto-creation (creates B2C orders & pickup via
 * OrderCreationFlow)
 */
public class PickupResolver {
    private final WebDriver driver;
    private final WmsApiClient wmsApiClient;

    public PickupResolver(WebDriver driver, WmsApiClient wmsApiClient) {
        this.driver = driver;
        this.wmsApiClient = wmsApiClient;
    }

    /**
     * Resolves a list of active Pickup Order IDs.
     * Supports multiple comma-separated IDs from DEFAULT_PICKUP_IDS or DEFAULT_PICKUP_ID.
     * Falls back to single pickup resolution if only one is configured or auto-discovered.
     */
    public List<String> resolvePickupIds(String token) {
        String multiConfig = ConfigReader.get("DEFAULT_PICKUP_IDS");
        if (multiConfig == null || multiConfig.isBlank()) {
            multiConfig = ConfigReader.get("DEFAULT_PICKUP_ID");
        }
        if (multiConfig != null && !multiConfig.isBlank()) {
            List<String> list = Arrays.stream(multiConfig.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isBlank())
                    .toList();
            if (!list.isEmpty()) {
                Log.info("Resolved pickup IDs from config (count={}): {}", list.size(), list);
                return list;
            }
        }
        return List.of(resolvePickupId(token));
    }

    public String resolvePickupId(String token) {
        boolean dynamicEnabled = ConfigReader.getBoolean("DYNAMIC_PICKUP_ENABLED", true);
        String configuredId = ConfigReader.get("DEFAULT_PICKUP_IDS");
        if (configuredId == null || configuredId.isBlank()) {
            configuredId = ConfigReader.get("DEFAULT_PICKUP_ID");
        }
        if (configuredId != null && configuredId.contains(",")) {
            configuredId = configuredId.split(",")[0].trim();
        }

        if (!dynamicEnabled) {
            Log.info("Dynamic pickup disabled by config. Using static DEFAULT_PICKUP_IDS={}", configuredId);
            return configuredId != null && !configuredId.isBlank() ? configuredId : ConfigReader.required("DEFAULT_PICKUP_IDS");
        }

        // Tier 1: Check configured DEFAULT_PICKUP_IDS
        if (configuredId != null && !configuredId.isBlank()) {
            Log.info("DynamicPickup Tier 1: Validating configured pickupId={}...", configuredId);
            if (wmsApiClient.isPickupReadyForPacking(configuredId, token)) {
                Log.info("DynamicPickup Tier 1 SUCCESS: Configured pickup {} is valid and ready for packing.",
                        configuredId);
                return configuredId;
            }
            Log.warn(
                    "DynamicPickup Tier 1: Configured pickup {} is already packed, completed, or invalid. Moving to Tier 2...",
                    configuredId);
        }

        // Tier 2: Auto-Discovery via API
        boolean autoDiscoverEnabled = ConfigReader.getBoolean("DYNAMIC_PICKUP_AUTO_DISCOVER", true);
        if (autoDiscoverEnabled) {
            Log.info("DynamicPickup Tier 2: Searching for active pickup ready for packing via WMS API...");
            String discoveredId = wmsApiClient.findAvailablePackingPickup(token);
            if (discoveredId != null && !discoveredId.isBlank()) {
                Log.info("DynamicPickup Tier 2 SUCCESS: Found available pickup on system: {}", discoveredId);
                return discoveredId;
            }
            Log.warn("DynamicPickup Tier 2: No active pickup ready for packing found via API. Moving to Tier 3...");
        }

        // Tier 3: On-Demand Auto-Creation
        boolean autoCreateEnabled = ConfigReader.getBoolean("AUTO_CREATE_PICKUP_IF_NONE", true);
        if (autoCreateEnabled) {
            Log.info("DynamicPickup Tier 3: Triggering automated order & pickup creation on-demand...");
            try {
                OrderCreationFlow orderCreationFlow = new OrderCreationFlow(driver);
                int orderCount = Integer.parseInt(ConfigReader.getOrDefault("CREATE_ORDER_COUNT", "1"));
                String newPickupId = orderCreationFlow.createOrdersAndPickupOnDemand(orderCount);
                if (newPickupId != null && !newPickupId.isBlank()) {
                    Log.info("DynamicPickup Tier 3 SUCCESS: Created new pickup {} on-demand.", newPickupId);
                    return newPickupId;
                }
                Log.error(
                        "DynamicPickup Tier 3: Automated creation completed but pickup code could not be determined.");
            } catch (Exception e) {
                Log.error("DynamicPickup Tier 3 FAILED with exception: {}", e.getMessage(), e);
            }
        }

        throw new IllegalStateException("Dynamic pickup resolution failed: Configured pickup '"
                + configuredId
                + "' is already packed/completed, no ready pickups found via API (Tier 2), and on-demand auto-creation (Tier 3) did not produce a valid pickup.");
    }
}
