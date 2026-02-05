package com.bloxbean.cardano.yaci.store.blockfrost.epoch.mapper;

import com.bloxbean.cardano.yaci.store.blockfrost.epoch.dto.BFEpochDto;
import com.bloxbean.cardano.yaci.store.epochaggr.domain.Epoch;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

import java.math.BigInteger;

@Mapper
public interface BFEpochMapper {
    BFEpochMapper INSTANCE = Mappers.getMapper(BFEpochMapper.class);

    @Mapping(target = "epoch", expression = "java((int) epoch.getNumber())")
    @Mapping(target = "startTime", source = "epoch.startTime")
    @Mapping(target = "endTime", source = "epoch.endTime")
    @Mapping(target = "firstBlockTime", source = "epoch.startTime")
    @Mapping(target = "lastBlockTime", source = "epoch.endTime")
    @Mapping(target = "blockCount", source = "epoch.blockCount")
    @Mapping(target = "txCount", source = "epoch.transactionCount")
    @Mapping(target = "output", source = "epoch.totalOutput", qualifiedByName = "stringOrZero")
    @Mapping(target = "fees", source = "epoch.totalFees", qualifiedByName = "stringOrZero")
    @Mapping(target = "activeStake", source = "activeStake")
    BFEpochDto toBFEpochDto(Epoch epoch, String activeStake);

    @Named("stringOrZero")
    default String stringOrZero(BigInteger value) {
        return value == null ? "0" : value.toString();
    }
}
