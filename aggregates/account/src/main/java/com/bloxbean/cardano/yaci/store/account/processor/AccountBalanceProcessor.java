package com.bloxbean.cardano.yaci.store.account.processor;

import com.bloxbean.cardano.client.address.Address;
import com.bloxbean.cardano.client.address.AddressProvider;
import com.bloxbean.cardano.client.util.Tuple;
import com.bloxbean.cardano.yaci.core.util.HexUtil;
import com.bloxbean.cardano.yaci.store.account.AccountStoreProperties;
import com.bloxbean.cardano.yaci.store.account.domain.AddressBalance;
import com.bloxbean.cardano.yaci.store.account.domain.StakeAddressBalance;
import com.bloxbean.cardano.yaci.store.account.service.AccountConfigService;
import com.bloxbean.cardano.yaci.store.account.storage.AccountBalanceStorage;
import com.bloxbean.cardano.yaci.store.account.storage.impl.jpa.model.AccountConfigEntity;
import com.bloxbean.cardano.yaci.store.account.util.ConfigIds;
import com.bloxbean.cardano.yaci.store.account.util.ConfigStatus;
import com.bloxbean.cardano.yaci.store.client.utxo.UtxoClient;
import com.bloxbean.cardano.yaci.store.common.domain.AddressUtxo;
import com.bloxbean.cardano.yaci.store.common.domain.Amt;
import com.bloxbean.cardano.yaci.store.common.domain.UtxoKey;
import com.bloxbean.cardano.yaci.store.common.util.StringUtil;
import com.bloxbean.cardano.yaci.store.events.EventMetadata;
import com.bloxbean.cardano.yaci.store.events.GenesisBalance;
import com.bloxbean.cardano.yaci.store.events.GenesisBlockEvent;
import com.bloxbean.cardano.yaci.store.events.internal.CommitEvent;
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
import java.util.stream.Collectors;

import static com.bloxbean.cardano.yaci.core.util.Constants.LOVELACE;

@Component
@RequiredArgsConstructor
@Slf4j
public class AccountBalanceProcessor {
    private final AccountBalanceStorage accountBalanceStorage;
    private final AccountBalanceHistoryCleanupHelper accountBalanceCleanupHelper;
    private final AccountStoreProperties accountStoreProperties;
    private final UtxoClient utxoClient;
    private final AccountConfigService accountConfigService;
    private final AccountBalanceBatchProcessingService accountBalanceBatchProcessingService;

    @EventListener
    @Transactional
    @SneakyThrows
    public void handleAddressUtxoEvent(AddressUtxoEvent addressUtxoEvent) {
        if (!accountStoreProperties.isBalanceAggregationEnabled())
            return; //Balance aggregation is disabled

        if (addressUtxoEvent.getEventMetadata().isParallelMode())
            return; //Ignore when parallel mode is enabled

        if (accountStoreProperties.isBatchBalanceAggregationEnabled()) {
            //If true, we can process balance for this block here, otherwise return
            if (!handlePendingBalanceCalculation(addressUtxoEvent))
                return;
        }

        accountConfigService.upateConfig(ConfigIds.LAST_ACCOUNT_BALANCE_PROCESSED_BLOCK, null, addressUtxoEvent.getEventMetadata().getBlock());
        if (addressUtxoEvent.getTxInputOutputs() == null || addressUtxoEvent.getTxInputOutputs().size() == 0)
            return;

        EventMetadata metadata = addressUtxoEvent.getEventMetadata();

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

    @EventListener
    @Transactional
    public void handleCommitEvent(CommitEvent commitEvent) {
        long currentBlock = commitEvent.getMetadata().getBlock();
        accountConfigService.upateConfig(ConfigIds.LAST_PROCESSED_BLOCK, null, currentBlock);
    }

    private boolean handlePendingBalanceCalculation(AddressUtxoEvent addressUtxoEvent) {
        var lastSyncBlockConfig = accountConfigService.getConfig(ConfigIds.LAST_ACCOUNT_BALANCE_PROCESSED_BLOCK);
        var lastBalanceSyncBlock = lastSyncBlockConfig.map(AccountConfigEntity::getBlock).orElse(null);

        if (lastBalanceSyncBlock != null && lastBalanceSyncBlock >= addressUtxoEvent.getEventMetadata().getBlock() - 1) {
            if (log.isDebugEnabled())
                log.debug("Balance calculation is already in sync with the block {}.", lastBalanceSyncBlock);
            return true;
        }

        var aggrJobConfig = accountConfigService.getConfig(ConfigIds.ACCOUNT_BALANCE_AGGR_JOB_ID);
        var aggrJobStatus = aggrJobConfig.map(AccountConfigEntity::getStatus).orElse(null);

        if (aggrJobStatus == ConfigStatus.BATCH_AGGR_IN_PROGRESS) {
            log.info("Aggregation job is in progress.");

            if (lastBalanceSyncBlock == null)
                return false;

            //Let's check if the main sync process can handle the balance calculation for remaining blocks
            long blockDiff = addressUtxoEvent.getEventMetadata().getBlock() - lastBalanceSyncBlock;
            if (lastBalanceSyncBlock != null &&
                    blockDiff < 2 * accountStoreProperties.getBatchBalanceAggregationSafeBlockDiff()) {
                accountConfigService
                        .upateConfig(ConfigIds.ACCOUNT_BALANCE_AGGR_JOB_ID, ConfigStatus.BATCH_AGGR_REQUEST_TO_STOP,
                                addressUtxoEvent.getEventMetadata().getBlock());
                log.info("Main sync process is requesting to stop the aggregation job.");
            }
            return false;
        } else if (aggrJobStatus == ConfigStatus.BATCH_AGGR_STOPPED) {
            log.info("Aggregation job has been stopped. " +
                    "So let's do remaining balance calculation in main sync.");
            long currentBlockMinus20 = addressUtxoEvent.getEventMetadata().getBlock() - 10;
            //As schedule job has probably stopped
            accountBalanceBatchProcessingService.runBalanceCalculationBatch(currentBlockMinus20, 600);
            accountBalanceBatchProcessingService.runBalanceCalculationBatch(addressUtxoEvent.getEventMetadata().getBlock() - 1, 1);

            accountConfigService.upateConfig(ConfigIds.ACCOUNT_BALANCE_SYNC_JOB_ID, ConfigStatus.IN_SYNC,
                    addressUtxoEvent.getEventMetadata().getBlock());
            return true;
        } else {
            if (log.isDebugEnabled())
                log.debug("Skipping balance calculation in main sync as aggregation job is not stopped yet.");
            return false;
        }

    }

    private List<AddressBalance> handleAddressBalance(AddressUtxoEvent addressUtxoEvent) {
        EventMetadata metadata = addressUtxoEvent.getEventMetadata();
        Map<String, AddressBalance> addressBalanceMap = new HashMap<>();
        for (TxInputOutput txInputOutput : addressUtxoEvent.getTxInputOutputs()) {
            List<AddressUtxo> inputs = txInputOutput.getInputs();
            List<AddressUtxo> outputs = txInputOutput.getOutputs();

            List<UtxoKey> inputUtxoKeys = inputs.stream()
                    .map(input -> new UtxoKey(input.getTxHash(), input.getOutputIndex()))
                    .collect(Collectors.toList());

            inputs = utxoClient.getUtxosByIds(inputUtxoKeys);
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
        if (!accountStoreProperties.isBalanceAggregationEnabled())
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

                    AddressBalance addressBalance = AddressBalance.builder()
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
