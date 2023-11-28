package com.bloxbean.cardano.yaci.store.account.processor;

import com.bloxbean.cardano.client.address.Address;
import com.bloxbean.cardano.client.address.AddressProvider;
import com.bloxbean.cardano.yaci.core.util.HexUtil;
import com.bloxbean.cardano.yaci.store.account.AccountStoreProperties;
import com.bloxbean.cardano.yaci.store.account.domain.AddressBalance;
import com.bloxbean.cardano.yaci.store.account.domain.StakeAddressBalance;
import com.bloxbean.cardano.yaci.store.account.service.AccountConfigService;
import com.bloxbean.cardano.yaci.store.account.storage.AccountBalanceStorage;
import com.bloxbean.cardano.yaci.store.account.util.ConfigIds;
import com.bloxbean.cardano.yaci.store.client.utxo.UtxoClient;
import com.bloxbean.cardano.yaci.store.common.domain.AddressUtxo;
import com.bloxbean.cardano.yaci.store.common.domain.Amt;
import com.bloxbean.cardano.yaci.store.common.domain.UtxoKey;
import com.bloxbean.cardano.yaci.store.common.executor.ParallelExecutor;
import com.bloxbean.cardano.yaci.store.events.EventMetadata;
import com.bloxbean.cardano.yaci.store.events.GenesisBalance;
import com.bloxbean.cardano.yaci.store.events.GenesisBlockEvent;
import com.bloxbean.cardano.yaci.store.events.internal.ReadyForBalanceAggregationEvent;
import com.bloxbean.cardano.yaci.store.utxo.domain.AddressUtxoEvent;
import jakarta.annotation.PostConstruct;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigInteger;
import java.util.*;
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
    private final ParallelExecutor parallelExecutor;

    private int nAddrBalanceRecordToKeep = 3;

    private Map<Long, AddressUtxoEvent> addressUtxoEventsMap = Collections.synchronizedMap(new HashMap<>());

    private final PlatformTransactionManager transactionManager;
    private TransactionTemplate transactionTemplate;

    @PostConstruct
    void init() {
        transactionTemplate = new TransactionTemplate(transactionManager);
        transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

    }

    @EventListener
    @Transactional
    @SneakyThrows
    public void handleAddressUtxoEvent(AddressUtxoEvent addressUtxoEvent) {
        addressUtxoEventsMap.put(addressUtxoEvent.getEventMetadata().getBlock(), addressUtxoEvent);
    }

    @EventListener
    @Transactional
    public void handlePostProcessingEvent(ReadyForBalanceAggregationEvent event) {

        try {
            Collection<AddressUtxoEvent> addressUtxoEvents = addressUtxoEventsMap.values();

            var accountConfigOpt = accountConfigService.getConfig(ConfigIds.LAST_ACCOUNT_BALANCE_PROCESSED_BLOCK);
            Long lastProcessedBlock = accountConfigOpt.map(accountConfigEntity -> accountConfigEntity.getBlock())
                    .orElse(null);

            List<AddressUtxoEvent> sortedAddressEventUtxo = addressUtxoEvents.stream()
                    .sorted(Comparator.comparingLong(addUtxoEvent -> addUtxoEvent.getEventMetadata().getBlock()))
                    .collect(Collectors.toList());

            //Create final address balance records for saving
            //Required to get balance before the slot mention in the metadata
            EventMetadata firstBlockInBatchMetadata = sortedAddressEventUtxo.get(0).getEventMetadata();

            if (lastProcessedBlock != null
                    && !((firstBlockInBatchMetadata.getBlock() - lastProcessedBlock) <= 1)) { // 1 block diff or same block
                log.warn("Account balance calculation will be ignored. " +
                        "Please run the aggregation app to calculate the account balance for pending blocks.");
                log.warn("The last processed block for account balance calculation is not the same as the expected last block.");
                log.warn("Last processed block for account balance: {}", lastProcessedBlock);
                log.warn("Current block: {}", sortedAddressEventUtxo.get(0).getEventMetadata().getBlock());
                return;
            }

            //Go through each block and return Address --> SlotAmount map for each block and add to List
            long t0 = System.currentTimeMillis();
            List<BlockAddressAmount> blocksBalanceList =
                    sortedAddressEventUtxo.stream()
                            .parallel()
                            .map(addressUtxoEvent -> {
                                var inputKeys = addressUtxoEvent.getTxInputOutputs()
                                        .stream()
                                        .flatMap(txInputOutput -> txInputOutput.getInputs().stream())
                                        .map(txInput -> new UtxoKey(txInput.getTxHash(), txInput.getOutputIndex()))
                                        .toList();

                                List<AddressUtxo> inputAddressUtxos = utxoClient.getUtxosByIds(inputKeys);
                                if (inputAddressUtxos.size() != inputKeys.size())
                                    throw new IllegalStateException("Unable to get inputs for all input keys for account balance calculation : " + inputKeys);

                                List<AddressUtxo> outputAddressUtxos = addressUtxoEvent.getTxInputOutputs()
                                        .stream()
                                        .flatMap(txInputOutput -> txInputOutput.getOutputs().stream())
                                        .toList();

                                return getAddressAmountMapForBlock(addressUtxoEvent.getEventMetadata(), inputAddressUtxos, outputAddressUtxos);
                            })
                            .filter(Objects::nonNull)
                            .toList();

            //Merge all address amounts in the block
            var batchAddressAmts = mergeAddressAmountsForAllBlocks(blocksBalanceList);

            var addressAmtMap = batchAddressAmts.getFirst();
            var stakeAddrAmtMap = batchAddressAmts.getSecond();

            if (log.isDebugEnabled()) {
                log.debug("Total no of addresses : " + addressAmtMap.size());
                log.debug("Total no of stakeAddresses : " + stakeAddrAmtMap.size());
                log.debug("Total time to first process : " + (System.currentTimeMillis() - t0));
            }


            //Create AddressBalance, StakeAddressBalance and store
            long t1 = System.currentTimeMillis();
            CompletableFuture<Void> addressBalFuture = CompletableFuture.supplyAsync(() -> getAddressBalances(firstBlockInBatchMetadata, addressAmtMap))
                    .thenAcceptAsync(addressBalances -> {
                        long t2 = System.currentTimeMillis();
                        accountBalanceStorage.saveAddressBalances(addressBalances);
                        long t3 = System.currentTimeMillis();
                        log.info("Total Address Balance records {}, Time taken to save: {}", addressBalances.size(), (t3 - t2));

                        if (addressBalances != null && addressBalances.size() > 0) {
                            List<Pair<String, String>> addresseUnitList =
                                    addressBalances.stream().map(addressBalance -> Pair.of(addressBalance.getAddress(), addressBalance.getUnit())).distinct().toList();
                            accountBalanceCleanupHelper.deleteAddressBalanceBeforeConfirmedSlot(addresseUnitList, firstBlockInBatchMetadata.getSlot());
                        }
                    }, parallelExecutor.getVirtualThreadExecutor());


            if (accountStoreProperties.isStakeAddressBalanceEnabled()) {
                CompletableFuture<Void> stakeAddrBalFuture = CompletableFuture.supplyAsync(() -> getStakeAddressBalances(firstBlockInBatchMetadata, stakeAddrAmtMap))
                        .thenAcceptAsync(stakeAddressBalances -> {
                            long t2 = System.currentTimeMillis();
                            accountBalanceStorage.saveStakeAddressBalances(stakeAddressBalances);
                            long t3 = System.currentTimeMillis();
                            log.info("Total Stake Address Balance records {}, Time taken to save: {}", stakeAddressBalances.size(), (t3 - t2));

                            if (stakeAddressBalances != null && stakeAddressBalances.size() > 0) {
                                List<String> stakeAddresses =
                                        stakeAddressBalances.stream().map(stakeAddrBalance -> stakeAddrBalance.getAddress()).distinct().toList();
                                accountBalanceCleanupHelper.deleteStakeBalanceBeforeConfirmedSlot(stakeAddresses, firstBlockInBatchMetadata.getSlot());
                            }
                        }, parallelExecutor.getVirtualThreadExecutor());

                CompletableFuture.allOf(addressBalFuture, stakeAddrBalFuture).join();
            } else {
                addressBalFuture.join();
            }

            log.info("Total balance processing and saving time {}", (System.currentTimeMillis() - t1));
            accountConfigService.upateConfig(ConfigIds.LAST_ACCOUNT_BALANCE_PROCESSED_BLOCK, null, event.getMetadata().getBlock(),
                    event.getMetadata().getBlockHash(), event.getMetadata().getSlot());

        } finally {
            addressUtxoEventsMap.clear();
        }
    }

    public BlockAddressAmount getAddressAmountMapForBlock(EventMetadata metadata, List<AddressUtxo> inputs, List<AddressUtxo> outputs) {
        Map<AddressUnitInfo, SlotAmount> addressBalanceMap = new HashMap<>();
        Map<StakeAddressInfo, SlotAmount> stakeAddrBalanceMap = new HashMap<>();
        //Update inputs
        for (AddressUtxo input : inputs) {
            if (input.getAmounts() == null) {
                log.error("Input amounts are null for tx: " + input.getTxHash());
                log.error("Input: " + input);
            }

            for (Amt amount : input.getAmounts()) {
                //Addr
                var addrBalKey = getAddrBalKey(input, amount);
                if (addressBalanceMap.get(addrBalKey) != null) {
                    SlotAmount slotAmount = addressBalanceMap.get(addrBalKey);
                    slotAmount.setQuantity(slotAmount.getQuantity().subtract(amount.getQuantity()));
                } else {
                    var slotAmount = SlotAmount.builder()
                            .quantity(BigInteger.ZERO.subtract(amount.getQuantity()))
                            .eventMetadata(metadata)
                            .build();
                    addressBalanceMap.put(addrBalKey, slotAmount);
                }

                //Stake Addr
                if (amount.getUnit().equals(LOVELACE) && input.getOwnerStakeAddr() != null) {
                    var stakeBalKey = getStakeBalKey(input);
                    if (stakeAddrBalanceMap.get(stakeBalKey) != null) {
                        SlotAmount slotAmount = stakeAddrBalanceMap.get(stakeBalKey);
                        slotAmount.setQuantity(slotAmount.getQuantity().subtract(amount.getQuantity()));
                    } else {
                        var slotAmount = SlotAmount.builder()
                                .quantity(BigInteger.ZERO.subtract(amount.getQuantity()))
                                .eventMetadata(metadata)
                                .build();
                        stakeAddrBalanceMap.put(stakeBalKey, slotAmount);
                    }
                }

            }
        }

        //Update outputs
        for (AddressUtxo output : outputs) {
            for (Amt amount : output.getAmounts()) {
                //address
                var addrBalKey = getAddrBalKey(output, amount);
                if (addressBalanceMap.get(addrBalKey) != null) {
                    SlotAmount slotAmount = addressBalanceMap.get(addrBalKey);
                    slotAmount.setQuantity(slotAmount.getQuantity().add(amount.getQuantity()));
                } else {
                    var amt = SlotAmount.builder()
                            .quantity(amount.getQuantity())
                            .eventMetadata(metadata)
                            .build();
                    addressBalanceMap.put(addrBalKey, amt);
                }

                //stakeAddress
                if (amount.getUnit().equals(LOVELACE) && output.getOwnerStakeAddr() != null) {
                    var stakeAddrBalKey = getStakeBalKey(output);
                    if (stakeAddrBalanceMap.get(stakeAddrBalKey) != null) {
                        SlotAmount slotAmount = stakeAddrBalanceMap.get(stakeAddrBalKey);
                        slotAmount.setQuantity(slotAmount.getQuantity().add(amount.getQuantity()));
                    } else {
                        var amt = SlotAmount.builder()
                                .quantity(amount.getQuantity())
                                .eventMetadata(metadata)
                                .build();
                        stakeAddrBalanceMap.put(stakeAddrBalKey, amt);
                    }
                }
            }
        }

        return new BlockAddressAmount(addressBalanceMap, stakeAddrBalanceMap);
    }

    public Pair<Map<AddressUnitInfo, List<SlotAmount>>, Map<StakeAddressInfo, List<SlotAmount>>> mergeAddressAmountsForAllBlocks(List<BlockAddressAmount> blockAddressAmountList) {
        Map<AddressUnitInfo, List<SlotAmount>> addressAmtMap = Collections.synchronizedMap(new HashMap<>());
        Map<StakeAddressInfo, List<SlotAmount>> stakeAddrAmtMap = Collections.synchronizedMap(new HashMap<>());
        //Create Address Amount Map / Stake Addr Amount Map with all amounts (for all blocks in the batch)
        for (var blockAddressAmount : blockAddressAmountList) {
            var blockAddressBalanceMap = blockAddressAmount.getAddressAmountMap();
            var blockStakeAddrBalanceMap = blockAddressAmount.getStakeAddressAmountMap();

            //For address Balance
            blockAddressBalanceMap.entrySet()
                    .stream()
                    .forEach(entry -> {
                        var amtList = addressAmtMap.get(entry.getKey());
                        if (amtList == null) {
                            synchronized (this) {
                                if (addressAmtMap.get(entry.getKey()) == null) {
                                    addressAmtMap.put(entry.getKey(), Collections.synchronizedList(new LinkedList<>()));
                                }
                            }
                            amtList = addressAmtMap.get(entry.getKey());
                        }

                        amtList.add(entry.getValue());
                    });

            //For Stake address Balance
            blockStakeAddrBalanceMap.entrySet()
                    .stream()
                    .forEach(entry -> {
                        var amtList = stakeAddrAmtMap.get(entry.getKey());
                        if (amtList == null) {
                            synchronized (this) {
                                if (stakeAddrAmtMap.get(entry.getKey()) == null) {
                                    stakeAddrAmtMap.put(entry.getKey(), Collections.synchronizedList(new LinkedList<>()));
                                }
                            }
                            amtList = stakeAddrAmtMap.get(entry.getKey());
                        }

                        amtList.add(entry.getValue());
                    });
        }

        return Pair.of(addressAmtMap, stakeAddrAmtMap);

    }

    public List<AddressBalance> getAddressBalances(EventMetadata firstBlockMetadata, Map<AddressUnitInfo, List<SlotAmount>> addressAmtMap) {
        var addressBalances = addressAmtMap.entrySet()
                .parallelStream()
                .map(entry -> {
                    var key = entry.getKey();

                    List<SlotAmount> slotAmountValues = entry.getValue();

                    //Sort slot values from low to high
                    List<SlotAmount> slotAmounts = null;
                    if (slotAmountValues.size() > 1)
                        slotAmounts = slotAmountValues.stream().sorted(Comparator.comparingLong(value -> value.getEventMetadata().getBlock())).toList();
                    else
                        slotAmounts = slotAmountValues;

                    var savedAddressBalance = accountBalanceStorage.getAddressBalance(key.getAddress(), key.getUnit(), firstBlockMetadata.getSlot() - 1);
                    List<AddressBalance> addressSlotBalances = new ArrayList<>();

                    BigInteger quantity = savedAddressBalance.map(addressBalance -> addressBalance.getQuantity())
                            .orElse(BigInteger.ZERO);

                    for (SlotAmount slotAmount : slotAmounts) {
                        AddressBalance newAddressBalance = null;
                        quantity = quantity.add(slotAmount.getQuantity());

                        newAddressBalance = AddressBalance.builder()
                                .address(key.getAddress())
                                .slot(slotAmount.getEventMetadata().getSlot())
                                .blockNumber(slotAmount.getEventMetadata().getBlock())
                                .blockHash(slotAmount.getEventMetadata().getBlockHash())
                                .blockTime(slotAmount.getEventMetadata().getBlockTime())
                                .epoch(slotAmount.getEventMetadata().getEpochNumber())
                                .paymentCredential(key.getPaymentCredential())
                                .stakeAddress(key.getStakeAddress())
                                .unit(key.getUnit())
                                .policy(key.getPolicyId())
                                .assetName(key.getAssetName())
                                .quantity(quantity)
                                .build();

                        addressSlotBalances.add(newAddressBalance);

                        if (newAddressBalance.getQuantity().compareTo(BigInteger.ZERO) < 0) {
                            log.error("[Inputs] Negative balance for address: " + key.getAddress() + " : " + newAddressBalance.getQuantity());
                            if (savedAddressBalance.isPresent()) {
                                log.info("Previous amount : " + savedAddressBalance.get().getQuantity());
                                log.info("SlotAmounts >> " + slotAmounts);
                                //log.info("Amount to add / deduct : " + totalQuantity);
                                log.info("Unit: " + savedAddressBalance.get().getUnit());
                            }
                            log.error("Existing AddressBalance >> " + savedAddressBalance);
                            throw new IllegalStateException("Error in address balance calculation");
                        }
                    }

                    if (nAddrBalanceRecordToKeep > 0 && addressSlotBalances.size() >= nAddrBalanceRecordToKeep) {
                        return addressSlotBalances.subList(addressSlotBalances.size() - nAddrBalanceRecordToKeep, addressSlotBalances.size());
                    } else {
                        return addressSlotBalances;
                    }
                }).flatMap(addressBalances1 -> addressBalances1.stream())
                .toList();
        return addressBalances;
    }

    public List<StakeAddressBalance> getStakeAddressBalances(EventMetadata firstBlockMetadata, Map<StakeAddressInfo, List<SlotAmount>> stakeAddrAmtMap) {
        var stakeAddrBalances = stakeAddrAmtMap.entrySet()
                .parallelStream()
                .map(entry -> {
                    var stakeAddrInfoKey = entry.getKey();
                    var slotAmounts = entry.getValue();

                    var savedStakeAddressBalance = accountBalanceStorage.getStakeAddressBalance(stakeAddrInfoKey.getAddress(), firstBlockMetadata.getSlot() - 1);
                    List<StakeAddressBalance> stakeAddressBalances = new ArrayList<>();

                    BigInteger quantity = savedStakeAddressBalance.map(stakeAddrBalance -> stakeAddrBalance.getQuantity())
                            .orElse(BigInteger.ZERO);

                    for (SlotAmount slotAmount : slotAmounts) {
                        StakeAddressBalance newStakeAddrBalance = null;
                        quantity = quantity.add(slotAmount.getQuantity());

                        newStakeAddrBalance = StakeAddressBalance.builder()
                                .address(stakeAddrInfoKey.getAddress())
                                .stakeCredential(stakeAddrInfoKey.getStakeCredential())
                                .slot(slotAmount.getEventMetadata().getSlot())
                                .blockNumber(slotAmount.getEventMetadata().getBlock())
                                .blockHash(slotAmount.getEventMetadata().getBlockHash())
                                .blockTime(slotAmount.getEventMetadata().getBlockTime())
                                .epoch(slotAmount.getEventMetadata().getEpochNumber())
                                .quantity(quantity)
                                .build();

                        stakeAddressBalances.add(newStakeAddrBalance);

                        if (newStakeAddrBalance.getQuantity().compareTo(BigInteger.ZERO) < 0) {
                            log.error("[Inputs] Negative balance for stakeAddress: " + stakeAddrInfoKey.getAddress() + " : " + newStakeAddrBalance.getQuantity());
                            log.info("SlotAmounts >> " + slotAmounts);
                            log.error("Existing StakeAddressBalance >> " + savedStakeAddressBalance);
                            throw new IllegalStateException("Error in stake address balance calculation");
                        }
                    }

                    if (nAddrBalanceRecordToKeep > 0 && stakeAddressBalances.size() >= nAddrBalanceRecordToKeep) {
                        return stakeAddressBalances.subList(stakeAddressBalances.size() - nAddrBalanceRecordToKeep, stakeAddressBalances.size());
                    } else {
                        return stakeAddressBalances;
                    }

                }).flatMap(stakeAddrBalance -> stakeAddrBalance.stream())
                .toList();

        return stakeAddrBalances;
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
                            .quantity(genesisBalance.getBalance())
                            .stakeCredential(HexUtil.encodeHexString(stakeAddress.getDelegationCredential().get().getBytes()))
                            .build();
                    return stakeAddrBalance;
                }).toList();
        accountBalanceStorage.saveStakeAddressBalances(stakeAddrBalances);
    }

    private AddressUnitInfo getAddrBalKey(AddressUtxo input, Amt amount) {
        return AddressUnitInfo.builder()
                .address(input.getOwnerAddr())
                .paymentCredential(input.getOwnerPaymentCredential())
                .unit(amount.getUnit())
                .policyId(amount.getPolicyId())
                .assetName(amount.getAssetName())
                .stakeAddress(input.getOwnerStakeAddr())
                .build();
    }

    private StakeAddressInfo getStakeBalKey(AddressUtxo input) {
        return StakeAddressInfo.builder()
                .address(input.getOwnerStakeAddr())
                .stakeCredential(input.getOwnerStakeCredential())
                .build();
    }


    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    static class AddressUnitInfo {
        private String address;
        private String stakeAddress;
        private String paymentCredential;
        private String unit;
        private String policyId;
        private String assetName;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            AddressUnitInfo that = (AddressUnitInfo) o;
            return Objects.equals(address, that.address) && Objects.equals(unit, that.unit);
        }

        @Override
        public int hashCode() {
            return Objects.hash(address, unit);
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    static class StakeAddressInfo {
        private String address;
        private String stakeCredential;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            StakeAddressInfo that = (StakeAddressInfo) o;
            return Objects.equals(address, that.address);
        }

        @Override
        public int hashCode() {
            return Objects.hash(address);
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    static class SlotAmount {
        private BigInteger quantity;
        private EventMetadata eventMetadata;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    static class BlockAddressAmount {
        private Map<AddressUnitInfo, SlotAmount> addressAmountMap;
        private Map<StakeAddressInfo, SlotAmount> stakeAddressAmountMap;
    }

}
