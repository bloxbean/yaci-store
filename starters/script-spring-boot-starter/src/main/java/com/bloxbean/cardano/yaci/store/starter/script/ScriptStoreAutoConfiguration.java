package com.bloxbean.cardano.yaci.store.starter.script;

import com.bloxbean.cardano.yaci.store.api.script.ScriptApiConfiguration;
import com.bloxbean.cardano.yaci.store.script.ScriptStoreConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Import;

@AutoConfiguration
@EnableConfigurationProperties(ScriptStoreProperties.class)
@Import({ScriptStoreConfiguration.class, ScriptApiConfiguration.class})
@Slf4j
public class ScriptStoreAutoConfiguration {

    @Autowired
    ScriptStoreProperties properties;
}
