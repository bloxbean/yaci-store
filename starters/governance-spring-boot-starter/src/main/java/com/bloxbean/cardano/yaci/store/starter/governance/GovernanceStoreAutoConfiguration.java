package com.bloxbean.cardano.yaci.store.starter.governance;

import com.bloxbean.cardano.yaci.store.governance.GovernanceStoreConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Import;

@AutoConfiguration
@EnableConfigurationProperties(GovernanceStoreProperties.class)
@Import(GovernanceStoreConfiguration.class)
@Slf4j
public class GovernanceStoreAutoConfiguration {

    @Autowired
    GovernanceStoreProperties properties;
}
