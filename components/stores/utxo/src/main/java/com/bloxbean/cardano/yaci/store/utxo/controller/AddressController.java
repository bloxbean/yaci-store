package com.bloxbean.cardano.yaci.store.utxo.controller;


import com.bloxbean.cardano.client.api.model.Utxo;
import com.bloxbean.cardano.yaci.store.common.model.Order;
import com.bloxbean.cardano.yaci.store.utxo.service.AddressService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("${apiPrefix}/addresses")
public class AddressController {

    private AddressService addressService;

    public AddressController(AddressService addressService) {
        this.addressService = addressService;
    }

    @GetMapping("{address}/utxos")
    public List<Utxo> getUtxos(@PathVariable String address, @RequestParam(required = false, defaultValue = "10")  int count,
                               @RequestParam(required = false, defaultValue = "0")  int page, @RequestParam(required = false, defaultValue = "asc") Order order) {
        //TODO -- Fix pagination index
        int p = page;
        if (p > 0)
            p = p - 1;


        return addressService.getUtxoByAddress(address, p, count, order);
    }

    @GetMapping("{address}/utxos/{asset}")
    public List<Utxo> getUtxosForAsset(@PathVariable String address, @PathVariable String asset, @RequestParam(required = false, defaultValue = "10")  int count,
                               @RequestParam(required = false, defaultValue = "0")  int page, @RequestParam(required = false, defaultValue = "asc") Order order) {
        //TODO -- Fix pagination index
        int p = page;
        if (p > 0)
            p = p - 1;
        return addressService.getUtxoByAddressAndAsset(address, asset, p, count, order);
    }
}
