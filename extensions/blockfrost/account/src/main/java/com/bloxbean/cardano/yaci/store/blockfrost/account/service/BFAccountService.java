package com.bloxbean.cardano.yaci.store.blockfrost.account.service;

import com.bloxbean.cardano.yaci.store.blockfrost.account.dto.*;
import com.bloxbean.cardano.yaci.store.blockfrost.account.mapper.BFAccountMapper;
import com.bloxbean.cardano.yaci.store.blockfrost.account.storage.BFAccountStorageReader;
import com.bloxbean.cardano.yaci.store.common.model.Order;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collections;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class BFAccountService {
    private final BFAccountStorageReader storageReader;
    private final BFAccountMapper mapper = BFAccountMapper.INSTANCE;

    public BFAccountContentDto getAccountInfo(String stakeAddress) {
        return storageReader.getAccountInfo(stakeAddress)
                .map(mapper::toContentDto)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "The requested component has not been found."));
    }

    public List<BFAccountRewardDto> findRewards(String stakeAddress, int page, int count, Order order) {
        return storageReader.findRewards(stakeAddress, page, count, order)
                .stream().map(mapper::toRewardDto).toList();
    }

    public List<BFAccountHistoryDto> findHistory(String stakeAddress, int page, int count, Order order) {
        return storageReader.findHistory(stakeAddress, page, count, order)
                .stream().map(mapper::toHistoryDto).toList();
    }

    public List<BFAccountDelegationDto> findDelegations(String stakeAddress, int page, int count, Order order) {
        return storageReader.findDelegations(stakeAddress, page, count, order)
                .stream().map(mapper::toDelegationDto).toList();
    }

    public List<BFAccountRegistrationDto> findRegistrations(String stakeAddress, int page, int count, Order order) {
        return storageReader.findRegistrations(stakeAddress, page, count, order)
                .stream().map(mapper::toRegistrationDto).toList();
    }

    public List<BFAccountWithdrawalDto> findWithdrawals(String stakeAddress, int page, int count, Order order) {
        return storageReader.findWithdrawals(stakeAddress, page, count, order)
                .stream().map(mapper::toWithdrawalDto).toList();
    }

    public List<BFAccountMirDto> findMIRs(String stakeAddress, int page, int count, Order order) {
        return storageReader.findMIRs(stakeAddress, page, count, order)
                .stream().map(mapper::toMirDto).toList();
    }

    public List<BFAccountAddressDto> findAddresses(String stakeAddress, int page, int count, Order order) {
        return storageReader.findAddresses(stakeAddress, page, count, order)
                .stream().map(mapper::toAddressDto).toList();
    }

    public List<BFAccountAddressAssetDto> findAddressAssets(String stakeAddress, int page, int count, Order order) {
        return storageReader.findAddressAssets(stakeAddress, page, count, order)
                .stream().map(mapper::toAddressAssetDto).toList();
    }

    public BFAccountAddressesTotalDto getAddressesTotal(String stakeAddress) {
        return storageReader.getAddressesTotal(stakeAddress)
                .map(mapper::toAddressesTotalDto)
                .orElseGet(() -> BFAccountAddressesTotalDto.builder()
                        .stakeAddress(stakeAddress)
                        .receivedSum(Collections.emptyList())
                        .sentSum(Collections.emptyList())
                        .txCount(0L)
                        .build());
    }

    public List<BFAccountUtxoDto> findUtxos(String stakeAddress, int page, int count, Order order) {
        return storageReader.findUtxos(stakeAddress, page, count, order)
                .stream().map(mapper::toUtxoDto).toList();
    }

    public List<BFAccountTransactionDto> findTransactions(String stakeAddress, int page, int count, Order order, String from, String to) {
        return storageReader.findTransactions(stakeAddress, page, count, order, from, to)
                .stream().map(mapper::toTransactionDto).toList();
    }
}
