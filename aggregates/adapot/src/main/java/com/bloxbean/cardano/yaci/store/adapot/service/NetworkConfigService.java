package com.bloxbean.cardano.yaci.store.adapot.service;

import com.bloxbean.cardano.yaci.core.model.Era;
import com.bloxbean.cardano.yaci.store.adapot.snapshot.UtxoSnapshotService;
import com.bloxbean.cardano.yaci.store.common.util.JsonUtil;
import com.bloxbean.cardano.yaci.store.core.configuration.GenesisConfig;
import com.bloxbean.cardano.yaci.store.core.service.EraService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.cardanofoundation.rewards.calculation.config.NetworkConfig;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.BigInteger;

@Component
@RequiredArgsConstructor
@Slf4j
public class NetworkConfigService {
    private final EraService eraService;
    private final GenesisConfig genesisConfig;
    private final UtxoSnapshotService utxoSnapshotService;

    public NetworkConfig getNetworkConfig(int protocolMagic, int epoch) {
        if (protocolMagic == NetworkConfig.MAINNET_NETWORK_MAGIC) {
            return NetworkConfig.getMainnetConfig();
        } else {
            var shelleyStartEpoch = eraService.getFirstNonByronEpoch().orElse(Integer.MAX_VALUE);
            var allegraStartEpoch = eraService.getEras()
                    .stream().filter(cardanoEra -> cardanoEra.getEra().value >= Era.Allegra.value)
                    .findFirst()
                    .map(cardanoEra -> {
                        var startSlot = cardanoEra.getStartSlot();
                        return eraService.getEpochNo(Era.Allegra, startSlot);
                    }).orElse(Integer.MAX_VALUE);

            var babbageStartEpoch = eraService.getEras()
                    .stream().filter(cardanoEra -> cardanoEra.getEra().value >= Era.Babbage.value)
                    .findFirst()
                    .map(cardanoEra -> {
                        var startSlot = cardanoEra.getStartSlot();
                        return eraService.getEpochNo(Era.Babbage, startSlot);
                    }).orElse(Integer.MAX_VALUE);

            BigInteger shelleyInitialReserves = BigInteger.ZERO;
            BigInteger shelleyInitialUtxo = BigInteger.ZERO;
            BigInteger boostrapAddressAmount = BigInteger.ZERO;

            if (epoch == shelleyStartEpoch) {
                //Get shelley initial reserves
                shelleyInitialUtxo = utxoSnapshotService.getTotalUtxosInEpoch(epoch, eraService.getFirstNonByronSlot());
                shelleyInitialReserves = genesisConfig.getMaxLovelaceSupply().subtract(shelleyInitialUtxo);
            }

            if (epoch == allegraStartEpoch) {
                boostrapAddressAmount = BigInteger.ZERO; //compute //TODO
            }

            var networkConfig = NetworkConfig.builder()
                    .networkMagic(protocolMagic)
                    .totalLovelace(genesisConfig.getMaxLovelaceSupply())
                    .poolDepositInLovelace(BigInteger.valueOf(500000000)) //TOD0 -- get from protocol params or remove from here
                    .expectedSlotsPerEpoch(genesisConfig.getEpochLength())
                    .shelleyInitialReserves(shelleyInitialReserves)
                    .shelleyInitialTreasury(BigInteger.ZERO)
                    .shelleyInitialUtxo(shelleyInitialUtxo)
                    .genesisConfigSecurityParameter(genesisConfig.getSecurityParam())
                    .shelleyStartEpoch(shelleyStartEpoch)
                    .allegraHardforkEpoch(allegraStartEpoch)
                    .vasilHardforkEpoch(babbageStartEpoch)
                    .bootstrapAddressAmount(boostrapAddressAmount)
                    .activeSlotCoefficient(genesisConfig.getActiveSlotsCoeff())
                    .randomnessStabilisationWindow(genesisConfig.getRandomnessStabilisationWindow()) // (4 * GENESIS_CONFIG_SECURITY_PARAMETER) / ACTIVE_SLOT_COEFFICIENT
                    .shelleyStartDecentralisation(BigDecimal.valueOf(1.0)) //TODO -- Get from protocol params or remove if not reuqired
                    .shelleyStartTreasuryGrowRate(BigDecimal.valueOf(0.2))
                    .shelleyStartMonetaryExpandRate(BigDecimal.valueOf(0.003))
                    .shelleyStartOptimalPoolCount(150)
                    .shelleyStartPoolOwnerInfluence(BigDecimal.valueOf(0.03))
                    .build();

            log.info("Network config : {}", JsonUtil.getJson(networkConfig));
            return networkConfig;
        }
    }
}
