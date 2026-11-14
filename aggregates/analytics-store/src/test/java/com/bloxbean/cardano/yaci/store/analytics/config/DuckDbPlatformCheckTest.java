package com.bloxbean.cardano.yaci.store.analytics.config;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class DuckDbPlatformCheckTest {

    /** A KVM guest with QEMU's default CPU model (measured on a benchmark VM): x86-64-v2, no AVX. */
    private static final String QEMU_V2_CPUINFO = """
            processor	: 0
            vendor_id	: AuthenticAMD
            model name	: QEMU Virtual CPU version 2.5+
            flags		: fpu de pse tsc msr pae mce cx8 apic sep mtrr pge mca cmov pat pse36 clflush mmx fxsr sse \
            sse2 ht syscall nx lm rep_good nopl xtopology cpuid extd_apicid tsc_known_freq pni ssse3 cx16 sse4_1 \
            sse4_2 x2apic popcnt aes hypervisor lahf_lm cmp_legacy 3dnowprefetch vmmcall

            processor	: 1
            """;

    /** Haswell, the first Intel generation at x86-64-v3. */
    private static final String HASWELL_CPUINFO = """
            model name	: Intel(R) Xeon(R) CPU E5-2680 v3 @ 2.50GHz
            flags		: fpu vme de pse tsc msr pae mce cx8 apic sep mtrr pge mca cmov pat pse36 clflush mmx fxsr \
            sse sse2 ss ht syscall nx pdpe1gb rdtscp lm constant_tsc rep_good nopl xtopology cpuid pni pclmulqdq ssse3 \
            fma cx16 pcid sse4_1 sse4_2 x2apic movbe popcnt aes xsave avx f16c rdrand hypervisor lahf_lm abm \
            fsgsbase bmi1 avx2 smep bmi2 erms invpcid xsaveopt
            """;

    @Test
    void passesOnASupportedPlatform() {
        assertThatCode(() -> new DuckDbPlatformCheck(new AnalyticsStoreProperties()).afterPropertiesSet())
                .doesNotThrowAnyException();
    }

    @Test
    void cpuBelowX8664V3IsReportedWithTheMissingFlags() {
        assertThat(DuckDbPlatformCheck.missingCpuFlags(QEMU_V2_CPUINFO))
                .containsExactly("avx", "avx2", "bmi1", "bmi2", "f16c", "fma", "abm", "movbe", "xsave");
    }

    @Test
    void x8664V3CpuPasses() {
        assertThat(DuckDbPlatformCheck.missingCpuFlags(HASWELL_CPUINFO)).isEmpty();
    }

    @Test
    void unknownCpuinfoFormatDoesNotBlock() {
        assertThat(DuckDbPlatformCheck.missingCpuFlags("processor : 0\n")).isEmpty();
    }

    @Test
    void cpuMessageExplainsTheCrashAndTheWayOut() {
        String message = DuckDbPlatformCheck.unsupportedCpuMessage(List.of("avx2", "bmi2"));
        assertThat(message)
                .contains("lacks avx2, bmi2")
                .contains("x86-64-v3")
                .contains("SIGILL")
                .contains("duckdb/duckdb#27691")
                .contains("yaci.store.analytics.enabled=false")
                .contains("yaci.store.analytics.duckdb.cpu-check=false");
    }

    @Test
    void intelMacMessageNamesTheClassifierJar() {
        Throwable cause = new ExceptionInInitializerError(new UnsatisfiedLinkError("Can't load library: duckdb_java"));
        String message = DuckDbPlatformCheck.unsupportedPlatformMessage("Mac OS X", "x86_64", cause);
        assertThat(message)
                .contains("macOS x86_64")
                .contains("duckdb_jdbc:<version>:macos_amd64")
                .contains("UnsatisfiedLinkError: Can't load library: duckdb_java")
                .contains("yaci.store.analytics.enabled=false");
    }

    @Test
    void otherPlatformsListTheBundledLibraries() {
        String message = DuckDbPlatformCheck.unsupportedPlatformMessage("FreeBSD", "riscv64",
                new UnsatisfiedLinkError("no library"));
        assertThat(message).contains("linux_amd64, linux_arm64, macos_arm64 and windows_amd64");
    }
}
