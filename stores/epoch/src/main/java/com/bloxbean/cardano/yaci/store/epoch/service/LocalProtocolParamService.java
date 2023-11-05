package com.bloxbean.cardano.yaci.store.epoch.service;

import co.nstant.in.cbor.model.Array;
import co.nstant.in.cbor.model.DataItem;
import co.nstant.in.cbor.model.Special;
import co.nstant.in.cbor.model.UnsignedInteger;
import com.bloxbean.cardano.client.api.model.ProtocolParams;
import com.bloxbean.cardano.yaci.core.model.ProtocolParamUpdate;
import com.bloxbean.cardano.yaci.core.protocol.chainsync.messages.Point;
import com.bloxbean.cardano.yaci.core.protocol.localstate.api.Era;
import com.bloxbean.cardano.yaci.core.protocol.localstate.queries.CurrentProtocolParamQueryResult;
import com.bloxbean.cardano.yaci.core.protocol.localstate.queries.CurrentProtocolParamsQuery;
import com.bloxbean.cardano.yaci.core.util.CborSerializationUtil;
import com.bloxbean.cardano.yaci.core.util.HexUtil;
import com.bloxbean.cardano.yaci.helper.LocalClientProvider;
import com.bloxbean.cardano.yaci.helper.LocalStateQueryClient;
import com.bloxbean.cardano.yaci.store.epoch.storage.impl.jpa.repository.LocalProtocolParamsRepository;
import com.bloxbean.cardano.yaci.store.epoch.storage.impl.jpa.model.LocalProtocolParamsEntity;
import com.bloxbean.cardano.yaci.store.epoch.util.PlutusOps;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Duration;
import java.util.*;

@Component
@ConditionalOnExpression("'${store.cardano.n2c-node-socket-path:}' != '' || '${store.cardano.n2c-host:}' != ''")
@Slf4j
public class LocalProtocolParamService {
    private final LocalClientProvider localClientProvider;
    private final LocalStateQueryClient localStateQueryClient;
    private LocalProtocolParamsRepository protocolParamsRepository;

    public LocalProtocolParamService(LocalClientProvider localClientProvider, LocalProtocolParamsRepository protocolParamsRepository) {
        this.localClientProvider = localClientProvider;
        this.localStateQueryClient = localClientProvider.getLocalStateQueryClient();
        this.protocolParamsRepository = protocolParamsRepository;
        log.info("ProtocolParamService initialized >>>");
    }

    @Transactional
    public void fetchAndSetCurrentProtocolParams() {
        getCurrentProtocolParamsFromNode().subscribe(protocolParamUpdate -> {
                    LocalProtocolParamsEntity entity = new LocalProtocolParamsEntity();
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
        //Try to release first before a new query to avoid stale data
        try {
            localStateQueryClient.release().block(Duration.ofSeconds(5));
        } catch (Exception e) {
            //Ignore the error
        }
        Mono<CurrentProtocolParamQueryResult> mono =
                localStateQueryClient.executeQuery(new CurrentProtocolParamsQuery(Era.Babbage));
        return mono.map(currentProtocolParamQueryResult -> currentProtocolParamQueryResult.getProtocolParams());
    }

    public Mono<ProtocolParamUpdate> getCurrentProtocolParamsFromNodeAt(Point point) {
        Mono<CurrentProtocolParamQueryResult> mono =
                localStateQueryClient.executeQuery(new CurrentProtocolParamsQuery(Era.Babbage));
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
        if (protocolParamUpdate.getExtraEntropy() != null)
            protocolParams.setExtraEntropy(protocolParamUpdate.getExtraEntropy()._2);
        protocolParams.setProtocolMajorVer(protocolParamUpdate.getProtocolMajorVer());
        protocolParams.setProtocolMinorVer(protocolParamUpdate.getProtocolMinorVer());
        protocolParams.setMinUtxo(String.valueOf(protocolParamUpdate.getMinUtxo()));
        protocolParams.setMinPoolCost(String.valueOf(protocolParamUpdate.getMinPoolCost()));
//        protocolParams.setNonce(currentProtocolParameters.getProtocolParameters().getNonce()); //TODO

        Map<String, Long> plutusV1CostModel
                = cborToCostModel(protocolParamUpdate.getCostModels().get(0), PlutusOps.getOperations(1));
        Map<String, Long> plutusV2CostModel
                = cborToCostModel(protocolParamUpdate.getCostModels().get(1), PlutusOps.getOperations(2));

        LinkedHashMap<String, Map<String, Long>> costModels = new LinkedHashMap<>();
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

    private Map<String, Long> cborToCostModel(String costModelCbor, List<String> ops) {
        Array array = (Array) CborSerializationUtil.deserializeOne(HexUtil.decodeHexString(costModelCbor));
        Map<String, Long> costModel = new LinkedHashMap<>();

        if (ops.size() == array.getDataItems().size()) {
            int index = 0;
            for (DataItem di : array.getDataItems()) {
                if (di == Special.BREAK)
                    continue;
                BigInteger val = ((UnsignedInteger) di).getValue();
                costModel.put(ops.get(index++), val.longValue());
            }
        } else {
            int index = 0;
            for (DataItem di : array.getDataItems()) {
                if (di == Special.BREAK)
                    continue;
                BigInteger val = ((UnsignedInteger) di).getValue();
                costModel.put(String.format("%03d", index++), val.longValue());
            }
        }

        return costModel;
    }
}
