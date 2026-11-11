package com.example.seleniumtestng.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Centralized logger utility wrapping SLF4J.
 * Replaces unstructured System.out.println with level-based logging.
 */
public final class Log {
    private static final Logger LOGGER = LoggerFactory.getLogger("AutomationTest");

    private Log() {
    }

    public static void info(String message, Object... args) {
        LOGGER.info(message, args);
    }

    public static void warn(String message, Object... args) {
        LOGGER.warn(message, args);
    }

    public static void error(String message, Object... args) {
        LOGGER.error(message, args);
    }

    public static void debug(String message, Object... args) {
        LOGGER.debug(message, args);
    }
}

