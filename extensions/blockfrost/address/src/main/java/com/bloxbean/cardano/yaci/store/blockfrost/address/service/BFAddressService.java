package com.bloxbean.cardano.yaci.store.blockfrost.address.service;

import com.bloxbean.cardano.client.address.Address;
import com.bloxbean.cardano.client.address.AddressProvider;
import com.bloxbean.cardano.client.address.AddressType;
import com.bloxbean.cardano.client.address.CredentialType;
import com.bloxbean.cardano.yaci.store.api.utxo.service.AddressService;
import com.bloxbean.cardano.yaci.store.api.utxo.service.UtxoUtil;
import com.bloxbean.cardano.yaci.store.blockfrost.address.dto.BFAddressDTO;
import com.bloxbean.cardano.yaci.store.blockfrost.address.dto.BFAddressTransactionDTO;
import com.bloxbean.cardano.yaci.store.blockfrost.address.dto.BFAddressUtxoDTO;
import com.bloxbean.cardano.yaci.store.blockfrost.address.mapper.BFAddressTransactionMapper;
import com.bloxbean.cardano.yaci.store.blockfrost.address.mapper.BFAddressUtxoMapper;
import com.bloxbean.cardano.yaci.store.common.domain.AddressUtxo;
import com.bloxbean.cardano.yaci.store.common.domain.Utxo;
import com.bloxbean.cardano.yaci.store.common.model.Order;
import com.bloxbean.cardano.yaci.store.utxo.domain.AddressTransaction;
import com.bloxbean.cardano.yaci.store.utxo.storage.UtxoStorage;
import com.bloxbean.cardano.yaci.store.utxo.storage.UtxoStorageReader;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class BFAddressService {

    private final UtxoStorageReader utxoStorageReader;

    public BFAddressService(UtxoStorageReader utxoStorageReader) {
        this.utxoStorageReader = utxoStorageReader;
    }

    public BFAddressDTO getAddressInfo(String address) {
        // Aggregate amounts by unit across all pages
        Map<String, BigInteger> amountMap = new HashMap<>();
        int page = 0;

        List<Utxo> utxos = utxoStorageReader.findAllUtxoByAddress(address)
                .stream()
                .map(UtxoUtil::addressUtxoToUtxo)
                .toList();

        System.out.println("Total utxos: " + utxos.size());

        utxos.stream()
                .flatMap(utxo -> utxo.getAmount().stream())
                .forEach(amount -> {
                    amountMap.merge(amount.getUnit(), amount.getQuantity(), BigInteger::add);
                });


        // Convert aggregated map to List<Utxo.Amount>, sorted with lovelace first, then alphabetically
        List<Utxo.Amount> aggregatedAmounts = amountMap.entrySet().stream()
                .sorted((e1, e2) -> {
                    String unit1 = e1.getKey();
                    String unit2 = e2.getKey();
                    // lovelace always comes first
                    if ("lovelace".equals(unit1)) return -1;
                    if ("lovelace".equals(unit2)) return 1;
                    // otherwise sort alphabetically
                    return unit1.compareTo(unit2);
                })
                .map(entry -> Utxo.Amount.builder()
                        .unit(entry.getKey())
                        .quantity(entry.getValue())
                        .build())
                .collect(Collectors.toList());

        // Derive stake address and address type
        String stakeAddress = null;
        String type = null;
        boolean isScript = false;

        try {
            Address addr = new Address(address);
            AddressType addressType = addr.getAddressType();

            // Get stake address for Base addresses
            if (addressType == AddressType.Base) {
                stakeAddress = AddressProvider.getStakeAddress(addr).getAddress();
            }

            // Map address type to Blockfrost type string
            type = mapAddressType(addressType);

            // Check if address uses script credential
            isScript = addr.getPaymentCredential()
                    .map(credential -> credential.getType() == CredentialType.Script)
                    .orElse(false);

        } catch (Exception e) {
            log.warn("Unable to parse address details for: {}", address, e);
        }

        return BFAddressDTO.builder()
                .address(address)
                .amount(aggregatedAmounts)
                .stake_address(stakeAddress)
                .type(type)
                .script(isScript)
                .build();
    }

    private String mapAddressType(AddressType addressType) {
        if (addressType == null) {
            return null;
        }
        if (addressType == AddressType.Byron) {
            return "byron";
        }
        return "shelley";
    }

    public List<BFAddressUtxoDTO> getAddressUtxos(@NonNull String address, int page, int count, Order order) {
        List<AddressUtxo> addressUtxos = utxoStorageReader.findUtxoByAddress(address, page, count, order);
        
        return addressUtxos.stream()
                .map(BFAddressUtxoMapper.INSTANCE::toBFAddressUtxoDTO)
                .collect(Collectors.toList());
    }

    public List<BFAddressUtxoDTO> getAddressUtxosForAsset(@NonNull String address, @NonNull String asset, int page, int count, Order order) {
        List<AddressUtxo> addressUtxos = utxoStorageReader.findUtxoByAddressAndAsset(address, asset, page, count, order);

        return addressUtxos.stream()
                .map(BFAddressUtxoMapper.INSTANCE::toBFAddressUtxoDTO)
                .collect(Collectors.toList());
    }

    public List<BFAddressTransactionDTO> getAddressTransactions(@NonNull String address, int page, int count, Order order) {
        List<AddressTransaction> addressUtxos = utxoStorageReader.findTransactionsByAddress(address, page, count, order);

        return addressUtxos.stream()
                .map(BFAddressTransactionMapper.INSTANCE::toBFAddressTransactionDTO)
                .collect(Collectors.toList());
    }
}
