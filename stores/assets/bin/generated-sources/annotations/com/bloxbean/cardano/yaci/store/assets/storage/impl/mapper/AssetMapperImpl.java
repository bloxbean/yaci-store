package com.bloxbean.cardano.yaci.store.assets.storage.impl.mapper;

import com.bloxbean.cardano.yaci.store.assets.domain.TxAsset;
import com.bloxbean.cardano.yaci.store.assets.storage.impl.model.TxAssetEntity;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2025-06-20T15:09:14+0100",
    comments = "version: 1.5.5.Final, compiler: Eclipse JDT (IDE) 3.42.0.v20250514-1000, environment: Java 21.0.7 (Eclipse Adoptium)"
)
@Component
public class AssetMapperImpl extends AssetMapper {

    @Override
    public TxAsset toTxAsset(TxAssetEntity txAssetEntity) {
        if ( txAssetEntity == null ) {
            return null;
        }

        TxAsset.TxAssetBuilder<?, ?> txAsset = TxAsset.builder();

        txAsset.blockNumber( txAssetEntity.getBlockNumber() );
        txAsset.blockTime( txAssetEntity.getBlockTime() );
        txAsset.assetName( txAssetEntity.getAssetName() );
        txAsset.fingerprint( txAssetEntity.getFingerprint() );
        txAsset.mintType( txAssetEntity.getMintType() );
        txAsset.policy( txAssetEntity.getPolicy() );
        txAsset.quantity( txAssetEntity.getQuantity() );
        txAsset.slot( txAssetEntity.getSlot() );
        txAsset.txHash( txAssetEntity.getTxHash() );
        txAsset.unit( txAssetEntity.getUnit() );

        return txAsset.build();
    }

    @Override
    public TxAssetEntity toTxAssetEntity(TxAsset txAsset) {
        if ( txAsset == null ) {
            return null;
        }

        TxAssetEntity.TxAssetEntityBuilder<?, ?> txAssetEntity = TxAssetEntity.builder();

        txAssetEntity.blockNumber( txAsset.getBlockNumber() );
        txAssetEntity.blockTime( txAsset.getBlockTime() );
        txAssetEntity.assetName( txAsset.getAssetName() );
        txAssetEntity.fingerprint( txAsset.getFingerprint() );
        txAssetEntity.mintType( txAsset.getMintType() );
        txAssetEntity.policy( txAsset.getPolicy() );
        txAssetEntity.quantity( txAsset.getQuantity() );
        txAssetEntity.slot( txAsset.getSlot() );
        txAssetEntity.txHash( txAsset.getTxHash() );
        txAssetEntity.unit( txAsset.getUnit() );

        return txAssetEntity.build();
    }
}
