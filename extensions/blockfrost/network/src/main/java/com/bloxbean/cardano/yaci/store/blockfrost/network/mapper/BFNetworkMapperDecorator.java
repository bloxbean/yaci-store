package com.bloxbean.cardano.yaci.store.blockfrost.network.mapper;

import com.bloxbean.cardano.yaci.store.api.adapot.dto.NetworkInfoDto;
import com.bloxbean.cardano.yaci.store.blockfrost.network.dto.BFNetworkDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import java.math.BigInteger;

/**
 * MapStruct decorator that adds computed and approximated fields on top of
 * the base BigInteger → String conversion performed by {@link BFNetworkMapper}.
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
           
            supply.setTotal(computeTotal(networkInfoDto.supply()));
            supply.setLocked("0");
        }

        BFNetworkDto.Stake stake = dto.getStake();
        if (stake != null) {
            stake.setLive(stake.getActive());
        }

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
