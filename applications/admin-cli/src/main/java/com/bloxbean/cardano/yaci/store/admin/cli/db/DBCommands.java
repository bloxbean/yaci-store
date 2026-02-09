package com.bloxbean.cardano.yaci.store.admin.cli.db;

import com.bloxbean.cardano.yaci.store.admin.cli.Groups;
import com.bloxbean.cardano.yaci.store.dbutils.index.model.IndexDefinition;
import com.bloxbean.cardano.yaci.store.dbutils.index.model.RollbackConfig;
import com.bloxbean.cardano.yaci.store.dbutils.index.model.RollbackContext;
import com.bloxbean.cardano.yaci.store.dbutils.index.model.TableIndex;
import com.bloxbean.cardano.yaci.store.dbutils.index.model.TableRollbackAction;
import com.bloxbean.cardano.yaci.store.dbutils.index.service.IndexService;
import com.bloxbean.cardano.yaci.store.dbutils.index.service.RollbackService;
import com.bloxbean.cardano.yaci.store.dbutils.index.util.DatabaseUtils;
import com.bloxbean.cardano.yaci.store.dbutils.index.util.IndexLoader;
import com.bloxbean.cardano.yaci.store.dbutils.index.util.RollbackLoader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.util.Pair;
import org.springframework.shell.command.annotation.Command;
import org.springframework.shell.command.annotation.Option;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static com.bloxbean.cardano.yaci.store.admin.cli.common.ConsoleWriter.*;

@Command(group = Groups.DB_CMD_GROUP)
@RequiredArgsConstructor
@Slf4j
public class DBCommands {
    private static final String INDEX_FILE = "index.yml";
    private static final String EXTRA_INDEX_FILE = "extra-index.yml";
    private static final String ROLLBACK_LEDGER_STATE_FILE = "rollback-ledger-state.yml";

    private final IndexService indexService;
    private final RollbackService rollbackService;
    private final DatabaseUtils databaseUtils;

    @Value("${store.admin-cli.db.rollback.rollback-files:rollback.yml}")
    private String rollbackFiles;

    @Value("${store.admin-cli.db.rollback.point.block:#{null}}")
    private Long rollbackPointBlock;

    @Value("${store.admin-cli.db.rollback.point.block-hash:#{null}}")
    private String rollbackPointBlockHash;

    @Value("${store.admin-cli.db.rollback.point.slot:#{null}}")
    private Long rollbackPointSlot;

    @Value("${store.admin-cli.db.rollback.point.era:#{null}}")
    private Integer rollbackPointEra;

    @Value("${store.utxo.pruning-enabled:false}")
    private boolean utxoPruningEnabled;

    @Value("${store.transaction.pruning-enabled:false}")
    private boolean transactionPruningEnabled;

    @Value("${store.adapot.reward-pruning-enabled:false}")
    private boolean rewardPruningEnabled;

    @Value("${store.adapot.epoch-stake-pruning-enabled:false}")
    private boolean epochStakePruningEnabled;

    @Value("${store.account.pruning-enabled:false}")
    private boolean accountPruningEnabled;

    @Command(description = "Apply the default indexes required for read operations.")
    public void applyIndexes(@Option(longNames = "skip-extra-indexes", defaultValue = "false", description = "Skip additional optional indexes.") boolean skipExtraIndexes) {
        writeLn(info("Start to apply index ..."));
        applyIndexes(INDEX_FILE);

        if (!skipExtraIndexes) {
            writeLn(info("Start to apply extra index ..."));
            applyIndexes(EXTRA_INDEX_FILE);
        }
    }

    @Command(description = "Apply only additional optional read indexes")
    public void applyExtraIndexes() {
        writeLn(info("Start to apply extra index ..."));
        applyIndexes(EXTRA_INDEX_FILE);
    }

    @Command(description = "Rollback data to a previous epoch")
    public void rollbackData(@Option(longNames = "epoch", required = true, description = "Epoch to rollback to") int epoch,
                         @Option(longNames = "event-publisher-id", defaultValue = "1", description = "Event Publisher ID") long eventPublisherId,
                         @Option(longNames = "rollback-files", description = "Comma-separated list of rollback YAML files to override default configuration") String rollbackFiles,
                         @Option(longNames = "block", description = "Block number for rollback point (required when block table is not available)") Long block,
                         @Option(longNames = "block-hash", description = "Block hash for rollback point (required when block table is not available)") String blockHash,
                         @Option(longNames = "slot", description = "Slot number for rollback point (required when block table is not available)") Long slot,
                         @Option(longNames = "era", description = "Era for rollback point (required when block table is not available)") Integer era) {
        writeLn(info("Start to rollback data ..."));
        validatePruningConfiguration();

        validateMutuallyExclusiveOptions(epoch, block, blockHash, slot, era);
        
        if (isRollbackEpochValid(epoch)) {
            String[] filesToUse = getRollbackFiles(rollbackFiles);
            verifyRollback(filesToUse);
            
            RollbackContext rollbackContext = RollbackContext.builder()
                    .epoch(epoch)
                    .eventPublisherId(eventPublisherId)
                    .rollbackPointBlock(block != null ? block : rollbackPointBlock)
                    .rollbackPointBlockHash(blockHash != null ? blockHash : rollbackPointBlockHash)
                    .rollbackPointSlot(slot != null ? slot : rollbackPointSlot)
                    .rollbackPointEra(era != null ? era : rollbackPointEra)
                    .build();

            applyRollback(filesToUse, rollbackContext);
        }
    }

