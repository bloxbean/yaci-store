package com.bloxbean.cardano.yaci.store.blockfrost.network.mapper;

import com.bloxbean.cardano.yaci.store.api.adapot.dto.NetworkInfoDto;
import com.bloxbean.cardano.yaci.store.blockfrost.network.dto.BFNetworkDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import java.math.BigInteger;

/**
 * MapStruct decorator that adds computed fields on top of
 * the base BigInteger → String conversion performed by {@link BFNetworkMapper}.
 * <p>
 * Computes {@code total = max - reserves}. The {@code locked}, {@code circulating},
 * and {@code live} fields are set by {@link com.bloxbean.cardano.yaci.store.blockfrost.network.service.BFNetworkService}
 * from UTxO-based queries via {@link com.bloxbean.cardano.yaci.store.blockfrost.network.storage.BFNetworkStorageReader}.
 */
public abstract class BFNetworkMapperDecorator implements BFNetworkMapper {

    @Autowired
    @Qualifier("delegate")
    private BFNetworkMapper delegate;

    @Override
    public BFNetworkDto toBFNetworkDto(NetworkInfoDto networkInfoDto) {
        // Delegate handles BigInteger → String conversion for all base fields.
        BFNetworkDto dto = delegate.toBFNetworkDto(networkInfoDto);

        if (dto == null) {
            return null;
        }

        BFNetworkDto.Supply supply = dto.getSupply();
        if (supply != null) {
            // total = max - reserves
            supply.setTotal(computeTotal(networkInfoDto.supply()));
            // locked and circulating are set by BFNetworkService after this mapper returns
        }

        // live stake is set by BFNetworkService after this mapper returns

        return dto;
    }

   
    private String computeTotal(NetworkInfoDto.Supply supply) {
        if (supply == null) {
            return null;
        }
        BigInteger max = supply.max();
        BigInteger reserves = supply.reserves();
        if (max == null) {
            return null;
        }
        if (reserves == null) {
            return max.toString();
        }
        return max.subtract(reserves).toString();
    }
}
