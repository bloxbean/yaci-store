package com.bloxbean.cardano.yaci.store.blockfrost.epoch.mapper;

import com.bloxbean.cardano.yaci.store.adapot.domain.EpochStake;
import com.bloxbean.cardano.yaci.store.blockfrost.epoch.dto.BFEpochStakeDto;
import com.bloxbean.cardano.yaci.store.blockfrost.epoch.dto.BFEpochStakePoolDto;
import com.bloxbean.cardano.yaci.store.common.util.PoolUtil;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

import java.math.BigInteger;

@Mapper
public interface BFEpochStakeMapper {
    BFEpochStakeMapper INSTANCE = Mappers.getMapper(BFEpochStakeMapper.class);

    @Mapping(target = "stakeAddress", source = "address")
    @Mapping(target = "poolId", source = "poolId", qualifiedByName = "toBech32PoolId")
    @Mapping(target = "amount", source = "amount", qualifiedByName = "stringOrNull")
    BFEpochStakeDto toBFEpochStakeDto(EpochStake epochStake);

    @Mapping(target = "stakeAddress", source = "address")
    @Mapping(target = "amount", source = "amount", qualifiedByName = "stringOrNull")
    BFEpochStakePoolDto toBFEpochStakePoolDto(EpochStake epochStake);

    @Named("stringOrNull")
    default String stringOrNull(BigInteger value) {
        return value == null ? null : value.toString();
    }

    @Named("toBech32PoolId")
    default String toBech32PoolId(String poolId) {
        if (poolId == null || poolId.isBlank()) {
            return null;
        }
        if (poolId.startsWith(PoolUtil.POOL_ID_PREFIX)) {
            return poolId;
        }
        try {
            return PoolUtil.getBech32PoolId(poolId);
        } catch (Exception e) {
            return poolId;
        }
    }
}
