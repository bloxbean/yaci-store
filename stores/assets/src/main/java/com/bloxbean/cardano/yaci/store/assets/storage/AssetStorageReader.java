package com.bloxbean.cardano.yaci.store.assets.storage;

import com.bloxbean.cardano.yaci.store.assets.domain.TxAsset;
import com.bloxbean.cardano.yaci.store.assets.storage.impl.model.TxAssetInfo;
import com.bloxbean.cardano.yaci.store.common.model.Order;
import org.springframework.data.domain.Slice;

import java.math.BigInteger;
import java.util.List;
import java.util.Optional;

public interface AssetStorageReader {
    List<TxAsset> findByTxHash(String txHash);

    List<TxAsset> findByFingerprint(String fingerprint, int page, int count);

    List<TxAsset> findByPolicy(String policyId, int page, int count);

    List<TxAsset> findByUnit(String unit, int page, int count);

    Optional<BigInteger> getSupplyByFingerprint(String fingerprint);
    Optional<BigInteger> getSupplyByUnit(String unit);
    Optional<BigInteger> getSupplyByPolicy(String policyId);

    Slice<TxAssetInfo> findAllGroupByUnit(int page, int count, Order order);
}
