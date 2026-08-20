package com.bloxbean.cardano.yaci.store.blockfrost.network.service;

import com.bloxbean.cardano.yaci.core.model.Era;
import com.bloxbean.cardano.yaci.store.api.adapot.service.NetworkInfoApiService;
import com.bloxbean.cardano.yaci.store.blockfrost.network.dto.BFEraDto;
import com.bloxbean.cardano.yaci.store.blockfrost.network.dto.BFGenesisDto;
import com.bloxbean.cardano.yaci.store.blockfrost.network.dto.BFNetworkDto;
import com.bloxbean.cardano.yaci.store.blockfrost.network.dto.BFRootDto;
import com.bloxbean.cardano.yaci.store.blockfrost.network.mapper.BFNetworkMapper;
import com.bloxbean.cardano.yaci.store.blockfrost.network.storage.BFNetworkStorageReader;
import com.bloxbean.cardano.yaci.store.blocks.domain.Block;
import com.bloxbean.cardano.yaci.store.blocks.storage.BlockStorageReader;
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
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class BFNetworkService {

    private static final String BF_API_VERSION = "0.1.30";

    private final ObjectProvider<NetworkInfoApiService> networkInfoApiServiceProvider;
    private final ObjectProvider<BFNetworkStorageReader> bfNetworkStorageReaderProvider;
    private final ObjectProvider<BlockStorageReader> blockStorageReaderProvider;
    private final EraService eraService;
    private final GenesisConfig genesisConfig;
    private final StoreProperties storeProperties;
    private final BFNetworkMapper bfNetworkMapper;
    private final org.springframework.core.env.Environment environment;

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
        List<EraStart> eraTimeline = buildEraTimeline(cardanoEras);
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
        long byronEndSlot = eraTimeline.isEmpty() ? 0L : eraTimeline.get(0).startSlot();
        long byronEndTimeAbs = genesisStartTime + byronEndSlot * byronSlotLength;
        int byronEndEpoch = (int) (byronEndSlot / byronEpochLength);

        // ── Byron era (always epoch 0, slot 0) ───────────────────────────────
        if (!eraTimeline.isEmpty()) {
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
        for (int i = 0; i < eraTimeline.size(); i++) {
            EraStart era = eraTimeline.get(i);
            EraStart nextEra = (i + 1 < eraTimeline.size()) ? eraTimeline.get(i + 1) : null;

            // Missing eras reuse the next actual era's boundary and therefore become zero-length summaries.
            int startEpoch = eraService.getEpochNo(era.era(), era.startSlot());
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
                int endEpoch = eraService.getEpochNo(era.era(), nextEra.startSlot());
                long endSlot = eraService.getShelleyAbsoluteSlot(endEpoch, 0);
                long endTimeRel = eraService.blockTime(Era.Shelley, endSlot) - genesisStartTime;
                end = BFEraDto.EraBoundary.builder()
                        .time(endTimeRel)
                        .slot(endSlot)
                        .epoch(endEpoch)
                        .build();
            } else {
                end = projectCurrentEraEnd(
                        shelleyEpochLength,
                        shelleySafeZone,
                        genesisStartTime);
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

    /**
     * Expands observed post-Byron transitions into the canonical era order expected by HFC history.
     * <pre> Example:
     * Preview input:  Alonzo@0, Babbage@259200, Conway@55814400
     * Preview output: Shelley@0, Allegra@0, Mary@0, Alonzo@0,
     *                 Babbage@259200, Conway@55814400
     * </pre>
     * An era absent from storage reuses the next observed era's start slot, which makes it a
     * zero-length summary once adjacent boundaries are linked. The projection is in-memory only
     * and stops at the latest observed era, so it neither changes storage nor exposes future eras.
     */
    private List<EraStart> buildEraTimeline(List<CardanoEra> cardanoEras) {
        List<CardanoEra> actualEras = cardanoEras.stream()
                .filter(era -> era.getEra() != null && era.getEra() != Era.Byron)
                .sorted(Comparator.comparingInt(era -> era.getEra().getValue()))
                .toList();
        if (actualEras.isEmpty()) {
            return List.of();
        }

        int currentEraValue = actualEras.getLast().getEra().getValue();
        int actualEraIndex = 0;
        List<EraStart> timeline = new ArrayList<>();

        for (Era era : Arrays.stream(Era.values())
                .filter(candidate -> candidate != Era.Byron && candidate.getValue() <= currentEraValue)
                .sorted(Comparator.comparingInt(Era::getValue))
                .toList()) {
            // Keep the pointer at the first observed era at or after the canonical candidate.
            while (actualEraIndex < actualEras.size()
                    && actualEras.get(actualEraIndex).getEra().getValue() < era.getValue()) {
                actualEraIndex++;
            }
            if (actualEraIndex >= actualEras.size()) {
                break;
            }

            CardanoEra nextActualEra = actualEras.get(actualEraIndex);
            // For a skipped era, this is the next observed boundary and can be reused by later gaps.
            timeline.add(new EraStart(era, nextActualEra.getStartSlot()));
            if (nextActualEra.getEra() == era) {
                actualEraIndex++;
            }
        }

        return timeline;
    }

    private BFEraDto.EraBoundary projectCurrentEraEnd(long epochLength,
                                                       long safeZone,
                                                       long genesisStartTime) {
        Block latestBlock = Optional.ofNullable(blockStorageReaderProvider.getIfAvailable())
                .flatMap(BlockStorageReader::findRecentBlock)
                .filter(block -> block.getEpochNumber() != null && block.getEpochSlot() != null)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.SERVICE_UNAVAILABLE,
                        "Latest block information is not available."));

        int endEpoch = latestBlock.getEpochNumber()
                + (latestBlock.getEpochSlot() >= epochLength - safeZone ? 2 : 1);
        long endSlot = eraService.getShelleyAbsoluteSlot(endEpoch, 0);
        long endTimeRel = eraService.blockTime(Era.Shelley, endSlot) - genesisStartTime;

        return BFEraDto.EraBoundary.builder()
                .time(endTimeRel)
                .slot(endSlot)
                .epoch(endEpoch)
                .build();
    }

    private record EraStart(Era era, long startSlot) {
    }

    // ── /genesis ─────────────────────────────────────────────────────────────

    public BFGenesisDto getGenesis() {
        long protocolMagic = storeProperties.getProtocolMagic();

        return BFGenesisDto.builder()
                .activeSlotsCoefficient(genesisConfig.getActiveSlotsCoeff())
                .updateQuorum(genesisConfig.getUpdateQuorum())
                .maxLovelaceSupply(genesisConfig.getMaxLovelaceSupply().toString())
                .networkMagic(protocolMagic)
                .epochLength(genesisConfig.getEpochLength())
                .systemStart(genesisConfig.getStartTime(protocolMagic))
                .slotsPerKesPeriod(genesisConfig.getSlotsPerKesPeriod())
                .slotLength((int) genesisConfig.slotDuration(Era.Shelley))
                .maxKesEvolutions(genesisConfig.getMaxKesEvolutions())
                .securityParam(genesisConfig.getSecurityParam())
                .build();
    }

    // ── / (root) ─────────────────────────────────────────────────────────────

    public BFRootDto getRoot() {
        String hostname = environment.getProperty("blockfrost.hostname", "");
        String apiPrefix = environment.getProperty("blockfrost.apiPrefix", "");
        return BFRootDto.builder()
                .url(hostname + apiPrefix)
                .version(BF_API_VERSION)
                .build();
    }
}
