package com.bloxbean.cardano.yaci.store.adminui.service;

import com.bloxbean.cardano.yaci.store.adminui.dto.ConfigSectionDto;
import com.bloxbean.cardano.yaci.store.common.config.StoreProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.boot.context.properties.ConfigurationPropertiesBean;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.util.ClassUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class ConfigurationService {
    private final StoreProperties storeProperties;
    private final Environment environment;
    private final ApplicationContext applicationContext;
    private static final String NOT_SET = "(not set)";

    public ConfigurationService(StoreProperties storeProperties, Environment environment, ApplicationContext applicationContext) {
        this.storeProperties = storeProperties;
        this.environment = environment;
        this.applicationContext = applicationContext;
    }

    public List<ConfigSectionDto> getConfiguration() {
        List<ConfigSectionDto> sections = new ArrayList<>();
        Map<String, ConfigurationPropertiesBean> configBeansByType = getConfigurationPropertiesBeansByType();

        // Cardano Network Section
        Map<String, Object> cardanoProps = new LinkedHashMap<>();
        cardanoProps.put("store.cardano.host", maskIfEmpty(storeProperties.getCardanoHost()));
        cardanoProps.put("store.cardano.port", storeProperties.getCardanoPort());
        cardanoProps.put("store.cardano.protocol-magic", storeProperties.getProtocolMagic());
        cardanoProps.put("store.cardano.n2c-node-socket-path", maskIfEmpty(storeProperties.getN2cNodeSocketPath()));
        cardanoProps.put("store.cardano.n2c-host", maskIfEmpty(storeProperties.getN2cHost()));
        cardanoProps.put("store.cardano.n2c-port", storeProperties.getN2cPort());
        cardanoProps.put("store.cardano.is-mainnet", storeProperties.isMainnet());
        sections.add(ConfigSectionDto.builder()
                .name("Cardano Network")
                .properties(cardanoProps)
                .build());

        // Sync Configuration Section
        Map<String, Object> syncProps = new LinkedHashMap<>();
        syncProps.put("store.sync-auto-start", storeProperties.isSyncAutoStart());
        syncProps.put("store.sync-start-slot", storeProperties.getSyncStartSlot());
        syncProps.put("store.sync-start-blockhash", maskIfEmpty(storeProperties.getSyncStartBlockhash()));
        syncProps.put("store.sync-stop-slot", storeProperties.getSyncStopSlot());
        syncProps.put("store.sync-stop-blockhash", maskIfEmpty(storeProperties.getSyncStopBlockhash()));
        syncProps.put("store.block-diff-to-start-sync-protocol", storeProperties.getBlockDiffToStartSyncProtocol());
        sections.add(ConfigSectionDto.builder()
                .name("Sync Configuration")
                .properties(syncProps)
                .build());

        // Executor Configuration Section
        Map<String, Object> executorProps = new LinkedHashMap<>();
        executorProps.put("store.executor.block-processing-threads", storeProperties.getBlockProcessingThreads());
        executorProps.put("store.executor.event-processing-threads", storeProperties.getEventProcessingThreads());
        executorProps.put("store.executor.enable-parallel-processing", storeProperties.isEnableParallelProcessing());
        executorProps.put("store.executor.blocks-batch-size", storeProperties.getBlocksBatchSize());
        executorProps.put("store.executor.blocks-partition-size", storeProperties.getBlocksPartitionSize());
        executorProps.put("store.executor.use-virtual-thread-for-batch-processing", storeProperties.isUseVirtualThreadForBatchProcessing());
        executorProps.put("store.executor.use-virtual-thread-for-event-processing", storeProperties.isUseVirtualThreadForEventProcessing());
        executorProps.put("store.executor.processing-threads-timeout", storeProperties.getProcessingThreadsTimeout() + " min");
        sections.add(ConfigSectionDto.builder()
                .name("Executor Configuration")
                .properties(executorProps)
                .build());

        // Database Configuration Section
        Map<String, Object> dbProps = new LinkedHashMap<>();
        dbProps.put("store.db.batch-size", storeProperties.getDbBatchSize());
        dbProps.put("store.db.parallel-insert", storeProperties.isDbParallelInsert());
        dbProps.put("store.db.parallel-write", storeProperties.isParallelWrite());
        dbProps.put("store.db.write-thread-count", storeProperties.getWriteThreadCount());
        dbProps.put("store.db.write-thread-default-batch-size", storeProperties.getWriteThreadDefaultBatchSize());
        dbProps.put("store.db.jooq-write-batch-size", storeProperties.getJooqWriteBatchSize());
        sections.add(ConfigSectionDto.builder()
                .name("Database Configuration")
                .properties(dbProps)
                .build());

        // Cursor Configuration Section
        Map<String, Object> cursorProps = new LinkedHashMap<>();
        cursorProps.put("store.cursor.no-of-blocks-to-keep", storeProperties.getCursorNoOfBlocksToKeep());
        cursorProps.put("store.cursor.cleanup-interval", storeProperties.getCursorCleanupInterval() + " sec");
        sections.add(ConfigSectionDto.builder()
                .name("Cursor Configuration")
                .properties(cursorProps)
                .build());

        // N2C Pool Configuration Section
        Map<String, Object> n2cPoolProps = new LinkedHashMap<>();
        n2cPoolProps.put("store.cardano.n2c-pool-enabled", storeProperties.isN2cPoolEnabled());
        n2cPoolProps.put("store.cardano.n2c-pool-max-total", storeProperties.getN2cPoolMaxTotal());
        n2cPoolProps.put("store.cardano.n2c-pool-min-idle", storeProperties.getN2cPoolMinIdle());
        n2cPoolProps.put("store.cardano.n2c-pool-max-idle", storeProperties.getN2cPoolMaxIdle());
        n2cPoolProps.put("store.cardano.n2c-pool-max-wait-millis", storeProperties.getN2cPoolMaxWaitInMillis() + " ms");
        sections.add(ConfigSectionDto.builder()
                .name("N2C Pool Configuration")
                .properties(n2cPoolProps)
                .build());

        // Plugin Configuration Section
        Map<String, Object> pluginProps = new LinkedHashMap<>();
        pluginProps.put("store.plugins.enabled", storeProperties.isPluginsEnabled());
        pluginProps.put("store.plugins.exit-on-error", storeProperties.isPluginExitOnError());
        pluginProps.put("store.plugins.metrics-enabled", storeProperties.isPluginMetricsEnabled());
        pluginProps.put("store.plugins.api-enabled", storeProperties.isPluginApiEnabled());
        sections.add(ConfigSectionDto.builder()
                .name("Plugin Configuration")
                .properties(pluginProps)
                .build());

        // Other Configuration Section
        Map<String, Object> otherProps = new LinkedHashMap<>();
        otherProps.put("store.read-only-mode", storeProperties.isReadOnlyMode());
        otherProps.put("store.primary-instance", storeProperties.isPrimaryInstance());
        otherProps.put("store.metrics.enabled", storeProperties.isMetricsEnabled());
        otherProps.put("store.keep-alive-interval", storeProperties.getKeepAliveInterval() + " ms");
        otherProps.put("store.block-receive-delay-seconds", storeProperties.getBlockReceiveDelaySeconds() + " sec");
        otherProps.put("store.continue-on-parse-error", storeProperties.isContinueOnParseError());
        sections.add(ConfigSectionDto.builder()
                .name("Other Configuration")
                .properties(otherProps)
                .build());

        // Utxo Store
        String utxoPrefix = "store.utxo";
        // Try to read defaults from the starter properties class if it is on the classpath.
        Object utxoDefaults = resolveStoreDefaultsRoot(configBeansByType, "com.bloxbean.cardano.yaci.store.starter.utxo.UtxoStoreAutoConfigProperties", "utxo");
        if (utxoDefaults != null) {
            Map<String, Object> utxoProps = new LinkedHashMap<>();
            Boolean utxoEnabled = putEnabledProperty(utxoProps, "store.utxo.enabled", utxoPrefix, utxoDefaults);
            if (!Boolean.FALSE.equals(utxoEnabled)) {
                Boolean utxoApiEnabled = putBooleanProperty(utxoProps, "store.utxo.api-enabled", utxoPrefix, utxoDefaults);
                if (!Boolean.FALSE.equals(utxoApiEnabled)) {
                    putBooleanProperty(utxoProps, "store.utxo.endpoints.address.enabled", utxoPrefix, utxoDefaults);
                    putBooleanProperty(utxoProps, "store.utxo.endpoints.asset.enabled", utxoPrefix, utxoDefaults);
                    putBooleanProperty(utxoProps, "store.utxo.endpoints.transaction.enabled", utxoPrefix, utxoDefaults);
                }
                putBooleanProperty(utxoProps, "store.utxo.save-address", utxoPrefix, utxoDefaults);
                putBooleanProperty(utxoProps, "store.utxo.pruning-enabled", utxoPrefix, utxoDefaults);
                putValueProperty(utxoProps, "store.utxo.pruning-interval", utxoPrefix, utxoDefaults);
                putValueProperty(utxoProps, "store.utxo.pruning-safe-blocks", utxoPrefix, utxoDefaults);
                putBooleanProperty(utxoProps, "store.utxo.address-cache-enabled", utxoPrefix, utxoDefaults);
                putValueProperty(utxoProps, "store.utxo.address-cache-size", utxoPrefix, utxoDefaults);
                putValueProperty(utxoProps, "store.utxo.address-cache-expiry-after-access", utxoPrefix, utxoDefaults);
                putBooleanProperty(utxoProps, "store.utxo.content-aware-rollback", utxoPrefix, utxoDefaults);
            }
            sections.add(ConfigSectionDto.builder()
                    .name("Utxo Store")
                    .properties(utxoProps)
                    .build());
        }

        // Blocks Store
        String blocksPrefix = "store.blocks";
        Object blocksDefaults = resolveStoreDefaultsRoot(configBeansByType, "com.bloxbean.cardano.yaci.store.starter.blocks.BlocksStoreAutoConfigProperties", "blocks");
        if (blocksDefaults != null) {
            Map<String, Object> blocksProps = new LinkedHashMap<>();
            Boolean blocksEnabled = putEnabledProperty(blocksProps, "store.blocks.enabled", blocksPrefix, blocksDefaults);
            if (!Boolean.FALSE.equals(blocksEnabled)) {
                Boolean blocksApiEnabled = putBooleanProperty(blocksProps, "store.blocks.api-enabled", blocksPrefix, blocksDefaults);
                if (!Boolean.FALSE.equals(blocksApiEnabled)) {
                    putBooleanProperty(blocksProps, "store.blocks.endpoints.block.enabled", blocksPrefix, blocksDefaults);
                }
                putBooleanProperty(blocksProps, "store.blocks.save-cbor", blocksPrefix, blocksDefaults);
                putBooleanProperty(blocksProps, "store.blocks.cbor-pruning-enabled", blocksPrefix, blocksDefaults);
                putValueProperty(blocksProps, "store.blocks.cbor-pruning-safe-slots", blocksPrefix, blocksDefaults);
                putBooleanProperty(blocksProps, "store.blocks.metrics.enabled", blocksPrefix, blocksDefaults);
                putValueProperty(blocksProps, "store.blocks.metrics.update-interval", blocksPrefix, blocksDefaults);
            }
            sections.add(ConfigSectionDto.builder()
                    .name("Blocks Store")
                    .properties(blocksProps)
                    .build());
        }

        // Transaction Store
        String txPrefix = "store.transaction";
        Object txDefaults = resolveStoreDefaultsRoot(configBeansByType, "com.bloxbean.cardano.yaci.store.starter.transaction.TransactionAutoConfigProperties", "transaction");
        if (txDefaults != null) {
            Map<String, Object> txProps = new LinkedHashMap<>();
            Boolean txEnabled = putEnabledProperty(txProps, "store.transaction.enabled", txPrefix, txDefaults);
            if (!Boolean.FALSE.equals(txEnabled)) {
                Boolean txApiEnabled = putBooleanProperty(txProps, "store.transaction.api-enabled", txPrefix, txDefaults);
                if (!Boolean.FALSE.equals(txApiEnabled)) {
                    putBooleanProperty(txProps, "store.transaction.endpoints.block.enabled", txPrefix, txDefaults);
                    putBooleanProperty(txProps, "store.transaction.endpoints.transaction.enabled", txPrefix, txDefaults);
                }
                putBooleanProperty(txProps, "store.transaction.pruning-enabled", txPrefix, txDefaults);
                putValueProperty(txProps, "store.transaction.pruning-interval", txPrefix, txDefaults);
                putValueProperty(txProps, "store.transaction.pruning-safe-slots", txPrefix, txDefaults);
                putBooleanProperty(txProps, "store.transaction.save-witness", txPrefix, txDefaults);
                putBooleanProperty(txProps, "store.transaction.save-cbor", txPrefix, txDefaults);
                putBooleanProperty(txProps, "store.transaction.cbor-pruning-enabled", txPrefix, txDefaults);
                putValueProperty(txProps, "store.transaction.cbor-pruning-safe-slots", txPrefix, txDefaults);
            }
            sections.add(ConfigSectionDto.builder()
                    .name("Transaction Store")
                    .properties(txProps)
                    .build());
        }

        // Script Store
        String scriptPrefix = "store.script";
        Object scriptDefaults = resolveStoreDefaultsRoot(configBeansByType, "com.bloxbean.cardano.yaci.store.starter.script.ScriptStoreProperties", "script");
        if (scriptDefaults != null) {
            Map<String, Object> scriptProps = new LinkedHashMap<>();
            Boolean scriptEnabled = putEnabledProperty(scriptProps, "store.script.enabled", scriptPrefix, scriptDefaults);
            if (!Boolean.FALSE.equals(scriptEnabled)) {
                Boolean scriptApiEnabled = putBooleanProperty(scriptProps, "store.script.api-enabled", scriptPrefix, scriptDefaults);
                if (!Boolean.FALSE.equals(scriptApiEnabled)) {
                    putBooleanProperty(scriptProps, "store.script.endpoints.script.enabled", scriptPrefix, scriptDefaults);
                }
            }
            sections.add(ConfigSectionDto.builder()
                    .name("Script Store")
                    .properties(scriptProps)
                    .build());
        }

        // Metadata Store
        String metadataPrefix = "store.metadata";
        Object metadataDefaults = resolveStoreDefaultsRoot(configBeansByType, "com.bloxbean.cardano.yaci.store.starter.metadata.MetadataStoreProperties", "metadata");
        if (metadataDefaults != null) {
            Map<String, Object> metadataProps = new LinkedHashMap<>();
            Boolean metadataEnabled = putEnabledProperty(metadataProps, "store.metadata.enabled", metadataPrefix, metadataDefaults);
            if (!Boolean.FALSE.equals(metadataEnabled)) {
                Boolean metadataApiEnabled = putBooleanProperty(metadataProps, "store.metadata.api-enabled", metadataPrefix, metadataDefaults);
                if (!Boolean.FALSE.equals(metadataApiEnabled)) {
                    putBooleanProperty(metadataProps, "store.metadata.endpoints.transaction.enabled", metadataPrefix, metadataDefaults);
                }
            }
            sections.add(ConfigSectionDto.builder()
                    .name("Metadata Store")
                    .properties(metadataProps)
                    .build());
        }

        // Assets Store
        String assetsPrefix = "store.assets";
        Object assetsDefaults = resolveStoreDefaultsRoot(configBeansByType, "com.bloxbean.cardano.yaci.store.starter.assets.AssetsStoreProperties", "assets");
        if (assetsDefaults != null) {
            Map<String, Object> assetsProps = new LinkedHashMap<>();
            Boolean assetsEnabled = putEnabledProperty(assetsProps, "store.assets.enabled", assetsPrefix, assetsDefaults);
            if (!Boolean.FALSE.equals(assetsEnabled)) {
                Boolean assetsApiEnabled = putBooleanProperty(assetsProps, "store.assets.api-enabled", assetsPrefix, assetsDefaults);
                if (!Boolean.FALSE.equals(assetsApiEnabled)) {
                    putBooleanProperty(assetsProps, "store.assets.endpoints.asset.enabled", assetsPrefix, assetsDefaults);
                }
            }
            sections.add(ConfigSectionDto.builder()
                    .name("Assets Store")
                    .properties(assetsProps)
                    .build());
        }

        // Epoch Store
        String epochPrefix = "store.epoch";
        Object epochDefaults = resolveStoreDefaultsRoot(configBeansByType, "com.bloxbean.cardano.yaci.store.starter.epoch.EpochStoreProperties", "epoch");
        if (epochDefaults != null) {
            Map<String, Object> epochProps = new LinkedHashMap<>();
            Boolean epochEnabled = putEnabledProperty(epochProps, "store.epoch.enabled", epochPrefix, epochDefaults);
            if (!Boolean.FALSE.equals(epochEnabled)) {
                Boolean epochApiEnabled = putBooleanProperty(epochProps, "store.epoch.api-enabled", epochPrefix, epochDefaults);
                if (!Boolean.FALSE.equals(epochApiEnabled)) {
                    putBooleanProperty(epochProps, "store.epoch.endpoints.epoch.enabled", epochPrefix, epochDefaults);
                    putBooleanProperty(epochProps, "store.epoch.endpoints.network.enabled", epochPrefix, epochDefaults);
                }
                putBooleanProperty(epochProps, "store.epoch.n2c-epoch-param-enabled", epochPrefix, epochDefaults);
                putValueProperty(epochProps, "store.epoch.n2c-protocol-param-fetching-interval-in-minutes", epochPrefix, epochDefaults);
            }
            sections.add(ConfigSectionDto.builder()
                    .name("Epoch Store")
                    .properties(epochProps)
                    .build());
        }

        // Staking Store
        String stakingPrefix = "store.staking";
        Object stakingDefaults = resolveStoreDefaultsRoot(configBeansByType, "com.bloxbean.cardano.yaci.store.starter.staking.StakingStoreProperties", "staking");
        if (stakingDefaults != null) {
            Map<String, Object> stakingProps = new LinkedHashMap<>();
            Boolean stakingEnabled = putEnabledProperty(stakingProps, "store.staking.enabled", stakingPrefix, stakingDefaults);
            if (!Boolean.FALSE.equals(stakingEnabled)) {
                Boolean stakingApiEnabled = putBooleanProperty(stakingProps, "store.staking.api-enabled", stakingPrefix, stakingDefaults);
                if (!Boolean.FALSE.equals(stakingApiEnabled)) {
                    putBooleanProperty(stakingProps, "store.staking.endpoints.pool.enabled", stakingPrefix, stakingDefaults);
                    putBooleanProperty(stakingProps, "store.staking.endpoints.account.enabled", stakingPrefix, stakingDefaults);
                }
            }
            sections.add(ConfigSectionDto.builder()
                    .name("Staking Store")
                    .properties(stakingProps)
                    .build());
        }

        // MIR Store
        String mirPrefix = "store.mir";
        Object mirDefaults = resolveStoreDefaultsRoot(configBeansByType, "com.bloxbean.cardano.yaci.store.starter.mir.MIRStoreProperties", "mir");
        if (mirDefaults != null) {
            Map<String, Object> mirProps = new LinkedHashMap<>();
            Boolean mirEnabled = putEnabledProperty(mirProps, "store.mir.enabled", mirPrefix, mirDefaults);
            if (!Boolean.FALSE.equals(mirEnabled)) {
                Boolean mirApiEnabled = putBooleanProperty(mirProps, "store.mir.api-enabled", mirPrefix, mirDefaults);
                if (!Boolean.FALSE.equals(mirApiEnabled)) {
                    putBooleanProperty(mirProps, "store.mir.endpoints.mir.enabled", mirPrefix, mirDefaults);
                }
            }
            sections.add(ConfigSectionDto.builder()
                    .name("MIR Store")
                    .properties(mirProps)
                    .build());
        }

        // Governance Store
        String governancePrefix = "store.governance";
        Object governanceDefaults = resolveStoreDefaultsRoot(configBeansByType, "com.bloxbean.cardano.yaci.store.starter.governance.GovernanceStoreProperties", "governance");
        if (governanceDefaults != null) {
            Map<String, Object> govProps = new LinkedHashMap<>();
            Boolean govEnabled = putEnabledProperty(govProps, "store.governance.enabled", governancePrefix, governanceDefaults);
            if (!Boolean.FALSE.equals(govEnabled)) {
                Boolean govApiEnabled = putBooleanProperty(govProps, "store.governance.api-enabled", governancePrefix, governanceDefaults);
                if (!Boolean.FALSE.equals(govApiEnabled)) {
                    putBooleanProperty(govProps, "store.governance.endpoints.proposal.enabled", governancePrefix, governanceDefaults);
                    putBooleanProperty(govProps, "store.governance.endpoints.vote.enabled", governancePrefix, governanceDefaults);
                    putBooleanProperty(govProps, "store.governance.endpoints.constitution.enabled", governancePrefix, governanceDefaults);
                    putBooleanProperty(govProps, "store.governance.endpoints.constitution.live.enabled", governancePrefix, governanceDefaults);
                    putBooleanProperty(govProps, "store.governance.endpoints.committee.enabled", governancePrefix, governanceDefaults);
                    putBooleanProperty(govProps, "store.governance.endpoints.committee.live.enabled", governancePrefix, governanceDefaults);
                    putBooleanProperty(govProps, "store.governance.endpoints.drep.enabled", governancePrefix, governanceDefaults);
                    putBooleanProperty(govProps, "store.governance.endpoints.drep.live.enabled", governancePrefix, governanceDefaults);
                    putBooleanProperty(govProps, "store.governance.endpoints.delegation-vote.enabled", governancePrefix, governanceDefaults);
                }
                putBooleanProperty(govProps, "store.governance.n2c-gov-state-enabled", governancePrefix, governanceDefaults);
                putValueProperty(govProps, "store.governance.n2c-gov-state-fetching-interval-in-minutes", governancePrefix, governanceDefaults);
                putBooleanProperty(govProps, "store.governance.n2c-drep-stake-enabled", governancePrefix, governanceDefaults);
            }
            sections.add(ConfigSectionDto.builder()
                    .name("Governance Store")
                    .properties(govProps)
                    .build());
        }

        // Submit Store
        String submitPrefix = "store.submit";
        Object submitDefaults = resolveStoreDefaultsRoot(configBeansByType, "com.bloxbean.cardano.yaci.store.starter.submit.SubmitStoreProperties", "submit");
        if (submitDefaults != null) {
            Map<String, Object> submitProps = new LinkedHashMap<>();
            putEnabledProperty(submitProps, "store.submit.enabled", submitPrefix, submitDefaults);
            sections.add(ConfigSectionDto.builder()
                    .name("Submit Store")
                    .properties(submitProps)
                    .build());
        }

        // Admin Store
        String adminPrefix = "store.admin";
        Object adminDefaults = resolveStoreDefaultsRoot(configBeansByType, "com.bloxbean.cardano.yaci.store.starter.admin.AdminStoreProperties", "admin");
        if (adminDefaults != null) {
            Map<String, Object> adminProps = new LinkedHashMap<>();
            putBooleanProperty(adminProps, "store.admin.api-enabled", adminPrefix, adminDefaults);
            putBooleanProperty(adminProps, "store.admin.auto-recovery-enabled", adminPrefix, adminDefaults);
            putValueProperty(adminProps, "store.admin.health-check-interval", adminPrefix, adminDefaults);

            sections.add(ConfigSectionDto.builder()
                    .name("Admin Store")
                    .properties(adminProps)
                    .build());
        }

        // Account Store
        String accountPrefix = "store.account";
        Object accountDefaults = resolveStoreDefaultsRoot(configBeansByType, "com.bloxbean.cardano.yaci.store.starter.account.AccountStoreAutoConfigProperties", "account");
        if (accountDefaults != null) {
            Map<String, Object> accountProps = new LinkedHashMap<>();
            Boolean accountEnabled = putEnabledProperty(accountProps, "store.account.enabled", accountPrefix, accountDefaults);
            if (!Boolean.FALSE.equals(accountEnabled)) {
                putBooleanProperty(accountProps, "store.account.api-enabled", accountPrefix, accountDefaults);
                putBooleanProperty(accountProps, "store.account.balance-aggregation-enabled", accountPrefix, accountDefaults);
                putBooleanProperty(accountProps, "store.account.pruning-enabled", accountPrefix, accountDefaults);
                putValueProperty(accountProps, "store.account.pruning-batch-size", accountPrefix, accountDefaults);
                putValueProperty(accountProps, "store.account.pruning-interval", accountPrefix, accountDefaults);
                putBooleanProperty(accountProps, "store.account.history-cleanup-enabled", accountPrefix, accountDefaults);
                putValueProperty(accountProps, "store.account.max-balance-records-per-address-per-batch", accountPrefix, accountDefaults);
                putBooleanProperty(accountProps, "store.account.address-balance-enabled", accountPrefix, accountDefaults);
                putBooleanProperty(accountProps, "store.account.stake-address-balance-enabled", accountPrefix, accountDefaults);
                putValueProperty(accountProps, "store.account.balance-history-cleanup-interval", accountPrefix, accountDefaults);
                putValueProperty(accountProps, "store.account.balance-cleanup-slot-count", accountPrefix, accountDefaults);
                putValueProperty(accountProps, "store.account.balance-cleanup-batch-threshold", accountPrefix, accountDefaults);
                putBooleanProperty(accountProps, "store.account.save-address-tx-amount", accountPrefix, accountDefaults);
                putBooleanProperty(accountProps, "store.account.address-tx-amount-include-zero-amount", accountPrefix, accountDefaults);
                putBooleanProperty(accountProps, "store.account.address-tx-amount-exclude-zero-token-amount", accountPrefix, accountDefaults);
                putValueProperty(accountProps, "store.account.initial-balance-snapshot-block", accountPrefix, accountDefaults);
                putValueProperty(accountProps, "store.account.balance-calc-job-batch-size", accountPrefix, accountDefaults);
                putValueProperty(accountProps, "store.account.balance-calc-job-partition-size", accountPrefix, accountDefaults);
                putValueProperty(accountProps, "store.account.balance-calc-batch-mode", accountPrefix, accountDefaults);
                putBooleanProperty(accountProps, "store.account.content-aware-rollback", accountPrefix, accountDefaults);
                putBooleanProperty(accountProps, "store.account.current-balance-enabled", accountPrefix, accountDefaults);
            }
            sections.add(ConfigSectionDto.builder()
                    .name("Account Store")
                    .properties(accountProps)
                    .build());
        }

        // Epoch Aggregation Store
        String epochAggrPrefix = "store.epoch-aggr";
        Object epochAggrDefaults = resolveStoreDefaultsRoot(configBeansByType, "com.bloxbean.cardano.yaci.store.starter.epochaggr.EpochAggrStoreAutoConfigProperties", "epochAggr");
        if (epochAggrDefaults != null) {
            Map<String, Object> epochAggrProps = new LinkedHashMap<>();
            Boolean epochAggrEnabled = putEnabledProperty(epochAggrProps, "store.epoch-aggr.enabled", epochAggrPrefix, epochAggrDefaults);
            if (!Boolean.FALSE.equals(epochAggrEnabled)) {
                Boolean epochAggrApiEnabled = putBooleanProperty(epochAggrProps, "store.epoch-aggr.api-enabled", epochAggrPrefix, epochAggrDefaults);
                if (!Boolean.FALSE.equals(epochAggrApiEnabled)) {
                    putBooleanProperty(epochAggrProps, "store.epoch-aggr.endpoints.epoch.enabled", epochAggrPrefix, epochAggrDefaults);
                }
                putBooleanProperty(epochAggrProps, "store.epoch-aggr.epoch-calculation-enabled", epochAggrPrefix, epochAggrDefaults);
                putValueProperty(epochAggrProps, "store.epoch-aggr.epoch-calculation-interval", epochAggrPrefix, epochAggrDefaults);
            }
            sections.add(ConfigSectionDto.builder()
                    .name("Epoch Aggregation Store")
                    .properties(epochAggrProps)
                    .build());
        }

        // AdaPot Store
        String adaPotPrefix = "store.adapot";
        Object adaPotDefaults = resolveStoreDefaultsRoot(configBeansByType, "com.bloxbean.cardano.yaci.store.starter.adapot.AdaPotAutoConfigProperties", "adaPot");
        if (adaPotDefaults != null) {
            Map<String, Object> adaPotProps = new LinkedHashMap<>();
            Boolean adaPotEnabled = putEnabledProperty(adaPotProps, "store.adapot.enabled", adaPotPrefix, adaPotDefaults);
            if (!Boolean.FALSE.equals(adaPotEnabled)) {
                putBooleanProperty(adaPotProps, "store.adapot.api-enabled", adaPotPrefix, adaPotDefaults);
                putValueProperty(adaPotProps, "store.adapot.update-reward-db-batch-size", adaPotPrefix, adaPotDefaults);
                putBooleanProperty(adaPotProps, "store.adapot.bulk-update-reward", adaPotPrefix, adaPotDefaults);
                putBooleanProperty(adaPotProps, "store.adapot.bulk-update-reward-with-copy", adaPotPrefix, adaPotDefaults);
                putValueProperty(adaPotProps, "store.adapot.reward-bulk-load-work-mem", adaPotPrefix, adaPotDefaults);
                putValueProperty(adaPotProps, "store.adapot.reward-bulk-load-maintenance-work-mem", adaPotPrefix, adaPotDefaults);
                putValueProperty(adaPotProps, "store.adapot.stake-snapshot-work-mem", adaPotPrefix, adaPotDefaults);
                putBooleanProperty(adaPotProps, "store.adapot.verify-adapot-calc-values", adaPotPrefix, adaPotDefaults);
                putBooleanProperty(adaPotProps, "store.adapot.epoch-stake-pruning-enabled", adaPotPrefix, adaPotDefaults);
                putValueProperty(adaPotProps, "store.adapot.epoch-stake-pruning-interval", adaPotPrefix, adaPotDefaults);
                putValueProperty(adaPotProps, "store.adapot.epoch-stake-safe-epochs", adaPotPrefix, adaPotDefaults);
                putValueProperty(adaPotProps, "store.adapot.epoch-stake-pruning-batch-size", adaPotPrefix, adaPotDefaults);
                putBooleanProperty(adaPotProps, "store.adapot.reward-pruning-enabled", adaPotPrefix, adaPotDefaults);
                putValueProperty(adaPotProps, "store.adapot.reward-pruning-interval", adaPotPrefix, adaPotDefaults);
                putValueProperty(adaPotProps, "store.adapot.reward-pruning-safe-slots", adaPotPrefix, adaPotDefaults);
                putValueProperty(adaPotProps, "store.adapot.reward-pruning-batch-size", adaPotPrefix, adaPotDefaults);
                putBooleanProperty(adaPotProps, "store.adapot.metrics.enabled", adaPotPrefix, adaPotDefaults);
                putValueProperty(adaPotProps, "store.adapot.metrics.update-interval", adaPotPrefix, adaPotDefaults);
            }
            sections.add(ConfigSectionDto.builder()
                    .name("AdaPot Store")
                    .properties(adaPotProps)
                    .build());
        }

        // Governance Aggregation Store
        String govAggrPrefix = "store.governance-aggr";
        Object govAggrDefaults = resolveStoreDefaultsRoot(configBeansByType, "com.bloxbean.cardano.yaci.store.starter.governanceaggr.GovernanceAggrAutoConfigProperties", "governanceAggr");
        if (govAggrDefaults != null) {
            Map<String, Object> govAggrProps = new LinkedHashMap<>();
            Boolean govAggrEnabled = putEnabledProperty(govAggrProps, "store.governance-aggr.enabled", govAggrPrefix, govAggrDefaults);
            if (!Boolean.FALSE.equals(govAggrEnabled)) {
                Boolean govAggrApiEnabled = putBooleanProperty(govAggrProps, "store.governance-aggr.api-enabled", govAggrPrefix, govAggrDefaults);
                if (!Boolean.FALSE.equals(govAggrApiEnabled)) {
                    putBooleanProperty(govAggrProps, "store.governance-aggr.endpoints.governance.enabled", govAggrPrefix, govAggrDefaults);
                }
                putBooleanProperty(govAggrProps, "store.governance-aggr.devnet-conway-bootstrap-available", govAggrPrefix, govAggrDefaults);
                putValueProperty(govAggrProps, "store.governance-aggr.drep-dist-work-mem", govAggrPrefix, govAggrDefaults);
            }
            sections.add(ConfigSectionDto.builder()
                    .name("Governance Aggregation Store")
                    .properties(govAggrProps)
                    .build());
        }

        return sections;
    }

    private void putValueProperty(Map<String, Object> properties, String key, String prefix, Object defaultsRoot) {
        // Prefer explicit config; fall back to default values from the starter properties class if available.
        String value = environment.getProperty(key);
        if (value != null && !value.isEmpty()) {
            properties.put(key, value);
            return;
        }

        Object defaultValue = getDefaultValue(defaultsRoot, key, prefix);
        if (defaultValue instanceof String && ((String) defaultValue).isEmpty()) {
            properties.put(key, NOT_SET);
        } else if (defaultValue != null) {
            properties.put(key, defaultValue);
        } else {
            properties.put(key, NOT_SET);
        }
    }

    private Boolean putBooleanProperty(Map<String, Object> properties, String key, String prefix, Object defaultsRoot) {
        // Booleans should stay typed for UI formatting; use defaults only when unset.
        Boolean value = environment.getProperty(key, Boolean.class);
        if (value != null) {
            properties.put(key, value);
            return value;
        }

        Object defaultValue = getDefaultValue(defaultsRoot, key, prefix);
        if (defaultValue instanceof Boolean) {
            properties.put(key, defaultValue);
            return (Boolean) defaultValue;
        }
        properties.put(key, NOT_SET);
        return null;
    }

    private Boolean putEnabledProperty(Map<String, Object> properties, String key, String prefix, Object defaultsRoot) {
        // Enabled gates the rest of the properties; resolve it first with the same fallback logic.
        Boolean value = environment.getProperty(key, Boolean.class);
        if (value != null) {
            properties.put(key, value);
            return value;
        }

        Object defaultValue = getDefaultValue(defaultsRoot, key, prefix);
        if (defaultValue instanceof Boolean) {
            properties.put(key, defaultValue);
            return (Boolean) defaultValue;
        }
        properties.put(key, NOT_SET);
        return null;
    }

    private Map<String, ConfigurationPropertiesBean> getConfigurationPropertiesBeansByType() {
        // Build a lookup by user class name so we can resolve defaults without class loading.
        Map<String, ConfigurationPropertiesBean> byType = new LinkedHashMap<>();
        for (ConfigurationPropertiesBean bean : ConfigurationPropertiesBean.getAll(applicationContext).values()) {
            Object instance = bean.getInstance();
            if (instance == null) {
                continue;
            }
            Class<?> userClass = ClassUtils.getUserClass(instance);
            if (userClass != null && !byType.containsKey(userClass.getName())) {
                byType.put(userClass.getName(), bean);
            }
        }
        return byType;
    }

    private Object resolveStoreDefaultsRoot(Map<String, ConfigurationPropertiesBean> configBeansByType, String className, String rootProperty) {
        // Use the already-registered @ConfigurationProperties bean to avoid reflection and AOT issues.
        ConfigurationPropertiesBean propertiesBean = configBeansByType.get(className);
        if (propertiesBean == null) {
            return null;
        }
        Object instance = propertiesBean.getInstance();
        if (instance == null) {
            return null;
        }

        try {
            BeanWrapper wrapper = new BeanWrapperImpl(instance);
            if (!wrapper.isReadableProperty(rootProperty)) {
                return null;
            }
            Object root = wrapper.getPropertyValue(rootProperty);
            if (root == null) {
                Class<?> rootType = wrapper.getPropertyType(rootProperty);
                if (rootType != null) {
                    // Ensure nested root exists so defaults can be read even if nothing is bound.
                    root = BeanUtils.instantiateClass(rootType);
                    wrapper.setPropertyValue(rootProperty, root);
                }
            }
            return root;
        } catch (Exception e) {
            log.debug("Unable to resolve defaults for {}", className, e);
            return null;
        }
    }

    private Object getDefaultValue(Object defaultsRoot, String key, String prefix) {
        // Map "store.<prefix>.<kebab-case>" to the bean property path to read defaults.
        if (defaultsRoot == null) {
            return null;
        }
        String propertyPath = toBeanPath(key, prefix);
        if (propertyPath == null || propertyPath.isEmpty()) {
            return null;
        }
        BeanWrapper wrapper = new BeanWrapperImpl(defaultsRoot);
        if (!wrapper.isReadableProperty(propertyPath)) {
            return null;
        }
        return wrapper.getPropertyValue(propertyPath);
    }

    private String toBeanPath(String key, String prefix) {
        // Strip the prefix and convert each segment to camelCase for BeanWrapper access.
        String trimmedKey = key;
        if (prefix != null && !prefix.isEmpty()) {
            String prefixWithDot = prefix + ".";
            if (!key.startsWith(prefixWithDot)) {
                return null;
            }
            trimmedKey = key.substring(prefixWithDot.length());
        }
        if (trimmedKey.isEmpty()) {
            return "";
        }
        String[] parts = trimmedKey.split("\\.");
        StringBuilder path = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            if (i > 0) {
                path.append('.');
            }
            path.append(toCamelCase(parts[i]));
        }
        return path.toString();
    }

    private String toCamelCase(String value) {
        // Convert kebab-case to camelCase (e.g., "cbor-pruning-safe-slots" -> "cborPruningSafeSlots").
        StringBuilder result = new StringBuilder();
        boolean upperNext = false;
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            if (ch == '-') {
                upperNext = true;
                continue;
            }
            if (upperNext) {
                result.append(Character.toUpperCase(ch));
                upperNext = false;
            } else {
                result.append(ch);
            }
        }
        return result.toString();
    }

    private String maskIfEmpty(String value) {
        return value == null || value.isEmpty() ? NOT_SET : value;
    }
}
