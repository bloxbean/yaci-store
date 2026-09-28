package com.bloxbean.cardano.yaci.store.api.core.controller;

import com.bloxbean.cardano.yaci.core.model.Era;
import com.bloxbean.cardano.yaci.store.api.core.dto.GenesisDto;
import com.bloxbean.cardano.yaci.store.common.config.StoreProperties;
import com.bloxbean.cardano.yaci.store.core.configuration.GenesisConfig;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController("GenesisController")
@RequestMapping("${apiPrefix:/api/v1}")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Network API", description = "APIs for Network information")
@ConditionalOnExpression("${store.core.endpoints.genesis.enabled:true}")
public class GenesisController {
    private final GenesisConfig genesisConfig;
    private final StoreProperties storeProperties;

    @GetMapping("/genesis")
    @Operation(description = "Get network genesis information (Blockfrost compatible shape). Use network_magic to verify which Cardano network this instance is serving.")
    public GenesisDto getGenesisInfo() {
        double slotDuration = genesisConfig.slotDuration(Era.Shelley);
        // Blockfrost defines slot_length as an integer number of seconds. Round fractional
        // devnet slot durations up so the response remains compatible with generated clients.
        long slotLength = Math.max(1L, (long) Math.ceil(slotDuration));

        return GenesisDto.builder()
                .activeSlotsCoefficient(genesisConfig.getActiveSlotsCoeff())
                .updateQuorum(genesisConfig.getUpdateQuorum())
                .maxLovelaceSupply(genesisConfig.getMaxLovelaceSupply() != null ?
                        genesisConfig.getMaxLovelaceSupply().toString() : null)
                .networkMagic(storeProperties.getProtocolMagic())
                .epochLength(genesisConfig.getEpochLength())
                .systemStart(genesisConfig.getStartTime(storeProperties.getProtocolMagic()))
                .slotsPerKesPeriod(genesisConfig.getSlotsPerKesPeriod())
                .slotLength(slotLength)
                .maxKesEvolutions(genesisConfig.getMaxKesEvolutions())
                .securityParam(genesisConfig.getSecurityParam())
                .build();
    }
}
