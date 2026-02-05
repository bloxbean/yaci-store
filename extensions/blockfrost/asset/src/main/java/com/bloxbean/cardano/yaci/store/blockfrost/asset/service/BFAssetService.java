package com.bloxbean.cardano.yaci.store.blockfrost.asset.service;

import com.bloxbean.cardano.yaci.store.assets.storage.AssetStorageReader;
import com.bloxbean.cardano.yaci.store.assets.storage.impl.model.TxAssetInfo;
import com.bloxbean.cardano.yaci.store.blockfrost.asset.dto.BFAssetDTO;
import com.bloxbean.cardano.yaci.store.blockfrost.asset.mapper.BFAssetMapper;
import com.bloxbean.cardano.yaci.store.common.model.Order;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class BFAssetService {

    private final AssetStorageReader assetStorageReader;

    public BFAssetService(AssetStorageReader assetStorageReader) {
        this.assetStorageReader = assetStorageReader;
    }

    public List<BFAssetDTO> getAssets(int page, int count, Order order) {
        Slice<TxAssetInfo> assetsSlice = assetStorageReader.findAllGroupByUnit(page, count, order);

        return assetsSlice.stream()
                .map(BFAssetMapper.INSTANCE::toBFAssetDTO)
                .collect(Collectors.toList());
    }
}

