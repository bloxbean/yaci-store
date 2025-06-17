package com.bloxbean.cardano.yaci.store.account.processor;

import com.bloxbean.cardano.yaci.store.account.AccountStoreProperties;
import com.bloxbean.cardano.yaci.store.account.domain.AddressTxAmount;
import com.bloxbean.cardano.yaci.store.account.storage.AddressTxAmountStorage;
import com.bloxbean.cardano.yaci.store.client.utxo.UtxoClient;
import com.bloxbean.cardano.yaci.store.common.aspect.EnableIf;
import com.bloxbean.cardano.yaci.store.common.domain.AddressUtxo;
import com.bloxbean.cardano.yaci.store.common.domain.Amt;
import com.bloxbean.cardano.yaci.store.common.domain.UtxoKey;
import com.bloxbean.cardano.yaci.store.events.EventMetadata;
import com.bloxbean.cardano.yaci.store.events.RollbackEvent;
import com.bloxbean.cardano.yaci.store.events.internal.PreCommitEvent;
import com.bloxbean.cardano.yaci.store.utxo.domain.AddressUtxoEvent;
import com.bloxbean.cardano.yaci.store.utxo.domain.TxInputOutput;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.event.EventListener;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigInteger;
import java.util.*;

import static com.bloxbean.cardano.yaci.core.util.Constants.LOVELACE;
import static com.bloxbean.cardano.yaci.store.account.AccountStoreConfiguration.STORE_ACCOUNT_ENABLED;
import static com.bloxbean.cardano.yaci.store.common.util.AddressUtil.getAddress;

@Component
@EnableIf(value = STORE_ACCOUNT_ENABLED, defaultValue = false)
@Slf4j
public class AddressTxAmountProcessor {
    public static final int BLOCK_ADDRESS_TX_AMT_THRESHOLD = 100; //Threshold to save address_tx_amounts records for block

    private final AddressTxAmountStorage addressTxAmountStorage;
    private final UtxoClient utxoClient;
    private final UtxoClient retryableUtxoClient;
    private final AccountStoreProperties accountStoreProperties;

    private List<Pair<EventMetadata, TxInputOutput>> pendingTxInputOutputListCache = Collections.synchronizedList(new ArrayList<>());
    private List<AddressTxAmount> addressTxAmountListCache = Collections.synchronizedList(new ArrayList<>());

    public AddressTxAmountProcessor(AddressTxAmountStorage addressTxAmountStorage,
                                    UtxoClient utxoClient,
                                    @Qualifier("retryableUtxoClient") UtxoClient retryableUtxoClient,
                                    AccountStoreProperties accountStoreProperties) {
        this.addressTxAmountStorage = addressTxAmountStorage;
        this.utxoClient = utxoClient;
        this.retryableUtxoClient = retryableUtxoClient;
        this.accountStoreProperties = accountStoreProperties;
    }

    @EventListener
    @Transactional
    public void processAddressUtxoEvent(AddressUtxoEvent addressUtxoEvent) {
        if (!accountStoreProperties.isSaveAddressTxAmount())
            return;

        //Ignore Genesis Txs as it's handled by GEnesisBlockAddressTxAmtProcessor
        if (addressUtxoEvent.getMetadata().getSlot() == -1)
            return;

        var txInputOutputList = addressUtxoEvent.getTxInputOutputs();
        if (txInputOutputList == null || txInputOutputList.isEmpty())
            return;

        List<AddressTxAmount> addressTxAmountList = new ArrayList<>();

        for (var txInputOutput : txInputOutputList) {
            var txAddressTxAmountEntities = processAddressAmountForTx(addressUtxoEvent.getMetadata(), txInputOutput, false);
            if (txAddressTxAmountEntities == null || txAddressTxAmountEntities.isEmpty()) continue;

            addressTxAmountList.addAll(txAddressTxAmountEntities);
        }

        if (addressTxAmountList.size() > BLOCK_ADDRESS_TX_AMT_THRESHOLD) {
            if (log.isDebugEnabled())
                log.debug("Saving address_tx_amounts records : {} -- {}", addressTxAmountList.size(), addressUtxoEvent.getMetadata().getBlock());
            addressTxAmountStorage.save(addressTxAmountList); //Save
        } else if (addressTxAmountList.size() > 0) {
            addressTxAmountListCache.addAll(addressTxAmountList);
        }
    }

    @SneakyThrows
    private List<AddressTxAmount> processAddressAmountForTx(EventMetadata metadata, TxInputOutput txInputOutput,
                                                            boolean throwExceptionOnFailure) {
        var txHash = txInputOutput.getTxHash();
        var inputs = txInputOutput.getInputs();
        var outputs = txInputOutput.getOutputs();
        if (inputs == null || inputs.isEmpty() || outputs == null || outputs.isEmpty())
            return null;

        var inputUtxoKeys = inputs.stream()
                .map(input -> new UtxoKey(input.getTxHash(), input.getOutputIndex()))
                .toList();

        List<AddressUtxo> inputAddressUtxos;
        if (throwExceptionOnFailure) {
            inputAddressUtxos = retryableUtxoClient.getUtxosByIds(inputUtxoKeys)
                    .stream()
                    .filter(Objects::nonNull)
                    .toList();
        } else {
            inputAddressUtxos = utxoClient.getUtxosByIds(inputUtxoKeys)
                    .stream()
                    .filter(Objects::nonNull)
                    .toList();
        }

        if (inputAddressUtxos.size() != inputUtxoKeys.size()) {
            log.debug("Unable to get inputs for all input keys for account balance calculation. Add this Tx to cache to process later : " + txHash);
            if (throwExceptionOnFailure)
                throw new IllegalStateException("Unable to get inputs for all input keys for account balance calculation : " + inputUtxoKeys);
            else
                pendingTxInputOutputListCache.add(Pair.of(metadata, txInputOutput));

            return Collections.emptyList();
        }

        var txAddressTxAmount =
                processTxAmount(txHash, metadata, inputAddressUtxos, outputs);
        return txAddressTxAmount;
    }

