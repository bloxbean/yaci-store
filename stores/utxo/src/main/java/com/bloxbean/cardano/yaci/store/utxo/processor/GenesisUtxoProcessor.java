package com.bloxbean.cardano.yaci.store.utxo.processor;

import com.bloxbean.cardano.client.address.Address;
import com.bloxbean.cardano.yaci.core.util.HexUtil;
import com.bloxbean.cardano.yaci.store.common.domain.AddressUtxo;
import com.bloxbean.cardano.yaci.store.common.domain.Amt;
import com.bloxbean.cardano.yaci.store.events.GenesisBalance;
import com.bloxbean.cardano.yaci.store.events.GenesisBlockEvent;
import com.bloxbean.cardano.yaci.store.utxo.storage.api.UtxoStorage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

import static com.bloxbean.cardano.yaci.core.util.Constants.LOVELACE;

@Component
@RequiredArgsConstructor
@Slf4j
public class GenesisUtxoProcessor {
    private final UtxoStorage utxoStorage;

    @EventListener
    @Transactional
    public void processGenesisUtxos(GenesisBlockEvent genesisBlockEvent) {
        log.info("Processing genesis utxos ...");
        List<GenesisBalance> genesisBalanceList = genesisBlockEvent.getGenesisBalances();
        if(genesisBalanceList == null || genesisBalanceList.size() == 0) {
            log.info("No genesis utxos found");
            return;
        }

        List<AddressUtxo> genesisUtxos = new ArrayList<>();
        for(GenesisBalance genesisBalance: genesisBalanceList) {
            String ownerPaymentCredential = null;
            String ownerStakeCredential = null;
            if (genesisBalance.getAddress() != null &&
                    genesisBalance.getAddress().startsWith("addr")) { //If shelley address
                Address address = new Address(genesisBalance.getAddress());
                ownerPaymentCredential = address.getPaymentCredential().map(paymentKeyHash -> HexUtil.encodeHexString(paymentKeyHash.getBytes()))
                        .orElse(null);
                ownerStakeCredential = address.getDelegationCredential().map(delegationHash -> HexUtil.encodeHexString(delegationHash.getBytes()))
                        .orElse(null);
            }

            AddressUtxo addressUtxo = AddressUtxo.builder()
                    .slot(genesisBlockEvent.getSlot())
                    .blockNumber(genesisBlockEvent.getBlock())
                    .blockHash(genesisBlockEvent.getBlockHash())
                    .blockTime(genesisBlockEvent.getBlockTime())
                    .epoch(0)
                    .txHash(genesisBalance.getTxnHash())
                    .outputIndex(0)
                    .ownerAddr(genesisBalance.getAddress())
                    .ownerPaymentCredential(ownerPaymentCredential)
                    .ownerStakeCredential(ownerStakeCredential)
                    .lovelaceAmount(genesisBalance.getBalance())
                    .amounts(List.of(new Amt(LOVELACE, "", LOVELACE, genesisBalance.getBalance())))
                    .build();
            genesisUtxos.add(addressUtxo);
        }

        utxoStorage.saveUnspent(genesisUtxos);
    }
}
