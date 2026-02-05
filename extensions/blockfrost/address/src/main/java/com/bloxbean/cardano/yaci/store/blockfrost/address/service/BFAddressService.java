package com.bloxbean.cardano.yaci.store.blockfrost.address.service;

import com.bloxbean.cardano.client.address.Address;
import com.bloxbean.cardano.client.address.AddressProvider;
import com.bloxbean.cardano.client.address.AddressType;
import com.bloxbean.cardano.client.address.CredentialType;
import com.bloxbean.cardano.yaci.store.blockfrost.address.dto.BFAddressDTO;
import com.bloxbean.cardano.yaci.store.blockfrost.address.dto.BFAddressTotalDTO;
import com.bloxbean.cardano.yaci.store.blockfrost.address.dto.BFAddressTransactionDTO;
import com.bloxbean.cardano.yaci.store.blockfrost.address.dto.BFAddressUtxoDTO;
import com.bloxbean.cardano.yaci.store.blockfrost.address.mapper.BFAddressUtxoMapper;
import com.bloxbean.cardano.yaci.store.blockfrost.address.storage.BFAddressStorageReader;
import com.bloxbean.cardano.yaci.store.blockfrost.address.storage.impl.model.BFAddressTotal;
import com.bloxbean.cardano.yaci.store.common.domain.AddressUtxo;
import com.bloxbean.cardano.yaci.store.common.domain.Utxo;
import com.bloxbean.cardano.yaci.store.common.model.Order;
import com.bloxbean.cardano.yaci.store.utxo.storage.UtxoStorageReader;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigInteger;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class BFAddressService {

    private final UtxoStorageReader utxoStorageReader;
    private final BFAddressStorageReader bfAddressStorageReader;
    private final Environment environment;

    public BFAddressService(UtxoStorageReader utxoStorageReader,
                            BFAddressStorageReader bfAddressStorageReader,
                            Environment environment) {
        this.utxoStorageReader = utxoStorageReader;
        this.bfAddressStorageReader = bfAddressStorageReader;
        this.environment = environment;
    }

    public BFAddressDTO getAddressInfo(String address) {
        Map<String, BigInteger> amountMap = isCurrentBalanceEnabled()
                ? bfAddressStorageReader.findCurrentAddressBalanceByUnit(address)
                : bfAddressStorageReader.findUnspentAddressBalanceByUnit(address);
        Map<String, BigInteger> normalized = normalizeUnits(amountMap);
        List<Utxo.Amount> aggregatedAmounts = toAmountList(normalized);

        String stakeAddress = null;
        String type = null;
        boolean isScript = false;

        try {
            Address addr = new Address(address);
            AddressType addressType = addr.getAddressType();

            if (addressType == AddressType.Base) {
                stakeAddress = AddressProvider.getStakeAddress(addr).getAddress();
            }

            type = mapAddressType(addressType);

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

    public List<BFAddressTransactionDTO> getAddressTransactions(@NonNull String address, int page, int count, Order order,
                                                                String from, String to) {
        return bfAddressStorageReader.findAddressTransactions(address, page, count, order, from, to);
    }

    public List<String> getAddressTxs(@NonNull String address, int page, int count, Order order) {
        return bfAddressStorageReader.findTxHashesByAddress(address, page, count, order);
    }

    public BFAddressTotalDTO getAddressTotal(@NonNull String address) {
        return bfAddressStorageReader.getAddressTotal(address)
                .map(total -> toAddressTotalDto(address, total))
                .orElseGet(() -> emptyAddressTotal(address));
    }

    private BFAddressTotalDTO toAddressTotalDto(String address, BFAddressTotal total) {
        Map<String, BigInteger> received = normalizeUnits(total.receivedSum());
        Map<String, BigInteger> sent = normalizeUnits(total.sentSum());

        return BFAddressTotalDTO.builder()
                .address(address)
                .receivedSum(toAmountList(received))
                .sentSum(toAmountList(sent))
                .txCount(total.txCount())
                .build();
    }

    private BFAddressTotalDTO emptyAddressTotal(String address) {
        return BFAddressTotalDTO.builder()
                .address(address)
                .receivedSum(Collections.emptyList())
                .sentSum(Collections.emptyList())
                .txCount(0L)
                .build();
    }

    private boolean isCurrentBalanceEnabled() {
        boolean enabled = Boolean.parseBoolean(environment.getProperty("store.account.enabled", "false"));
        boolean currentEnabled = Boolean.parseBoolean(environment.getProperty("store.account.currentBalanceEnabled", "false"));
        return enabled && currentEnabled;
    }

    private Map<String, BigInteger> normalizeUnits(Map<String, BigInteger> amountMap) {
        if (amountMap == null || amountMap.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<String, BigInteger> normalized = new HashMap<>();
        for (Map.Entry<String, BigInteger> entry : amountMap.entrySet()) {
            String unit = normalizeUnit(entry.getKey());
            normalized.merge(unit, entry.getValue(), BigInteger::add);
        }
        return normalized;
    }

    private List<Utxo.Amount> toAmountList(Map<String, BigInteger> amountMap) {
        if (amountMap == null || amountMap.isEmpty()) {
            return Collections.emptyList();
        }

        return amountMap.entrySet().stream()
                .sorted((e1, e2) -> {
                    String unit1 = e1.getKey();
                    String unit2 = e2.getKey();
                    if ("lovelace".equals(unit1)) return -1;
                    if ("lovelace".equals(unit2)) return 1;
                    return unit1.compareTo(unit2);
                })
                .map(entry -> Utxo.Amount.builder()
                        .unit(entry.getKey())
                        .quantity(entry.getValue())
                        .build())
                .collect(Collectors.toList());
    }

    private String normalizeUnit(String unit) {
        if (unit == null) {
            return null;
        }
        return unit.contains(".") ? unit.replace(".", "") : unit;
    }
}
