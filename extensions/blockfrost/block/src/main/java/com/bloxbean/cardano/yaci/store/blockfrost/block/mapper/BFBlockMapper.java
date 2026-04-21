package com.bloxbean.cardano.yaci.store.blockfrost.block.mapper;

import com.bloxbean.cardano.client.crypto.Bech32;
import com.bloxbean.cardano.client.util.HexUtil;
import com.bloxbean.cardano.yaci.store.blockfrost.block.dto.BFBlockAddressTxDTO;
import com.bloxbean.cardano.yaci.store.blockfrost.block.dto.BFBlockDTO;
import com.bloxbean.cardano.yaci.store.blockfrost.block.dto.BFBlockTxCborDTO;
import com.bloxbean.cardano.yaci.store.blockfrost.block.storage.impl.model.BFBlockAddressTxRow;
import com.bloxbean.cardano.yaci.store.blockfrost.block.storage.impl.model.BFBlockRow;
import com.bloxbean.cardano.yaci.store.blockfrost.block.storage.impl.model.BFBlockTxCborRow;
import com.bloxbean.cardano.yaci.store.common.util.PoolUtil;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

import java.math.BigInteger;

import static com.bloxbean.cardano.yaci.store.common.util.Bech32Prefixes.VRF_VK_PREFIX;

@Mapper
public interface BFBlockMapper {

    BFBlockMapper INSTANCE = Mappers.getMapper(BFBlockMapper.class);

    @Mapping(target = "output", source = "output", qualifiedByName = "toStringOrNull")
    @Mapping(target = "fees", source = "fees", qualifiedByName = "toStringOrNull")
    @Mapping(target = "opCertCounter", source = "opCertCounter", qualifiedByName = "longToStringOrNull")
    @Mapping(target = "blockVrf", source = "blockVrf", qualifiedByName = "toVrfVKeyBech32")
    @Mapping(target = "slotLeader", source = "slotLeader", qualifiedByName = "toBech32SlotLeader")
    BFBlockDTO toBFBlockDTO(BFBlockRow source);

    @Mapping(target = "cbor", source = "cborData", qualifiedByName = "bytesToHex")
    BFBlockTxCborDTO toBFBlockTxCborDTO(BFBlockTxCborRow source);

    BFBlockAddressTxDTO toBFBlockAddressTxDTO(BFBlockAddressTxRow source);

    @Named("toStringOrNull")
    default String toStringOrNull(BigInteger value) {
        return value == null ? null : value.toString();
    }

    @Named("longToStringOrNull")
    default String longToStringOrNull(Long value) {
        return value == null ? null : value.toString();
    }

    @Named("bytesToHex")
    default String bytesToHex(byte[] bytes) {
        if (bytes == null) {
            return null;
        }

        return HexUtil.encodeHexString(bytes);
    }

    @Named("toVrfVKeyBech32")
    default String toVrfVKeyBech32(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            return Bech32.encode(HexUtil.decodeHexString(value), VRF_VK_PREFIX);
        } catch (Exception e) {
            // Fallback keeps raw value if input is not valid hex.
            return value;
        }
    }

    @Named("toBech32SlotLeader")
    default String toBech32SlotLeader(String slotLeader) {
        if (slotLeader == null || slotLeader.isBlank()) {
            return slotLeader;
        }

        if (slotLeader.startsWith(PoolUtil.POOL_ID_PREFIX)) {
            return slotLeader;
        }

        // Shelley/Byron pool key hashes are typically 28-byte (56-char hex).
        if (slotLeader.matches("^[0-9a-fA-F]{56}$")) {
            try {
                return PoolUtil.getBech32PoolId(slotLeader);
            } catch (Exception e) {
                return slotLeader;
            }
        }

        // Keep specific block descriptions or unknown formats unchanged.
        return slotLeader;
    }
}
