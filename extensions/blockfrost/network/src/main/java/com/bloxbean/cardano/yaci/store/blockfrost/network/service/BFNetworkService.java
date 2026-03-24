package com.bloxbean.cardano.yaci.store.blockfrost.network.service;

import com.bloxbean.cardano.yaci.core.model.Era;
import com.bloxbean.cardano.yaci.store.api.adapot.service.NetworkInfoApiService;
import com.bloxbean.cardano.yaci.store.blockfrost.network.dto.BFEraDto;
import com.bloxbean.cardano.yaci.store.blockfrost.network.dto.BFGenesisDto;
import com.bloxbean.cardano.yaci.store.blockfrost.network.dto.BFNetworkDto;
import com.bloxbean.cardano.yaci.store.blockfrost.network.dto.BFRootDto;
import com.bloxbean.cardano.yaci.store.blockfrost.network.mapper.BFNetworkMapper;
import com.bloxbean.cardano.yaci.store.common.config.StoreProperties;
import com.bloxbean.cardano.yaci.store.core.configuration.GenesisConfig;
import com.bloxbean.cardano.yaci.store.core.domain.CardanoEra;
import com.bloxbean.cardano.yaci.store.core.service.EraService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class BFNetworkService {

    private static final String BF_API_VERSION = "0.1.30";

    // Standard Cardano protocol constants (identical across mainnet, preprod, preview)
    private static final long SLOTS_PER_KES_PERIOD = 129600L;
    private static final int MAX_KES_EVOLUTIONS = 62;
    private static final int UPDATE_QUORUM = 5;

    private final ObjectProvider<NetworkInfoApiService> networkInfoApiServiceProvider;
    private final EraService eraService;
    private final GenesisConfig genesisConfig;
    private final StoreProperties storeProperties;
    private final BFNetworkMapper bfNetworkMapper;

    // ── /network ─────────────────────────────────────────────────────────────

    public BFNetworkDto getNetworkInfo() {
        NetworkInfoApiService networkInfoApiService = networkInfoApiServiceProvider.getIfAvailable();
        if (networkInfoApiService == null) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Network info service not available. Enable the adapot aggregate.");
        }

        return networkInfoApiService.getNetworkInfo()
                .map(bfNetworkMapper::toBFNetworkDto)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Network information not found."));
    }

    // ── /network/eras ─────────────────────────────────────────────────────────

    public List<BFEraDto> getNetworkEras() {
        List<CardanoEra> cardanoEras = eraService.getEras();
        List<BFEraDto> result = new ArrayList<>();

        long protocolMagic = storeProperties.getProtocolMagic();
        long genesisStartTime = genesisConfig.getStartTime(protocolMagic);
        long byronSlotLength = (long) genesisConfig.slotDuration(Era.Byron);
        long byronEpochLength = genesisConfig.slotsPerEpoch(Era.Byron);
        long shelleyEpochLength = genesisConfig.slotsPerEpoch(Era.Shelley);
        int shelleySlotLength = (int) genesisConfig.slotDuration(Era.Shelley);
        int securityParam = genesisConfig.getSecurityParam();
        double activeSlotsCoeff = genesisConfig.getActiveSlotsCoeff();

        // Safe-zone formulas derived from Cardano protocol constants
        long byronSafeZone = (long) (securityParam * 2.0 / 5.0);
        long shelleySafeZone = (long) (3.0 * securityParam / activeSlotsCoeff);

        // ── Byron era (always epoch 0, slot 0) ───────────────────────────────
        if (!cardanoEras.isEmpty()) {
            CardanoEra firstNonByron = cardanoEras.get(0);
            long byronEndSlot = firstNonByron.getStartSlot() - 1;
            long byronEndTime = genesisStartTime + byronEndSlot * byronSlotLength;
            int byronEndEpoch = (int) (byronEndSlot / byronEpochLength);

            BFEraDto byronEra = BFEraDto.builder()
                    .start(BFEraDto.EraBoundary.builder()
                            .time(genesisStartTime)
                            .slot(0L)
                            .epoch(0)
                            .build())
                    .end(BFEraDto.EraBoundary.builder()
                            .time(byronEndTime)
                            .slot(byronEndSlot)
                            .epoch(byronEndEpoch)
                            .build())
                    .parameters(BFEraDto.EraParameters.builder()
                            .epochLength(byronEpochLength)
                            .slotLength((int) byronSlotLength)
                            .safeZone(byronSafeZone)
                            .build())
                    .build();
            result.add(byronEra);
        }

        // ── Post-Byron eras ──────────────────────────────────────────────────
        for (int i = 0; i < cardanoEras.size(); i++) {
            CardanoEra era = cardanoEras.get(i);
            CardanoEra nextEra = (i + 1 < cardanoEras.size()) ? cardanoEras.get(i + 1) : null;

            long startSlot = era.getStartSlot();
            long startTime = eraService.blockTime(era.getEra(), startSlot);
            int startEpoch = eraService.getEpochNo(era.getEra(), startSlot);

            BFEraDto.EraBoundary start = BFEraDto.EraBoundary.builder()
                    .time(startTime)
                    .slot(startSlot)
                    .epoch(startEpoch)
                    .build();

            BFEraDto.EraBoundary end = null;
            if (nextEra != null) {
                long endSlot = nextEra.getStartSlot() - 1;
                long endTime = eraService.blockTime(era.getEra(), endSlot);
                int endEpoch = eraService.getEpochNo(era.getEra(), endSlot);
                end = BFEraDto.EraBoundary.builder()
                        .time(endTime)
                        .slot(endSlot)
                        .epoch(endEpoch)
                        .build();
            }

            BFEraDto.EraParameters parameters = BFEraDto.EraParameters.builder()
                    .epochLength(shelleyEpochLength)
                    .slotLength(shelleySlotLength)
                    .safeZone(shelleySafeZone)
                    .build();

            result.add(BFEraDto.builder()
                    .start(start)
                    .end(end)
                    .parameters(parameters)
                    .build());
        }

        return result;
    }

    // ── /genesis ─────────────────────────────────────────────────────────────

    public BFGenesisDto getGenesis() {
        long protocolMagic = storeProperties.getProtocolMagic();

        return BFGenesisDto.builder()
                .activeSlotsCoefficient(genesisConfig.getActiveSlotsCoeff())
                .updateQuorum(UPDATE_QUORUM)
                .maxLovelaceSupply(genesisConfig.getMaxLovelaceSupply().toString())
                .networkMagic(protocolMagic)
                .epochLength(genesisConfig.getEpochLength())
                .systemStart(genesisConfig.getStartTime(protocolMagic))
                .slotsPerKesPeriod(SLOTS_PER_KES_PERIOD)
                .slotLength((int) genesisConfig.slotDuration(Era.Shelley))
                .maxKesEvolutions(MAX_KES_EVOLUTIONS)
                .securityParam(genesisConfig.getSecurityParam())
                .build();
    }

    // ── / (root) ─────────────────────────────────────────────────────────────

    public BFRootDto getRoot() {
        return BFRootDto.builder()
                .url("https://cardano-mainnet.blockfrost.io/api")
                .version(BF_API_VERSION)
                .build();
    }
}
