package com.bloxbean.cardano.yaci.store.analytics.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Locale;

/**
 * Fails the analytics store at startup, with a clear message, when the DuckDB JDBC driver has no
 * native library for this platform (ADR 0008 D1).
 *
 * <p>From 2.0 the default {@code duckdb_jdbc} jar bundles {@code macos_arm64} instead of a macOS
 * universal library, so on Intel macOS the first DuckDB connection would otherwise fail deep
 * inside an export with an {@code UnsatisfiedLinkError}.</p>
 */
@Component
@ConditionalOnProperty(prefix = "yaci.store.analytics", name = "enabled", havingValue = "true")
@Order(Ordered.HIGHEST_PRECEDENCE)
@Slf4j
public class DuckDbPlatformCheck implements InitializingBean {

    @Override
    public void afterPropertiesSet() {
        try (Connection ignored = DriverManager.getConnection("jdbc:duckdb:")) {
            log.debug("DuckDB native library loaded for {} {}", osName(), osArch());
        } catch (Throwable t) {
            throw new IllegalStateException(unsupportedPlatformMessage(osName(), osArch(), t), t);
        }
    }

    static String unsupportedPlatformMessage(String osName, String osArch, Throwable cause) {
        String os = osName == null ? "" : osName.toLowerCase(Locale.ROOT);
        String arch = osArch == null ? "" : osArch.toLowerCase(Locale.ROOT);
        String base = "The analytics store cannot load DuckDB's native library on " + osName + " " + osArch
                + " (" + rootMessage(cause) + "). ";
        if (os.contains("mac") && (arch.equals("x86_64") || arch.equals("amd64"))) {
            return base + "DuckDB 2.x has no bundled library for macOS x86_64: add "
                    + "org.duckdb:duckdb_jdbc:<version>:macos_amd64 to the classpath, run on Apple silicon or "
                    + "Linux, or disable the analytics store (yaci.store.analytics.enabled=false).";
        }
        return base + "Bundled libraries cover linux_amd64, linux_arm64, macos_arm64 and windows_amd64. "
                + "Run on one of those platforms, or disable the analytics store "
                + "(yaci.store.analytics.enabled=false).";
    }

    private static String rootMessage(Throwable t) {
        Throwable root = t;
        while (root.getCause() != null && root.getCause() != root) {
            root = root.getCause();
        }
        return root.getClass().getSimpleName() + ": " + root.getMessage();
    }

    private static String osName() {
        return System.getProperty("os.name");
    }

    private static String osArch() {
        return System.getProperty("os.arch");
    }
}
