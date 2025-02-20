package com.bloxbean.cardano.yaci.store.account.processor;

import com.bloxbean.cardano.yaci.core.model.Era;
import com.bloxbean.cardano.yaci.store.account.AccountStoreProperties;
import com.bloxbean.cardano.yaci.store.account.domain.StakeAddressBalance;
import com.bloxbean.cardano.yaci.store.account.storage.AccountBalanceStorage;
import com.bloxbean.cardano.yaci.store.common.model.Order;
import com.bloxbean.cardano.yaci.store.core.service.EraService;
import com.bloxbean.cardano.yaci.store.events.internal.PreEpochTransitionEvent;
import com.bloxbean.cardano.yaci.store.utxo.domain.PtrAddress;
import com.bloxbean.cardano.yaci.store.utxo.storage.AddressStorage;
import com.bloxbean.cardano.yaci.store.utxo.storage.UtxoStorageReader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.math.BigInteger;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class PtrAddressProcessor {
    private final AccountBalanceStorage accountBalanceStorage;
    private final AddressStorage addressStorage;
    private final UtxoStorageReader utxoStorageReader;
    private final EraService eraService;
    private final AccountStoreProperties accountStoreProperties;

    @EventListener
    public void handlePtrAddressRemovalOnConwayEra(PreEpochTransitionEvent event) {
        if (!accountStoreProperties.isStakeAddressBalanceEnabled()) return;

        Era prevEra = event.getPreviousEra();
        Era newEra = event.getEra();

        if (prevEra == Era.Babbage && newEra == Era.Conway) {
            //Adjust pointer address balance
            var ptrAddressList = addressStorage.findPtrAddresses();
            if (ptrAddressList == null || ptrAddressList.size() == 0)
                return;

            long firstSlotOfNewEpoch = eraService.getShelleyAbsoluteSlot(event.getEpoch(), 0);
            long lastSlotOfPrevEpoch = firstSlotOfNewEpoch - 1;

            ptrAddressList.forEach(ptrAddress ->  {
                if (ptrAddress.getStakeAddress() == null || ptrAddress.getStakeAddress().isEmpty())
                    return;

                removePtrAddressBalanceFromStakeBalance(ptrAddress, event.getPreviousEpoch(), lastSlotOfPrevEpoch);
            });
        }
    }

    private void removePtrAddressBalanceFromStakeBalance(PtrAddress ptrAddress, int epoch, long slot) {
        String stakeAddress = ptrAddress.getStakeAddress();

        Optional<StakeAddressBalance> stakeAddressBalance =
                accountBalanceStorage.getStakeAddressBalance(stakeAddress, slot);

        if (stakeAddressBalance.isEmpty())
            return;

        //Find ptrAddress balance
        var ptrAddressBalance = calculatePtrAddressBalance(ptrAddress.getAddress());
        if (BigInteger.ZERO.equals(ptrAddressBalance)) {
            log.info("Pointer address's balance is zero. Nothing to update. {}", ptrAddress.getAddress());
            return;
        }

        var updatedStakeBalance = stakeAddressBalance
                .map(balance -> {
                    var updatedQtry = balance.getQuantity().subtract(ptrAddressBalance);
                    if (updatedQtry.compareTo(BigInteger.ZERO) < 0)
                        updatedQtry = BigInteger.ZERO;

                    return StakeAddressBalance.builder()
                            .address(stakeAddress)
                            .slot(slot)
                            .epoch(epoch)
                            .quantity(updatedQtry)
                            .blockNumber(null)
                            .blockTime(null)
                            .build();
                }).orElse(null);

        if (updatedStakeBalance != null) {
            accountBalanceStorage.saveStakeAddressBalances(List.of(updatedStakeBalance));
            log.info("Updating stake address balance to remove the balance of pointer address in Conway era.\n Stake Address: {}, New Balance: {}",
                    stakeAddress, updatedStakeBalance.getQuantity());
        }
    }

    private BigInteger calculatePtrAddressBalance(String address) {
        int page = 0;
        int count = 50; // Example page size
        BigInteger totalBalance = BigInteger.ZERO;

        while (true) {
            var utxos = utxoStorageReader.findUtxoByAddress(address, page, count, Order.desc);
            if (utxos == null || utxos.isEmpty())
                break;

            for (var utxo : utxos) {
                totalBalance = totalBalance.add(utxo.getLovelaceAmount());
            }

            page++;
        }

        return totalBalance;

    }

}
