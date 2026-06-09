package com.example.seleniumtestng.config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public final class ConfigReader {
    private static final Properties PROPERTIES = new Properties();
    private static final Map<String, String> DOTENV = new HashMap<>();

    static {
        try (InputStream stream = ConfigReader.class.getClassLoader().getResourceAsStream("config.properties")) {
            if (stream != null) {
                PROPERTIES.load(stream);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Unable to load config.properties", e);
        }
        loadDotEnv();
    }

    private ConfigReader() {
    }

    public static String get(String key) {
        String fromSystemProperty = System.getProperty(key);
        if (hasText(fromSystemProperty)) {
            return fromSystemProperty;
        }

        String fromEnv = System.getenv(key);
        if (hasText(fromEnv)) {
            return fromEnv;
        }

        String fromDotEnv = DOTENV.get(key);
        if (hasText(fromDotEnv)) {
            return fromDotEnv;
        }

        return PROPERTIES.getProperty(key);
    }

    public static String required(String key) {
        String value = get(key);
        if (!hasText(value)) {
            throw new IllegalStateException("Missing required config value: " + key);
        }
        return value;
    }

    public static Duration timeout() {
        return Duration.ofMillis(Long.parseLong(getOrDefault("TEST_TIMEOUT", "15000")));
    }

    public static String getOrDefault(String key, String defaultValue) {
        String value = get(key);
        return hasText(value) ? value : defaultValue;
    }

    private static void loadDotEnv() {
        Path envPath = Path.of(".env");
        if (!Files.exists(envPath)) {
            return;
        }

        try {
            for (String rawLine : Files.readAllLines(envPath, StandardCharsets.UTF_8)) {
                String line = rawLine.trim();
                if (line.isEmpty() || line.startsWith("#") || !line.contains("=")) {
                    continue;
                }
                int separator = line.indexOf('=');
                String key = line.substring(0, separator).trim();
                String value = line.substring(separator + 1).trim();
                DOTENV.put(key, stripQuotes(value));
            }
        } catch (IOException e) {
            throw new IllegalStateException("Unable to read .env", e);
        }
    }

    private static String stripQuotes(String value) {
        if ((value.startsWith("\"") && value.endsWith("\"")) || (value.startsWith("'") && value.endsWith("'"))) {
            return value.substring(1, value.length() - 1);
        }
        return value;
    }

    private static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
