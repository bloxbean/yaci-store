package com.bloxbean.cardano.yaci.store.api.utxo.controller;

import com.bloxbean.cardano.yaci.store.api.utxo.service.AddressService;
import com.bloxbean.cardano.yaci.store.common.domain.Utxo;
import com.bloxbean.cardano.yaci.store.common.model.Order;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.bloxbean.cardano.yaci.store.common.util.Bech32Prefixes.ADDR_VKEY_HASH_PREFIX;
import static com.bloxbean.cardano.yaci.store.common.util.Bech32Prefixes.STAKE_ADDR_PREFIX;

@RestController
@Tag(name = "Address Service")
@RequestMapping("${apiPrefix}/addresses")
@ConditionalOnExpression("${store.utxo.endpoints.address.enabled:true}")
public class AddressController {

    private final AddressService addressService;

    public AddressController(AddressService addressService) {
        this.addressService = addressService;
    }

    @GetMapping("{address}/utxos")
    @Operation(summary = "Get UTxOs for an address or address verification key hash (addr_vkh). If the address is a stake address, it will return UTXOs for all base addresses associated with the stake address")
    public List<Utxo> getUtxos(@PathVariable String address, @RequestParam(required = false, defaultValue = "10") @Min(1) @Max(100) int count,
                               @RequestParam(required = false, defaultValue = "0") @Min(0) int page, @RequestParam(required = false, defaultValue = "asc") Order order) {
        //TODO -- Fix pagination index
        int p = page;
        if (p > 0)
            p = p - 1;

        if (address.startsWith(ADDR_VKEY_HASH_PREFIX)) { //By payment verification key hash
            return addressService.getUtxoByPaymentCredential(address, p, count, order);
        } else if (address.startsWith(STAKE_ADDR_PREFIX)) { //stake address
            return addressService.getUtxoByStakeAddress(address, p, count, order);
        } else { //By address
            return addressService.getUtxoByAddress(address, p, count, order);
        }
    }

    @GetMapping("{address}/utxos/{asset}")
    @Operation(summary = "Get UTxOs for an address or address verification key hash (addr_vkh) for a specific asset. If the address is a stake address, it will return UTXOs for all base addresses associated with the stake address")
    public List<Utxo> getUtxosForAsset(@PathVariable String address, @PathVariable String asset, @RequestParam(required = false, defaultValue = "10") @Min(1) @Max(100) int count,
                                       @RequestParam(required = false, defaultValue = "0") @Min(0) int page, @RequestParam(required = false, defaultValue = "asc") Order order) {
        //TODO -- Fix pagination index
        int p = page;
        if (p > 0)
            p = p - 1;

        if (address.startsWith(ADDR_VKEY_HASH_PREFIX)) { //By payment verification key hash
            return addressService.getUtxoByPaymentCredentialAndAsset(address, asset, p, count, order);
        } else if (address.startsWith(STAKE_ADDR_PREFIX)) { //stake address
            return addressService.getUtxoByStakeAddressAndAsset(address, asset, p, count, order);
        } else { //By address
            return addressService.getUtxoByAddressAndAsset(address, asset, p, count, order);
        }
    }
}
