package com.bloxbean.cardano.yaci.store.extensions.assetstore.api.model.v2;

import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip26.entity.TokenMetadata;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.model.Cip68TokenMetadata;

public record Standards(TokenMetadata cip26, Cip68TokenMetadata cip68) {

    public static Standards empty() {
        return new Standards(null, null);
    }

    public Standards merge(Standards that) {
        TokenMetadata finalCip26 = cip26 != null ? cip26 : that.cip26();
        Cip68TokenMetadata finalCip68 = cip68 != null ? cip68 : that.cip68();
        return new Standards(finalCip26, finalCip68);
    }

}
