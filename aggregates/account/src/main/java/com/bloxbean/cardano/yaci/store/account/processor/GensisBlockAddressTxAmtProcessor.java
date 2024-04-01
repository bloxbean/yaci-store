package com.bloxbean.cardano.yaci.store.account.processor;

import com.bloxbean.cardano.client.address.Address;
import com.bloxbean.cardano.client.address.AddressProvider;
import com.bloxbean.cardano.yaci.store.account.AccountStoreProperties;
import com.bloxbean.cardano.yaci.store.account.domain.AddressTxAmount;
import com.bloxbean.cardano.yaci.store.account.storage.AddressTxAmountStorage;
import com.bloxbean.cardano.yaci.store.events.GenesisBlockEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.bloxbean.cardano.yaci.core.util.Constants.LOVELACE;
import static com.bloxbean.cardano.yaci.store.account.util.AddressUtil.getAddress;

@Component
@RequiredArgsConstructor
@Slf4j
public class GensisBlockAddressTxAmtProcessor {
    private final AddressTxAmountStorage addressTxAmountStorage;
    private final AccountStoreProperties accountStoreProperties;

    @EventListener
    @Transactional
    public void handleAddressTxAmtForGenesisBlock(GenesisBlockEvent genesisBlockEvent) {
        var genesisBalances = genesisBlockEvent.getGenesisBalances();
        if (genesisBalances == null || genesisBalances.isEmpty()) {
            log.info("No genesis balances found");
            return;
        }

        List<AddressTxAmount> genesisAddressTxAmtList = new ArrayList<>();
        Set<com.bloxbean.cardano.yaci.store.account.domain.Address> addresses = new HashSet<>();
        for (var genesisBalance : genesisBalances) {
            var receiverAddress = genesisBalance.getAddress();
            var txnHash = genesisBalance.getTxnHash();
            var balance = genesisBalance.getBalance();

            if (balance == null ||
                    (!accountStoreProperties.isAddressTxAmountIncludeZeroAmount() && balance.compareTo(BigInteger.ZERO) == 0)) {
                continue;
            }

            //address and full address if the address is too long
            var addressTuple = getAddress(receiverAddress);

            String stakeAddress = null;
            if (genesisBalance.getAddress() != null &&
                    genesisBalance.getAddress().startsWith("addr")) { //If shelley address
                try {
                    Address address = new Address(genesisBalance.getAddress());
                    stakeAddress = address.getDelegationCredential().map(delegationHash -> AddressProvider.getStakeAddress(address).toBech32())
                            .orElse(null);
                } catch (Exception e) {
                    log.warn("Error getting address details: " + genesisBalance.getAddress());
                }
            }

            var addressTxAmount = AddressTxAmount.builder()
                    .address(addressTuple._1)
                    .unit(LOVELACE)
                    .txHash(txnHash)
                    .slot(genesisBlockEvent.getSlot())
                    .quantity(balance)
                    .stakeAddress(stakeAddress)
                    .epoch(0)
                    .blockNumber(genesisBlockEvent.getBlock())
                    .blockTime(genesisBlockEvent.getBlockTime())
                    .build();

            genesisAddressTxAmtList.add(addressTxAmount);
        }

        if (genesisAddressTxAmtList.size() > 0) {
            addressTxAmountStorage.save(genesisAddressTxAmtList);
        }

    }

}
