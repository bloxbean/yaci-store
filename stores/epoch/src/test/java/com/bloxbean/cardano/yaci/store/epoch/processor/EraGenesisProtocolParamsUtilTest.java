package com.bloxbean.cardano.yaci.store.epoch.processor;

import com.bloxbean.cardano.yaci.core.model.Era;
import com.bloxbean.cardano.yaci.store.common.config.StoreProperties;
import com.bloxbean.cardano.yaci.store.common.domain.ProtocolParams;
import com.bloxbean.cardano.yaci.store.epoch.storage.EpochParamStorage;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class EraGenesisProtocolParamsUtilTest {
    private static final long PREPROD = 1; //bundled genesis files

    private final EraGenesisProtocolParamsUtil util =
            new EraGenesisProtocolParamsUtil(new StoreProperties(), mock(EpochParamStorage.class));

    @Test
    void networkStartingInDijkstraGetsTheSameParamsAsOneStartingInConway() {
        //Leios Musashi: every era forks at epoch 0, so the first block is a Dijkstra block
        ProtocolParams dijkstra = util.getGenesisProtocolParameters(Era.Dijkstra, null, PREPROD).orElseThrow();
        ProtocolParams conway = util.getGenesisProtocolParameters(Era.Conway, null, PREPROD).orElseThrow();

        assertThat(dijkstra).usingRecursiveComparison().isEqualTo(conway);
        assertThat(dijkstra.getMinFeeA()).isNotNull();
        assertThat(dijkstra.getCostModels()).isNotEmpty();
        assertThat(dijkstra.getDrepDeposit()).isNotNull();
    }

    @Test
    void dijkstraAfterConwayAddsNothingYet() {
        assertThat(util.getGenesisProtocolParameters(Era.Dijkstra, Era.Conway, PREPROD)).isEmpty();
    }
}
