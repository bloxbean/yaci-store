package com.bloxbean.cardano.yaci.store.extensions.assetstore.invalidtoken;

import jakarta.annotation.Nullable;
import lombok.Builder;

import java.util.Map;

/**
 * What a writer tells {@link InvalidTokenRecorder} about one dropped item. {@code standard}, {@code reasonCode} and {@code reason} are
 * required; the rest is whatever the writer knows.
 */
@Builder
public record InvalidTokenReport(
        String standard,
        @Nullable String combinedWith,
        String reasonCode,
        String reason,
        @Nullable String policyId,
        @Nullable String assetName,
        @Nullable String subject,
        @Nullable Long slot,
        @Nullable String txHash,
        @Nullable Integer txIndex,
        @Nullable Integer outputIndex,
        @Nullable String origin,
        @Nullable Map<String, Object> details) {
}
