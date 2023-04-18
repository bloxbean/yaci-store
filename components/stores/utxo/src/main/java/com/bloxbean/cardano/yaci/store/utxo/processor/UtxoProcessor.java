package com.bloxbean.cardano.yaci.store.utxo.processor;

import com.bloxbean.cardano.yaci.store.common.domain.AddressUtxo;
import com.bloxbean.cardano.yaci.store.common.domain.Amt;
import com.bloxbean.cardano.client.address.Address;
import com.bloxbean.cardano.client.address.AddressProvider;
import com.bloxbean.cardano.client.address.AddressType;
import com.bloxbean.cardano.yaci.helper.model.Transaction;
import com.bloxbean.cardano.yaci.helper.model.Utxo;
import com.bloxbean.cardano.yaci.store.events.EventMetadata;
import com.bloxbean.cardano.yaci.store.events.TransactionEvent;
import com.bloxbean.cardano.yaci.store.utxo.domain.AddressUtxoEvent;
import com.bloxbean.cardano.yaci.store.utxo.domain.InvalidTransaction;
import com.bloxbean.cardano.yaci.store.common.domain.UtxoKey;
import com.bloxbean.cardano.yaci.store.utxo.storage.api.InvalidTransactionStorage;
import com.bloxbean.cardano.yaci.store.utxo.storage.api.UtxoStorage;
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
import java.util.stream.Collectors;

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

    @EventListener
    @Order(2)
    @Transactional
    public void handleTransactionEvent(TransactionEvent event) {
        try {
            List<Transaction> transactions = event.getTransactions();
            if (transactions == null)
                return;

            transactions.stream().forEach(
                    transaction -> {
                        if (transaction.isInvalid()) {
                            handleInvalidTransaction(event.getMetadata(), transaction);
                        } else {
                            handleValidTransaction(event.getMetadata(), transaction);
                        }
                    });
        } catch (Exception e) {
            log.error("Error saving : " + event.getMetadata(), e);
            log.error("Stopping fetcher");
            throw new RuntimeException(e);
        }
    }

    private void handleValidTransaction(EventMetadata metadata, Transaction transaction) {
        if (transaction.isInvalid())
            return;

        //set spent for input
        List<AddressUtxo> inputAddressUtxos = transaction.getBody().getInputs().stream()
                .map(transactionInput -> new UtxoKey(transactionInput.getTransactionId(), transactionInput.getIndex()))
                .map(utxoKey -> {
                    AddressUtxo addressUtxo = utxoStorage.findById(utxoKey.getTxHash(), utxoKey.getOutputIndex())
                            .orElse(AddressUtxo.builder()        //If not present, then create a record with pk
                                    .txHash(utxoKey.getTxHash())
                                    .outputIndex(utxoKey.getOutputIndex()).build());
                    addressUtxo.setSpent(true);
                    addressUtxo.setSpentTxHash(transaction.getTxHash());
                    return addressUtxo;
                }).collect(Collectors.toList());

        List<AddressUtxo> outputAddressUtxos = transaction.getUtxos().stream()
                .map(utxo -> getAddressUtxo(metadata, utxo))
                .map(addressUtxo -> { //Check if utxo is already there, only possible in a multi-instance environment
                    utxoStorage.findById(addressUtxo.getTxHash(), addressUtxo.getOutputIndex())
                            .ifPresent(existingAddressUtxo -> addressUtxo.setSpent(existingAddressUtxo.getSpent()));
                    return addressUtxo;
                })
                .collect(Collectors.toList());

        if (outputAddressUtxos.size() > 0) //unspent utxos
            utxoStorage.saveAll(outputAddressUtxos);

        //Update existing utxos as spent
        if (inputAddressUtxos.size() > 0) //spent utxos
            utxoStorage.saveAll(inputAddressUtxos);

        //publish event
        if (outputAddressUtxos.size() > 0)
            publisher.publishEvent(new AddressUtxoEvent(metadata, outputAddressUtxos));
    }

    private void handleInvalidTransaction(EventMetadata metadata, Transaction transaction) {
        if (!transaction.isInvalid())
            return;

        //insert invalid transactions and collateral return utxo if any
        InvalidTransaction invalidTransaction = InvalidTransaction.builder()
                .txHash(transaction.getTxHash())
                .slot(metadata.getSlot())
                .blockHash(metadata.getBlockHash())
                .transaction(transaction)
                .build();
        invalidTransactionStorage.save(invalidTransaction);

        //collateral output
        AddressUtxo collateralOutputUtxo = transaction.getCollateralReturnUtxo()
                .map(utxo -> getCollateralReturnAddressUtxo(metadata, utxo))
                .orElse(null);

        //collateral inputs will be marked as spent
        List<AddressUtxo> collateralInputUtxos = transaction.getBody().getCollateralInputs().stream()
                .map(transactionInput -> {
                    AddressUtxo addressUtxo = utxoStorage.findById(transactionInput.getTransactionId(), transactionInput.getIndex())
                            .orElse(AddressUtxo.builder()
                                    .txHash(transactionInput.getTransactionId())
                                    .outputIndex(transactionInput.getIndex())
                                    .build()
                            );

                    addressUtxo.setSpent(true);
                    addressUtxo.setSpentTxHash(transaction.getTxHash());
                    return addressUtxo;
                }).collect(Collectors.toList());


        //Check if collateral utxos are already present. If yes, then update everything except spent field
        //Only possible in multi-instance environment.
        if (collateralOutputUtxo != null) {
            utxoStorage.findById(collateralOutputUtxo.getTxHash(), collateralOutputUtxo.getOutputIndex())
                    .ifPresent(existingAddressUtxo -> collateralOutputUtxo.setSpent(existingAddressUtxo.getSpent()));
        }


        if (collateralOutputUtxo != null)
            utxoStorage.save(collateralOutputUtxo);
        if (collateralInputUtxos != null && collateralInputUtxos.size() > 0)
            utxoStorage.saveAll(collateralInputUtxos);

        //publish event
        if (collateralOutputUtxo != null)
            publisher.publishEvent(new AddressUtxoEvent(metadata, List.of(collateralOutputUtxo)));
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

        return AddressUtxo.builder()
                .slot(eventMetadata.getSlot())
                .block(eventMetadata.getBlock())
                .blockHash(eventMetadata.getBlockHash())
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
                .scriptRef(utxo.getScriptRef())
                .build();
    }

    private AddressUtxo getCollateralReturnAddressUtxo(EventMetadata metadata, Utxo utxo) {
        AddressUtxo addressUtxo = getAddressUtxo(metadata, utxo);
        addressUtxo.setIsCollateralReturn(Boolean.TRUE);
        return addressUtxo;
    }

}
