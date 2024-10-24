package com.bloxbean.cardano.yaci.store.adapot.service;

import com.bloxbean.cardano.yaci.store.adapot.snapshot.StakeSnapshotService;
import com.bloxbean.cardano.yaci.store.common.config.StoreProperties;
import org.jooq.impl.DefaultDSLContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;

import java.io.IOException;

@SpringBootTest
@ComponentScan
public class StakesnapshotServiceTest {
    @Autowired
    private StakeSnapshotService stakeSnapshotService;

    @Autowired
    private StoreProperties storeProperties;
    @Autowired
    private DefaultDSLContext dslContext;

    @BeforeEach
    public void setup() {
        storeProperties.setMainnet(true);
    }

//    @Test
    void takeSnapshot() throws IOException {
        dslContext.execute("delete from epoch_stake where epoch >= 207");
        stakeSnapshotService.takeStakeSnapshot(207);
    }


}
