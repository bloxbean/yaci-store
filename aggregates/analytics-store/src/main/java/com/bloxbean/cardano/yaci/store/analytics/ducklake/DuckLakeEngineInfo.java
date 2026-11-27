package com.bloxbean.cardano.yaci.store.analytics.ducklake;

import com.bloxbean.cardano.yaci.store.analytics.helper.DuckDbConnectionHelper.CatalogVersions;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Engine and catalog versions found by {@link DuckLakeCatalogInitializer} at startup, for
 * {@code GET /api/v1/analytics/admin/status} (ADR 0008 D2). Empty until the catalog is attached.
 */
@Component
@ConditionalOnProperty(prefix = "yaci.store.analytics.storage", name = "type", havingValue = "ducklake")
public class DuckLakeEngineInfo {

    private volatile Snapshot snapshot;

    void record(String catalogType, CatalogVersions versions) {
        this.snapshot = new Snapshot(catalogType, versions.duckdbVersion(),
                versions.ducklakeExtensionVersion(), versions.catalogSpecVersion());
    }

    /** The recorded versions, or {@code null} before the catalog has been attached. */
    public Snapshot snapshot() {
        return snapshot;
    }

    public record Snapshot(String catalogType, String duckdbVersion, String ducklakeExtensionVersion,
                           String catalogSpecVersion) {
    }
}
