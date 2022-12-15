package com.bloxbean.cardano.yaci.store.protocolparams.service;

import co.nstant.in.cbor.model.Array;
import co.nstant.in.cbor.model.DataItem;
import co.nstant.in.cbor.model.UnsignedInteger;
import com.bloxbean.cardano.client.api.model.ProtocolParams;
import com.bloxbean.cardano.yaci.core.model.Era;
import com.bloxbean.cardano.yaci.core.model.ProtocolParamUpdate;
import com.bloxbean.cardano.yaci.core.protocol.chainsync.messages.Point;
import com.bloxbean.cardano.yaci.core.protocol.localstate.queries.CurrentProtocolParamQueryResult;
import com.bloxbean.cardano.yaci.core.protocol.localstate.queries.CurrentProtocolParamsQuery;
import com.bloxbean.cardano.yaci.core.util.CborSerializationUtil;
import com.bloxbean.cardano.yaci.core.util.HexUtil;
import com.bloxbean.cardano.yaci.helper.LocalClientProvider;
import com.bloxbean.cardano.yaci.helper.LocalStateQueryClient;
import com.bloxbean.cardano.yaci.store.protocolparams.model.ProtocolParamsEntity;
import com.bloxbean.cardano.yaci.store.protocolparams.repository.ProtocolParamsRepository;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Component
@Transactional
@ConditionalOnBean(LocalClientProvider.class)
public class ProtocolParamService {
    private LocalClientProvider localClientProvider;
    private LocalStateQueryClient localStateQueryClient;
    private ProtocolParamsRepository protocolParamsRepository;

    public ProtocolParamService(LocalClientProvider localClientProvider, ProtocolParamsRepository protocolParamsRepository) {
        this.localClientProvider = localClientProvider;
        this.localStateQueryClient = localClientProvider.getLocalStateQueryClient();
        this.protocolParamsRepository = protocolParamsRepository;
    }

    @PostConstruct
    private void postConstruct() {
        if (localClientProvider != null && !localClientProvider.isRunning())
            localClientProvider.start();
    }

    @PreDestroy
    private void destroy() {
        if (localClientProvider != null)
            localClientProvider.shutdown();
    }

    public void fetchAndSetCurrentProtocolParams() {
        getCurrentProtocolParamsFromNode().subscribe(protocolParamUpdate -> {
            ProtocolParamsEntity entity = new ProtocolParamsEntity();
            entity.setId(1L);
            entity.setProtocolParams(convertProtoParams(protocolParamUpdate));

            protocolParamsRepository.save(entity);
        });
    }

    public Optional<ProtocolParams> getCurrentProtocolParams() {
        return protocolParamsRepository.findById(Long.valueOf(1))
                .map(protocolParamsEntity -> Optional.ofNullable(protocolParamsEntity.getProtocolParams()))
                .orElse(Optional.empty());
    }

    public Mono<ProtocolParamUpdate> getCurrentProtocolParamsFromNode() {
        Mono<CurrentProtocolParamQueryResult> mono =
                localStateQueryClient.executeQuery(new CurrentProtocolParamsQuery(Era.Alonzo));
        return mono.map(currentProtocolParamQueryResult -> currentProtocolParamQueryResult.getProtocolParams());
    }

    public Mono<ProtocolParamUpdate> getCurrentProtocolParamsFromNodeAt(Point point) {
        Mono<CurrentProtocolParamQueryResult> mono =
                localStateQueryClient.executeQuery(new CurrentProtocolParamsQuery(Era.Alonzo));
        return mono.map(currentProtocolParamQueryResult -> currentProtocolParamQueryResult.getProtocolParams());
    }

