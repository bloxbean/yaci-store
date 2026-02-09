package com.bloxbean.cardano.yaci.store.blockfrost.asset.service;

import com.bloxbean.cardano.yaci.store.blockfrost.asset.dto.BFAssetAddressDTO;
import com.bloxbean.cardano.yaci.store.blockfrost.asset.dto.BFAssetDTO;
import com.bloxbean.cardano.yaci.store.blockfrost.asset.dto.BFAssetDetailDTO;
import com.bloxbean.cardano.yaci.store.blockfrost.asset.dto.BFAssetHistoryDTO;
import com.bloxbean.cardano.yaci.store.blockfrost.asset.dto.BFAssetTransactionDTO;
import com.bloxbean.cardano.yaci.store.blockfrost.asset.mapper.BFAssetMapper;
import com.bloxbean.cardano.yaci.store.blockfrost.asset.storage.BFAssetStorageReader;
import com.bloxbean.cardano.yaci.store.blockfrost.asset.storage.impl.model.BFAssetInfo;
import com.bloxbean.cardano.yaci.store.common.model.Order;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class BFAssetService {

    private final BFAssetStorageReader bfAssetStorageReader;
    private final BFAssetMapper bfAssetMapper = BFAssetMapper.INSTANCE;

    public List<BFAssetDTO> getAssets(int page, int count, Order order) {
        return bfAssetStorageReader.findAssets(page, count, order).stream()
                .map(bfAssetMapper::toBFAssetDTO)
                .toList();
    }

    public BFAssetDetailDTO getAsset(String asset) {
        BFAssetInfo assetInfo = requireAssetInfo(asset);
        return bfAssetMapper.toBFAssetDetailDTO(assetInfo);
    }

    public List<BFAssetHistoryDTO> getAssetHistory(String asset, int page, int count, Order order) {
        requireAssetInfo(asset);

        return bfAssetStorageReader.findAssetHistory(asset, page, count, order).stream()
                .map(bfAssetMapper::toBFAssetHistoryDTO)
                .toList();
    }

    public List<String> getAssetTxs(String asset, int page, int count, Order order) {
        requireAssetInfo(asset);
        return bfAssetStorageReader.findAssetTxHashes(asset, page, count, order);
    }

    public List<BFAssetTransactionDTO> getAssetTransactions(String asset, int page, int count, Order order) {
        requireAssetInfo(asset);

        return bfAssetStorageReader.findAssetTransactions(asset, page, count, order).stream()
                .map(bfAssetMapper::toBFAssetTransactionDTO)
                .toList();
    }

    public List<BFAssetAddressDTO> getAssetAddresses(String asset, int page, int count, Order order) {
        requireAssetInfo(asset);

        return bfAssetStorageReader.findAssetAddresses(asset, page, count, order).stream()
                .map(bfAssetMapper::toBFAssetAddressDTO)
                .toList();
    }

    public List<BFAssetDTO> getPolicyAssets(String policyId, int page, int count, Order order) {
        return bfAssetStorageReader.findAssetsByPolicy(policyId, page, count, order).stream()
                .map(bfAssetMapper::toBFAssetDTO)
                .toList();
    }

    private BFAssetInfo requireAssetInfo(String asset) {
        return bfAssetStorageReader.findAssetInfo(asset)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Asset not found: " + asset));
    }
}