    @Command(description = "Rollback ledger state data to a previous epoch")
    public void rollbackLedgerStateData(@Option(longNames = "epoch", required = true, description = "Epoch to rollback to") int epoch) {

        writeLn(info("Start to rollback data ..."));
        if (isRollbackEpochValid(epoch)) {
            String[] files = {ROLLBACK_LEDGER_STATE_FILE};
            verifyRollback(files);
            RollbackContext rollbackContext = RollbackContext.builder()
                    .epoch(epoch)
                    .rollbackLedgerState(true)
                    .build();

            applyRollback(files, rollbackContext);
        }
    }

    private void applyIndexes(String indexFile) {
        IndexLoader indexLoader = new IndexLoader();
        List<IndexDefinition> indexDefinitionList = indexLoader.loadIndexes(indexFile);
        if (indexDefinitionList == null || indexDefinitionList.isEmpty()) {
            log.warn("No optional index found to apply");
            writeLn(warn("No optional index found to apply : {}", indexFile));
            return;
        }

        var result = indexService.applyIndexes(indexDefinitionList);

        if (!result.getSecond().isEmpty()) {
            log.warn(">> Failed to apply these indexes : " + result.getSecond());
        }
    }

    @Command(description = "List missing read indexes")
    public void verifyIndexes() {
        verifyIndexes(INDEX_FILE);
        verifyIndexes(EXTRA_INDEX_FILE);
    }

    private void verifyIndexes(String indexFile) {
        IndexLoader indexLoader = new IndexLoader();
        List<IndexDefinition> indexDefinitionList = indexLoader.loadIndexes(indexFile);
        if (indexDefinitionList == null || indexDefinitionList.isEmpty()) {
            writeLn(warn("No index found in {}", indexFile));
            return;
        }

        var resultPair = indexService.verifyIndexes(indexDefinitionList);

        List<TableIndex> notExistsList = resultPair.getSecond();

        if (!notExistsList.isEmpty()) {
            writeLn(warn("The following indexes are not found from %s", indexFile));
            for (TableIndex tableIndex: notExistsList) {
                writeLn("\tTable: %s, Index: %s", tableIndex.getTableName(), tableIndex.getIndex());
            }
        } else {
            writeLn(success("All optional indexes are present from file : %s", indexFile));
        }
    }

