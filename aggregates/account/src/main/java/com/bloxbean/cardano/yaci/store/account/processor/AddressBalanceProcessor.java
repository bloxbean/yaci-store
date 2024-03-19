package com.bloxbean.cardano.yaci.store.account.processor;

import com.bloxbean.cardano.client.address.Address;
import com.bloxbean.cardano.client.api.util.AssetUtil;
import com.bloxbean.cardano.client.util.HexUtil;
import com.bloxbean.cardano.yaci.store.account.domain.AddressBalance;
import com.bloxbean.cardano.yaci.store.account.domain.AddressTxAmount;
import com.bloxbean.cardano.yaci.store.account.domain.StakeAddressBalance;
import com.bloxbean.cardano.yaci.store.account.domain.event.AddressTxAmountBatchEvent;
import com.bloxbean.cardano.yaci.store.account.service.AccountConfigService;
import com.bloxbean.cardano.yaci.store.account.storage.AccountBalanceStorage;
import com.bloxbean.cardano.yaci.store.account.util.ConfigIds;
import com.bloxbean.cardano.yaci.store.common.util.Tuple;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigInteger;
import java.util.*;

import static com.bloxbean.cardano.client.common.CardanoConstants.LOVELACE;

@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnExpression("${store.account.balance.address-tx-mode:true}")
public class AddressBalanceProcessor {
    private final AccountConfigService accountConfigService;
    private final AccountBalanceStorage accountBalanceStorage;

    @EventListener
    @Transactional
    public void handleAccountBalance(AddressTxAmountBatchEvent event) {
        Long lastBalanceProcessedSlot = accountConfigService.getConfig(ConfigIds.LAST_ACCOUNT_BALANCE_PROCESSED_BLOCK)
                .map(accountConfigEntity -> accountConfigEntity.getSlot())
                .orElse(-2L); //Because Block No start from -1 (Genesis Block

        //TODO -- check if last balance record is not available

        if (lastBalanceProcessedSlot > event.getMetadata().getSlot())
            throw new IllegalStateException("Last processed slot is greater than current slot. Last processed slot : " + lastBalanceProcessedSlot + ", Current slot : " + event.getMetadata().getSlot());

        calculateAddressBalance(event, lastBalanceProcessedSlot);
        calculateStakeAddressBalance(event, lastBalanceProcessedSlot);

        accountConfigService.upateConfig(ConfigIds.LAST_ACCOUNT_BALANCE_PROCESSED_BLOCK, null, event.getMetadata().getBlock(),
                event.getMetadata().getBlockHash(), event.getMetadata().getSlot());
    }

