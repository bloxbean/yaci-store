package com.bloxbean.cardano.yaci.indexer.utxo.processor;

import com.bloxbean.carano.yaci.indexer.common.model.Amt;
import com.bloxbean.cardano.client.address.Address;
import com.bloxbean.cardano.client.address.AddressService;
import com.bloxbean.cardano.client.address.AddressType;
import com.bloxbean.cardano.client.util.Tuple;
import com.bloxbean.cardano.yaci.helper.model.Transaction;
import com.bloxbean.cardano.yaci.helper.model.Utxo;
import com.bloxbean.cardano.yaci.indexer.events.EventMetadata;
import com.bloxbean.cardano.yaci.indexer.events.TransactionEvent;
import com.bloxbean.cardano.yaci.indexer.utxo.entity.AddressUtxo;
import com.bloxbean.cardano.yaci.indexer.utxo.entity.InvalidTransaction;
import com.bloxbean.cardano.yaci.indexer.utxo.entity.UtxoId;
import com.bloxbean.cardano.yaci.indexer.utxo.repository.InvalidTransactionRepository;
import com.bloxbean.cardano.yaci.indexer.utxo.repository.UtxoRepository;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class UtxoProcessor {
    @Autowired
    private UtxoRepository utxoRepository;

    @Autowired
    private InvalidTransactionRepository invalidTransactionRepository;

    public UtxoProcessor(UtxoRepository utxoRepository, InvalidTransactionRepository invalidTransactionRepository) {
        this.utxoRepository = utxoRepository;
        this.invalidTransactionRepository = invalidTransactionRepository;
    }

    @EventListener
    @Order(2)
    @Transactional
    public void handleTransactionEvent(TransactionEvent event) {
        try {
            List<Transaction> transactions = event.getTransactions();

            handleInvalidTransactions(event);

            //filter valid transactions
            var validTransactions = transactions.stream()
                    .filter(transactionEvent -> !transactionEvent.isInvalid())
                    .collect(Collectors.toList());

            //Create new utxos
            List<AddressUtxo> addressUtxos = validTransactions.stream()
                    // .filter(transactionEvent -> !transactionEvent.isInvalid())
                    .flatMap(transactionEvent -> transactionEvent.getUtxos().stream())
                    //  .filter(utxo -> utxo.getAddress().equals("addr1w9qzpelu9hn45pefc0xr4ac4kdxeswq7pndul2vuj59u8tqaxdznu"))
                    .map(utxo -> getAddressUtxo(event.getMetadata(), utxo))
                    .map(addressUtxo -> { //Check if utxo is already there, only possible in a multi-instance environment
                        utxoRepository.findById(new UtxoId(addressUtxo.getTxHash(), addressUtxo.getOutputIndex()))
                                .ifPresent(existingAddressUtxo -> addressUtxo.setSpent(existingAddressUtxo.isSpent()));
                        return addressUtxo;
                    })
                    .collect(Collectors.toList());

            //Set inputs as spent
            List<AddressUtxo> existingAddressUtxos = validTransactions.stream()
                    .flatMap(transactionEvent -> transactionEvent.getBody().getInputs().stream()
                            .map(transactionInput -> new Tuple<>(transactionEvent.getTxHash(), transactionInput)))
                    .map(txHashTxInputTuple -> new Tuple<>(txHashTxInputTuple._1, new UtxoId(txHashTxInputTuple._2.getTransactionId(), txHashTxInputTuple._2.getIndex())))
                    .map(tuple -> {
                        AddressUtxo addressUtxo = utxoRepository.findById(tuple._2)
                                .orElse(AddressUtxo.builder()        //If not present, then create a record with pk
                                        .txHash(tuple._2.getTxHash())
                                        .outputIndex(tuple._2.getOutputIndex()).build());
                        addressUtxo.setSpent(true);
                        addressUtxo.setSpentTxHash(tuple._1);
                        return addressUtxo;
                    }).collect(Collectors.toList());

            if (addressUtxos.size() > 0) //unspent utxos
                utxoRepository.saveAll(addressUtxos);

            //Update existing utxos as spent
            //Update spentUtxos at the end to avoid incorrect spent during txn chaining
            if (existingAddressUtxos.size() > 0) //spent utxos
                utxoRepository.saveAll(existingAddressUtxos);

        } catch (Exception e) {
            log.error("Error saving", e);
            log.error("Stopping fetcher");
            throw e;
        }
    }

    private void handleInvalidTransactions(TransactionEvent event) {
        //insert invalid transactions and collateral return utxo if any
        List<AddressUtxo> collateralReturnUtxos = new ArrayList<>();
        List<AddressUtxo> collateralSpentInputs = null;
        for (Transaction transaction : event.getTransactions()) {
            if (transaction.isInvalid()) {
                InvalidTransaction invalidTransaction = InvalidTransaction.builder()
                        .txHash(transaction.getTxHash())
                        .slot(event.getMetadata().getSlot())
                        .blockHash(event.getMetadata().getBlockHash())
                        .transaction(transaction)
                        .build();
                invalidTransactionRepository.save(invalidTransaction);

                transaction.getCollateralReturnUtxo()
                        .ifPresent(utxo -> collateralReturnUtxos.add(getCollateralReturnAddressUtxo(event.getMetadata(), utxo)));

                //collateral inputs will be marked as spent
                collateralSpentInputs = transaction.getBody().getCollateralInputs().stream()
                        .map(transactionInput -> {
                            AddressUtxo addressUtxo = utxoRepository.findById(new UtxoId(transactionInput.getTransactionId(), transactionInput.getIndex()))
                                    .orElse(AddressUtxo.builder()
                                            .txHash(transactionInput.getTransactionId())
                                            .outputIndex(transactionInput.getIndex())
                                            .build()
                                    );

                            addressUtxo.setSpent(true);
                            addressUtxo.setSpentTxHash(transaction.getTxHash());
                            return addressUtxo;
                        }).collect(Collectors.toList());
            }
        }

        //Check if collateral utxos are already present. If yes, then update everything except spent field
        //Only possible in multi-instance environment.
        collateralReturnUtxos.stream()
                .forEach(addressUtxo -> {
                    utxoRepository.findById(new UtxoId(addressUtxo.getTxHash(), addressUtxo.getOutputIndex()))
                            .ifPresent(existingAddressUtxo -> addressUtxo.setSpent(existingAddressUtxo.isSpent()));
                });

        if (collateralSpentInputs != null && collateralSpentInputs.size() > 0)
            utxoRepository.saveAll(collateralSpentInputs);
        if (collateralReturnUtxos.size() > 0) //unspent utxos
            utxoRepository.saveAll(collateralReturnUtxos);

    }

    private AddressUtxo getAddressUtxo(@NonNull EventMetadata eventMetadata, @NonNull Utxo utxo) {
        //Fix -- some asset name contains \u0000 -- postgres can't convert this to text. so replace
        List<Amt> amounts = utxo.getAmounts().stream().map(amount ->
                        Amt.builder()
                                .unit(amount.getUnit())
                                .policyId(amount.getPolicyId())
                                .assetName(amount.getAssetName().replace('\u0000', ' '))
                                .quantity(amount.getQuantity())
                                .build())
                .collect(Collectors.toList());

        String stakeAddress = null;
        try {
            Address addr = new Address(utxo.getAddress());
            if (addr.getAddressType() == AddressType.Base)
                stakeAddress = AddressService.getInstance().getStakeAddress(addr).getAddress();
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
                .amounts(amounts)
                .dataHash(utxo.getDatumHash())
                .inlineDatum(utxo.getInlineDatum())
                .referenceScriptHash(utxo.getScriptRef())
                .build();
    }

    private AddressUtxo getCollateralReturnAddressUtxo(EventMetadata metadata, Utxo utxo) {
        AddressUtxo addressUtxo = getAddressUtxo(metadata, utxo);
        addressUtxo.setCollateralReturn(true);
        return addressUtxo;
    }

}
