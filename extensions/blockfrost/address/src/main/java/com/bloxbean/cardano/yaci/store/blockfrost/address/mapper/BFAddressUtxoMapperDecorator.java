package com.bloxbean.cardano.yaci.store.blockfrost.address.mapper;

import com.bloxbean.cardano.client.crypto.Blake2bUtil;
import com.bloxbean.cardano.client.util.HexUtil;
import com.bloxbean.cardano.yaci.store.blockfrost.address.dto.BFAddressUtxoDTO;
import com.bloxbean.cardano.yaci.store.common.domain.AddressUtxo;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public class BFAddressUtxoMapperDecorator implements BFAddressUtxoMapper {

    private final BFAddressUtxoMapper delegate;

    public BFAddressUtxoMapperDecorator(BFAddressUtxoMapper delegate) {
        this.delegate = delegate;
    }

    @Override
    public BFAddressUtxoDTO toBFAddressUtxoDTO(AddressUtxo addressUtxo) {
        BFAddressUtxoDTO dto = delegate.toBFAddressUtxoDTO(addressUtxo);
        
        // Handle amount mapping with unit normalization
        if (addressUtxo.getAmounts() != null) {
            List<BFAddressUtxoDTO.Amount> amounts = addressUtxo.getAmounts().stream()
                    .map(amt -> {
                        String unit = amt.getUnit();
                        if (unit != null && unit.contains(".")) {
                            unit = unit.replace(".", "");
                        }
                        return BFAddressUtxoDTO.Amount.builder()
                                .unit(unit)
                                .quantity(amt.getQuantity().toString())
                                .build();
                    })
                    .collect(Collectors.toList());
            dto.setAmount(amounts);
        }

        // Backfill data_hash for inline-datum outputs. The indexer only persists address_utxo.data_hash
        // for hash-referenced datums; for inline datums the column is null. Blockfrost reports data_hash =
        // blake2b-256 of the inline datum bytes, so we derive it here when missing. No reindex required.
        if ((dto.getDataHash() == null || dto.getDataHash().isBlank())
                && addressUtxo.getInlineDatum() != null && !addressUtxo.getInlineDatum().isBlank()) {
            dto.setDataHash(computeDatumHash(addressUtxo.getInlineDatum()));
        }

        return dto;
    }

    private static String computeDatumHash(String inlineDatumHex) {
        try {
            byte[] datumBytes = HexUtil.decodeHexString(inlineDatumHex);
            return HexUtil.encodeHexString(Blake2bUtil.blake2bHash256(datumBytes));
        } catch (Exception e) {
            log.warn("Unable to compute datum hash from inline datum", e);
            return null;
        }
    }
}

