package com.bloxbean.cardano.yaci.store.extensions.assetstore.api.dto;

import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip113.model.ProgrammableTokenCip113;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Marker interface for V2 API extensions.
 * Each CIP standard that enriches the Subject response implements this interface.
 * Extensions are serialized into the {@code extensions} map keyed by their CIP identifier.
 */
@Schema(description = "Base type for CIP extensions.",
        oneOf = {ProgrammableTokenCip113.class})
public interface Extension {
}
