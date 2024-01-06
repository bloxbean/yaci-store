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
import com.google.common.collect.ArrayListMultimap;
import jakarta.annotation.PostConstruct;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
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
                            .sorted(Comparator.comparingLong(blockAddressAmount -> blockAddressAmount.getEventMetadata().getBlock()))
                            .toList();

            //Create AddressBalance, StakeAddressBalance and store
            long t1 = System.currentTimeMillis();
            CompletableFuture<Void> addressBalFuture = CompletableFuture.supplyAsync(() -> getAddressBalances(firstBlockInBatchMetadata, blocksBalanceList))
                    .thenAcceptAsync(addressBalances -> {
                        if (addressBalances != null && addressBalances.size() > 0) {
                            long t2 = System.currentTimeMillis();
                            accountBalanceStorage.saveAddressBalances(addressBalances);
                            long t3 = System.currentTimeMillis();
                            log.info("Total Address Balance records {}, Time taken to save: {}", addressBalances.size(), (t3 - t2));
                        }

                        if (addressBalances != null && addressBalances.size() > 0) {
                            List<String> addressList =
                                    addressBalances.stream().map(addressBalance -> addressBalance.getAddress()).distinct().toList();
                            accountBalanceCleanupHelper.deleteAddressBalanceBeforeConfirmedSlot(addressList, firstBlockInBatchMetadata.getSlot());
                        }
                    }, parallelExecutor.getVirtualThreadExecutor());


            CompletableFuture<Void> stakeAddrBalFuture = null;
            if (accountStoreProperties.isStakeAddressBalanceEnabled()) {
                stakeAddrBalFuture = CompletableFuture.supplyAsync(() -> getStakeAddressBalances(firstBlockInBatchMetadata, blocksBalanceList))
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

            try {
                addressBalFuture.get();
                if (stakeAddrBalFuture != null)
                    stakeAddrBalFuture.get();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            } catch (ExecutionException e) {
                throw new RuntimeException(e);
            }

            log.info("Total balance processing and saving time {}", (System.currentTimeMillis() - t1));
            accountConfigService.upateConfig(ConfigIds.LAST_ACCOUNT_BALANCE_PROCESSED_BLOCK, null, event.getMetadata().getBlock(),
                    event.getMetadata().getBlockHash(), event.getMetadata().getSlot());

        } finally {
            addressUtxoEventsMap.clear();
        }
    }

    public BlockAddressAmount getAddressAmountMapForBlock(EventMetadata metadata, List<AddressUtxo> inputs, List<AddressUtxo> outputs) {
        Map<AddressInfo, Map<UnitInfo, SlotAmount>> addressBalanceMap = new HashMap<>();
        Map<StakeAddressInfo, SlotAmount> stakeAddrBalanceMap = new HashMap<>();
        //Update inputs
        for (AddressUtxo input : inputs) {
            if (input.getAmounts() == null) {
                log.error("Input amounts are null for tx: " + input.getTxHash());
                log.error("Input: " + input);
            }

            for (Amt amount : input.getAmounts()) {
                //Addr
                var addressInfoKey = getAddressInfoKey(input);
                var unitInfoKey = getUnitInfoKey(amount);
                if (addressBalanceMap.get(addressInfoKey) != null) {
                    var slotAmountsMap = addressBalanceMap.get(addressInfoKey);
                    var unitSlotAmount = slotAmountsMap.get(unitInfoKey);
                    if (unitSlotAmount != null) {
                        unitSlotAmount.setQuantity(unitSlotAmount.getQuantity().subtract(amount.getQuantity()));
                    } else {
                        var slotAmount = SlotAmount.builder()
                                .quantity(BigInteger.ZERO.subtract(amount.getQuantity()))
                                .eventMetadata(metadata)
                                .build();
                        slotAmountsMap.put(unitInfoKey, slotAmount);
                    }
                } else {
                    var slotAmount = SlotAmount.builder()
                            .quantity(BigInteger.ZERO.subtract(amount.getQuantity()))
                            .eventMetadata(metadata)
                            .build();
                    var slotAmountsMap = new HashMap<UnitInfo, SlotAmount>();
                    slotAmountsMap.put(unitInfoKey, slotAmount);
                    addressBalanceMap.put(addressInfoKey, slotAmountsMap);
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
                var addressInfoKey = getAddressInfoKey(output);
                var unitInfoKey = getUnitInfoKey(amount);
                if (addressBalanceMap.get(addressInfoKey) != null) {
                    var slotAmountsMap = addressBalanceMap.get(addressInfoKey);
                    var unitSlotAmount = slotAmountsMap.get(unitInfoKey);

                    if (unitSlotAmount != null) {
                        unitSlotAmount.setQuantity(unitSlotAmount.getQuantity().add(amount.getQuantity()));
                    } else {
                        var slotAmount = SlotAmount.builder()
                                .quantity(amount.getQuantity())
                                .eventMetadata(metadata)
                                .build();
                        slotAmountsMap.put(unitInfoKey, slotAmount);
                    }
                } else {
                    var amt = SlotAmount.builder()
                            .quantity(amount.getQuantity())
                            .eventMetadata(metadata)
                            .build();
                    var slotAmountsMap = new HashMap<UnitInfo, SlotAmount>();
                    slotAmountsMap.put(unitInfoKey, amt);
                    addressBalanceMap.put(addressInfoKey, slotAmountsMap);
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

        return new BlockAddressAmount(metadata, addressBalanceMap, stakeAddrBalanceMap);
    }

    public List<AddressBalance> getAddressBalances(EventMetadata firstBlockMetadata, List<BlockAddressAmount> blockAddressAmounts) {
        ArrayListMultimap<String, AddressBalance> addressBalanceMap = ArrayListMultimap.create();
        for (var blockAddressAmount : blockAddressAmounts) {
            var blockAddressAmountMap = blockAddressAmount.getAddressAmountMap();
            calculateAddressBalancesForBlock(firstBlockMetadata, addressBalanceMap, blockAddressAmountMap);
        }

        return addressBalanceMap.values().stream().toList();
    }

    private void calculateAddressBalancesForBlock(EventMetadata firstBlockMetadata, ArrayListMultimap<String, AddressBalance> addressBalanceMap,
                                                  Map<AddressInfo, Map<UnitInfo, SlotAmount>> addressAmtMap) {
        addressAmtMap.entrySet()
                .forEach(entry -> {
                    var addressInfo = entry.getKey();
                    var unitSlotAmtsMap = entry.getValue();

                    var addressBalancesPerBlock = addressBalanceMap.get(addressInfo.getAddress());

                    AddressBalance lastAddressBalance = null;
                    if (addressBalancesPerBlock == null || addressBalancesPerBlock.size() == 0) {
                        lastAddressBalance = accountBalanceStorage.getAddressBalance(addressInfo.getAddress(), firstBlockMetadata.getSlot() - 1)
                                .orElse(null);
                    } else {
                        if (nAddrBalanceRecordToKeep > 0 && addressBalancesPerBlock.size() >= nAddrBalanceRecordToKeep) {
                            for (int i = 0; i < addressBalancesPerBlock.size() - nAddrBalanceRecordToKeep; i++) {
                                addressBalancesPerBlock.remove(i);
                            }
                        }

                        lastAddressBalance = addressBalancesPerBlock.get(addressBalancesPerBlock.size() - 1);
                    }

                    //Create unit --> balance
                    Map<String, Amt> lastUnitQtyMap = new HashMap<>();
                    if (lastAddressBalance != null) {
                        lastAddressBalance.getAmounts()
                                .stream()
                                .map(amt -> {
                                    var amtCopy = Amt.builder()
                                            .unit(amt.getUnit())
                                            .policyId(amt.getPolicyId())
                                            .assetName(amt.getAssetName())
                                            .quantity(amt.getQuantity())
                                            .build();
                                    lastUnitQtyMap.put(amt.getUnit(), amtCopy);
                                    return amt;
                                }).collect(Collectors.toList());
                    }


                    //Iterate through each unit and calculate the balance
                    for (var unitAmtEntry : unitSlotAmtsMap.entrySet()) {
                        var newUnitAmt = unitAmtEntry.getValue();
                        var lastUnitAmt = lastUnitQtyMap.get(unitAmtEntry.getKey().getUnit());

                        if (lastUnitAmt == null) {
                            lastUnitAmt = Amt.builder()
                                    .unit(unitAmtEntry.getKey().getUnit())
                                    .quantity(BigInteger.ZERO)
                                    .policyId(unitAmtEntry.getKey().getPolicyId())
                                    .assetName(unitAmtEntry.getKey().getAssetName())
                                    .build();
                            lastUnitQtyMap.put(unitAmtEntry.getKey().getUnit(), lastUnitAmt);
                        }

                        //Update amt
                        lastUnitAmt.setQuantity(lastUnitAmt.getQuantity().add(newUnitAmt.getQuantity()));

                        if (lastUnitAmt.getQuantity().compareTo(BigInteger.ZERO) < 0) {
                            log.error("[Inputs] Negative balance for address: " + addressInfo.getAddress() + " : " + lastUnitAmt.getQuantity());
                            log.error("Unit: " + lastUnitAmt.getUnit());
                            log.error("Existing AddressBalance >> " + lastAddressBalance);
                            throw new IllegalStateException("Error in address balance calculation");
                        }
                    }

                    EventMetadata blockEventMetadata = unitSlotAmtsMap.entrySet().iterator().next().getValue().getEventMetadata();
                    var finalAmtList = lastUnitQtyMap.entrySet()
                            .stream().map(unitAmtEntry -> unitAmtEntry.getValue())
                            .filter(amt -> amt.getQuantity().compareTo(BigInteger.ZERO) > 0)
                            .toList();

                    //Create AddressBalance
                    var newAddressBalance = AddressBalance.builder()
                            .address(addressInfo.getAddress())
                            .slot(blockEventMetadata.getSlot())
                            .blockNumber(blockEventMetadata.getBlock())
                            .blockHash(blockEventMetadata.getBlockHash())
                            .blockTime(blockEventMetadata.getBlockTime())
                            .epoch(blockEventMetadata.getEpochNumber())
                            .paymentCredential(addressInfo.getPaymentCredential())
                            .stakeAddress(addressInfo.getStakeAddress())
                            .amounts(finalAmtList)
                            .build();

                    addressBalanceMap.put(addressInfo.getAddress(), newAddressBalance);

                });
    }

    public List<StakeAddressBalance> getStakeAddressBalances(EventMetadata firstBlockMetadata, List<BlockAddressAmount> blockAddressAmounts) {
        ArrayListMultimap<String, StakeAddressBalance> stakeAddressBalanceMap = ArrayListMultimap.create();
        for (var blockAddressAmount : blockAddressAmounts) {
            var blockStakeAddressAmountMap = blockAddressAmount.getStakeAddressAmountMap();
            calculateStakeAddressBalancesForBlock(firstBlockMetadata, stakeAddressBalanceMap, blockStakeAddressAmountMap);
        }

        return stakeAddressBalanceMap.values().stream().toList();
    }

    private void calculateStakeAddressBalancesForBlock(EventMetadata firstBlockMetadata, ArrayListMultimap<String, StakeAddressBalance> stakeAddressBalanceMap,
                                                       Map<StakeAddressInfo, SlotAmount> stakeAddrAmtMap) {
        stakeAddrAmtMap.entrySet()
//                .parallelStream()
                .forEach(entry -> {
                    var stakeAddrInfoKey = entry.getKey();
                    var slotAmount = entry.getValue();

                    var stakeAddressBalancePerBlocks = stakeAddressBalanceMap.get(stakeAddrInfoKey.getAddress());

                    StakeAddressBalance lastStakeAddressBalance = null;
                    if (stakeAddressBalancePerBlocks == null || stakeAddressBalancePerBlocks.size() == 0) {
                        lastStakeAddressBalance = accountBalanceStorage.getStakeAddressBalance(stakeAddrInfoKey.getAddress(), firstBlockMetadata.getSlot() - 1)
                                .orElse(null);
                    } else {
                        if (nAddrBalanceRecordToKeep > 0 && stakeAddressBalancePerBlocks.size() >= nAddrBalanceRecordToKeep) {
                            for (int i = 0; i < stakeAddressBalancePerBlocks.size() - nAddrBalanceRecordToKeep; i++) {
                                stakeAddressBalancePerBlocks.remove(i);
                            }
                        }

                        lastStakeAddressBalance = stakeAddressBalancePerBlocks.get(stakeAddressBalancePerBlocks.size() - 1);
                    }

                    BigInteger quantity = lastStakeAddressBalance != null ? lastStakeAddressBalance.getQuantity() : BigInteger.ZERO;

                    quantity = quantity.add(slotAmount.getQuantity());

                    StakeAddressBalance newStakeAddrBalance = StakeAddressBalance.builder()
                            .address(stakeAddrInfoKey.getAddress())
                            .stakeCredential(stakeAddrInfoKey.getStakeCredential())
                            .slot(slotAmount.getEventMetadata().getSlot())
                            .blockNumber(slotAmount.getEventMetadata().getBlock())
                            .blockHash(slotAmount.getEventMetadata().getBlockHash())
                            .blockTime(slotAmount.getEventMetadata().getBlockTime())
                            .epoch(slotAmount.getEventMetadata().getEpochNumber())
                            .quantity(quantity)
                            .build();

                    stakeAddressBalanceMap.put(stakeAddrInfoKey.getAddress(), newStakeAddrBalance);

                    if (newStakeAddrBalance.getQuantity().compareTo(BigInteger.ZERO) < 0) {
                        log.error("[Inputs] Negative balance for stakeAddress: " + stakeAddrInfoKey.getAddress() + " : " + newStakeAddrBalance.getQuantity());
                        log.info("SlotAmount >> " + slotAmount);
                        log.error("Existing StakeAddressBalance >> " + lastStakeAddressBalance);
                        throw new IllegalStateException("Error in stake address balance calculation");
                    }

                });
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
                            .amounts(List.of(Amt.builder()
                                    .unit(LOVELACE)
                                    .assetName(LOVELACE)
                                    .quantity(genesisBalance.getBalance())
                                    .build()))
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

    private AddressInfo getAddressInfoKey(AddressUtxo input) {
        return AddressInfo.builder()
                .address(input.getOwnerAddr())
                .paymentCredential(input.getOwnerPaymentCredential())
                .stakeAddress(input.getOwnerStakeAddr())
                .build();
    }

    private UnitInfo getUnitInfoKey(Amt amount) {
        return UnitInfo.builder()
                .unit(amount.getUnit())
                .policyId(amount.getPolicyId())
                .assetName(amount.getAssetName())
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
    static class AddressInfo {
        private String address;
        private String stakeAddress;
        private String paymentCredential;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            AddressInfo that = (AddressInfo) o;
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
    static class UnitInfo {
        private String unit;
        private String policyId;
        private String assetName;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            UnitInfo unitInfo = (UnitInfo) o;
            return Objects.equals(unit, unitInfo.unit);
        }

        @Override
        public int hashCode() {
            return Objects.hash(unit);
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
        private EventMetadata eventMetadata;
        private Map<AddressInfo, Map<UnitInfo, SlotAmount>> addressAmountMap;
        private Map<StakeAddressInfo, SlotAmount> stakeAddressAmountMap;
    }

}
