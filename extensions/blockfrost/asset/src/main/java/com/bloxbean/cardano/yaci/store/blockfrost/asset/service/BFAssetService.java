package com.bloxbean.cardano.yaci.store.blockfrost.asset.service;

import com.bloxbean.cardano.yaci.store.blockfrost.asset.dto.BFAssetAddressDTO;
import com.bloxbean.cardano.yaci.store.blockfrost.asset.dto.BFAssetDTO;
import com.bloxbean.cardano.yaci.store.blockfrost.asset.dto.BFAssetDetailDTO;
import com.bloxbean.cardano.yaci.store.blockfrost.asset.dto.BFAssetHistoryDTO;
import com.bloxbean.cardano.yaci.store.blockfrost.asset.dto.BFAssetTransactionDTO;
import com.bloxbean.cardano.yaci.store.blockfrost.asset.storage.BFAssetStorageReader;
import com.bloxbean.cardano.yaci.store.blockfrost.asset.storage.impl.model.BFAssetInfo;
import com.bloxbean.cardano.yaci.store.blockfrost.asset.storage.impl.model.BFPolicyAsset;
import com.bloxbean.cardano.yaci.store.common.model.Order;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class BFAssetService {

    private final BFAssetStorageReader bfAssetStorageReader;

    public List<BFAssetDTO> getAssets(int page, int count, Order order) {
        return bfAssetStorageReader.findAssets(page, count, order).stream()
                .map(asset -> BFAssetDTO.builder()
                        .asset(asset.unit())
                        .quantity(asset.quantity().toString())
                        .build())
                .collect(Collectors.toList());
    }

    public BFAssetDetailDTO getAsset(String asset) {
        BFAssetInfo assetInfo = requireAssetInfo(asset);

        return BFAssetDetailDTO.builder()
                .asset(assetInfo.unit())
                .policyId(assetInfo.policyId())
                .assetName(extractAssetNameHex(assetInfo.unit(), assetInfo.assetName()))
                .fingerprint(assetInfo.fingerprint())
                .quantity(assetInfo.quantity().toString())
                .initialMintTxHash(assetInfo.initialMintTxHash())
                .mintOrBurnCount(assetInfo.mintOrBurnCount())
                .metadata(null)
                .onchainMetadata(null)
                .onchainMetadataStandard(null)
                .onchainMetadataExtra(null)
                .build();
    }

    public List<BFAssetHistoryDTO> getAssetHistory(String asset, int page, int count, Order order) {
        requireAssetInfo(asset);

        return bfAssetStorageReader.findAssetHistory(asset, page, count, order).stream()
                .map(history -> BFAssetHistoryDTO.builder()
                        .txHash(history.txHash())
                        .action(history.action())
                        .amount(history.amount().toString())
                        .build())
                .collect(Collectors.toList());
    }

    public List<String> getAssetTxs(String asset, int page, int count, Order order) {
        requireAssetInfo(asset);
        return bfAssetStorageReader.findAssetTxHashes(asset, page, count, order);
    }

    public List<BFAssetTransactionDTO> getAssetTransactions(String asset, int page, int count, Order order) {
        requireAssetInfo(asset);

        return bfAssetStorageReader.findAssetTransactions(asset, page, count, order).stream()
                .map(transaction -> BFAssetTransactionDTO.builder()
                        .txHash(transaction.txHash())
                        .txIndex(transaction.txIndex())
                        .blockHeight(transaction.blockHeight())
                        .blockTime(transaction.blockTime())
                        .build())
                .collect(Collectors.toList());
    }

    public List<BFAssetAddressDTO> getAssetAddresses(String asset, int page, int count, Order order) {
        requireAssetInfo(asset);

        return bfAssetStorageReader.findAssetAddresses(asset, page, count, order).stream()
                .map(address -> BFAssetAddressDTO.builder()
                        .address(address.address())
                        .quantity(address.quantity().toString())
                        .build())
                .collect(Collectors.toList());
    }

    public List<BFAssetDTO> getPolicyAssets(String policyId, int page, int count, Order order) {
        List<BFPolicyAsset> policyAssets = bfAssetStorageReader.findAssetsByPolicy(policyId, page, count, order);

        return policyAssets.stream()
                .map(policyAsset -> BFAssetDTO.builder()
                        .asset(policyAsset.unit())
                        .quantity(policyAsset.quantity().toString())
                        .build())
                .collect(Collectors.toList());
    }

    private BFAssetInfo requireAssetInfo(String asset) {
        return bfAssetStorageReader.findAssetInfo(asset)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Asset not found: " + asset));
    }

    private String extractAssetNameHex(String unit, String fallbackAssetName) {
        if (unit == null) {
            return fallbackAssetName;
        }
        if (unit.length() < 56) {
            return fallbackAssetName;
        }
        if (unit.length() == 56) {
            return "";
        }

        return unit.substring(56);
    }
}
