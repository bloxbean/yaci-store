package com.bloxbean.cardano.yaci.store.utxo.controller;


import com.bloxbean.cardano.client.api.model.Utxo;
import com.bloxbean.cardano.yaci.store.utxo.service.AddressService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/addresses")
public class AddressController {

    private AddressService addressService;

    public AddressController(AddressService addressService) {
        this.addressService = addressService;
    }

    @GetMapping("{address}/utxos")
    public List<Utxo> getUtxos(@PathVariable String address, @RequestParam(required = false, defaultValue = "10")  int count,
                               @RequestParam(required = false, defaultValue = "0")  int page, @RequestParam(required = false, defaultValue = "asc") String order) {

        return addressService.getUnspentUtxos(address, page, count, order);
    }
}
