package com.bloxbean.cardano.yaci.store.assets.storage.impl;

import com.bloxbean.cardano.yaci.store.assets.domain.TxAsset;
import com.bloxbean.cardano.yaci.store.assets.storage.AssetStorage;
import com.bloxbean.cardano.yaci.store.assets.storage.impl.mapper.AssetMapper;
import com.bloxbean.cardano.yaci.store.assets.storage.impl.model.TxAssetEntity;
import com.bloxbean.cardano.yaci.store.assets.storage.impl.repository.TxAssetRepository;
import com.bloxbean.cardano.yaci.store.common.util.UUIDUtil;
import com.bloxbean.cardano.yaci.store.plugin.aspect.Plugin;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;

import java.time.LocalDateTime;
import java.util.List;

import static com.bloxbean.cardano.yaci.store.assets.jooq.Tables.ASSETS;

@RequiredArgsConstructor
public class AssetStorageImpl implements AssetStorage {
    private final static String PLUGIN_ASSET_SAVE = "asset.save";

    private final TxAssetRepository txAssetRepository;
    private final AssetMapper assetMapper;
    private final DSLContext dsl;

    @Override
    @Plugin(key = PLUGIN_ASSET_SAVE)
    public void saveAll(List<TxAsset> txAssetList) {
        List<TxAssetEntity> txAssetEntities = txAssetList.stream().map(assetMapper::toTxAssetEntity).toList();

        //Id from (tx_hash, unit), so a block processed again overwrites its rows. Unit is policy + hex asset name,
        //unique in a tx's mint, while asset_name is a decoded name which can repeat
        var idField = UUIDUtil.idField(dsl, ASSETS.ID);
        LocalDateTime localDateTime = LocalDateTime.now();
        var inserts = txAssetEntities.stream()
                .map(entity -> dsl.insertInto(ASSETS)
                        .set(idField, UUIDUtil.nameUUID(entity.getTxHash(), entity.getUnit()))
                        .set(ASSETS.SLOT, entity.getSlot())
                        .set(ASSETS.TX_HASH, entity.getTxHash())
                        .set(ASSETS.POLICY, entity.getPolicy())
                        .set(ASSETS.ASSET_NAME, entity.getAssetName())
                        .set(ASSETS.UNIT, entity.getUnit())
                        .set(ASSETS.FINGERPRINT, entity.getFingerprint())
                        .set(ASSETS.QUANTITY, entity.getQuantity() != null ? entity.getQuantity().longValue() : null)
                        .set(ASSETS.MINT_TYPE, entity.getMintType() != null ? entity.getMintType().name() : null)
                        .set(ASSETS.BLOCK, entity.getBlockNumber())
                        .set(ASSETS.BLOCK_TIME, entity.getBlockTime())
                        .set(ASSETS.UPDATE_DATETIME, localDateTime)
                        .onDuplicateKeyUpdate()
                        .set(ASSETS.SLOT, entity.getSlot())
                        .set(ASSETS.TX_HASH, entity.getTxHash())
                        .set(ASSETS.POLICY, entity.getPolicy())
                        .set(ASSETS.ASSET_NAME, entity.getAssetName())
                        .set(ASSETS.UNIT, entity.getUnit())
                        .set(ASSETS.FINGERPRINT, entity.getFingerprint())
                        .set(ASSETS.QUANTITY, entity.getQuantity() != null ? entity.getQuantity().longValue() : null)
                        .set(ASSETS.MINT_TYPE, entity.getMintType() != null ? entity.getMintType().name() : null)
                        .set(ASSETS.BLOCK, entity.getBlockNumber())
                        .set(ASSETS.BLOCK_TIME, entity.getBlockTime())
                        .set(ASSETS.UPDATE_DATETIME, localDateTime)).toList();

        dsl.batch(inserts).execute();
    }

    @Override
    public int deleteBySlotGreaterThan(long slot) {
        return txAssetRepository.deleteBySlotGreaterThan(slot);
    }

}
