package com.bloxbean.cardano.yaci.store.utxo.processor;

import com.bloxbean.cardano.client.address.Address;
import com.bloxbean.cardano.client.address.AddressProvider;
import com.bloxbean.cardano.client.address.AddressType;
import com.bloxbean.cardano.yaci.core.util.HexUtil;
import com.bloxbean.cardano.yaci.helper.model.Transaction;
import com.bloxbean.cardano.yaci.helper.model.Utxo;
import com.bloxbean.cardano.yaci.store.common.domain.AddressUtxo;
import com.bloxbean.cardano.yaci.store.common.domain.Amt;
import com.bloxbean.cardano.yaci.store.common.domain.UtxoKey;
import com.bloxbean.cardano.yaci.store.common.util.ScriptReferenceUtil;
import com.bloxbean.cardano.yaci.store.common.util.StringUtil;
import com.bloxbean.cardano.yaci.store.events.EventMetadata;
import com.bloxbean.cardano.yaci.store.events.TransactionEvent;
import com.bloxbean.cardano.yaci.store.utxo.domain.AddressUtxoEvent;
import com.bloxbean.cardano.yaci.store.utxo.domain.InvalidTransaction;
import com.bloxbean.cardano.yaci.store.utxo.domain.TxInputOutput;
import com.bloxbean.cardano.yaci.store.utxo.storage.api.InvalidTransactionStorage;
import com.bloxbean.cardano.yaci.store.utxo.storage.api.UtxoStorage;
import io.micrometer.core.annotation.Timed;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigInteger;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.bloxbean.cardano.yaci.core.util.Constants.LOVELACE;
import static com.bloxbean.cardano.yaci.store.utxo.util.Util.getPaymentKeyHash;
import static com.bloxbean.cardano.yaci.store.utxo.util.Util.getStakeKeyHash;

@Component
@RequiredArgsConstructor
@Slf4j
public class UtxoProcessor {
    private final UtxoStorage utxoStorage;
    private final InvalidTransactionStorage invalidTransactionStorage;
    private final ApplicationEventPublisher publisher;
    private final MeterRegistry meterRegistry;

    private Counter counter;

    @PostConstruct
    public void init() {
        counter = this.meterRegistry.counter("transactions.processed");
    }

    @EventListener
    @Order(2)
    @Transactional
    @Timed(value = "store.utxo.process", percentiles = {0.5, 0.95, 0.99}, histogram = true)
    public void handleTransactionEvent(TransactionEvent event) {
        try {
            List<Transaction> transactions = event.getTransactions();
            if (transactions == null)
                return;

//            boolean dependencyFound = false;
//            Set<String> txHashes = transactions.stream().map(transaction -> transaction.getTxHash())
//                    .collect(Collectors.toSet());
//            for (Transaction transaction: transactions) {
//                for (TransactionInput input: transaction.getBody().getInputs()) {
//                    if (txHashes.contains(input.getTransactionId())) {
//                        //This is a self transaction. Ignore
//                        //log.info("Transaction chaining found >>>>>>>>>>>>>>>>>> input >> " + input.getTransactionId() + ", taget txHash: " + transaction.getTxHash());
//                        dependencyFound = true;
//                        break;
//                    }
//                }
//            }


            Stream<Transaction> transactionStream = transactions.stream(); //dependencyFound? transactions.stream(): transactions.parallelStream();
            List<TxInputOutput> txInputOutputs = transactionStream.map(
                    transaction -> {
                        counter.increment();
                        Optional<TxInputOutput> txInputOutputOptional;
                        if (transaction.isInvalid()) {
                            txInputOutputOptional = handleInvalidTransaction(event.getMetadata(), transaction);
                        } else {
                            txInputOutputOptional = handleValidTransaction(event.getMetadata(), transaction);
                        }

                        return txInputOutputOptional;
                    }).filter(Optional::isPresent)
                    .map(Optional::get)
                    .collect(Collectors.toList());

            utxoStorage.saveSpent(txInputOutputs.stream()
                    .flatMap(txInputOutput -> txInputOutput.getInputs().stream()).collect(Collectors.toList()));

            utxoStorage.saveUnspent(txInputOutputs.stream()
                    .flatMap(txInputOutput -> txInputOutput.getOutputs().stream()).collect(Collectors.toList()));

            if(txInputOutputs.size() > 0)
                publisher.publishEvent(new AddressUtxoEvent(event.getMetadata(), txInputOutputs));
        } catch (Exception e) {
            log.error("Error saving : " + event.getMetadata(), e);
            log.error("Stopping fetcher");
            throw new RuntimeException(e);
        }
    }

