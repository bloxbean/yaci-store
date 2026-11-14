package com.bloxbean.cardano.yaci.store.analytics.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Fails the analytics store at startup, with a clear message, on a platform where DuckDB 2.x
 * cannot run (ADR 0008 D1).
 *
 * <ul>
 *   <li><b>Native library.</b> From 2.0 the default {@code duckdb_jdbc} jar bundles
 *       {@code macos_arm64} instead of a macOS universal library, so on Intel macOS the first
 *       DuckDB connection would otherwise fail deep inside an export with an
 *       {@code UnsatisfiedLinkError}.</li>
 *   <li><b>CPU level on x86-64.</b> The {@code ducklake} and {@code postgres} extensions that
 *       DuckDB 2.0.0 downloads for {@code linux_amd64} are compiled for x86-64-v3 (AVX2, BMI2,
 *       FMA, ...; duckdb/duckdb#27691). On an older CPU, or a VM whose virtual CPU model hides
 *       those instructions, {@code LOAD ducklake} raises SIGILL and kills the whole JVM, indexer
 *       included. The core library in the JDBC jar still runs there, so this check reads the CPU
 *       flags instead of relying on a failed load.</li>
 * </ul>
 */
@Component
@ConditionalOnProperty(prefix = "yaci.store.analytics", name = "enabled", havingValue = "true")
@Order(Ordered.HIGHEST_PRECEDENCE)
@RequiredArgsConstructor
@Slf4j
public class DuckDbPlatformCheck implements InitializingBean {

    /** x86-64-v3 as Linux names the flags in /proc/cpuinfo ({@code abm} is LZCNT). */
    static final List<String> X86_64_V3_FLAGS =
            List.of("avx", "avx2", "bmi1", "bmi2", "f16c", "fma", "abm", "movbe", "xsave");

    private static final Path CPUINFO = Path.of("/proc/cpuinfo");

    private final AnalyticsStoreProperties properties;

    @Override
    public void afterPropertiesSet() {
        if (properties.getDuckdb().isCpuCheck()) {
            checkCpu();
        }
        try (Connection ignored = DriverManager.getConnection("jdbc:duckdb:")) {
            log.debug("DuckDB native library loaded for {} {}", osName(), osArch());
        } catch (Throwable t) {
            throw new IllegalStateException(unsupportedPlatformMessage(osName(), osArch(), t), t);
        }
    }

    private void checkCpu() {
        if (!isX86_64(osArch())) {
            return;
        }
        String os = osName() == null ? "" : osName().toLowerCase(Locale.ROOT);
        if (!os.contains("linux")) {
            log.info("The analytics store needs an x86-64-v3 CPU (AVX2, BMI2, FMA) for DuckDB's ducklake and "
                    + "postgres extensions; this cannot be verified on {}.", osName());
            return;
        }
        String cpuinfo;
        try {
            cpuinfo = Files.readString(CPUINFO);
        } catch (IOException | RuntimeException e) {
            log.warn("Could not read {} to check the CPU level DuckDB's extensions need (x86-64-v3): {}",
                    CPUINFO, e.toString());
            return;
        }
        List<String> missing = missingCpuFlags(cpuinfo);
        if (!missing.isEmpty()) {
            throw new IllegalStateException(unsupportedCpuMessage(missing));
        }
    }

    /**
     * The x86-64-v3 flags absent from the first {@code flags} line of {@code /proc/cpuinfo}, or an
     * empty list when there is no such line (unknown format: do not block).
     */
    static List<String> missingCpuFlags(String cpuinfo) {
        Set<String> flags = cpuinfo.lines()
                .filter(l -> l.startsWith("flags") && l.indexOf(':') > 0)
                .findFirst()
                .map(l -> Arrays.stream(l.substring(l.indexOf(':') + 1).trim().split("\\s+"))
                        .collect(Collectors.toSet()))
                .orElse(null);
        if (flags == null) {
            return List.of();
        }
        return X86_64_V3_FLAGS.stream().filter(f -> !flags.contains(f)).toList();
    }

    static String unsupportedCpuMessage(List<String> missing) {
        return "The analytics store cannot run on this CPU: it lacks " + String.join(", ", missing)
                + ". DuckDB 2.0's ducklake and postgres extensions for linux_amd64 need an x86-64-v3 CPU "
                + "(AVX2, BMI2, FMA: Intel Haswell or later, any AMD Zen), and loading them on this CPU would "
                + "crash the whole process with SIGILL (duckdb/duckdb#27691). In a VM, expose the host CPU "
                + "model (e.g. QEMU/KVM '-cpu host' or 'x86-64-v3', Proxmox CPU type 'host'). Otherwise disable "
                + "the analytics store (yaci.store.analytics.enabled=false). Set "
                + "yaci.store.analytics.duckdb.cpu-check=false only if the extensions in use are known to run "
                + "on this CPU.";
    }

    static String unsupportedPlatformMessage(String osName, String osArch, Throwable cause) {
        String os = osName == null ? "" : osName.toLowerCase(Locale.ROOT);
        String base = "The analytics store cannot load DuckDB's native library on " + osName + " " + osArch
                + " (" + rootMessage(cause) + "). ";
        if (os.contains("mac") && isX86_64(osArch)) {
            return base + "DuckDB 2.x has no bundled library for macOS x86_64: add "
                    + "org.duckdb:duckdb_jdbc:<version>:macos_amd64 to the classpath, run on Apple silicon or "
                    + "Linux, or disable the analytics store (yaci.store.analytics.enabled=false).";
        }
        return base + "Bundled libraries cover linux_amd64, linux_arm64, macos_arm64 and windows_amd64. "
                + "Run on one of those platforms, or disable the analytics store "
                + "(yaci.store.analytics.enabled=false).";
    }

    private static boolean isX86_64(String osArch) {
        String arch = osArch == null ? "" : osArch.toLowerCase(Locale.ROOT);
        return arch.equals("x86_64") || arch.equals("amd64");
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