    private String[] getRollbackFiles(String rollbackFiles) {
        if (rollbackFiles != null && !rollbackFiles.trim().isEmpty()) {
            return Arrays.stream(rollbackFiles.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .toArray(String[]::new);
        }

        return new String[]{this.rollbackFiles};
    }

    private void applyRollback(String[] rollbackFiles, RollbackContext rollbackContext) {
        RollbackLoader rollbackLoader = new RollbackLoader();
        RollbackConfig rollbackConfig = rollbackLoader.loadRollbackConfigFromMultipleFiles(rollbackFiles);

        if (rollbackConfig.getTables() == null || rollbackConfig.getTables().isEmpty()) {
            log.warn("No table found to rollback");
            writeLn(warn("No table found to rollback from files: {}", String.join(", ", rollbackFiles)));
            return;
        }

        Pair<List<TableRollbackAction>, Boolean> result = rollbackService.executeRollback(rollbackConfig, rollbackContext);

        if (result.getSecond().equals(Boolean.FALSE)) {
            log.warn(">> Failed to rollback data");
            List<TableRollbackAction> failedTableRollbackActions = result.getFirst();

            for (TableRollbackAction tableRollbackAction : failedTableRollbackActions) {
                writeLn(warn("Failed to rollback table : %s, action : %s", tableRollbackAction.getTableName(), tableRollbackAction.getSql()));
            }
        } else {
            writeLn(success("Data rollback is successful, data is rolled back to epoch : %d", rollbackContext.getEpoch()));
        }
    }

    private void verifyRollback(String[] rollbackFiles) {
        RollbackLoader rollbackLoader = new RollbackLoader();
        List<String> tableNames;
        
        RollbackConfig config = rollbackLoader.loadRollbackConfigFromMultipleFiles(rollbackFiles);
        tableNames = config.getTables().stream()
                .map(RollbackConfig.TableRollbackDefinition::getName)
                .collect(Collectors.toList());

        rollbackService.verifyRollbackActions(tableNames);
    }

    private boolean isRollbackEpochValid(int epoch) {
        if (!rollbackService.isValidRollbackEpoch(epoch)) {
            writeLn(warn("Epoch %d is not a valid rollback epoch. The rollback epoch must be " +
                    "less than or equal to the current max epoch in the database and greater than 0.", epoch));
            return false;
        }
        return true;
    }

    /**
     * Validates that epoch and manual rollback point options are used correctly.
     * Manual rollback point information should only be provided when block table is not available.
     * 
     * @param epoch The epoch number (always required)
     * @param block Manual block number (optional)
     * @param blockHash Manual block hash (optional)
     * @param slot Manual slot number (optional)
     * @param era Manual era (optional)
     * @throws IllegalArgumentException if manual rollback point is provided when block table is available
     */
    private void validateMutuallyExclusiveOptions(int epoch, Long block, String blockHash, Long slot, Integer era) {
        boolean hasManualBlock = block != null || blockHash != null || slot != null || era != null;
        boolean hasConfigBlock = rollbackPointBlock != null || rollbackPointBlockHash != null || 
                               rollbackPointSlot != null || rollbackPointEra != null;
        
        boolean blockTableExists = databaseUtils.tableExists("block");
        
        if ((hasManualBlock || hasConfigBlock) && blockTableExists) {
            String errorMsg = String.format(
                "Error: Manual rollback point information should not be provided when block table is available. " +
                "You provided epoch=%d and manual rollback point information, but block table exists in the database. " +
                "Please use epoch-based rollback instead:\n" +
                "rollback-data --epoch %d\n" +
                "Note: Manual rollback point is only needed when block table is not available in the database.",
                epoch, epoch
            );
            
            writeLn(error(errorMsg));
            throw new IllegalArgumentException("Manual rollback point should not be used when block table is available");
        }
    }

    /**
     * Validates that pruning configurations are safe for rollback operations.
     * Checks all types of pruning: UTXO, Transaction, Reward, Epoch Stake, and Account Balance.
     */
    private void validatePruningConfiguration() {
        boolean anyPruningEnabled = false;
        StringBuilder warningMsg = new StringBuilder();
        
        if (utxoPruningEnabled) {
            anyPruningEnabled = true;
            warningMsg.append("- UTXO Pruning (store.utxo.pruning-enabled=true)\n")
                     .append("  → Historical UTXOs may have been pruned\n")
                     .append("  → Rollback limited to last pruning point + 1\n\n");
        }
        
        if (transactionPruningEnabled) {
            anyPruningEnabled = true;
            warningMsg.append("- Transaction Pruning (store.transaction.pruning-enabled=true)\n")
                     .append("  → Transaction and witness records may have been deleted\n")
                     .append("  → Historical transaction data may be incomplete after rollback\n\n");
        }
        
        if (rewardPruningEnabled) {
            anyPruningEnabled = true;
            warningMsg.append("- Reward Pruning (store.adapot.reward-pruning-enabled=true)\n")
                     .append("  → Withdrawn reward records may have been pruned\n")
                     .append("  → Reward history may be incomplete after rollback\n\n");
        }
        
        if (epochStakePruningEnabled) {
            anyPruningEnabled = true;
            warningMsg.append("- Epoch Stake Pruning (store.adapot.epoch-stake-pruning-enabled=true)\n")
                     .append("  → Old epoch stake records may have been deleted\n")
                     .append("  → Stake snapshots may be incomplete for old epochs\n\n");
        }
        
        if (accountPruningEnabled) {
            anyPruningEnabled = true;
            warningMsg.append("- Account Balance Pruning (store.account.pruning-enabled=true)\n")
                     .append("  → Historical address and stake balance records may have been pruned\n")
                     .append("  → Balance history may be incomplete after rollback\n\n");
        }
        
        if (anyPruningEnabled) {
            String fullWarning = String.format(
                "\n⚠️  WARNING: DATA PRUNING IS ENABLED ⚠️\n\n" +
                "%s" +
                "ROLLBACK RISKS:\n" +
                "1. Historical data needed for rollback may have been permanently deleted\n" +
                "2. Application may fail to start after rollback due to missing data\n" +
                "3. You can only safely rollback to points AFTER the last pruning operation\n" +
                "4. Consider disabling pruning before performing rollbacks\n",
                warningMsg.toString()
            );
            
            writeLn(warn(fullWarning));
        }
    }
}
