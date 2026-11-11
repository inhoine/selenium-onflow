package com.example.seleniumtestng.config;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
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
                PROPERTIES.load(new InputStreamReader(stream, StandardCharsets.UTF_8));
            }
        } catch (IOException e) {
            throw new IllegalStateException("Unable to load config.properties", e);
        }

        // Optional multi-environment overlay (e.g. -Denv=uat -> loads config.uat.properties)
        String env = System.getProperty("env");
        if (env != null && !env.isBlank()) {
            String envConfigFile = "config." + env.trim().toLowerCase() + ".properties";
            try (InputStream envStream = ConfigReader.class.getClassLoader().getResourceAsStream(envConfigFile)) {
                if (envStream != null) {
                    PROPERTIES.load(new InputStreamReader(envStream, StandardCharsets.UTF_8));
                }
            } catch (IOException ignored) {
            }
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

    public static int requiredInt(String key) {
        String value = required(key);
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new IllegalStateException("Config value must be an integer: " + key + "=" + value, e);
        }
    }

    public static boolean getBoolean(String key, boolean defaultValue) {
        String value = get(key);
        if (!hasText(value)) {
            return defaultValue;
        }
        return Boolean.parseBoolean(value.trim());
    }

    public static String getOrDefault(String key, String defaultValue) {
        String value = get(key);
        return hasText(value) ? value : defaultValue;
    }

    public static Duration timeout() {
        return Duration.ofMillis(Long.parseLong(getOrDefault("TEST_TIMEOUT", "15000")));
    }

    private static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private static void loadDotEnv() {
        Path dotEnvPath = Path.of(".env");
        if (!Files.isRegularFile(dotEnvPath)) {
            return;
        }
        try {
            for (String rawLine : Files.readAllLines(dotEnvPath, StandardCharsets.UTF_8)) {
                String line = rawLine.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                int equalsIndex = line.indexOf('=');
                if (equalsIndex <= 0) {
                    continue;
                }
                String key = line.substring(0, equalsIndex).trim();
                String value = line.substring(equalsIndex + 1).trim();
                if ((value.startsWith("\"") && value.endsWith("\"")) || (value.startsWith("'") && value.endsWith("'"))) {
                    value = value.substring(1, value.length() - 1);
                }
                DOTENV.put(key, value);
            }
        } catch (IOException ignored) {
        }
    }
}
