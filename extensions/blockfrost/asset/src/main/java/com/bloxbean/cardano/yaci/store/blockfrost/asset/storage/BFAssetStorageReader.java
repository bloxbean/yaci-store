package com.bloxbean.cardano.yaci.store.blockfrost.asset.storage;

import com.bloxbean.cardano.yaci.store.blockfrost.asset.storage.impl.model.BFAssetAddress;
import com.bloxbean.cardano.yaci.store.blockfrost.asset.storage.impl.model.BFAssetHistory;
import com.bloxbean.cardano.yaci.store.blockfrost.asset.storage.impl.model.BFAssetInfo;
import com.bloxbean.cardano.yaci.store.blockfrost.asset.storage.impl.model.BFAssetTransaction;
import com.bloxbean.cardano.yaci.store.blockfrost.asset.storage.impl.model.BFPolicyAsset;
import com.bloxbean.cardano.yaci.store.common.model.Order;

import java.util.List;
import java.util.Optional;

public interface BFAssetStorageReader {
    List<BFPolicyAsset> findAssets(int page, int count, Order order);

    Optional<BFAssetInfo> findAssetInfo(String unit);

    List<BFAssetHistory> findAssetHistory(String unit, int page, int count, Order order);

    List<String> findAssetTxHashes(String unit, int page, int count, Order order);

    List<BFAssetTransaction> findAssetTransactions(String unit, int page, int count, Order order);

    List<BFAssetAddress> findAssetAddresses(String unit, int page, int count, Order order);

    List<BFPolicyAsset> findAssetsByPolicy(String policyId, int page, int count, Order order);
}