    private List<AddressTxAmount> processTxAmount(String txHash, EventMetadata metadata, List<AddressUtxo> inputs, List<AddressUtxo> outputs) {
        Map<Pair<String, String>, BigInteger> addressTxAmountMap = new HashMap<>();
        Map<String, AddressDetails> addressToAddressDetailsMap = new HashMap<>();
        Map<String, AssetDetails> unitToAssetDetailsMap = new HashMap<>();

        //Subtract input amounts
        for (var input : inputs) {
            for (Amt amt : input.getAmounts()) {
                var key = Pair.of(input.getOwnerAddr(), amt.getUnit());
                var amount = addressTxAmountMap.getOrDefault(key, BigInteger.ZERO);
                amount = amount.subtract(amt.getQuantity());
                addressTxAmountMap.put(key, amount);

                var addressDetails = new AddressDetails(input.getOwnerStakeAddr(), input.getOwnerPaymentCredential(), input.getOwnerStakeCredential());
                addressToAddressDetailsMap.put(input.getOwnerAddr(), addressDetails);

                var assetDetails = new AssetDetails(amt.getPolicyId(), amt.getAssetName());
                unitToAssetDetailsMap.put(amt.getUnit(), assetDetails);
            }
        }

        //Add output amounts
        for (var output : outputs) {
            for (Amt amt : output.getAmounts()) {
                var key = Pair.of(output.getOwnerAddr(), amt.getUnit());
                var amount = addressTxAmountMap.getOrDefault(key, BigInteger.ZERO);
                amount = amount.add(amt.getQuantity());
                addressTxAmountMap.put(key, amount);

                var addressDetails = new AddressDetails(output.getOwnerStakeAddr(), output.getOwnerPaymentCredential(), output.getOwnerStakeCredential());
                addressToAddressDetailsMap.put(output.getOwnerAddr(), addressDetails);

                var assetDetails = new AssetDetails(amt.getPolicyId(), amt.getAssetName());
                unitToAssetDetailsMap.put(amt.getUnit(), assetDetails);
            }
        }

        return (List<AddressTxAmount>) addressTxAmountMap.entrySet()
                .stream()
                .filter(entry -> (accountStoreProperties.isAddressTxAmountIncludeZeroAmount() &&
                        accountStoreProperties.isAddressTxAmountExcludeTokenZeroAmount() && entry.getKey().getSecond().equals(LOVELACE))
                        || (accountStoreProperties.isAddressTxAmountIncludeZeroAmount() && !accountStoreProperties.isAddressTxAmountExcludeTokenZeroAmount())
                        || entry.getValue().compareTo(BigInteger.ZERO) != 0)
                .map(entry -> {
                    var addressDetails = addressToAddressDetailsMap.get(entry.getKey().getFirst());

                    //address and full address if the address is too long
                    var addressTuple = getAddress(entry.getKey().getFirst());

                    return AddressTxAmount.builder()
                            .address(addressTuple._1)
                            .unit(entry.getKey().getSecond())
                            .txHash(txHash)
                            .slot(metadata.getSlot())
                            .quantity(entry.getValue())
                            .stakeAddress(addressDetails.ownerStakeAddress)
                            .epoch(metadata.getEpochNumber())
                            .blockNumber(metadata.getBlock())
                            .blockTime(metadata.getBlockTime())
                            .build();
                }).toList();
    }

    @EventListener
    @Transactional //We can also listen to CommitEvent here
    public void handleRemainingTxInputOuputs(PreCommitEvent preCommitEvent) {
        if (!accountStoreProperties.isSaveAddressTxAmount())
            return;

        try {
            List<AddressTxAmount> addressTxAmountList = new ArrayList<>();
            for (var pair : pendingTxInputOutputListCache) {
                EventMetadata metadata = pair.getFirst();
                TxInputOutput txInputOutput = pair.getSecond();

                var addrAmountEntitiesForTx = processAddressAmountForTx(metadata, txInputOutput, true);

                if (addrAmountEntitiesForTx != null) {
                    addressTxAmountList.addAll(addrAmountEntitiesForTx);
                }
            }

            if (addressTxAmountList.size() > 0) {
                addressTxAmountListCache.addAll(addressTxAmountList);
            }

            long t1 = System.currentTimeMillis();
            if (addressTxAmountListCache.size() > 0) {
                addressTxAmountStorage.save(addressTxAmountListCache);
            }

            long t2 = System.currentTimeMillis();
            log.info("Time taken to save additional address_tx_amounts records : {}, time: {} ms",
                    addressTxAmountListCache.size(), (t2 - t1));
        } finally {
            pendingTxInputOutputListCache.clear();
            addressTxAmountListCache.clear();
        }
    }

    @EventListener
    @Transactional
    public void handleRollback(RollbackEvent rollbackEvent) {
        int addressTxAmountDeleted = addressTxAmountStorage.deleteAddressBalanceBySlotGreaterThan(rollbackEvent.getRollbackTo().getSlot());

        log.info("Rollback -- {} address_tx_amounts records", addressTxAmountDeleted);
    }

    record AssetDetails(String policy, String assetName) {
    }

    record AddressDetails(String ownerStakeAddress, String ownerPaymentCredential, String ownerStakeCredential) {
    }
}
