package com.bloxbean.cardano.yaci.store.blockfrost.asset.mapper;

import com.bloxbean.cardano.yaci.store.blockfrost.asset.dto.BFAssetAddressDTO;
import com.bloxbean.cardano.yaci.store.blockfrost.asset.dto.BFAssetDTO;
import com.bloxbean.cardano.yaci.store.blockfrost.asset.dto.BFAssetDetailDTO;
import com.bloxbean.cardano.yaci.store.blockfrost.asset.dto.BFAssetHistoryDTO;
import com.bloxbean.cardano.yaci.store.blockfrost.asset.dto.BFAssetTransactionDTO;
import com.bloxbean.cardano.yaci.store.blockfrost.asset.storage.impl.model.BFAssetAddress;
import com.bloxbean.cardano.yaci.store.blockfrost.asset.storage.impl.model.BFAssetHistory;
import com.bloxbean.cardano.yaci.store.blockfrost.asset.storage.impl.model.BFAssetInfo;
import com.bloxbean.cardano.yaci.store.blockfrost.asset.storage.impl.model.BFAssetTransaction;
import com.bloxbean.cardano.yaci.store.blockfrost.asset.storage.impl.model.BFPolicyAsset;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

import java.math.BigInteger;

@Mapper
public interface BFAssetMapper {

    BFAssetMapper INSTANCE = Mappers.getMapper(BFAssetMapper.class);

    @Mapping(target = "asset", source = "unit")
    @Mapping(target = "quantity", source = "quantity", qualifiedByName = "stringOrZero")
    BFAssetDTO toBFAssetDTO(BFPolicyAsset source);

    @Mapping(target = "asset", source = "unit")
    @Mapping(target = "assetName", expression = "java(extractAssetNameHex(source.unit(), source.assetName()))")
    @Mapping(target = "quantity", source = "quantity", qualifiedByName = "stringOrZero")
    @Mapping(target = "metadata", ignore = true)
    @Mapping(target = "onchainMetadata", ignore = true)
    @Mapping(target = "onchainMetadataStandard", ignore = true)
    @Mapping(target = "onchainMetadataExtra", ignore = true)
    BFAssetDetailDTO toBFAssetDetailDTO(BFAssetInfo source);

    @Mapping(target = "amount", source = "amount", qualifiedByName = "stringOrZero")
    BFAssetHistoryDTO toBFAssetHistoryDTO(BFAssetHistory source);

    BFAssetTransactionDTO toBFAssetTransactionDTO(BFAssetTransaction source);

    @Mapping(target = "quantity", source = "quantity", qualifiedByName = "stringOrZero")
    BFAssetAddressDTO toBFAssetAddressDTO(BFAssetAddress source);

    @Named("stringOrZero")
    default String stringOrZero(BigInteger value) {
        return value == null ? "0" : value.toString();
    }

    default String extractAssetNameHex(String unit, String fallbackAssetName) {
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
