package com.bloxbean.cardano.yaci.store.epoch.storage.impl;

import com.bloxbean.cardano.yaci.store.common.domain.ProtocolParams;
import com.bloxbean.cardano.yaci.store.core.service.EraService;
import com.bloxbean.cardano.yaci.store.epoch.domain.EpochParam;
import com.bloxbean.cardano.yaci.store.epoch.storage.EpochParamStorage;
import com.bloxbean.cardano.yaci.store.epoch.storage.impl.repository.CostModelRepository;
import com.bloxbean.cardano.yaci.store.epoch.storage.impl.repository.EpochParamRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class EpochParamStorageImplIT {
    private static final String OLD_HASH = "old-cost-model-hash";
    private static final String NEW_HASH = "new-cost-model-hash";

    @Autowired
    private EpochParamStorage epochParamStorage;

    @Autowired
    private CostModelRepository costModelRepository;

    @Autowired
    private EpochParamRepository epochParamRepository;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @MockBean
    private EraService eraService;

    @AfterEach
    void cleanup() {
        epochParamRepository.deleteAll();
        costModelRepository.deleteAll();
    }

    /**
     * Mirrors EpochParamProcessor.resolveEpochParam: in one transaction, read the previous epoch's params,
     * merge a cost model update into them and save the next epoch. The previous cost model row must stay unchanged.
     */
    @Test
    void savingNextEpochWithUpdatedCostModels_shouldNotOverwritePreviousCostModel() {
        Map<String, long[]> oldCosts = new HashMap<>();
        oldCosts.put("PlutusV1", new long[]{1, 2, 3});
        epochParamStorage.save(epochParam(10, oldCosts, OLD_HASH));

        transactionTemplate.executeWithoutResult(status -> {
            EpochParam previous = epochParamStorage.getProtocolParams(10).orElseThrow();

            Map<String, long[]> update = new HashMap<>();
            update.put("PlutusV1", new long[]{4, 5, 6, 7});
            update.put("PlutusV2", new long[]{8, 9});

            ProtocolParams next = new ProtocolParams();
            next.merge(previous.getParams());
            next.merge(ProtocolParams.builder().costModels(update).costModelsHash(NEW_HASH).build());

            epochParamStorage.save(epochParam(11, next.getCostModels(), NEW_HASH, next));
        });

        Map<String, long[]> storedOld = costModelRepository.findById(OLD_HASH).orElseThrow().getCosts();
        assertThat(storedOld).containsOnlyKeys("PlutusV1");
        assertThat(storedOld.get("PlutusV1")).containsExactly(1, 2, 3);

        Map<String, long[]> storedNew = costModelRepository.findById(NEW_HASH).orElseThrow().getCosts();
        assertThat(storedNew).containsOnlyKeys("PlutusV1", "PlutusV2");
        assertThat(storedNew.get("PlutusV1")).containsExactly(4, 5, 6, 7);

        //cost models live only in cost_model; reading epoch 10 must not write them into its epoch_param.params row
        assertThat(epochParamRepository.findById(10).orElseThrow().getParams().getCostModels()).isNull();

        assertThat(epochParamStorage.getProtocolParams(10).orElseThrow().getParams().getCostModels().get("PlutusV1"))
                .containsExactly(1, 2, 3);
    }

    private EpochParam epochParam(int epoch, Map<String, long[]> costs, String hash) {
        return epochParam(epoch, costs, hash, ProtocolParams.builder().protocolMajorVer(9).build());
    }

    private EpochParam epochParam(int epoch, Map<String, long[]> costs, String hash, ProtocolParams params) {
        params.setCostModels(costs);
        params.setCostModelsHash(hash);
        return EpochParam.builder()
                .epoch(epoch)
                .params(params)
                .slot(epoch * 1000L)
                .blockNumber(epoch * 10L)
                .blockTime(epoch * 1000L)
                .build();
    }
}
