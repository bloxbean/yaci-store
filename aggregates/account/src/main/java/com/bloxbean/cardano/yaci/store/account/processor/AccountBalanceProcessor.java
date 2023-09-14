package com.bloxbean.cardano.yaci.store.account.processor;

import com.bloxbean.cardano.client.address.Address;
import com.bloxbean.cardano.client.address.AddressProvider;
import com.bloxbean.cardano.client.util.Tuple;
import com.bloxbean.cardano.yaci.core.util.HexUtil;
import com.bloxbean.cardano.yaci.store.account.AccountStoreConfiguration;
import com.bloxbean.cardano.yaci.store.account.domain.AddressBalance;
import com.bloxbean.cardano.yaci.store.account.domain.StakeAddressBalance;
import com.bloxbean.cardano.yaci.store.account.storage.AccountBalanceStorage;
import com.bloxbean.cardano.yaci.store.common.domain.AddressUtxo;
import com.bloxbean.cardano.yaci.store.common.domain.Amt;
import com.bloxbean.cardano.yaci.store.common.util.StringUtil;
import com.bloxbean.cardano.yaci.store.events.EventMetadata;
import com.bloxbean.cardano.yaci.store.events.GenesisBalance;
import com.bloxbean.cardano.yaci.store.events.GenesisBlockEvent;
import com.bloxbean.cardano.yaci.store.utxo.domain.AddressUtxoEvent;
import com.bloxbean.cardano.yaci.store.utxo.domain.TxInputOutput;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static com.bloxbean.cardano.yaci.core.util.Constants.LOVELACE;

@Component
@RequiredArgsConstructor
@Slf4j
public class AccountBalanceProcessor {
    private final AccountBalanceStorage accountBalanceStorage;
    private final AccountBalanceHistoryCleanupHelper accountBalanceCleanupHelper;
    private final AccountStoreConfiguration accountStoreConfiguration;

//    private boolean warmedUp = false;

    @EventListener
    @Transactional
    @SneakyThrows
    public void handleAddressUtxoEvent(AddressUtxoEvent addressUtxoEvent) {
        if (!accountStoreConfiguration.isBalanceAggregationEnabled())
            return; //Balance aggregation is disabled

        if (addressUtxoEvent.getTxInputOutputs() == null || addressUtxoEvent.getTxInputOutputs().size() == 0)
            return;

        EventMetadata metadata = addressUtxoEvent.getEventMetadata();
//        if (!warmedUp) {//Probably a restart, so delete all state after this slot no
//            int noOfDeleted = accountBalanceStorage.deleteAddressBalanceBySlotGreaterThan(metadata.getSlot() - 1);
//            log.info("Deleted {} address balances after slot {}", noOfDeleted, metadata.getSlot() - 1);
//            noOfDeleted = accountBalanceStorage.deleteStakeAddressBalanceBySlotGreaterThan(metadata.getSlot() - 1);
//            log.info("Deleted {} stake balances after slot {}", noOfDeleted, metadata.getSlot() - 1);
//            warmedUp = true;
//        }

        CompletableFuture<Void> addressBalancesFuture = CompletableFuture.supplyAsync(() -> handleAddressBalance(addressUtxoEvent))
                .thenAcceptAsync(addressBalances -> {
                    accountBalanceStorage.saveAddressBalances(addressBalances);

                    if (addressBalances != null && addressBalances.size() > 0) {
                        List<Tuple<String, String>> addresseUnitList =
                                addressBalances.stream().map(addressBalance -> new Tuple<>(addressBalance.getAddress(), addressBalance.getUnit())).distinct().toList();
                        accountBalanceCleanupHelper.deleteAddressBalanceBeforeConfirmedSlot(addresseUnitList, metadata.getSlot());
                    }
                });


        CompletableFuture<Void> stakeBalancesFuture = CompletableFuture.supplyAsync(() -> handleStakeAddressBalance(addressUtxoEvent))
                .thenAcceptAsync(stakeBalances -> {
                    accountBalanceStorage.saveStakeAddressBalances(stakeBalances);

                    if (stakeBalances != null && stakeBalances.size() > 0) {
                        List<Tuple<String, String>> stakeAddrUnitList =
                                stakeBalances.stream().map(stakeAddrBalance -> new Tuple<>(stakeAddrBalance.getAddress(), stakeAddrBalance.getUnit())).distinct().toList();
                        accountBalanceCleanupHelper.deleteStakeBalanceBeforeConfirmedSlot(stakeAddrUnitList, metadata.getSlot());
                    }
                });

        CompletableFuture<Void> combinedFuture = CompletableFuture.allOf(addressBalancesFuture, stakeBalancesFuture);

        // Wait for both steps to complete
        combinedFuture.join();
    }

