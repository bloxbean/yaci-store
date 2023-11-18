package com.bloxbean.cardano.yaci.store.api.account.controller;

import com.bloxbean.cardano.yaci.store.api.account.dto.AddressAssetBalanceDto;
import com.bloxbean.cardano.yaci.store.api.account.service.AddressAssetService;
import com.bloxbean.cardano.yaci.store.common.model.Order;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController("AddressAssetController")
@RequestMapping("${apiPrefix}")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Asset API", description = "APIs for address asset related operations.")
public class AddressAssetController {
    private final AddressAssetService addressService;

    @GetMapping("/assets/{unit}/addresses")
    @Operation(description = "Get addresses by asset")
    public List<AddressAssetBalanceDto> getAddressesByAsset(@PathVariable String unit,
                                                            @RequestParam(name = "page", defaultValue = "0") int page,
                                                            @RequestParam(name = "count", defaultValue = "10") int count,
                                                            @RequestParam(name = "sort", defaultValue = "asc") String sort) {
        //TODO -- Fix pagination index
        int p = page;
        if (p > 0) p = p - 1;

        if (count > 100) throw new IllegalArgumentException("Max no of records allowed is 100");

        return addressService.getAddressesByAsset(unit, page, count, Order.valueOf(sort));
    }
}
