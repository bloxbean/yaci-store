package com.bloxbean.cardano.yaci.store.blockfrost.network.service;

import com.bloxbean.cardano.yaci.core.model.Era;
import com.bloxbean.cardano.yaci.store.api.adapot.service.NetworkInfoApiService;
import com.bloxbean.cardano.yaci.store.blockfrost.network.dto.BFEraDto;
import com.bloxbean.cardano.yaci.store.blockfrost.network.dto.BFGenesisDto;
import com.bloxbean.cardano.yaci.store.blockfrost.network.dto.BFNetworkDto;
import com.bloxbean.cardano.yaci.store.blockfrost.network.dto.BFRootDto;
import com.bloxbean.cardano.yaci.store.blockfrost.network.mapper.BFNetworkMapper;
import com.bloxbean.cardano.yaci.store.blockfrost.network.storage.BFNetworkStorageReader;
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

import java.math.BigInteger;
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
    private final ObjectProvider<BFNetworkStorageReader> bfNetworkStorageReaderProvider;
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

        BFNetworkDto dto = networkInfoApiService.getNetworkInfo()
                .map(bfNetworkMapper::toBFNetworkDto)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Network information not found."));

        // Enrich with UTxO-based locked supply, circulating supply, and live stake
        BFNetworkStorageReader storageReader = bfNetworkStorageReaderProvider.getIfAvailable();
        if (storageReader != null) {
            BFNetworkDto.Supply supply = dto.getSupply();
            if (supply != null) {
                // locked = SUM(lovelace) from unspent UTxOs at script addresses
                BigInteger locked = storageReader.getLockedSupply();
                supply.setLocked(locked.toString());

                // circulating = UTxO sum + spendable rewards + spendable reward_rest - withdrawals
                // Uses the same formula as Blockfrost (cardano-db-sync backend)
                int currentEpoch = storageReader.getCurrentEpoch();
                BigInteger circulating = storageReader.getCirculatingSupply(currentEpoch);
                supply.setCirculating(circulating.toString());
            }

            BFNetworkDto.Stake stake = dto.getStake();
            if (stake != null) {
                // live = latest epoch_stake snapshot total (approximation)
                BigInteger liveStake = storageReader.getLiveStake();
                stake.setLive(liveStake.toString());
            }
        }

        return dto;
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

        // Safe-zone: Byron = 2k, Shelley = 3k/f  (Cardano consensus constants)
        long byronSafeZone = securityParam * 2L;
        long shelleySafeZone = (long) (3.0 * securityParam / activeSlotsCoeff);

        // Compute Byron→Shelley boundary (byronEndSlot is the actual first Shelley slot,
        // which on all known networks coincides with the theoretical epoch boundary)
        long byronEndSlot = cardanoEras.isEmpty() ? 0L : cardanoEras.get(0).getStartSlot();
        long byronEndTimeAbs = genesisStartTime + byronEndSlot * byronSlotLength;
        int byronEndEpoch = (int) (byronEndSlot / byronEpochLength);

        // ── Byron era (always epoch 0, slot 0) ───────────────────────────────
        if (!cardanoEras.isEmpty()) {
            BFEraDto byronEra = BFEraDto.builder()
                    .start(BFEraDto.EraBoundary.builder()
                            .time(0L)
                            .slot(0L)
                            .epoch(0)
                            .build())
                    .end(BFEraDto.EraBoundary.builder()
                            .time(byronEndTimeAbs - genesisStartTime)
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

            // epoch number derived from the actual first-block slot is reliable
            int startEpoch = eraService.getEpochNo(era.getEra(), era.getStartSlot());
            // getShelleyAbsoluteSlot(epoch, 0) gives the theoretical epoch-boundary slot,
            // matching Blockfrost which uses protocol-parameter values, not actual first-block slots
            long startSlot = eraService.getShelleyAbsoluteSlot(startEpoch, 0);
            long startTimeRel = eraService.blockTime(Era.Shelley, startSlot) - genesisStartTime;

            BFEraDto.EraBoundary start = BFEraDto.EraBoundary.builder()
                    .time(startTimeRel)
                    .slot(startSlot)
                    .epoch(startEpoch)
                    .build();

            BFEraDto.EraBoundary end = null;
            if (nextEra != null) {
                int endEpoch = eraService.getEpochNo(era.getEra(), nextEra.getStartSlot());
                long endSlot = eraService.getShelleyAbsoluteSlot(endEpoch, 0);
                long endTimeRel = eraService.blockTime(Era.Shelley, endSlot) - genesisStartTime;
                end = BFEraDto.EraBoundary.builder()
                        .time(endTimeRel)
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
