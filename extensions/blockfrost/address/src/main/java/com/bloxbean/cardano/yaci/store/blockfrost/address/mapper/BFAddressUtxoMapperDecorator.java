package com.bloxbean.cardano.yaci.store.blockfrost.address.mapper;

import com.bloxbean.cardano.yaci.store.blockfrost.address.dto.BFAddressUtxoDTO;
import com.bloxbean.cardano.yaci.store.common.domain.AddressUtxo;

import java.util.List;
import java.util.stream.Collectors;

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
        
        return dto;
    }
}