    private void calculateAddressBalance(AddressTxAmountBatchEvent event, Long lastBalanceProcessedSlot) {
        Map<AddressUnit, AddressTxAmount> addressTxAmountMap = new HashMap<>();
        for (AddressTxAmount addressTxAmount : event.getAddressTxAmountList()) {
            var key = new AddressUnit(addressTxAmount.getAddress(), addressTxAmount.getUnit());

            //check if map already contains the key
            if (addressTxAmountMap.containsKey(key)) {
                var existingAddressTxAmount = addressTxAmountMap.get(key);
                var newQuantity = existingAddressTxAmount.getQuantity().add(addressTxAmount.getQuantity());
                existingAddressTxAmount.setQuantity(newQuantity);
                if (existingAddressTxAmount.getSlot() < addressTxAmount.getSlot()) {
                    existingAddressTxAmount.setSlot(addressTxAmount.getSlot());
                    existingAddressTxAmount.setBlockNumber(addressTxAmount.getBlockNumber());
                    existingAddressTxAmount.setBlockTime(addressTxAmount.getBlockTime());
                    existingAddressTxAmount.setEpoch(addressTxAmount.getEpoch());
                }
            } else {
                var addressTxAmt = AddressTxAmount.builder()
                        .address(addressTxAmount.getAddress())
                        .unit(addressTxAmount.getUnit())
                        .quantity(addressTxAmount.getQuantity())
                        .slot(addressTxAmount.getSlot())
                        .blockNumber(addressTxAmount.getBlockNumber())
                        .blockTime(addressTxAmount.getBlockTime())
                        .epoch(addressTxAmount.getEpoch())
                        .build();
                addressTxAmountMap.put(key, addressTxAmt);
            }
        }

        var groupedAddressTxAmountList = addressTxAmountMap.values();

        if (groupedAddressTxAmountList.size() == 0)
            return;

        long t1 = System.currentTimeMillis();

        long block = event.getMetadata().getBlock();

        List<AddressBalance> addressBalancesToSave = Collections.synchronizedList(new ArrayList<>());
        groupedAddressTxAmountList.parallelStream()
                .forEach(addressTxAmount -> {
                    Tuple<String, String> policyAssetName = getPolicyAndAssetName(addressTxAmount.getUnit());
                    var currentBalance = accountBalanceStorage.getAddressBalance(addressTxAmount.getAddress(), addressTxAmount.getUnit(), lastBalanceProcessedSlot)
                            .orElse(AddressBalance.builder()
                                    .address(addressTxAmount.getAddress())
                                    .unit(addressTxAmount.getUnit())
                                    .quantity(BigInteger.ZERO)
                                    .slot(lastBalanceProcessedSlot)
                                    .blockHash(null)
                                    .epoch(null)
                                    .build()
                            );

                    if (currentBalance.getSlot().equals(addressTxAmount.getSlot())) {
                        log.warn("Duplicate balance record found in address_balance for address : " + addressTxAmount.getAddress() + ", slot : " + addressTxAmount.getSlot());
                        log.warn("Skipping address_balance calculation for this batch");
                        return;
                    }

                    var newBalance = currentBalance.getQuantity().add(addressTxAmount.getQuantity());

                    var newAddrBalance = AddressBalance.builder()
                            .address(addressTxAmount.getAddress())
                            .unit(addressTxAmount.getUnit())
                            .quantity(newBalance)
                            .policy(policyAssetName._1)
                            .assetName(policyAssetName._2)
                            .stakeAddress(addressTxAmount.getStakeAddress())
                            .slot(addressTxAmount.getSlot())
                            .blockNumber(addressTxAmount.getBlockNumber())
                            .blockTime(addressTxAmount.getBlockTime())
                            .epoch(addressTxAmount.getEpoch())
                            .build();

                    addressBalancesToSave.add(newAddrBalance);
                });

        var negativeBalance = addressBalancesToSave
                .parallelStream().filter(addressBalance -> addressBalance.getQuantity().compareTo(BigInteger.ZERO) < 0)
                .toList();

        if (negativeBalance.size() > 0) {
            log.error("Negative balance found for addresses : " + negativeBalance);
            throw new IllegalStateException("Negative balance found for addresses : " + negativeBalance);
        }

        if (addressBalancesToSave.size() > 0)
            accountBalanceStorage.saveAddressBalances(addressBalancesToSave);

        accountConfigService.upateConfig(ConfigIds.LAST_ACCOUNT_BALANCE_PROCESSED_BLOCK, null, event.getMetadata().getBlock(),
                event.getMetadata().getBlockHash(), event.getMetadata().getSlot());

        long t2 = System.currentTimeMillis();
        log.info("Account balance snapshot for block : " + block + " took " + (t2 - t1) + " ms");
    }