    private Optional<TxInputOutput> handleValidTransaction(EventMetadata metadata, Transaction transaction) {
        if (transaction.isInvalid())
            return Optional.empty();

        //set spent for input
        List<AddressUtxo> inputAddressUtxos = transaction.getBody().getInputs().stream()
                .map(transactionInput -> new UtxoKey(transactionInput.getTransactionId(), transactionInput.getIndex()))
                .map(utxoKey -> {
                    AddressUtxo addressUtxo = new AddressUtxo();
                    addressUtxo.setTxHash(utxoKey.getTxHash());
                    addressUtxo.setOutputIndex(utxoKey.getOutputIndex());
                    addressUtxo.setSpent(true);
                    addressUtxo.setSpentAtSlot(metadata.getSlot());
                    addressUtxo.setSpentEpoch(metadata.getEpochNumber());
                    addressUtxo.setSpentTxHash(transaction.getTxHash());
                    return addressUtxo;
                }).collect(Collectors.toList());

        //Check if utxo is already there, only possible in a multi-instance environment
        //TODO-1
        //                    utxoStorage.findById(addressUtxo.getTxHash(), addressUtxo.getOutputIndex())
        //                            .ifPresent(existingAddressUtxo -> addressUtxo.setSpent(existingAddressUtxo.getSpent()));
        List<AddressUtxo> outputAddressUtxos = transaction.getUtxos().stream()
                .map(utxo -> getAddressUtxo(metadata, utxo))
                .collect(Collectors.toList());

        //publish event
        if (outputAddressUtxos.size() > 0)
            return Optional.of(new TxInputOutput(transaction.getTxHash(), inputAddressUtxos, outputAddressUtxos));
        else
            return Optional.empty();
    }

    private Optional<TxInputOutput> handleInvalidTransaction(EventMetadata metadata, Transaction transaction) {
        if (!transaction.isInvalid())
            return Optional.empty();

        //insert invalid transactions and collateral return utxo if any
        InvalidTransaction invalidTransaction = InvalidTransaction.builder()
                .txHash(transaction.getTxHash())
                .slot(metadata.getSlot())
                .blockHash(metadata.getBlockHash())
                .transaction(transaction)
                .build();
        invalidTransactionStorage.save(invalidTransaction);

        //collateral output
        AddressUtxo collateralOutputUtxo = Optional.ofNullable(transaction.getCollateralReturnUtxo())
                .map(utxo -> getCollateralReturnAddressUtxo(metadata, utxo))
                .orElse(null);

        //Also, change in Yaci to return collateral output index as size of tx outputs
        if (collateralOutputUtxo != null)
            collateralOutputUtxo.setOutputIndex(transaction.getBody().getOutputs().size());

        //collateral inputs will be marked as spent
        List<AddressUtxo> collateralInputUtxos = transaction.getBody().getCollateralInputs().stream()
                .map(transactionInput -> {
                    AddressUtxo addressUtxo = new AddressUtxo();
                    addressUtxo.setTxHash(transactionInput.getTransactionId());
                    addressUtxo.setOutputIndex(transactionInput.getIndex());
                    addressUtxo.setSpent(true);
                    addressUtxo.setSpentAtSlot(metadata.getSlot());
                    addressUtxo.setSpentEpoch(metadata.getEpochNumber());
                    addressUtxo.setSpentTxHash(transaction.getTxHash());
                    return addressUtxo;
                }).collect(Collectors.toList());


        //Check if collateral utxos are already present. If yes, then update everything except spent field
        //Only possible in multi-instance environment.
        if (collateralOutputUtxo != null) {
            //TODO --1
//            utxoStorage.findById(collateralOutputUtxo.getTxHash(), collateralOutputUtxo.getOutputIndex())
//                    .ifPresent(existingAddressUtxo -> collateralOutputUtxo.setSpent(existingAddressUtxo.getSpent()));
        }

        if (collateralOutputUtxo != null)
            return Optional.of(new TxInputOutput(transaction.getTxHash(), collateralInputUtxos, List.of(collateralOutputUtxo)));
        else
            return Optional.empty();
    }

