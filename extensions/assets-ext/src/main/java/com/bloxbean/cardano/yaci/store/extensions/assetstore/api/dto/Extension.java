package com.bloxbean.cardano.yaci.store.extensions.assetstore.api.dto;

import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip113.model.ProgrammableTokenCip113;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * What a CIP adds to a subject beyond its display metadata, returned in {@code extensions} under the CIP's key.
 */
@Schema(description = "Base type for CIP extensions.",
        oneOf = {ProgrammableTokenCip113.class})
public interface Extension {
}
