package com.bloxbean.cardano.yaci.store.blockfrost.pools.mapper;

import com.bloxbean.cardano.yaci.store.blockfrost.pools.dto.*;
import com.bloxbean.cardano.yaci.store.blockfrost.pools.storage.impl.model.*;
import com.bloxbean.cardano.yaci.store.common.util.PoolUtil;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;

@Mapper
public interface BFPoolsMapper {

    BFPoolsMapper INSTANCE = Mappers.getMapper(BFPoolsMapper.class);

    @Mapping(target = "poolId", source = "poolId", qualifiedByName = "toBech32PoolId")
    @Mapping(target = "epoch", source = "retireEpoch")
    BFPoolRetireItemDto toBFPoolRetireItemDto(BFPoolRetireItem source);

    @Mapping(target = "poolId", source = "poolId", qualifiedByName = "toBech32PoolId")
    @Mapping(target = "hex", source = "poolId")
    @Mapping(target = "vrfKey", source = "vrfKey")
    @Mapping(target = "blocksMinted", source = "blocksMinted")
    @Mapping(target = "blocksEpoch", source = "blocksEpoch")
    @Mapping(target = "liveStake", constant = "0")
    @Mapping(target = "liveSize", constant = "0.0")
    @Mapping(target = "liveSaturation", constant = "0.0")
    @Mapping(target = "liveDelegators", constant = "0")
    @Mapping(target = "activeStake", constant = "0")
    @Mapping(target = "activeSize", constant = "0.0")
    @Mapping(target = "declaredPledge", source = "pledge", qualifiedByName = "bigIntegerToString")
    @Mapping(target = "livePledge", constant = "0")
    @Mapping(target = "marginCost", source = "source", qualifiedByName = "toMarginCost")
    @Mapping(target = "fixedCost", source = "cost", qualifiedByName = "bigIntegerToString")
    @Mapping(target = "rewardAccount", source = "rewardAccount")
    @Mapping(target = "owners", source = "owners")
    @Mapping(target = "registration", source = "registration")
    @Mapping(target = "retirement", source = "retirement")
    @Mapping(target = "calidusKey", ignore = true)
    BFPoolDto toBFPoolDto(BFPoolSummary source);

    @Mapping(target = "poolId", source = "poolId", qualifiedByName = "toBech32PoolId")
    @Mapping(target = "hex", source = "poolId")
    @Mapping(target = "url", source = "metadataUrl")
    @Mapping(target = "hash", source = "metadataHash")
    @Mapping(target = "ticker", ignore = true)
    @Mapping(target = "name", ignore = true)
    @Mapping(target = "description", ignore = true)
    @Mapping(target = "homepage", ignore = true)
    BFPoolMetadataDto toBFPoolMetadataDto(BFPoolMetadata source);

    @Mapping(target = "poolId", source = "poolId", qualifiedByName = "toBech32PoolId")
    @Mapping(target = "hex", source = "poolId")
    @Mapping(target = "activeStake", constant = "0")
    @Mapping(target = "liveStake", constant = "0")
    @Mapping(target = "liveSaturation", constant = "0.0")
    @Mapping(target = "blocksMinted", source = "blocksMinted")
    @Mapping(target = "declaredPledge", source = "pledge", qualifiedByName = "bigIntegerToString")
    @Mapping(target = "marginCost", source = "source", qualifiedByName = "toMarginCostFromInfo")
    @Mapping(target = "fixedCost", source = "cost", qualifiedByName = "bigIntegerToString")
    @Mapping(target = "metadata", source = "source", qualifiedByName = "toMetadataEmbed")
    BFPoolListItemDto toBFPoolListItemDto(BFPoolRegistrationInfo source);

    @Mapping(target = "txHash", source = "txHash")
    @Mapping(target = "certIndex", source = "certIndex")
    @Mapping(target = "action", source = "action")
    BFPoolUpdateDto toBFPoolUpdateDto(BFPoolUpdate source);

    @Mapping(target = "txHash", source = "txHash")
    @Mapping(target = "certIndex", source = "certIndex")
    @Mapping(target = "vote", source = "vote", qualifiedByName = "normalizeVote")
    BFPoolVoteDto toBFPoolVoteDto(BFPoolVote source);

    @Named("toBech32PoolId")
    default String toBech32PoolId(String poolIdHex) {
        if (poolIdHex == null) return null;
        try {
            return PoolUtil.getBech32PoolId(poolIdHex);
        } catch (Exception e) {
            return poolIdHex;
        }
    }

    @Named("bigIntegerToString")
    default String bigIntegerToString(BigInteger value) {
        return value == null ? "0" : value.toString();
    }

    @Named("toMarginCost")
    default double toMarginCost(BFPoolSummary source) {
        if (source.marginDenominator() == null || source.marginDenominator().compareTo(BigInteger.ZERO) == 0) {
            return 0.0;
        }
        return new BigDecimal(source.marginNumerator())
                .divide(new BigDecimal(source.marginDenominator()), 10, RoundingMode.HALF_UP)
                .doubleValue();
    }

    @Named("toMarginCostFromInfo")
    default double toMarginCostFromInfo(BFPoolRegistrationInfo source) {
        if (source.marginDenominator() == null || source.marginDenominator().compareTo(BigInteger.ZERO) == 0) {
            return 0.0;
        }
        return new BigDecimal(source.marginNumerator())
                .divide(new BigDecimal(source.marginDenominator()), 10, RoundingMode.HALF_UP)
                .doubleValue();
    }

    @Named("toMetadataEmbed")
    default BFPoolMetadataEmbedDto toMetadataEmbed(BFPoolRegistrationInfo source) {
        return BFPoolMetadataEmbedDto.builder()
                .url(source.metadataUrl())
                .hash(source.metadataHash())
                .ticker(null)
                .name(null)
                .description(null)
                .homepage(null)
                .build();
    }

    @Named("normalizeVote")
    default String normalizeVote(String vote) {
        if (vote == null) return null;
        return switch (vote.toUpperCase()) {
            case "YES" -> "yes";
            case "NO" -> "no";
            case "ABSTAIN" -> "abstain";
            default -> vote.toLowerCase();
        };
    }
}
