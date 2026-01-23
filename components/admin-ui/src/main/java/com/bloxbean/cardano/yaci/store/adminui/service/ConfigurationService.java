package com.bloxbean.cardano.yaci.store.adminui.service;

import com.bloxbean.cardano.yaci.store.adminui.dto.ConfigSectionDto;
import com.bloxbean.cardano.yaci.store.common.config.StoreProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class ConfigurationService {
    private final StoreProperties storeProperties;
    private final Environment environment;

    @Autowired(required = false)
    private com.bloxbean.cardano.yaci.store.starter.utxo.UtxoStoreAutoConfigProperties utxoStoreAutoConfigProperties;
    @Autowired(required = false)
    private com.bloxbean.cardano.yaci.store.starter.blocks.BlocksStoreAutoConfigProperties blocksStoreAutoConfigProperties;
    @Autowired(required = false)
    private com.bloxbean.cardano.yaci.store.starter.transaction.TransactionAutoConfigProperties transactionAutoConfigProperties;
    @Autowired(required = false)
    private com.bloxbean.cardano.yaci.store.starter.script.ScriptStoreProperties scriptStoreProperties;
    @Autowired(required = false)
    private com.bloxbean.cardano.yaci.store.starter.metadata.MetadataStoreProperties metadataStoreProperties;
    @Autowired(required = false)
    private com.bloxbean.cardano.yaci.store.starter.assets.AssetsStoreProperties assetsStoreProperties;
    @Autowired(required = false)
    private com.bloxbean.cardano.yaci.store.starter.epoch.EpochStoreProperties epochStoreProperties;
    @Autowired(required = false)
    private com.bloxbean.cardano.yaci.store.starter.staking.StakingStoreProperties stakingStoreProperties;
    @Autowired(required = false)
    private com.bloxbean.cardano.yaci.store.starter.mir.MIRStoreProperties mirStoreProperties;
    @Autowired(required = false)
    private com.bloxbean.cardano.yaci.store.starter.governance.GovernanceStoreProperties governanceStoreProperties;
    @Autowired(required = false)
    private com.bloxbean.cardano.yaci.store.starter.submit.SubmitStoreProperties submitStoreProperties;
    @Autowired(required = false)
    private com.bloxbean.cardano.yaci.store.starter.account.AccountStoreAutoConfigProperties accountStoreAutoConfigProperties;
    @Autowired(required = false)
    private com.bloxbean.cardano.yaci.store.starter.epochaggr.EpochAggrStoreAutoConfigProperties epochAggrStoreAutoConfigProperties;
    @Autowired(required = false)
    private com.bloxbean.cardano.yaci.store.starter.adapot.AdaPotAutoConfigProperties adaPotAutoConfigProperties;
    @Autowired(required = false)
    private com.bloxbean.cardano.yaci.store.starter.governanceaggr.GovernanceAggrAutoConfigProperties governanceAggrAutoConfigProperties;
    @Autowired(required = false)
    private com.bloxbean.cardano.yaci.store.starter.live.LiveStoreProperties liveStoreProperties;
    @Autowired(required = false)
    private com.bloxbean.cardano.yaci.store.starter.admin.AdminStoreProperties adminStoreProperties;

    public ConfigurationService(StoreProperties storeProperties, Environment environment) {
        this.storeProperties = storeProperties;
        this.environment = environment;
    }

    public List<ConfigSectionDto> getConfiguration() {
        List<ConfigSectionDto> sections = new ArrayList<>();

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
        Map<String, Object> utxoProps = new LinkedHashMap<>();
        if (utxoStoreAutoConfigProperties != null && utxoStoreAutoConfigProperties.getUtxo() != null) {
            var utxo = utxoStoreAutoConfigProperties.getUtxo();
            utxoProps.put("store.utxo.enabled", utxo.isEnabled());
            utxoProps.put("store.utxo.api-enabled", utxo.isApiEnabled());
            utxoProps.put("store.utxo.save-address", utxo.isSaveAddress());
            utxoProps.put("store.utxo.pruning-enabled", utxo.isPruningEnabled());
            utxoProps.put("store.utxo.pruning-interval", utxo.getPruningInterval());
            utxoProps.put("store.utxo.pruning-safe-blocks", utxo.getPruningSafeBlocks());
            utxoProps.put("store.utxo.address-cache-enabled", utxo.isAddressCacheEnabled());
            utxoProps.put("store.utxo.address-cache-size", utxo.getAddressCacheSize());
            utxoProps.put("store.utxo.address-cache-expiry-after-access", utxo.getAddressCacheExpiryAfterAccess());
            utxoProps.put("store.utxo.content-aware-rollback", utxo.isContentAwareRollback());
        }
        sections.add(ConfigSectionDto.builder()
                .name("Utxo Store")
                .properties(utxoProps)
                .build());

        // Blocks Store
        Map<String, Object> blocksProps = new LinkedHashMap<>();
        if (blocksStoreAutoConfigProperties != null && blocksStoreAutoConfigProperties.getBlocks() != null) {
            var blocks = blocksStoreAutoConfigProperties.getBlocks();
            blocksProps.put("store.blocks.enabled", blocks.isEnabled());
            blocksProps.put("store.blocks.api-enabled", blocks.isApiEnabled());
            blocksProps.put("store.blocks.save-cbor", blocks.isSaveCbor());
            blocksProps.put("store.blocks.cbor-pruning-enabled", blocks.isCborPruningEnabled());
            blocksProps.put("store.blocks.cbor-pruning-safe-slots", blocks.getCborPruningSafeSlots());
            if (blocks.getMetrics() != null) {
                blocksProps.put("store.blocks.metrics.enabled", blocks.getMetrics().isEnabled());
                blocksProps.put("store.blocks.metrics.update-interval", blocks.getMetrics().getUpdateInterval());
            }
        }
        sections.add(ConfigSectionDto.builder()
                .name("Blocks Store")
                .properties(blocksProps)
                .build());

        // Transaction Store
        Map<String, Object> txProps = new LinkedHashMap<>();
        if (transactionAutoConfigProperties != null && transactionAutoConfigProperties.getTransaction() != null) {
            var tx = transactionAutoConfigProperties.getTransaction();
            txProps.put("store.transaction.enabled", tx.isEnabled());
            txProps.put("store.transaction.api-enabled", tx.isApiEnabled());
            txProps.put("store.transaction.pruning-enabled", tx.isPruningEnabled());
            txProps.put("store.transaction.pruning-interval", tx.getPruningInterval());
            txProps.put("store.transaction.pruning-safe-slots", tx.getPruningSafeSlots());
            txProps.put("store.transaction.save-witness", tx.isSaveWitness());
            txProps.put("store.transaction.save-cbor", tx.isSaveCbor());
            txProps.put("store.transaction.cbor-pruning-enabled", tx.isCborPruningEnabled());
            txProps.put("store.transaction.cbor-pruning-safe-slots", tx.getCborPruningSafeSlots());
        }
        sections.add(ConfigSectionDto.builder()
                .name("Transaction Store")
                .properties(txProps)
                .build());

        // Script Store
        Map<String, Object> scriptProps = new LinkedHashMap<>();
        if (scriptStoreProperties != null && scriptStoreProperties.getScript() != null) {
            var script = scriptStoreProperties.getScript();
            scriptProps.put("store.script.enabled", script.isEnabled());
            scriptProps.put("store.script.api-enabled", script.isApiEnabled());
        }
        sections.add(ConfigSectionDto.builder()
                .name("Script Store")
                .properties(scriptProps)
                .build());

        // Metadata Store
        Map<String, Object> metadataProps = new LinkedHashMap<>();
        if (metadataStoreProperties != null && metadataStoreProperties.getMetadata() != null) {
            var metadata = metadataStoreProperties.getMetadata();
            metadataProps.put("store.metadata.enabled", metadata.isEnabled());
            metadataProps.put("store.metadata.api-enabled", metadata.isApiEnabled());
        }
        sections.add(ConfigSectionDto.builder()
                .name("Metadata Store")
                .properties(metadataProps)
                .build());

        // Assets Store
        Map<String, Object> assetsProps = new LinkedHashMap<>();
        if (assetsStoreProperties != null && assetsStoreProperties.getAssets() != null) {
            var assets = assetsStoreProperties.getAssets();
            assetsProps.put("store.assets.enabled", assets.isEnabled());
            assetsProps.put("store.assets.api-enabled", assets.isApiEnabled());
        }
        sections.add(ConfigSectionDto.builder()
                .name("Assets Store")
                .properties(assetsProps)
                .build());

        // Epoch Store
        Map<String, Object> epochProps = new LinkedHashMap<>();
        if (epochStoreProperties != null && epochStoreProperties.getEpoch() != null) {
            var epoch = epochStoreProperties.getEpoch();
            epochProps.put("store.epoch.enabled", epoch.isEnabled());
            epochProps.put("store.epoch.api-enabled", epoch.isApiEnabled());
            epochProps.put("store.epoch.n2c-epoch-param-enabled", epoch.isN2cEpochParamEnabled());
            epochProps.put("store.epoch.n2c-protocol-param-fetching-interval-in-minutes", epoch.getN2cProtocolParamFetchingIntervalInMinutes());
        }
        sections.add(ConfigSectionDto.builder()
                .name("Epoch Store")
                .properties(epochProps)
                .build());

        // Staking Store
        Map<String, Object> stakingProps = new LinkedHashMap<>();
        if (stakingStoreProperties != null && stakingStoreProperties.getStaking() != null) {
            var staking = stakingStoreProperties.getStaking();
            stakingProps.put("store.staking.enabled", staking.isEnabled());
            stakingProps.put("store.staking.api-enabled", staking.isApiEnabled());
        }
        sections.add(ConfigSectionDto.builder()
                .name("Staking Store")
                .properties(stakingProps)
                .build());

        // MIR Store
        Map<String, Object> mirProps = new LinkedHashMap<>();
        if (mirStoreProperties != null && mirStoreProperties.getMir() != null) {
            var mir = mirStoreProperties.getMir();
            mirProps.put("store.mir.enabled", mir.isEnabled());
            mirProps.put("store.mir.api-enabled", mir.isApiEnabled());
        }
        sections.add(ConfigSectionDto.builder()
                .name("MIR Store")
                .properties(mirProps)
                .build());

        // Governance Store
        Map<String, Object> govProps = new LinkedHashMap<>();
        if (governanceStoreProperties != null && governanceStoreProperties.getGovernance() != null) {
            var gov = governanceStoreProperties.getGovernance();
            govProps.put("store.governance.enabled", gov.isEnabled());
            govProps.put("store.governance.api-enabled", gov.isApiEnabled());
            govProps.put("store.governance.n2c-gov-state-enabled", gov.isN2cGovStateEnabled());
            govProps.put("store.governance.n2c-gov-state-fetching-interval-in-minutes", gov.getN2cGovStateFetchingIntervalInMinutes());
            govProps.put("store.governance.n2c-drep-stake-enabled", gov.isN2cDrepStakeEnabled());
        }
        sections.add(ConfigSectionDto.builder()
                .name("Governance Store")
                .properties(govProps)
                .build());

        // Submit Store
        Map<String, Object> submitProps = new LinkedHashMap<>();
        if (submitStoreProperties != null && submitStoreProperties.getSubmit() != null) {
            submitProps.put("store.submit.enabled", submitStoreProperties.getSubmit().isEnabled());
        }
        sections.add(ConfigSectionDto.builder()
                .name("Submit Store")
                .properties(submitProps)
                .build());

        // Admin Store
        Map<String, Object> adminProps = new LinkedHashMap<>();
        if (adminStoreProperties != null && adminStoreProperties.getAdmin() != null) {
            var admin = adminStoreProperties.getAdmin();
            adminProps.put("store.admin.api-enabled", admin.isApiEnabled());
            adminProps.put("store.admin.auto-recovery-enabled", admin.isAutoRecoveryEnabled());
            adminProps.put("store.admin.health-check-interval", admin.getHealthCheckInterval());
        }

        sections.add(ConfigSectionDto.builder()
                .name("Admin Store")
                .properties(adminProps)
                .build());

        // Account Store
        Map<String, Object> accountProps = new LinkedHashMap<>();
        if (accountStoreAutoConfigProperties != null && accountStoreAutoConfigProperties.getAccount() != null) {
            var account = accountStoreAutoConfigProperties.getAccount();
            accountProps.put("store.account.enabled", account.isEnabled());
            accountProps.put("store.account.api-enabled", account.isApiEnabled());
            accountProps.put("store.account.balance-aggregation-enabled", account.isBalanceAggregationEnabled());
            accountProps.put("store.account.pruning-enabled", account.isPruningEnabled());
            accountProps.put("store.account.pruning-batch-size", account.getPruningBatchSize());
            accountProps.put("store.account.pruning-interval", account.getPruningInterval());
            accountProps.put("store.account.history-cleanup-enabled", account.isHistoryCleanupEnabled());
            accountProps.put("store.account.address-balance-enabled", account.isAddressBalanceEnabled());
            accountProps.put("store.account.stake-address-balance-enabled", account.isStakeAddressBalanceEnabled());
            accountProps.put("store.account.save-address-tx-amount", account.isSaveAddressTxAmount());
            accountProps.put("store.account.balance-calc-job-batch-size", account.getBalanceCalcJobBatchSize());
            accountProps.put("store.account.balance-calc-job-partition-size", account.getBalanceCalcJobPartitionSize());
            accountProps.put("store.account.balance-calc-batch-mode", account.getBalanceCalcBatchMode());
            accountProps.put("store.account.content-aware-rollback", account.isContentAwareRollback());
            accountProps.put("store.account.current-balance-enabled", account.isCurrentBalanceEnabled());
        }
        sections.add(ConfigSectionDto.builder()
                .name("Account Store")
                .properties(accountProps)
                .build());

        // Epoch Aggregation Store
        Map<String, Object> epochAggrProps = new LinkedHashMap<>();
        if (epochAggrStoreAutoConfigProperties != null && epochAggrStoreAutoConfigProperties.getEpochAggr() != null) {
            var epochAggr = epochAggrStoreAutoConfigProperties.getEpochAggr();
            epochAggrProps.put("store.epoch-aggr.enabled", epochAggr.isEnabled());
            epochAggrProps.put("store.epoch-aggr.api-enabled", epochAggr.isApiEnabled());
            epochAggrProps.put("store.epoch-aggr.epoch-calculation-enabled", epochAggr.isEpochCalculationEnabled());
            epochAggrProps.put("store.epoch-aggr.epoch-calculation-interval", epochAggr.getEpochCalculationInterval());
        }
        sections.add(ConfigSectionDto.builder()
                .name("Epoch Aggregation Store")
                .properties(epochAggrProps)
                .build());

        // AdaPot Store
        Map<String, Object> adaPotProps = new LinkedHashMap<>();
        if (adaPotAutoConfigProperties != null && adaPotAutoConfigProperties.getAdaPot() != null) {
            var adaPot = adaPotAutoConfigProperties.getAdaPot();
            adaPotProps.put("store.adapot.enabled", adaPot.isEnabled());
            adaPotProps.put("store.adapot.api-enabled", adaPot.isApiEnabled());
            adaPotProps.put("store.adapot.update-reward-db-batch-size", adaPot.getUpdateRewardDbBatchSize());
            adaPotProps.put("store.adapot.bulk-update-reward", adaPot.isBulkUpdateReward());
            adaPotProps.put("store.adapot.bulk-update-reward-with-copy", adaPot.isBulkUpdateRewardWithCopy());
            adaPotProps.put("store.adapot.verify-adapot-calc-values", adaPot.isVerifyAdapotCalcValues());
            adaPotProps.put("store.adapot.epoch-stake-pruning-enabled", adaPot.isEpochStakePruningEnabled());
            adaPotProps.put("store.adapot.epoch-stake-pruning-interval", adaPot.getEpochStakePruningInterval());
            adaPotProps.put("store.adapot.epoch-stake-safe-epochs", adaPot.getEpochStakeSafeEpochs());
            adaPotProps.put("store.adapot.reward-pruning-enabled", adaPot.isRewardPruningEnabled());
            adaPotProps.put("store.adapot.reward-pruning-interval", adaPot.getRewardPruningInterval());
            adaPotProps.put("store.adapot.reward-pruning-safe-slots", adaPot.getRewardPruningSafeSlots());
            if (adaPot.getMetrics() != null) {
                adaPotProps.put("store.adapot.metrics.enabled", adaPot.getMetrics().isEnabled());
                adaPotProps.put("store.adapot.metrics.update-interval", adaPot.getMetrics().getUpdateInterval());
            }
        }
        sections.add(ConfigSectionDto.builder()
                .name("AdaPot Store")
                .properties(adaPotProps)
                .build());

        // Governance Aggregation Store
        Map<String, Object> govAggrProps = new LinkedHashMap<>();
        if (governanceAggrAutoConfigProperties != null && governanceAggrAutoConfigProperties.getGovernanceAggr() != null) {
            var govAggr = governanceAggrAutoConfigProperties.getGovernanceAggr();
            govAggrProps.put("store.governance-aggr.enabled", govAggr.isEnabled());
            govAggrProps.put("store.governance-aggr.api-enabled", govAggr.isApiEnabled());
            govAggrProps.put("store.governance-aggr.devnet-conway-bootstrap-available", govAggr.isDevnetConwayBootstrapAvailable());
            govAggrProps.put("store.governance-aggr.drep-dist-work-mem", maskIfEmpty(govAggr.getDrepDistWorkMem()));
        }
        sections.add(ConfigSectionDto.builder()
                .name("Governance Aggregation Store")
                .properties(govAggrProps)
                .build());

        // Live Store
        Map<String, Object> liveProps = new LinkedHashMap<>();
        if (liveStoreProperties != null && liveStoreProperties.getLive() != null) {
            var live = liveStoreProperties.getLive();
            liveProps.put("store.live.enabled", live.isEnabled());
            liveProps.put("store.live.api-enabled", live.isApiEnabled());
        }
        sections.add(ConfigSectionDto.builder()
                .name("Live Store")
                .properties(liveProps)
                .build());

        return sections;
    }

    private String maskIfEmpty(String value) {
        return value == null || value.isEmpty() ? "(not set)" : value;
    }
}
