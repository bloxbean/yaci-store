package com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.model;

import com.bloxbean.cardano.yaci.store.extensions.assetstore.validation.ValidationError;
import jakarta.annotation.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Set;

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
 * <p>
 * {@link #validationErrors} are the ways the datum departs from CIP-68 that do not depend on the label of the token
 * (a wrong type, a version CIP-68 does not define, a Plutus constructor in a property, ...). What a label requires
 * is checked later, by {@code Cip68Validator}.
 */
public record ParsedCip68Datum(
        @Nullable Long decimals,
        @Nullable String description,
        @Nullable String logo,
        @Nullable String name,
        @Nullable String ticker,
        @Nullable String url,
        @Nullable Long version,
        @Nullable String image,
        @Nullable String mediaType,
        @Nullable Map<String, Object> properties,
        List<ValidationError> validationErrors
) {

    /** The metadata fields of CIP-68 tokens, as the parser names them in its errors, and the nested {@code "721"} map. */
    private static final Set<String> TOKEN_FIELDS = Set.of(
            "decimals", "description", "logo", "name", "ticker", "url", "image", "mediaType", "files", "721");

    /** A datum that departs from nothing the parser checks (tests and callers that do not care about errors). */
    public ParsedCip68Datum(@Nullable Long decimals, @Nullable String description, @Nullable String logo,
                            @Nullable String name, @Nullable String ticker, @Nullable String url,
                            @Nullable Long version, @Nullable String image, @Nullable String mediaType,
                            @Nullable Map<String, Object> properties) {
        this(decimals, description, logo, name, ticker, url, version, image, mediaType, properties, List.of());
    }

    /**
     * Whether the datum has any of the fields CIP-68 token metadata is made of, usable or reported as wrong, or a nested
     * {@code "721"} map. The {@code version} does not count: it is part of every CIP-68 datum, metadata or not.
     */
    public boolean hasTokenFields() {
        return decimals != null || description != null || logo != null || name != null || ticker != null || url != null
                || image != null || mediaType != null || (properties != null && properties.containsKey("files"))
                || validationErrors.stream().anyMatch(error -> TOKEN_FIELDS.contains(error.fieldName()));
    }

    /**
     * Project to the FT-shape DTO for read-path consumers (the API today only surfaces
     * FT data; NFT-specific fields are stored but not yet exposed).
     */
    public FungibleTokenMetadata toFungibleTokenMetadata() {
        return new FungibleTokenMetadata(decimals, description, logo, name, ticker, url, version);
    }
}
