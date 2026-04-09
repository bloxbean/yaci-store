package com.bloxbean.cardano.yaci.store.blockfrost.account.mapper;

import com.bloxbean.cardano.yaci.store.blockfrost.account.dto.*;
import com.bloxbean.cardano.yaci.store.blockfrost.account.storage.impl.model.*;
import com.bloxbean.cardano.yaci.store.common.util.PoolUtil;
import com.bloxbean.cardano.yaci.store.utxo.domain.Amount;
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
    @Mapping(target = "drepId", source = "drepId")
    BFAccountContentDto toContentDto(AccountInfo source);

    @Mapping(target = "amount", source = "amount", qualifiedByName = "stringOrZero")
    @Mapping(target = "poolId", source = "poolId", qualifiedByName = "toBech32PoolId")
    @Mapping(target = "type", source = "type", qualifiedByName = "normalizeRewardType")
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

    @Named("normalizeRewardType")
    default String normalizeRewardType(String type) {
        if (type == null || type.isBlank()) {
            return type;
        }

        if ("refund".equalsIgnoreCase(type)) {
            return "pool_deposit_refund";
        }

        return type;
    }

    default String computeWithdrawableAmount(AccountInfo source) {
        BigInteger rewards = source.rewardsSum() != null ? source.rewardsSum() : BigInteger.ZERO;
        BigInteger withdrawals = source.withdrawalsSum() != null ? source.withdrawalsSum() : BigInteger.ZERO;
        BigInteger withdrawable = rewards.subtract(withdrawals);
        return withdrawable.compareTo(BigInteger.ZERO) < 0 ? "0" : withdrawable.toString();
    }

    default List<Amount> mapToAddressesTotalAmounts(Map<String, BigInteger> map) {
        if (map == null) return Collections.emptyList();
        return map.entrySet().stream()
                .map(e -> Amount.builder()
                        .unit(e.getKey())
                        .quantity(e.getValue() == null ? BigInteger.ZERO : e.getValue())
                        .build())
                .toList();
    }

    default List<Amount> mapToUtxoAmounts(Map<String, BigInteger> map) {
        if (map == null) return Collections.emptyList();
        // Blockfrost returns lovelace first, then other assets sorted alphabetically by unit
        return map.entrySet().stream()
                .sorted((a, b) -> {
                    if ("lovelace".equals(a.getKey())) return -1;
                    if ("lovelace".equals(b.getKey())) return 1;
                    return a.getKey().compareTo(b.getKey());
                })
                .map(e -> Amount.builder()
                        .unit(e.getKey())
                        .quantity(e.getValue() == null ? BigInteger.ZERO : e.getValue())
                        .build())
                .toList();
    }
}
