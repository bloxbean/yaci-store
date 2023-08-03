package com.bloxbean.cardano.yaci.store.account.service;

import com.bloxbean.cardano.yaci.store.client.utxo.UtxoClient;
import com.bloxbean.cardano.yaci.store.common.domain.Utxo;
import com.bloxbean.cardano.yaci.store.utxo.domain.Amount;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class UtxoAccountService {
    private final UtxoClient utxoClient;

    public List<Amount> getAmountsAtAddress(String address) {
        int page = 0;

        Map<String, Amount> amountMap = new HashMap<>();
        while (true) {
            List<Utxo> utxos = utxoClient.getUtxoByAddress(address, page, 100);
            if (utxos == null || utxos.size() == 0)
                break;

            utxos.stream()
                    .flatMap(utxo -> utxo.getAmount().stream())
                    .forEach(utxoAmt -> {
                        Amount existingAmount = amountMap.get(utxoAmt.getUnit());
                        if(existingAmount == null) {
                            Amount newAmount = new Amount(utxoAmt.getUnit(), utxoAmt.getQuantity());
                            amountMap.put(utxoAmt.getUnit(), newAmount);
                        } else {
                            BigInteger newQty = existingAmount.getQuantity().add(utxoAmt.getQuantity());
                            existingAmount.setQuantity(newQty);
                        }
                    });

            page++;
        }

        return new ArrayList<>(amountMap.values());
    }
}