    private List<AddressBalance> handleAddressBalance(AddressUtxoEvent addressUtxoEvent) {
        EventMetadata metadata = addressUtxoEvent.getEventMetadata();
        Map<String, AddressBalance> addressBalanceMap = new HashMap<>();
        for (TxInputOutput txInputOutput : addressUtxoEvent.getTxInputOutputs()) {
            List<AddressUtxo> inputs = txInputOutput.getInputs();
            List<AddressUtxo> outputs = txInputOutput.getOutputs();

            //Update inputs
            for (AddressUtxo input : inputs) {
                if (input.getAmounts() == null) {
                    log.error("Input amounts are null for tx: " + txInputOutput.getTxHash());
                    log.error("Input: " + input);
                }

                for (Amt amount : input.getAmounts()) {
                    String key = getKey(input.getOwnerAddr(), amount.getUnit());
                    if (addressBalanceMap.get(key) != null) {
                        AddressBalance addressBalance = addressBalanceMap.get(key);
                        addressBalance.setQuantity(addressBalance.getQuantity().subtract(amount.getQuantity()));
                    } else {
                        accountBalanceStorage.getAddressBalance(input.getOwnerAddr(), amount.getUnit(), metadata.getSlot() - 1)
                                .ifPresentOrElse(addressBalance -> {
                                    BigInteger newBalance = addressBalance.getQuantity().subtract(amount.getQuantity());
                                    if (newBalance.compareTo(BigInteger.ZERO) < 0) {
                                        log.error("[Inputs] Negative balance for address: " + input.getOwnerAddr() + " : " + newBalance);
                                        log.error("Existing AddressBalance >> " + addressBalance);
                                        log.error("Existing Balance: " + addressBalance.getQuantity() + ", Input: " + amount.getQuantity()
                                                + ", unit: " + amount.getUnit()
                                                + ", block: " + metadata.getBlock() + ", slot: " + metadata.getSlot());
                                        log.error("Current OutputTx: " + (outputs.size() > 0 ? outputs.get(0).getTxHash() : null));
                                        throw new IllegalStateException("Error in address balance calculation");
                                    }
                                    AddressBalance newAddressBalance = AddressBalance.builder()
                                            .address(input.getOwnerAddr())
                                            .blockHash(metadata.getBlockHash())
                                            .slot(metadata.getSlot())
                                            .blockNumber(metadata.getBlock())
                                            .blockTime(metadata.getBlockTime())
                                            .epoch(metadata.getEpochNumber())
                                            .paymentCredential(input.getOwnerPaymentCredential())
                                            .stakeAddress(input.getOwnerStakeAddr())
                                            .unit(amount.getUnit())
                                            .policy(amount.getPolicyId())
                                            .assetName(amount.getAssetName())
                                            .quantity(newBalance)
                                            .build();
                                    addressBalanceMap.put(key, newAddressBalance);

                                }, () -> {
                                    log.error("No balance found for address: " + input.getOwnerAddr() + ", unit: " + amount.getUnit()
                                            + ", block: " + metadata.getBlock() + ", slot: " + metadata.getSlot());
                                    log.error("Input AddressUtxo: " + input);
                                    log.error("Current OutputTx: " + (outputs.size() > 0 ? outputs.get(0).getTxHash() : null));
                                    throw new IllegalStateException("Error in address balance calculation");
                                });
                    }
                }
            }

            //Update outputs
            for (AddressUtxo output : outputs) {
                for (Amt amount : output.getAmounts()) {
                    String key = getKey(output.getOwnerAddr(), amount.getUnit());
                    if (addressBalanceMap.get(key) != null) {
                        AddressBalance addressBalance = addressBalanceMap.get(key);
                        //addressBalance.setQuantity(addressBalance.getQuantity().add(output.getLovelaceAmount()));
                        addressBalance.setQuantity(addressBalance.getQuantity().add(amount.getQuantity()));
                    } else {
                        accountBalanceStorage.getAddressBalance(output.getOwnerAddr(), amount.getUnit(), metadata.getSlot() - 1)
                                .ifPresentOrElse(addressBalance -> {
                                    BigInteger newBalance = addressBalance.getQuantity().add(amount.getQuantity());
                                    if (newBalance.compareTo(BigInteger.ZERO) < 0) {
                                        log.error("[Outputs] Negative balance for address: " + output.getOwnerAddr() + " : " + newBalance);
                                        log.error("Existing Balance: " + addressBalance.getQuantity() + ", Output: " + amount.getQuantity()
                                                + ", unit: " + amount.getUnit()
                                                + ", block: " + metadata.getBlock() + ", slot: " + metadata.getSlot());

                                        throw new IllegalStateException("Error in address balance calculation");
                                    }
                                    AddressBalance newAddressBalance = AddressBalance.builder()
                                            .address(output.getOwnerAddr())
                                            .blockHash(metadata.getBlockHash())
                                            .slot(metadata.getSlot())
                                            .blockNumber(metadata.getBlock())
                                            .blockTime(metadata.getBlockTime())
                                            .epoch(metadata.getEpochNumber())
                                            .paymentCredential(output.getOwnerPaymentCredential())
                                            .stakeAddress(output.getOwnerStakeAddr())
                                            .unit(amount.getUnit())
                                            .policy(amount.getPolicyId())
                                            .assetName(amount.getAssetName())
                                            .quantity(newBalance)
                                            .build();
                                    addressBalanceMap.put(key, newAddressBalance);

                                }, () -> {
                                    AddressBalance newAddressBalance = AddressBalance.builder()
                                            .address(output.getOwnerAddr())
                                            .blockHash(metadata.getBlockHash())
                                            .slot(metadata.getSlot())
                                            .blockNumber(metadata.getBlock())
                                            .blockTime(metadata.getBlockTime())
                                            .epoch(metadata.getEpochNumber())
                                            .paymentCredential(output.getOwnerPaymentCredential())
                                            .stakeAddress(output.getOwnerStakeAddr())
                                            .unit(amount.getUnit())
                                            .policy(amount.getPolicyId())
                                            .assetName(amount.getAssetName())
                                            .quantity(amount.getQuantity())
                                            .build();
                                    addressBalanceMap.put(key, newAddressBalance);
                                });
                    }
                }
            }
        }

        return addressBalanceMap.values().stream().toList();
    }

