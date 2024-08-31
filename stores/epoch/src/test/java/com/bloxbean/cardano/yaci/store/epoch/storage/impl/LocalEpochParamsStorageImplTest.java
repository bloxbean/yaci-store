package com.bloxbean.cardano.yaci.store.epoch.storage.impl;

import com.bloxbean.cardano.yaci.store.common.domain.ProtocolParams;
import com.bloxbean.cardano.yaci.store.epoch.domain.EpochParam;
import com.bloxbean.cardano.yaci.store.epoch.storage.LocalEpochParamsStorage;
import com.bloxbean.cardano.yaci.store.epoch.storage.impl.repository.LocalEpochParamsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class LocalEpochParamsStorageImplTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private LocalEpochParamsRepository repository;

    private LocalEpochParamsStorage localProtocolParamsStorage;

    @BeforeEach
    public void setup() {
        localProtocolParamsStorage = new LocalEpochParamsStorageImpl(repository);
    }

    @Test
    void saveAndGet() {
        prepareData();

        assertThat(repository.count()).isEqualTo(5);
        assertThat(localProtocolParamsStorage.getEpochParam(209).isPresent()).isTrue();
        localProtocolParamsStorage.getEpochParam(209).ifPresent(e -> {
            assertEquals(209, e.getEpoch());
            assertEquals(10, e.getParams().getMaxEpoch());
            assertEquals(44, e.getParams().getMinFeeA());
            assertEquals(1001, e.getParams().getMinFeeB());
        });
    }

    @Test
    void getEpochParam() {
        prepareData();
        localProtocolParamsStorage.getEpochParam(209).ifPresent(e -> {
            assertEquals(209, e.getEpoch());
            assertEquals(10, e.getParams().getMaxEpoch());
            assertEquals(44, e.getParams().getMinFeeA());
            assertEquals(1001, e.getParams().getMinFeeB());
        });
        assertThat(localProtocolParamsStorage.getEpochParam(209)).isPresent();

        localProtocolParamsStorage.getEpochParam(210).ifPresent(e -> {
            assertEquals(210, e.getEpoch());
            assertEquals(11, e.getParams().getMaxEpoch());
            assertEquals(45, e.getParams().getMinFeeA());
            assertEquals(1002, e.getParams().getMinFeeB());
        });
        assertThat(localProtocolParamsStorage.getEpochParam(210)).isPresent();
    }

    @Test
    void getLatestEpochParam() {
        prepareData();
        assertThat(localProtocolParamsStorage.getLatestEpochParam()).isPresent();
        localProtocolParamsStorage.getLatestEpochParam().ifPresent(e -> {
            assertEquals(211, e.getEpoch());
            assertEquals(12, e.getParams().getMaxEpoch());
            assertEquals(46, e.getParams().getMinFeeA());
            assertEquals(1003, e.getParams().getMinFeeB());
        });
    }

    private void prepareData() {
        EpochParam epochParam209 = EpochParam.builder()
                .epoch(209)
                .params(ProtocolParams.builder()
                        .maxEpoch(10)
                        .minFeeA(44)
                        .minFeeB(1001)
                        .build()).build();

        EpochParam epochParam210 = EpochParam.builder()
                .epoch(210)
                .params(ProtocolParams.builder()
                        .maxEpoch(11)
                        .minFeeA(45)
                        .minFeeB(1002)
                        .build()).build();

        EpochParam epochParam211 = EpochParam.builder()
                .epoch(211)
                .params(ProtocolParams.builder()
                        .maxEpoch(12)
                        .minFeeA(46)
                        .minFeeB(1003)
                        .build()).build();


        EpochParam epochParam208 = EpochParam.builder()
                .epoch(208)
                .params(ProtocolParams.builder()
                        .maxEpoch(8)
                        .minFeeA(46)
                        .minFeeB(900)
                        .build()).build();

        EpochParam epochParam207 = EpochParam.builder()
                .epoch(207)
                .params(ProtocolParams.builder()
                        .maxEpoch(7)
                        .minFeeA(47)
                        .minFeeB(900)
                        .build()).build();

        localProtocolParamsStorage.save(epochParam209);
        localProtocolParamsStorage.save(epochParam210);
        localProtocolParamsStorage.save(epochParam211);
        localProtocolParamsStorage.save(epochParam208);
        localProtocolParamsStorage.save(epochParam207);
    }
}
