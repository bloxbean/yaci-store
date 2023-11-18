package com.bloxbean.cardano.yaci.store.api.utxo.service;

import com.bloxbean.cardano.client.address.Address;
import com.bloxbean.cardano.client.crypto.Bech32;
import com.bloxbean.cardano.client.util.HexUtil;
import com.bloxbean.cardano.yaci.store.common.domain.Utxo;
import com.bloxbean.cardano.yaci.store.common.model.Order;
import com.bloxbean.cardano.yaci.store.utxo.storage.UtxoStorageReader;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

import static com.bloxbean.cardano.yaci.store.api.utxo.service.UtxoUtil.addressUtxoToUtxo;
import static com.bloxbean.cardano.yaci.store.common.util.Bech32Prefixes.ADDR_PREFIX;
import static com.bloxbean.cardano.yaci.store.common.util.Bech32Prefixes.ADDR_VKEY_HASH_PREFIX;

@Component
@RequiredArgsConstructor
@Slf4j
public class AddressService {
    private final UtxoStorageReader utxoStorage;

    public List<Utxo> getUtxoByAddress(@NonNull String address, int page, int count, Order order) {
        return utxoStorage.findUtxoByAddress(address, page, count, order)
                .stream()
                .map(addressUtxo -> addressUtxoToUtxo(addressUtxo)).collect(Collectors.toList());
    }

    public List<Utxo> getUtxoByAddressAndAsset(@NonNull String address, String asset, int page, int count, Order order) {
        return utxoStorage.findUtxoByAddressAndAsset(address, asset, page, count, order)
                .stream()
                .map(addressUtxo -> addressUtxoToUtxo(addressUtxo)).collect(Collectors.toList());
    }

    public List<Utxo> getUtxoByPaymentCredential(@NonNull String addrOrPaymentCredOrVkh, int page, int count, Order order) {
        String paymentCred = getPaymentCredential(addrOrPaymentCredOrVkh);

        return utxoStorage.findUtxoByPaymentCredential(paymentCred, page, count, order)
                .stream()
                .map(addressUtxo -> addressUtxoToUtxo(addressUtxo)).collect(Collectors.toList());
    }

    public List<Utxo> getUtxoByPaymentCredentialAndAsset(@NonNull String addrOrPaymentCredOrVkh, String asset, int page, int count, Order order) {
        String paymentCred = getPaymentCredential(addrOrPaymentCredOrVkh);

        return utxoStorage.findUtxoByPaymentCredentialAndAsset(paymentCred, asset, page, count, order)
                .stream()
                .map(addressUtxo -> addressUtxoToUtxo(addressUtxo)).collect(Collectors.toList());
    }

    public List<Utxo> getUtxoByStakeAddress(@NonNull String stakeAddress, int page, int count, Order order) {
        return utxoStorage.findUtxoByStakeAddress(stakeAddress, page, count, order)
                .stream()
                .map(addressUtxo -> addressUtxoToUtxo(addressUtxo)).collect(Collectors.toList());
    }

    public List<Utxo> getUtxoByStakeAddressAndAsset(@NonNull String stakeAddress, String asset, int page, int count, Order order) {
        return utxoStorage.findUtxoByStakeAddressAndAsset(stakeAddress, asset, page, count, order)
                .stream()
                .map(addressUtxo -> addressUtxoToUtxo(addressUtxo)).collect(Collectors.toList());
    }

    private static String getPaymentCredential(String address) {
        String paymentCredential = null;
        if (address.startsWith(ADDR_VKEY_HASH_PREFIX)) {
            paymentCredential = HexUtil.encodeHexString(Bech32.decode(address).data);

        } else if (address.startsWith(ADDR_PREFIX)) {
            Address _address = new Address(address);
            paymentCredential = _address.getPaymentCredential()
                    .map(credential -> HexUtil.encodeHexString(credential.getBytes()))
                    .orElse(null);
        } else
            paymentCredential = address;

        return paymentCredential;
    }
}