    private ProtocolParams convertProtoParams(ProtocolParamUpdate protocolParamUpdate) {
        ProtocolParams protocolParams = new ProtocolParams();
        protocolParams.setMinFeeA(protocolParamUpdate.getMinFeeA());
        protocolParams.setMinFeeB(protocolParamUpdate.getMinFeeB());
        protocolParams.setMaxBlockSize(protocolParamUpdate.getMaxBlockSize());
        protocolParams.setMaxTxSize(protocolParamUpdate.getMaxTxSize());
        protocolParams.setMaxBlockHeaderSize(protocolParamUpdate.getMaxBlockHeaderSize());
        protocolParams.setKeyDeposit(String.valueOf(protocolParamUpdate.getKeyDeposit()));
        protocolParams.setPoolDeposit(String.valueOf(protocolParamUpdate.getPoolDeposit()));
        protocolParams.setEMax(protocolParamUpdate.getMaxEpoch());
        protocolParams.setNOpt(protocolParamUpdate.getNOpt());
        protocolParams.setA0(protocolParamUpdate.getPoolPledgeInfluence());
        protocolParams.setRho(protocolParamUpdate.getExpansionRate());
        protocolParams.setTau(protocolParamUpdate.getTreasuryGrowthRate());
        protocolParams.setDecentralisationParam(protocolParamUpdate.getDecentralisationParam()); //Deprecated. Not there
        protocolParams.setExtraEntropy(protocolParamUpdate.getExtraEntropy());
        protocolParams.setProtocolMajorVer(protocolParamUpdate.getProtocolMajorVer());
        protocolParams.setProtocolMinorVer(protocolParamUpdate.getProtocolMinorVer());
        protocolParams.setMinUtxo(String.valueOf(protocolParamUpdate.getMinUtxo()));
        protocolParams.setMinPoolCost(String.valueOf(protocolParamUpdate.getMinPoolCost()));
//        protocolParams.setNonce(currentProtocolParameters.getProtocolParameters().getNonce()); //TODO

        Map<String, Long> plutusV1CostModel = cborToCostModel(protocolParamUpdate.getCostModels().get(0));
        Map<String, Long> plutusV2CostModel = cborToCostModel(protocolParamUpdate.getCostModels().get(1));

        Map<String, Map<String, Long>> costModels = new HashMap<>();
        costModels.put("PlutusV1", plutusV1CostModel);
        costModels.put("PlutusV2", plutusV2CostModel);
        protocolParams.setCostModels(costModels);

        protocolParams.setPriceMem(protocolParamUpdate.getPriceMem());
        protocolParams.setPriceStep(protocolParamUpdate.getPriceStep());
        protocolParams.setMaxTxExMem(String.valueOf(protocolParamUpdate.getMaxTxExMem()));
        protocolParams.setMaxTxExSteps(String.valueOf(protocolParamUpdate.getMaxTxExSteps()));
        protocolParams.setMaxBlockExMem(String.valueOf(protocolParamUpdate.getMaxBlockExMem()));
        protocolParams.setMaxBlockExSteps(String.valueOf(protocolParamUpdate.getMaxBlockExSteps()));
        protocolParams.setMaxValSize(String.valueOf(protocolParamUpdate.getMaxValSize()));
        protocolParams.setCollateralPercent(BigDecimal.valueOf(protocolParamUpdate.getCollateralPercent()));
        protocolParams.setMaxCollateralInputs(protocolParamUpdate.getMaxCollateralInputs());
        protocolParams.setCoinsPerUtxoSize(String.valueOf(protocolParamUpdate.getAdaPerUtxoByte()));
        return protocolParams;
    }

    private Map<String, Long> cborToCostModel(String costModelCbor) {
        Array array = (Array) CborSerializationUtil.deserializeOne(HexUtil.decodeHexString(costModelCbor));
        Map<String, Long> costModel = new HashMap<>();
        int index = 1;
        for (DataItem di : array.getDataItems()) {
            BigInteger val = ((UnsignedInteger) di).getValue();
            costModel.put(String.valueOf(index++), val.longValue());
        }

        return costModel;
    }
}
