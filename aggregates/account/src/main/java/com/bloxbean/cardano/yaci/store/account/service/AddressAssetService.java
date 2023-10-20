package com.bloxbean.cardano.yaci.store.account.service;

import com.bloxbean.cardano.yaci.store.account.dto.AddressAssetBalanceDto;
import com.bloxbean.cardano.yaci.store.account.storage.AccountBalanceStorage;
import com.bloxbean.cardano.yaci.store.common.model.Order;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AddressAssetService {
    private final AccountBalanceStorage accountBalanceStorage;

    public List<AddressAssetBalanceDto> getAddressesByAsset(String unit, int page, int count, Order sort) {

        return accountBalanceStorage.getAddressesByAsset(unit, page, count, sort)
                .stream().map(addressAssetBalance -> new AddressAssetBalanceDto(addressAssetBalance.getAddress(),
                        String.valueOf(addressAssetBalance.getQuantity()))).toList();
    }
}
