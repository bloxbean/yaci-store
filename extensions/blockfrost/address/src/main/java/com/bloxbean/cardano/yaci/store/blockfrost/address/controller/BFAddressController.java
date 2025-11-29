package com.bloxbean.cardano.yaci.store.blockfrost.address.controller;


import com.bloxbean.cardano.yaci.store.blockfrost.address.dto.BFAddressDTO;
import com.bloxbean.cardano.yaci.store.blockfrost.address.service.BFAddressService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@Tag(name = "Blockfrost Addresses")
@RequestMapping("${blockfrost.apiPrefix}/addresses")
@ConditionalOnExpression("${store.extensions.blockfrost.address.enabled:true}")
public class BFAddressController {

    private final BFAddressService bfAddressService;

    @PostConstruct
    public void postConstruct() {
        log.info("Blockfrost AddressController initialized >>>");
    }

    @GetMapping("{address}")
    public BFAddressDTO getAddressInfo(@PathVariable String address) {
        return bfAddressService.getAddressInfo(address);
    }
}