    private AddressUtxo getAddressUtxo(@NonNull EventMetadata eventMetadata, @NonNull Utxo utxo) {
        //Fix -- some asset name contains \u0000 -- postgres can't convert this to text. so replace
        List<Amt> amounts = utxo.getAmounts().stream().map(amount ->
                        Amt.builder()
                                .unit(amount.getUnit() != null? amount.getUnit().replace(".", ""): null) //remove . from unit as yaci provides policy.assetName as unit
                                .policyId(amount.getPolicyId())
                                .assetName(amount.getAssetName().replace('\u0000', ' '))
                                .quantity(amount.getQuantity())
                                .build())
                .collect(Collectors.toList());

        BigInteger lovelaceAmount = amounts.stream()
                .filter(amount -> LOVELACE.equals(amount.getUnit()))
                .findFirst()
                .map(Amt::getQuantity).orElse(BigInteger.ZERO);

        String stakeAddress = null;
        String paymentKeyHash = null;
        String stakeKeyHash = null;
        try {
            Address addr = new Address(utxo.getAddress());
            if (addr.getAddressType() == AddressType.Base)
                stakeAddress = AddressProvider.getStakeAddress(addr).getAddress();

            paymentKeyHash = getPaymentKeyHash(addr).orElse(null);
            stakeKeyHash = getStakeKeyHash(addr).orElse(null);
        } catch (Exception e) {
            //TODO -- Store the address in db
            if (log.isTraceEnabled())
                log.error("Unable to get stake address for address : " + utxo.getAddress(), e);
        }

        //Derive reference script hash if scriptRef is present
        String referenceScriptHash = null;
        if (!StringUtil.isEmpty(utxo.getScriptRef())) {
            try {
                referenceScriptHash = ScriptReferenceUtil.getReferenceScriptHash(HexUtil.decodeHexString(utxo.getScriptRef()));
            } catch (Exception e) {
                log.error("Unable to get reference script hash for script ref. Block: {}, TxHash:  {}", eventMetadata.getBlock(), utxo.getTxHash());
                log.error("Unable to get reference script hash for script ref : " + utxo.getScriptRef(), e);
                //throw new IllegalArgumentException("Unable to get reference script hash for script ref : " + utxo.getScriptRef());
            }
        }

        return AddressUtxo.builder()
                .slot(eventMetadata.getSlot())
                .blockNumber(eventMetadata.getBlock())
                .blockHash(eventMetadata.getBlockHash())
                .blockTime(eventMetadata.getBlockTime())
                .epoch(eventMetadata.getEpochNumber())
                .txHash(utxo.getTxHash())
                .outputIndex(utxo.getIndex())
                .ownerAddr(utxo.getAddress())
                .ownerStakeAddr(stakeAddress)
                .ownerPaymentCredential(paymentKeyHash)
                .ownerStakeCredential(stakeKeyHash)
                .lovelaceAmount(lovelaceAmount)
                .amounts(amounts)
                .dataHash(utxo.getDatumHash())
                .inlineDatum(utxo.getInlineDatum())
                .referenceScriptHash(referenceScriptHash)
                .scriptRef(utxo.getScriptRef())
                .build();
    }

    private AddressUtxo getCollateralReturnAddressUtxo(EventMetadata metadata, Utxo utxo) {
        AddressUtxo addressUtxo = getAddressUtxo(metadata, utxo);
        addressUtxo.setIsCollateralReturn(Boolean.TRUE);
        return addressUtxo;
    }
}