    private List<StakeAddressBalance> handleStakeAddressBalance(AddressUtxoEvent addressUtxoEvent) {
        EventMetadata metadata = addressUtxoEvent.getEventMetadata();
        Map<String, StakeAddressBalance> stakeBalanceMap = new HashMap<>();
        for (TxInputOutput txInputOutput : addressUtxoEvent.getTxInputOutputs()) {
            List<AddressUtxo> inputs = txInputOutput.getInputs();
            List<AddressUtxo> outputs = txInputOutput.getOutputs();

            //Update inputs
            for (AddressUtxo input : inputs) {
                if (StringUtil.isEmpty(input.getOwnerStakeAddr())) //Don't process if stake address is empty
                    continue;

                for (Amt amount : input.getAmounts()) {
                    String key = getKey(input.getOwnerStakeAddr(), amount.getUnit());
                    if (stakeBalanceMap.get(key) != null) {
                        StakeAddressBalance addressBalance = stakeBalanceMap.get(key);
                        addressBalance.setQuantity(addressBalance.getQuantity().subtract(amount.getQuantity()));
                    } else {
                        accountBalanceStorage.getStakeAddressBalance(input.getOwnerStakeAddr(), amount.getUnit(), metadata.getSlot() - 1)
                                .ifPresentOrElse(stakeAddrBalance -> {
                                    BigInteger newBalance = stakeAddrBalance.getQuantity().subtract(amount.getQuantity());
                                    if (newBalance.compareTo(BigInteger.ZERO) < 0) {
                                        log.error("[Inputs] Negative balance for address: " + input.getOwnerStakeAddr() + " : " + newBalance);
                                        log.error("Existing StakeAddrBalance >> " + stakeAddrBalance);
                                        log.error("Existing Balance: " + stakeAddrBalance.getQuantity() + ", Input: " + amount.getQuantity()
                                                + ", unit: " + amount.getUnit()
                                                + ", block: " + metadata.getBlock() + ", slot: " + metadata.getSlot());
                                        log.error("Current OutputTx: " + (outputs.size() > 0 ? outputs.get(0).getTxHash() : null));
                                        throw new IllegalStateException("Error in stake addr balance calculation");
                                    }
                                    StakeAddressBalance newStakeAddrBalance = StakeAddressBalance.builder()
                                            .address(input.getOwnerStakeAddr())
                                            .blockHash(metadata.getBlockHash())
                                            .slot(metadata.getSlot())
                                            .blockNumber(metadata.getBlock())
                                            .blockTime(metadata.getBlockTime())
                                            .epoch(metadata.getEpochNumber())
                                            .stakeCredential(input.getOwnerStakeCredential())
                                            .unit(amount.getUnit())
                                            .policy(amount.getPolicyId())
                                            .assetName(amount.getAssetName())
                                            .quantity(newBalance)
                                            .build();
                                    stakeBalanceMap.put(key, newStakeAddrBalance);

                                }, () -> {
                                    log.error("No balance found for address: " + input.getOwnerAddr());
                                    log.error("Input AddressUtxo: " + input);
                                    log.error("Current OutputTx: " + (outputs.size() > 0 ? outputs.get(0).getTxHash() : null));
                                    throw new IllegalStateException("Error in address balance calculation");
                                });
                    }
                }
            }

            //Update outputs
            for (AddressUtxo output : outputs) {
                if (StringUtil.isEmpty(output.getOwnerStakeAddr())) //Don't process if stake address is empty
                    continue;

                for (Amt amount : output.getAmounts()) {
                    String key = getKey(output.getOwnerStakeAddr(), amount.getUnit());
                    if (stakeBalanceMap.get(key) != null) {
                        StakeAddressBalance stakeAddrBalance = stakeBalanceMap.get(key);
                        stakeAddrBalance.setQuantity(stakeAddrBalance.getQuantity().add(amount.getQuantity()));
                    } else {
                        accountBalanceStorage.getStakeAddressBalance(output.getOwnerStakeAddr(), amount.getUnit(), metadata.getSlot() - 1)
                                .ifPresentOrElse(stakeAddrBalance -> {
                                    BigInteger newBalance = stakeAddrBalance.getQuantity().add(amount.getQuantity());
                                    if (newBalance.compareTo(BigInteger.ZERO) < 0) {
                                        log.error("[Outputs] Negative balance for address: " + output.getOwnerStakeAddr() + " : " + newBalance);
                                        log.error("Existing Balance: " + stakeAddrBalance.getQuantity() + ", Output: " + amount.getQuantity()
                                                + ", unit: " + amount.getUnit()
                                                + ", block: " + metadata.getBlock() + ", slot: " + metadata.getSlot());

                                        throw new IllegalStateException("Error in stake addr balance calculation");
                                    }
                                    StakeAddressBalance newStakeAddrBalance = StakeAddressBalance.builder()
                                            .address(output.getOwnerStakeAddr())
                                            .blockHash(metadata.getBlockHash())
                                            .slot(metadata.getSlot())
                                            .blockNumber(metadata.getBlock())
                                            .blockTime(metadata.getBlockTime())
                                            .epoch(metadata.getEpochNumber())
                                            .stakeCredential(output.getOwnerStakeCredential())
                                            .unit(amount.getUnit())
                                            .policy(amount.getPolicyId())
                                            .assetName(amount.getAssetName())
                                            .quantity(newBalance)
                                            .build();
                                    stakeBalanceMap.put(key, newStakeAddrBalance);

                                }, () -> {
                                    StakeAddressBalance newStakeAddrBalance = StakeAddressBalance.builder()
                                            .address(output.getOwnerStakeAddr())
                                            .blockHash(metadata.getBlockHash())
                                            .slot(metadata.getSlot())
                                            .blockNumber(metadata.getBlock())
                                            .blockTime(metadata.getBlockTime())
                                            .epoch(metadata.getEpochNumber())
                                            .stakeCredential(output.getOwnerStakeCredential())
                                            .unit(amount.getUnit())
                                            .policy(amount.getPolicyId())
                                            .assetName(amount.getAssetName())
                                            .quantity(amount.getQuantity())
                                            .build();
                                    stakeBalanceMap.put(key, newStakeAddrBalance);
                                });
                    }
                }
            }
        }

        return stakeBalanceMap.values().stream().toList();
    }

