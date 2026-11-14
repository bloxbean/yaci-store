package com.bloxbean.cardano.yaci.store.analytics.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class DuckDbPlatformCheckTest {

    @Test
    void passesOnASupportedPlatform() {
        assertThatCode(() -> new DuckDbPlatformCheck().afterPropertiesSet()).doesNotThrowAnyException();
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
