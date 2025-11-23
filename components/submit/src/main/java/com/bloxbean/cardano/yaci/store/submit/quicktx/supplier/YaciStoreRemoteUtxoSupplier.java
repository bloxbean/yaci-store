package com.bloxbean.cardano.yaci.store.submit.quicktx.supplier;

import com.bloxbean.cardano.client.api.UtxoSupplier;
import com.bloxbean.cardano.client.api.common.OrderEnum;
import com.bloxbean.cardano.client.api.model.Amount;
import com.bloxbean.cardano.client.api.model.Utxo;
import com.bloxbean.cardano.client.common.CardanoConstants;
import com.bloxbean.cardano.yaci.store.client.utxo.UtxoClient;
import com.bloxbean.cardano.yaci.store.common.domain.AddressUtxo;
import com.bloxbean.cardano.yaci.store.common.domain.Amt;
import com.bloxbean.cardano.yaci.store.common.domain.UtxoKey;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * {@link UtxoSupplier} implementation backed by the REST {@link UtxoClient}.
 * This is used when in-memory storage beans (UtxoStorageReader) are not available.
 */
@Slf4j
@RequiredArgsConstructor
public class YaciStoreRemoteUtxoSupplier implements UtxoSupplier {

    private final UtxoClient utxoClient;

    @Override
    public List<Utxo> getPage(String address, Integer nrOfItems, Integer page, OrderEnum order) {
        if (!StringUtils.hasText(address)) {
            return Collections.emptyList();
        }

        int size = Optional.ofNullable(nrOfItems).orElse(DEFAULT_NR_OF_ITEMS_TO_FETCH);
        int pageNumber = Optional.ofNullable(page).orElse(0);

        List<com.bloxbean.cardano.yaci.store.common.domain.Utxo> utxos =
                utxoClient.getUtxoByAddress(address, pageNumber, size);

        return utxos.stream()
                .map(this::toClientUtxo)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<Utxo> getTxOutput(String txHash, int outputIndex) {
        if (!StringUtils.hasText(txHash)) {
            return Optional.empty();
        }

        return utxoClient.getUtxoById(new UtxoKey(txHash, outputIndex))
                .map(this::toClientUtxo);
    }

    private Utxo toClientUtxo(AddressUtxo addressUtxo) {
        List<Amount> amounts = new ArrayList<>();
        if (!CollectionUtils.isEmpty(addressUtxo.getAmounts())) {
            for (Amt amt : addressUtxo.getAmounts()) {
                if (amt == null || amt.getQuantity() == null) {
                    continue;
                }

                String unit = resolveUnit(amt.getUnit(), amt.getPolicyId());
                amounts.add(Amount.asset(unit, amt.getQuantity()));
            }
        }

        if (addressUtxo.getLovelaceAmount() != null) {
            amounts.add(Amount.lovelace(addressUtxo.getLovelaceAmount()));
        }

        return Utxo.builder()
                .txHash(addressUtxo.getTxHash())
                .outputIndex(addressUtxo.getOutputIndex())
                .address(addressUtxo.getOwnerAddr())
                .amount(amounts)
                .dataHash(addressUtxo.getDataHash())
                .inlineDatum(addressUtxo.getInlineDatum())
                .referenceScriptHash(addressUtxo.getReferenceScriptHash())
                .build();
    }

    private Utxo toClientUtxo(com.bloxbean.cardano.yaci.store.common.domain.Utxo utxo) {
        List<Amount> amounts = new ArrayList<>();
        if (!CollectionUtils.isEmpty(utxo.getAmount())) {
            for (com.bloxbean.cardano.yaci.store.common.domain.Utxo.Amount amt : utxo.getAmount()) {
                if (amt == null || amt.getQuantity() == null) {
                    continue;
                }

                String unit = StringUtils.hasText(amt.getUnit()) ? amt.getUnit() : CardanoConstants.LOVELACE;
                amounts.add(Amount.builder()
                        .unit(unit)
                        .quantity(Optional.ofNullable(amt.getQuantity()).orElse(BigInteger.ZERO))
                        .build());
            }
        }

        return Utxo.builder()
                .txHash(utxo.getTxHash())
                .outputIndex(utxo.getOutputIndex())
                .address(utxo.getAddress())
                .amount(amounts)
                .dataHash(utxo.getDataHash())
                .inlineDatum(utxo.getInlineDatum())
                .referenceScriptHash(utxo.getReferenceScriptHash())
                .build();
    }

    private String resolveUnit(String unit, String policyId) {
        if (StringUtils.hasText(unit)) {
            return unit;
        }

        if (StringUtils.hasText(policyId)) {
            return policyId;
        }

        return CardanoConstants.LOVELACE;
    }
}
