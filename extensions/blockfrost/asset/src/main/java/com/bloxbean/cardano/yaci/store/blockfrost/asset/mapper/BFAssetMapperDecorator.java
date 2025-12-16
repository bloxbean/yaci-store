package com.bloxbean.cardano.yaci.store.blockfrost.asset.mapper;

import com.bloxbean.cardano.yaci.store.blockfrost.asset.dto.BFAssetDTO;
import com.bloxbean.cardano.yaci.store.assets.storage.impl.model.TxAssetInfo;

public class BFAssetMapperDecorator implements BFAssetMapper {

    private final BFAssetMapper delegate;

    public BFAssetMapperDecorator(BFAssetMapper delegate) {
        this.delegate = delegate;
    }

    @Override
    public BFAssetDTO toBFAssetDTO(TxAssetInfo txAssetInfo) {
        BFAssetDTO dto = delegate.toBFAssetDTO(txAssetInfo);

        if (txAssetInfo != null) {
            if (txAssetInfo.getQuantity() != null) {
                dto.setQuantity(txAssetInfo.getQuantity().toString());
            }
        }

        return dto;
    }
}


