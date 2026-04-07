package com.bloxbean.cardano.yaci.store.starter.assetstore;

import com.bloxbean.cardano.yaci.store.extensions.assetstore.AssetsExtConfiguration;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.AssetsStoreProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

@AutoConfiguration
@EnableConfigurationProperties(AssetsExtProperties.class)
@Import({AssetsExtConfiguration.class})
@Slf4j
public class AssetsExtAutoConfiguration {

    @Autowired
    AssetsExtProperties properties;

    @Bean
    @ConditionalOnMissingBean
    public AssetsStoreProperties assetsStoreProperties() {
        AssetsStoreProperties.Cip26 cip26 = new AssetsStoreProperties.Cip26();
        cip26.setEnabled(properties.getCip26().isEnabled());
        cip26.setGitOrganization(properties.getCip26().getGitOrganization());
        cip26.setGitProjectName(properties.getCip26().getGitProjectName());
        cip26.setGitMappingsFolder(properties.getCip26().getGitMappingsFolder());
        cip26.setGitTmpFolder(properties.getCip26().getGitTmpFolder());

        AssetsStoreProperties.Cip113 cip113 = new AssetsStoreProperties.Cip113();
        cip113.setRegistryNftPolicyIds(properties.getCip113().getRegistryNftPolicyIds());

        AssetsStoreProperties assetsStoreProperties = new AssetsStoreProperties();
        assetsStoreProperties.setCip26(cip26);
        assetsStoreProperties.setCip113(cip113);
        assetsStoreProperties.setDefaultQueryPriority(properties.getQuery().getPriority());
        return assetsStoreProperties;
    }
}
