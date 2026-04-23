package com.bloxbean.cardano.yaci.store.blockfrost.network.controller;

import com.bloxbean.cardano.yaci.store.blockfrost.network.dto.BFEraDto;
import com.bloxbean.cardano.yaci.store.blockfrost.network.dto.BFGenesisDto;
import com.bloxbean.cardano.yaci.store.blockfrost.network.dto.BFNetworkDto;
import com.bloxbean.cardano.yaci.store.blockfrost.network.dto.BFRootDto;
import com.bloxbean.cardano.yaci.store.blockfrost.network.service.BFNetworkService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@Tag(name = "Blockfrost Network")
@RequestMapping("${blockfrost.apiPrefix}")
@ConditionalOnExpression("${store.extensions.blockfrost.network.enabled:false}")
public class BFNetworkController {

    private final BFNetworkService bfNetworkService;

    @PostConstruct
    public void postConstruct() {
        log.info("Blockfrost NetworkController initialized >>>");
    }

    @GetMapping
    @Operation(summary = "Root endpoint", description = "Root endpoint.")
    public BFRootDto getRoot() {
        return bfNetworkService.getRoot();
    }

    @GetMapping("/network")
    @Operation(
            summary = "Network information",
            description = "Return detailed network information including supply and stake statistics.")
    public BFNetworkDto getNetworkInfo() {
        return bfNetworkService.getNetworkInfo();
    }

    @GetMapping("/network/eras")
    @Operation(
            summary = "Blockchain eras",
            description = "Return summaries of all blockchain eras: start/end boundaries and era parameters.")
    public List<BFEraDto> getNetworkEras() {
        return bfNetworkService.getNetworkEras();
    }

    @GetMapping("/genesis")
    @Operation(
            summary = "Blockchain genesis",
            description = "Return the blockchain genesis parameters.")
    public BFGenesisDto getGenesis() {
        return bfNetworkService.getGenesis();
    }
}
