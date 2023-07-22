package com.bloxbean.cardano.yaci.store.transaction.processor;

import com.bloxbean.cardano.yaci.store.common.domain.UtxoKey;
import com.bloxbean.cardano.yaci.store.events.GenesisBalance;
import com.bloxbean.cardano.yaci.store.events.GenesisBlockEvent;
import com.bloxbean.cardano.yaci.store.transaction.domain.Txn;
import com.bloxbean.cardano.yaci.store.transaction.storage.api.TransactionStorage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class GenesisTransactionProcessor {
    private final TransactionStorage transactionStorage;

    @EventListener
    @Transactional
    public void processGenesisUtxos(GenesisBlockEvent genesisBlockEvent) {
        log.info("Processing genesis transactions ...");
        List<GenesisBalance> genesisBalanceList = genesisBlockEvent.getGenesisBalances();
        if(genesisBalanceList == null || genesisBalanceList.size() == 0) {
            log.info("No genesis transaction found");
            return;
        }

        List<Txn> genesisTxns = new ArrayList<>();
        for(GenesisBalance genesisBalance: genesisBalanceList) {
            UtxoKey output = new UtxoKey(genesisBalance.getTxnHash(), 0);
            Txn txn = Txn.builder()
                    .txHash(genesisBalance.getTxnHash())
                    .blockHash(genesisBlockEvent.getBlockHash())
                    .blockNumber(genesisBlockEvent.getBlock())
                    .blockTime(genesisBlockEvent.getBlockTime())
                    .slot(genesisBlockEvent.getSlot())
                    .inputs(Collections.emptyList())
                    .outputs(List.of(output))
                    .fee(BigInteger.ZERO)
                    .ttl(0L)
                    .invalid(false)
                    .build();

            genesisTxns.add(txn);
        }

        transactionStorage.saveAll(genesisTxns);
    }
}
