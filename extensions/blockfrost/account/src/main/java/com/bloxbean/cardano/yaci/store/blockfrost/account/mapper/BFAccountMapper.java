package com.bloxbean.cardano.yaci.store.blockfrost.account.mapper;

import com.bloxbean.cardano.yaci.store.blockfrost.account.dto.*;
import com.bloxbean.cardano.yaci.store.blockfrost.account.storage.impl.model.*;
import com.bloxbean.cardano.yaci.store.common.util.PoolUtil;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

import java.math.BigInteger;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Mapper
public interface BFAccountMapper {

    BFAccountMapper INSTANCE = Mappers.getMapper(BFAccountMapper.class);

    @Mapping(target = "controlledAmount", source = "controlledAmount", qualifiedByName = "stringOrZero")
    @Mapping(target = "rewardsSum", source = "rewardsSum", qualifiedByName = "stringOrZero")
    @Mapping(target = "withdrawalsSum", source = "withdrawalsSum", qualifiedByName = "stringOrZero")
    @Mapping(target = "reservesSum", source = "reservesSum", qualifiedByName = "stringOrZero")
    @Mapping(target = "treasurySum", source = "treasurySum", qualifiedByName = "stringOrZero")
    @Mapping(target = "withdrawableAmount", expression = "java(computeWithdrawableAmount(source))")
    @Mapping(target = "poolId", source = "poolId", qualifiedByName = "toBech32PoolId")
    @Mapping(target = "drepId", ignore = true)
    BFAccountContentDto toContentDto(AccountInfo source);

    @Mapping(target = "amount", source = "amount", qualifiedByName = "stringOrZero")
    @Mapping(target = "poolId", source = "poolId", qualifiedByName = "toBech32PoolId")
    BFAccountRewardDto toRewardDto(AccountReward source);

    @Mapping(target = "amount", source = "amount", qualifiedByName = "stringOrZero")
    @Mapping(target = "poolId", source = "poolId", qualifiedByName = "toBech32PoolId")
    BFAccountHistoryDto toHistoryDto(AccountHistory source);

    @Mapping(target = "amount", source = "amount", qualifiedByName = "stringOrZero")
    @Mapping(target = "poolId", source = "poolId", qualifiedByName = "toBech32PoolId")
    BFAccountDelegationDto toDelegationDto(AccountDelegation source);

    @Mapping(target = "action", source = "type", qualifiedByName = "registrationTypeToAction")
    BFAccountRegistrationDto toRegistrationDto(AccountRegistration source);

    @Mapping(target = "amount", source = "amount", qualifiedByName = "stringOrZero")
    BFAccountWithdrawalDto toWithdrawalDto(AccountWithdrawal source);

    @Mapping(target = "amount", source = "amount", qualifiedByName = "stringOrZero")
    BFAccountMirDto toMirDto(AccountMir source);

    BFAccountAddressDto toAddressDto(AccountAddress source);

    @Mapping(target = "quantity", source = "quantity", qualifiedByName = "stringOrZero")
    BFAccountAddressAssetDto toAddressAssetDto(AccountAddressAsset source);

    @Mapping(target = "receivedSum", expression = "java(mapToAddressesTotalAmounts(source.receivedSum()))")
    @Mapping(target = "sentSum", expression = "java(mapToAddressesTotalAmounts(source.sentSum()))")
    BFAccountAddressesTotalDto toAddressesTotalDto(AccountAddressesTotal source);

    @Mapping(target = "amount", expression = "java(mapToUtxoAmounts(source.amounts()))")
    @Mapping(target = "block", source = "blockHash")
    @Mapping(target = "txIndex", source = "outputIndex")
    BFAccountUtxoDto toUtxoDto(AccountUtxo source);

    BFAccountTransactionDto toTransactionDto(AccountTransaction source);

    @Named("stringOrZero")
    default String stringOrZero(BigInteger value) {
        return value == null ? "0" : value.toString();
    }

    @Named("toBech32PoolId")
    default String toBech32PoolId(String poolId) {
        if (poolId == null || poolId.isBlank()) return null;
        if (poolId.startsWith(PoolUtil.POOL_ID_PREFIX)) return poolId;
        try {
            return PoolUtil.getBech32PoolId(poolId);
        } catch (Exception e) {
            return poolId;
        }
    }

    @Named("registrationTypeToAction")
    default String registrationTypeToAction(String type) {
        if (type != null && type.toUpperCase().contains("DEREGISTRATION")) {
            return "deregistered";
        }
        return "registered";
    }

    default String computeWithdrawableAmount(AccountInfo source) {
        BigInteger rewards = source.rewardsSum() != null ? source.rewardsSum() : BigInteger.ZERO;
        BigInteger withdrawals = source.withdrawalsSum() != null ? source.withdrawalsSum() : BigInteger.ZERO;
        BigInteger withdrawable = rewards.subtract(withdrawals);
        return withdrawable.compareTo(BigInteger.ZERO) < 0 ? "0" : withdrawable.toString();
    }

    default List<BFAccountAddressesTotalDto.Amount> mapToAddressesTotalAmounts(Map<String, BigInteger> map) {
        if (map == null) return Collections.emptyList();
        return map.entrySet().stream()
                .map(e -> BFAccountAddressesTotalDto.Amount.builder()
                        .unit(e.getKey())
                        .quantity(e.getValue() == null ? "0" : e.getValue().toString())
                        .build())
                .toList();
    }

    default List<BFAccountUtxoDto.Amount> mapToUtxoAmounts(Map<String, BigInteger> map) {
        if (map == null) return Collections.emptyList();
        return map.entrySet().stream()
                .map(e -> BFAccountUtxoDto.Amount.builder()
                        .unit(e.getKey())
                        .quantity(e.getValue() == null ? "0" : e.getValue().toString())
                        .build())
                .toList();
    }
}
