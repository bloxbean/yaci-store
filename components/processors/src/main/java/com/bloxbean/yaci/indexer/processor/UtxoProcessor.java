package com.bloxbean.yaci.indexer.processor;

import com.bloxbean.cardano.client.address.Address;
import com.bloxbean.cardano.client.address.AddressService;
import com.bloxbean.cardano.client.address.AddressType;
import com.bloxbean.cardano.yaci.helper.model.Transaction;
import com.bloxbean.cardano.yaci.indexer.entity.AddressUtxo;
import com.bloxbean.cardano.yaci.indexer.entity.Amt;
import com.bloxbean.cardano.yaci.indexer.entity.InvalidTransaction;
import com.bloxbean.cardano.yaci.indexer.entity.UtxoId;
import com.bloxbean.cardano.yaci.indexer.events.TransactionEvent;
import com.bloxbean.cardano.yaci.indexer.repository.InvalidTransactionRepository;
import com.bloxbean.cardano.yaci.indexer.repository.UtxoRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
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
    public void handleTransactionEvent(TransactionEvent event) {
        try {
            List<Transaction> transactions = event.getTransactions();
            //insert invalid transactions
            transactions.stream()
                    .forEach(transactionEvent -> {
                        if (transactionEvent.isInvalid()) {
                            InvalidTransaction invalidTransaction = InvalidTransaction.builder()
                                    .txHash(transactionEvent.getTxHash())
                                    .build();
                            invalidTransactionRepository.save(invalidTransaction);
                        }
                    });

            //filter valid transactions
            var validTransactions = transactions.stream()
                    .filter(transactionEvent -> !transactionEvent.isInvalid())
                    .collect(Collectors.toList());

            List<AddressUtxo> addressUtxos = validTransactions.stream()
                    .filter(transactionEvent -> !transactionEvent.isInvalid())
                    .flatMap(transactionEvent -> transactionEvent.getUtxos().stream())
                    //  .filter(utxo -> utxo.getAddress().equals("addr1w9qzpelu9hn45pefc0xr4ac4kdxeswq7pndul2vuj59u8tqaxdznu"))
                    .map(utxo -> {
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
                                .block(event.getMetadata().getBlock())
                                .blockHash(event.getMetadata().getBlockHash())
                                .txHash(utxo.getTxHash())
                                .outputIndex(utxo.getIndex())
                                .ownerAddr(utxo.getAddress())
                                .ownerStakeAddr(stakeAddress)
                                .amounts(amounts)
                                .dataHash(utxo.getDatumHash())
                                .inlineDatum(utxo.getInlineDatum())
                                .referenceScriptHash(utxo.getScriptRef())
                                .build();
                    })
                    .collect(Collectors.toList());

            List<AddressUtxo> existingAddressUtxos = validTransactions.stream().flatMap(transactionEvent -> transactionEvent.getBody().getInputs().stream())
                    .map(transactionInput -> UtxoId.builder()
                            .txHash(transactionInput.getTransactionId())
                            .outputIndex(transactionInput.getIndex()).build())
                    .map(utxoId -> utxoRepository.findById(utxoId))
                    .filter(addressUtxo -> addressUtxo.isPresent())
                    .map(addressUtxoOptional -> {
                        AddressUtxo addressUtxo = addressUtxoOptional.get();
                        addressUtxo.setSpent(true);
                        return addressUtxo;
                    }).collect(Collectors.toList());

            //Update existing utxos as spent
            if (existingAddressUtxos.size() > 0)
                utxoRepository.saveAll(existingAddressUtxos);
            if (addressUtxos.size() > 0)
                utxoRepository.saveAll(addressUtxos);

        } catch (Exception e) {
            log.error("Error saving", e);
            log.error("Stopping fetcher");
            throw e;
        }
    }
}
