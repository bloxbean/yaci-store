package com.bloxbean.cardano.yaci.store.starter.admin;

import com.bloxbean.cardano.yaci.store.admin.AdminConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Import;

@AutoConfiguration
@EnableConfigurationProperties(AdminStoreProperties.class)
@Import({AdminConfiguration.class,})
@Slf4j
public class AdminStoreAutoConfiguration {

    @Autowired
    AdminStoreProperties properties;
}
