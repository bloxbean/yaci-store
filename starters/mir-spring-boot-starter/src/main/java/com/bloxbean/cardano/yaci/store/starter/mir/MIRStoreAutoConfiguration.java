package com.bloxbean.cardano.yaci.store.starter.mir;

import com.bloxbean.cardano.yaci.store.api.mir.MIRApiConfiguration;
import com.bloxbean.cardano.yaci.store.mir.MIRStoreConfiguration;
import com.bloxbean.cardano.yaci.store.mir.MIRStoreProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Import;

@AutoConfiguration
@Import({MIRStoreConfiguration.class, MIRApiConfiguration.class})
@Slf4j
public class MIRStoreAutoConfiguration {

    @Autowired
    MIRStoreProperties properties;
}
