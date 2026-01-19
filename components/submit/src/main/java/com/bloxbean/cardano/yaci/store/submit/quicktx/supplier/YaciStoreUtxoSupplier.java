package com.bloxbean.cardano.yaci.store.submit.quicktx.supplier;

import com.bloxbean.cardano.client.address.Address;
import com.bloxbean.cardano.client.api.UtxoSupplier;
import com.bloxbean.cardano.client.api.common.OrderEnum;
import com.bloxbean.cardano.client.api.model.Amount;
import com.bloxbean.cardano.client.api.model.Utxo;
import com.bloxbean.cardano.client.api.util.AssetUtil;
import com.bloxbean.cardano.client.common.CardanoConstants;
import com.bloxbean.cardano.client.exception.AddressRuntimeException;
import com.bloxbean.cardano.yaci.store.common.domain.AddressUtxo;
import com.bloxbean.cardano.yaci.store.common.domain.Amt;
import com.bloxbean.cardano.yaci.store.common.model.Order;
import com.bloxbean.cardano.yaci.store.utxo.storage.UtxoStorageReader;
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
 * {@link UtxoSupplier} backed by the Yaci Store UTXO tables.
 * This supplier avoids external services when building transactions with QuickTx.
 */
@Slf4j
@RequiredArgsConstructor
public class YaciStoreUtxoSupplier implements UtxoSupplier {

    private final UtxoStorageReader utxoStorageReader;
    private volatile boolean searchByAddressVkh;

    @Override
    public List<Utxo> getPage(String address, Integer nrOfItems, Integer page, OrderEnum order) {
        if (!StringUtils.hasText(address)) {
            return Collections.emptyList();
        }

        int size = Optional.ofNullable(nrOfItems).orElse(DEFAULT_NR_OF_ITEMS_TO_FETCH);
        int pageNumber = Optional.ofNullable(page).orElse(0);
        Order sortOrder = OrderEnum.desc.equals(order) ? Order.desc : Order.asc;

        List<AddressUtxo> utxos;
        if (searchByAddressVkh || isAddrVkh(address)) {
            String paymentCredential = resolvePaymentCredential(address);
            if (!StringUtils.hasText(paymentCredential)) {
                log.debug("Unable to resolve payment credential for {}", address);
                return Collections.emptyList();
            }
            utxos = utxoStorageReader.findUtxoByPaymentCredential(paymentCredential, pageNumber, size, sortOrder);
        } else {
            utxos = utxoStorageReader.findUtxoByAddress(address.trim(), pageNumber, size, sortOrder);
        }

        return utxos.stream()
                .map(this::toUtxo)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<Utxo> getTxOutput(String txHash, int outputIndex) {
        if (!StringUtils.hasText(txHash)) {
            return Optional.empty();
        }

        return utxoStorageReader.findById(txHash, outputIndex)
                .map(this::toUtxo);
    }

    @Override
    public boolean isUsedAddress(Address address) {
        if (address == null) {
            return false;
        }

        List<AddressUtxo> utxos = utxoStorageReader.findUtxoByAddress(address.toBech32(), 0, 1, Order.asc);
        return !utxos.isEmpty();
    }

    @Override
    public void setSearchByAddressVkh(boolean flag) {
        this.searchByAddressVkh = flag;
    }

    private boolean isAddrVkh(String address) {
        return StringUtils.hasText(address) && address.startsWith(Address.ADDR_VKH_PREFIX);
    }

    private String resolvePaymentCredential(String value) {
        String trimmed = value != null ? value.trim() : null;
        if (!StringUtils.hasText(trimmed)) {
            return null;
        }

        if (trimmed.startsWith(Address.ADDR_VKH_PREFIX)) {
            return trimmed;
        }

        try {
            return new Address(trimmed).getBech32VerificationKeyHash().orElse(null);
        } catch (AddressRuntimeException ex) {
            log.warn("Failed to derive payment credential from address {}", trimmed, ex);
            return null;
        }
    }

    private Utxo toUtxo(AddressUtxo addressUtxo) {
        List<Amount> amounts = new ArrayList<>();
        BigInteger lovelace = Optional.ofNullable(addressUtxo.getLovelaceAmount()).orElse(BigInteger.ZERO);
        amounts.add(Amount.lovelace(lovelace));

        if (!CollectionUtils.isEmpty(addressUtxo.getAmounts())) {
            for (Amt amt : addressUtxo.getAmounts()) {
                if (amt == null || amt.getQuantity() == null) {
                    continue;
                }

                String unit = resolveUnit(amt);
                if (CardanoConstants.LOVELACE.equals(unit)) {
                    continue;
                }

                amounts.add(Amount.asset(unit, amt.getQuantity()));
            }
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

    private String resolveUnit(Amt amt) {
        if (StringUtils.hasText(amt.getUnit())) {
            return amt.getUnit();
        }

        if (StringUtils.hasText(amt.getPolicyId())) {
            return AssetUtil.getUnit(amt.getPolicyId(), amt.getAssetName());
        }

        return CardanoConstants.LOVELACE;
    }
}
