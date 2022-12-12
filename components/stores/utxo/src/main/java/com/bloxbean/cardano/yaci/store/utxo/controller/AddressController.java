package com.bloxbean.cardano.yaci.store.utxo.controller;


import com.bloxbean.cardano.client.api.model.Utxo;
import com.bloxbean.cardano.yaci.store.utxo.service.AddressService;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.List;

@RestController
@RequestMapping("/addresses")
public class AddressController {

    private AddressService addressService;

    public AddressController(AddressService addressService) {
        this.addressService = addressService;
    }

    @GetMapping("{address}/utxos")
    public Flux<Utxo> getUtxos(@PathVariable String address, @RequestParam(required = false, defaultValue = "10")  int count,
                               @RequestParam(required = false, defaultValue = "0")  int page, @RequestParam(required = false, defaultValue = "asc") String order) {

        List<Utxo> utxos = addressService.getUnspentUtxos(address, page, count, order);
        return Flux.fromStream(utxos.stream());
    }
}
