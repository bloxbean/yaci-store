package com.bloxbean.cardano.yaci.indexer.utxo.processor;

import com.bloxbean.carano.yaci.indexer.common.domain.Amt;
import com.bloxbean.cardano.client.address.Address;
import com.bloxbean.cardano.client.address.AddressService;
import com.bloxbean.cardano.client.address.AddressType;
import com.bloxbean.cardano.yaci.helper.model.Transaction;
import com.bloxbean.cardano.yaci.helper.model.Utxo;
import com.bloxbean.cardano.yaci.indexer.events.EventMetadata;
import com.bloxbean.cardano.yaci.indexer.events.TransactionEvent;
import com.bloxbean.cardano.yaci.indexer.utxo.model.AddressUtxo;
import com.bloxbean.cardano.yaci.indexer.utxo.model.InvalidTransaction;
import com.bloxbean.cardano.yaci.indexer.utxo.model.UtxoId;
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
            log.error("Error saving", e);
            log.error("Stopping fetcher");
            throw e;
        }
    }

    private void handleValidTransaction(EventMetadata metadata, Transaction transaction) {
        if (transaction.isInvalid())
            return;

        //set spent for input
        List<AddressUtxo> inputAddressUtxos = transaction.getBody().getInputs().stream()
                .map(transactionInput -> new UtxoId(transactionInput.getTransactionId(), transactionInput.getIndex()))
                .map(utxoId -> {
                    AddressUtxo addressUtxo = utxoRepository.findById(utxoId)
                            .orElse(AddressUtxo.builder()        //If not present, then create a record with pk
                                    .txHash(utxoId.getTxHash())
                                    .outputIndex(utxoId.getOutputIndex()).build());
                    addressUtxo.setSpent(true);
                    addressUtxo.setSpentTxHash(transaction.getTxHash());
                    return addressUtxo;
                }).collect(Collectors.toList());

        List<AddressUtxo> outputAddressUtxos = transaction.getUtxos().stream()
                .map(utxo -> getAddressUtxo(metadata, utxo))
                .map(addressUtxo -> { //Check if utxo is already there, only possible in a multi-instance environment
                    utxoRepository.findById(new UtxoId(addressUtxo.getTxHash(), addressUtxo.getOutputIndex()))
                            .ifPresent(existingAddressUtxo -> addressUtxo.setSpent(existingAddressUtxo.isSpent()));
                    return addressUtxo;
                })
                .collect(Collectors.toList());

        if (outputAddressUtxos.size() > 0) //unspent utxos
            utxoRepository.saveAll(outputAddressUtxos);

        //Update existing utxos as spent
        if (inputAddressUtxos.size() > 0) //spent utxos
            utxoRepository.saveAll(inputAddressUtxos);
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
        invalidTransactionRepository.save(invalidTransaction);

        //collateral output
        AddressUtxo collateralOutputUtxo = transaction.getCollateralReturnUtxo()
                .map(utxo -> getCollateralReturnAddressUtxo(metadata, utxo))
                .orElse(null);

        //collateral inputs will be marked as spent
        List<AddressUtxo> collateralInputUtxos = transaction.getBody().getCollateralInputs().stream()
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


        //Check if collateral utxos are already present. If yes, then update everything except spent field
        //Only possible in multi-instance environment.
        if (collateralOutputUtxo != null) {
            utxoRepository.findById(new UtxoId(collateralOutputUtxo.getTxHash(), collateralOutputUtxo.getOutputIndex()))
                    .ifPresent(existingAddressUtxo -> collateralOutputUtxo.setSpent(existingAddressUtxo.isSpent()));
        }


        if (collateralOutputUtxo != null)
            utxoRepository.save(collateralOutputUtxo);
        if (collateralInputUtxos != null && collateralInputUtxos.size() > 0)
            utxoRepository.saveAll(collateralInputUtxos);
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