    @EventListener
    @Transactional
    public void handleGenesisBalanceEvent(GenesisBlockEvent genesisBlockEvent) {
        if (!accountStoreConfiguration.isBalanceAggregationEnabled())
            return; //Balance aggregation is disabled

        List<GenesisBalance> genesisBalanceList = genesisBlockEvent.getGenesisBalances();
        if (genesisBalanceList == null || genesisBalanceList.size() == 0)
            return;

        List<AddressBalance> addressBalances = genesisBalanceList.stream()
                .map(genesisBalance -> {
                    String paymentCredential = null;
                    String stakeAddress = null;
                    try {
                        Address address = new Address(genesisBalance.getAddress());
                        paymentCredential = address.getPaymentCredential().map(credential -> HexUtil.encodeHexString(credential.getBytes()))
                                .orElse(null);
                        stakeAddress = address.getDelegationCredential().map(delegCred -> AddressProvider.getStakeAddress(address).toBech32())
                                .orElse(null);
                    } catch (Exception e) {
                        //Not a valid shelley address
                    }

                    AddressBalance addressBalance =  AddressBalance.builder()
                            .address(genesisBalance.getAddress())
                            .blockHash(genesisBlockEvent.getBlockHash())
                            .slot(genesisBlockEvent.getSlot())
                            .blockNumber(genesisBlockEvent.getBlock())
                            .blockTime(genesisBlockEvent.getBlockTime())
                            .unit(LOVELACE)
                            .assetName(LOVELACE)
                            .quantity(genesisBalance.getBalance())
                            .paymentCredential(paymentCredential)
                            .stakeAddress(stakeAddress)
                            .build();
                    return addressBalance;
                }).toList();
        accountBalanceStorage.saveAddressBalances(addressBalances);

        List<StakeAddressBalance> stakeAddrBalances = genesisBalanceList.stream()
                .filter(genesisBalance -> {
                    try {
                        Address address = new Address(genesisBalance.getAddress());
                        return address.getDelegationCredential().isPresent();
                    } catch (Exception e) {
                        //Not a valid shelley address
                        return false;
                    }
                }).map(genesisBalance -> {
                    Address address = new Address(genesisBalance.getAddress());
                    Address stakeAddress = AddressProvider.getStakeAddress(address);
                    StakeAddressBalance stakeAddrBalance = StakeAddressBalance.builder()
                            .address(stakeAddress.toBech32())
                            .blockHash(genesisBlockEvent.getBlockHash())
                            .slot(genesisBlockEvent.getSlot())
                            .blockNumber(genesisBlockEvent.getBlock())
                            .blockTime(genesisBlockEvent.getBlockTime())
                            .unit(LOVELACE)
                            .assetName(LOVELACE)
                            .quantity(genesisBalance.getBalance())
                            .stakeCredential(HexUtil.encodeHexString(stakeAddress.getDelegationCredential().get().getBytes()))
                            .build();
                    return stakeAddrBalance;
                }).toList();
        accountBalanceStorage.saveStakeAddressBalances(stakeAddrBalances);
    }

    private String getKey(String address, String unit) {
        return address + "-" + unit;
    }
}
