package com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.model;

import jakarta.annotation.Nullable;

import java.util.Map;

/**
 * Internal representation of a parsed CIP-68 inline datum.
 * <p>
 * Richer than {@link FungibleTokenMetadata} (the public DTO the API surfaces for FTs) —
 * carries the additional fields that NFT and RFT datums populate (image, mediaType, files,
 * arbitrary collection-specific properties). The parser produces this; the processor uses
 * it to populate the {@code cip68_metadata} row, including {@code image}, {@code media_type},
 * and the {@code properties} JSONB column.
 * <p>
 * The {@link #properties} map's documented shape is:
 * <pre>{@code
 * {
 *   "files": [{"name": "...", "mediaType": "...", "src": "..."}, ...],
 *   "additional_properties": { ...arbitrary collection-specific keys... }
 * }
 * }</pre>
 * Either key may be absent. The {@link #properties} field as a whole may be {@code null}
 * if the datum has no files and no additional properties beyond the well-known scalars.
 */
public record ParsedCip68Datum(
        @Nullable Long decimals,
        @Nullable String description,
        @Nullable String logo,
        @Nullable String name,
        @Nullable String ticker,
        @Nullable String url,
        Long version,
        @Nullable String image,
        @Nullable String mediaType,
        @Nullable Map<String, Object> properties
) {
    /**
     * Project to the FT-shape DTO for read-path consumers (the API today only surfaces
     * FT data; NFT-specific fields are stored but not yet exposed).
     */
    public FungibleTokenMetadata toFungibleTokenMetadata() {
        return new FungibleTokenMetadata(decimals, description, logo, name, ticker, url, version);
    }
}
