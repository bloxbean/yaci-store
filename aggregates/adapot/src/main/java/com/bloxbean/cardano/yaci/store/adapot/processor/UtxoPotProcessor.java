package com.bloxbean.cardano.yaci.store.adapot.processor;

import com.bloxbean.cardano.yaci.store.client.utxo.UtxoClient;
import com.bloxbean.cardano.yaci.store.common.aspect.EnableIf;
import com.bloxbean.cardano.yaci.store.common.domain.UtxoKey;
import com.bloxbean.cardano.yaci.store.events.GenesisBalance;
import com.bloxbean.cardano.yaci.store.events.GenesisBlockEvent;
import com.bloxbean.cardano.yaci.store.events.internal.PreCommitEvent;
import com.bloxbean.cardano.yaci.store.utxo.domain.AddressUtxoEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import static com.bloxbean.cardano.yaci.store.adapot.AdaPotConfiguration.STORE_ADAPOT_ENABLED;

//@Component
//TODO : Enable this with a second database as temp database
@RequiredArgsConstructor
@EnableIf(value = STORE_ADAPOT_ENABLED, defaultValue = false)
@Slf4j
public class UtxoPotProcessor {
    private final UtxoClient utxoClient;

    private List<AddressUtxoEvent> addressUtxoEvents = Collections.synchronizedList(new ArrayList<>());
    private BigInteger netBatchAmt = BigInteger.ZERO;

    @EventListener
    public void handleAddressUtxoEvent(AddressUtxoEvent addressUtxoEvent) {
        addressUtxoEvents.add(addressUtxoEvent);
    }

    @EventListener
    @Transactional
    public void handleGenesisBalanceEvent(GenesisBlockEvent genesisBlockEvent) {
        List<GenesisBalance> gensisBalances = genesisBlockEvent.getGenesisBalances();
        var totalGenesisBalance = gensisBalances.stream()
                .map(genesisBalance -> genesisBalance.getBalance())
                .reduce(BigInteger::add)
                .orElse(BigInteger.ZERO);

        netBatchAmt = netBatchAmt.add(totalGenesisBalance);
        log.info("Total Genesis balance : {}", totalGenesisBalance);
    }

   @EventListener
    public void calculateNetUtxoAmtInBatach(PreCommitEvent preCommitEvent) {

        Collections.sort(addressUtxoEvents, Comparator.comparingLong(addressUtxoEvent -> addressUtxoEvent.getEventMetadata().getSlot()));

        for (AddressUtxoEvent addressUtxoEvent : addressUtxoEvents) {
            var txInputOutputs = addressUtxoEvent.getTxInputOutputs();
            if (txInputOutputs == null || txInputOutputs.isEmpty())
                continue;

            var netBlockAmt = BigInteger.ZERO;

            for (var txInputOutput : txInputOutputs) {
                var inputKeys = txInputOutput.getInputs().stream()
                        .map(txInput -> new UtxoKey(txInput.getTxHash(), txInput.getOutputIndex()))
                                .collect(Collectors.toList());

                var inputAddressUtxos = utxoClient.getUtxosByIds(inputKeys);
                if (inputAddressUtxos.size() != inputKeys.size()) {
                    log.error("Input Utxos not found for transaction : {}. Something wrong !!!", txInputOutput.getTxHash());
                    throw new IllegalStateException("Input Utxos not found for transaction : " + txInputOutput.getTxHash());
                }

                var inputAmt = inputAddressUtxos.stream()
                        .map(utxo -> utxo.getLovelaceAmount())
                        .reduce(BigInteger::add)
                        .orElse(BigInteger.ZERO);

                //calculate output amount
                var outputAmt = txInputOutput.getOutputs().stream()
                        .map(output -> output.getLovelaceAmount())
                        .reduce(BigInteger::add)
                        .orElse(BigInteger.ZERO);

                var netAmt = outputAmt.subtract(inputAmt);
                netBlockAmt = netBlockAmt.add(netAmt);
            }

            netBatchAmt = netBatchAmt.add(netBlockAmt);
        }

        addressUtxoEvents.clear();
    }

    public BigInteger getNetUtxoAmount() {
        return netBatchAmt;
    }

    public void reset() {
        addressUtxoEvents.clear();
        netBatchAmt = BigInteger.ZERO;
    }

}
