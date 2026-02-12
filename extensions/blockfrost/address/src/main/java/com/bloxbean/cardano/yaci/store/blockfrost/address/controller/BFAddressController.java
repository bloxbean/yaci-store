package com.bloxbean.cardano.yaci.store.blockfrost.address.controller;


import com.bloxbean.cardano.yaci.store.blockfrost.address.dto.BFAddressDTO;
import com.bloxbean.cardano.yaci.store.blockfrost.address.dto.BFAddressTotalDTO;
import com.bloxbean.cardano.yaci.store.blockfrost.address.dto.BFAddressTransactionDTO;
import com.bloxbean.cardano.yaci.store.blockfrost.address.dto.BFAddressUtxoDTO;
import com.bloxbean.cardano.yaci.store.blockfrost.address.service.BFAddressService;
import com.bloxbean.cardano.yaci.store.common.model.Order;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.PostConstruct;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@Tag(name = "Blockfrost Addresses")
@RequestMapping("${blockfrost.apiPrefix}/addresses")
@ConditionalOnExpression("${store.extensions.blockfrost.address.enabled:false}")
public class BFAddressController {

    private final BFAddressService bfAddressService;

    @PostConstruct
    public void postConstruct() {
        log.info("Blockfrost AddressController initialized >>>");
    }

    @GetMapping("{address}")
    @Operation(summary = "Specific address", description = "Obtain information about a specific address.")
    public BFAddressDTO getAddressInfo(@PathVariable String address) {
        return bfAddressService.getAddressInfo(address);
    }

    //TODO: Need to create a new DTO to return extended information to the client.
    @GetMapping("{address}/extended")
    @Operation(summary = "Extended information of a specific address", description = "Obtain extended information about a specific address.")
    public BFAddressDTO getExtendedAddressInfo(@PathVariable String address) {
        return bfAddressService.getAddressInfo(address);
    }

    @GetMapping("{address}/total")
    @Operation(summary = "Address details", description = "Obtain details about an address.")
    public BFAddressTotalDTO getAddressTotal(@PathVariable String address) {
        return bfAddressService.getAddressTotal(address);
    }

    @GetMapping("{address}/utxos")
    @Operation(summary = "Address UTXOs", description = "UTXOs of the address.")
    public List<BFAddressUtxoDTO> getAddressUtxos(@PathVariable String address,
                                                   @RequestParam(required = false, defaultValue = "100") @Min(1) @Max(100) int count,
                                                   @RequestParam(required = false, defaultValue = "1") @Min(1) int page,
                                                   @RequestParam(required = false, defaultValue = "asc") Order order) {
        int p = page - 1;

        return bfAddressService.getAddressUtxos(address, p, count, order);
    }

    @GetMapping("{address}/utxos/{asset}")
    @Operation(summary = "Address UTXOs of a given asset", description = "UTXOs of the address.")
    public List<BFAddressUtxoDTO> getAddressUtxosForAsset(@PathVariable String address, @PathVariable String asset,
                                                  @RequestParam(required = false, defaultValue = "100") @Min(1) @Max(100) int count,
                                                  @RequestParam(required = false, defaultValue = "1") @Min(1) int page,
                                                  @RequestParam(required = false, defaultValue = "asc") Order order) {
        int p = page - 1;

        return bfAddressService.getAddressUtxosForAsset(address, asset, p, count, order);
    }

    @GetMapping("{address}/transactions")
    @Operation(summary = "Address transactions", description = "Transactions on the address.")
    public List<BFAddressTransactionDTO> getAddressTransactions(@PathVariable String address,
                                                                @RequestParam(required = false, defaultValue = "100") @Min(1) @Max(100) int count,
                                                                @RequestParam(required = false, defaultValue = "1") @Min(1) int page,
                                                                @RequestParam(required = false, defaultValue = "asc") Order order,
                                                                @RequestParam(required = false) String from,
                                                                @RequestParam(required = false) String to) {
        int p = page - 1;

        return bfAddressService.getAddressTransactions(address, p, count, order, from, to);
    }


}
