package com.bloxbean.cardano.yaci.store.api.adapot.controller;

import com.bloxbean.cardano.yaci.store.api.adapot.dto.NetworkInfoDto;
import com.bloxbean.cardano.yaci.store.api.adapot.service.NetworkInfoApiService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController("NetworkController")
@RequestMapping("${apiPrefix}")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Network API", description = "APIs for Network information")
public class NetworkController {
    private final NetworkInfoApiService networkInfoService;

    @GetMapping("/network")
    @Operation(description = "Get Network information (Supply and Stake)")
    public NetworkInfoDto getNetworkInfo() {
        return networkInfoService.getNetworkInfo()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Network info not found"));
    }

    @GetMapping("/network/epochs/{epoch}")
    @Operation(description = "Get Network information (Supply and Stake) for an epoch")
    public NetworkInfoDto getNetworkInfoForEpoch(@PathVariable Integer epoch) {
        return networkInfoService.getNetworkInfo(epoch)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Network info not found for epoch " + epoch));
    }

}