    private void calculateStakeAddressBalance(AddressTxAmountBatchEvent event, Long lastBalanceProcessedSlot) {
        //Filter only lovelace tx amounts
        var lovelaceTxAmounts = event.getAddressTxAmountList().stream()
                .filter(addressTxAmount -> addressTxAmount.getUnit().equals(LOVELACE) && addressTxAmount.getStakeAddress() != null)
                .toList();

        if (lovelaceTxAmounts.isEmpty())
            return;

        Map<String, AddressTxAmount> addressTxAmountMap = new HashMap<>();
        for (AddressTxAmount addressTxAmount : lovelaceTxAmounts) {
            String key = addressTxAmount.getStakeAddress();
            //check if map already contains the key
            if (addressTxAmountMap.containsKey(key)) {
                var existingAddressTxAmount = addressTxAmountMap.get(key);
                var newQuantity = existingAddressTxAmount.getQuantity().add(addressTxAmount.getQuantity());
                existingAddressTxAmount.setQuantity(newQuantity);
                if (existingAddressTxAmount.getSlot() < addressTxAmount.getSlot()) {
                    existingAddressTxAmount.setSlot(addressTxAmount.getSlot());
                    existingAddressTxAmount.setBlockNumber(addressTxAmount.getBlockNumber());
                    existingAddressTxAmount.setBlockTime(addressTxAmount.getBlockTime());
                    existingAddressTxAmount.setEpoch(addressTxAmount.getEpoch());
                }
            } else {
                var addressTxAmt = AddressTxAmount.builder()
                        .address(addressTxAmount.getAddress())
                        .unit(addressTxAmount.getUnit())
                        .quantity(addressTxAmount.getQuantity())
                        .stakeAddress(addressTxAmount.getStakeAddress())
                        .slot(addressTxAmount.getSlot())
                        .blockNumber(addressTxAmount.getBlockNumber())
                        .blockTime(addressTxAmount.getBlockTime())
                        .epoch(addressTxAmount.getEpoch())
                        .build();
                addressTxAmountMap.put(key, addressTxAmt);
            }
        }

        var groupedAddressTxAmountList = addressTxAmountMap.values();

        if (groupedAddressTxAmountList.size() == 0)
            return;

        long t1 = System.currentTimeMillis();

        long block = event.getMetadata().getBlock();

        List<StakeAddressBalance> addressBalancesToSave = Collections.synchronizedList(new ArrayList<>());
        groupedAddressTxAmountList.parallelStream()
                .forEach(addressTxAmount -> {

                    String stakeCredential = null;
                    try {
                        stakeCredential = new Address(addressTxAmount.getStakeAddress()).getDelegationCredential()
                                .map(delegationHash -> HexUtil.encodeHexString(delegationHash.getBytes()))
                                .orElse(null);
                    } catch (Exception e) {
                        log.warn("Unable to get delegation credential : " + addressTxAmount.getStakeAddress(), e);
                    }

                    var currentBalance = accountBalanceStorage.getStakeAddressBalance(addressTxAmount.getStakeAddress(), lastBalanceProcessedSlot)
                            .orElse(StakeAddressBalance.builder()
                                    .address(addressTxAmount.getStakeAddress())
                                    .quantity(BigInteger.ZERO)
                                    .slot(lastBalanceProcessedSlot)
                                    .blockHash(null)
                                    .epoch(null)
                                    .build()
                            );

                    if (currentBalance.getSlot().equals(addressTxAmount.getSlot())) {
                        log.warn("Duplicate balance record found in stake_address_balance for address : " + addressTxAmount.getAddress() + ", slot : " + addressTxAmount.getSlot());
                        log.warn("Skipping stake_address_balance calculation for this batch");
                        return;
                    }

                    var newBalance = currentBalance.getQuantity().add(addressTxAmount.getQuantity());

                    var newAddrBalance = StakeAddressBalance.builder()
                            .address(addressTxAmount.getStakeAddress())
                            .quantity(newBalance)
                            .stakeCredential(stakeCredential)
                            .slot(addressTxAmount.getSlot())
                            .blockNumber(addressTxAmount.getBlockNumber())
                            .blockTime(addressTxAmount.getBlockTime())
                            .epoch(addressTxAmount.getEpoch())
                            .build();

                    addressBalancesToSave.add(newAddrBalance);
                });

        var negativeBalance = addressBalancesToSave
                .parallelStream().filter(addressBalance -> addressBalance.getQuantity().compareTo(BigInteger.ZERO) < 0)
                .toList();

        if (negativeBalance.size() > 0) {
            log.error("Negative balance found for stake addresses : " + negativeBalance);
            throw new IllegalStateException("Negative balance found for stake addresses : " + negativeBalance);
        }

        if (addressBalancesToSave.size() > 0)
            accountBalanceStorage.saveStakeAddressBalances(addressBalancesToSave);

        long t2 = System.currentTimeMillis();
        log.info("Stake Account balance snapshot for block : " + block + " took " + (t2 - t1) + " ms");
    }


    private Tuple<String, String> getPolicyAndAssetName(String unit) {
        if (unit.equals(LOVELACE))
            return new Tuple<>(null, LOVELACE);

        var tuple = AssetUtil.getPolicyIdAndAssetName(unit);
        return new Tuple<>(tuple._1, tuple._2);
    }

//    private List<AddressTxAmount> getAddressTxAmountListBetween(long startSlot, long endSlot) {
//        return dsl.
//                select(ADDRESS_TX_AMOUNT.ADDRESS, ADDRESS_TX_AMOUNT.UNIT,
//                        sum(ADDRESS_TX_AMOUNT.QUANTITY).as("quantity"), max(ADDRESS_TX_AMOUNT.SLOT).as("slot"),
//                        max(ADDRESS_TX_AMOUNT.BLOCK).as("block"), max(ADDRESS_TX_AMOUNT.BLOCK_TIME).as("block_time"))
//                .from(ADDRESS_TX_AMOUNT)
//                .where(ADDRESS_TX_AMOUNT.SLOT.greaterThan(startSlot)
//                        .and(ADDRESS_TX_AMOUNT.SLOT.le(endSlot)))
//                .groupBy(ADDRESS_TX_AMOUNT.ADDRESS, ADDRESS_TX_AMOUNT.UNIT)
//                .fetchInto(AddressTxAmount.class);
//    }
}


