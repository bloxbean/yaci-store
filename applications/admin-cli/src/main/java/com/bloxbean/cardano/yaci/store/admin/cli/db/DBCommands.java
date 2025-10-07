package com.bloxbean.cardano.yaci.store.admin.cli.db;

import com.bloxbean.cardano.yaci.store.admin.cli.Groups;
import com.bloxbean.cardano.yaci.store.dbutils.index.model.IndexDefinition;
import com.bloxbean.cardano.yaci.store.dbutils.index.model.RollbackConfig;
import com.bloxbean.cardano.yaci.store.dbutils.index.model.RollbackContext;
import com.bloxbean.cardano.yaci.store.dbutils.index.model.TableIndex;
import com.bloxbean.cardano.yaci.store.dbutils.index.model.TableRollbackAction;
import com.bloxbean.cardano.yaci.store.dbutils.index.service.IndexService;
import com.bloxbean.cardano.yaci.store.dbutils.index.service.RollbackService;
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

    @Value("${store.dbutils.rollback_files:rollback.yml}")
    private String rollbackFiles;

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
                         @Option(longNames = "rollback-files", description = "Comma-separated list of rollback YAML files to override default configuration") String rollbackFiles) {
        writeLn(info("Start to rollback data ..."));
        if (isRollbackEpochValid(epoch)) {
            String[] filesToUse = getRollbackFiles(rollbackFiles);
            verifyRollback(filesToUse);
            RollbackContext rollbackContext = RollbackContext.builder()
                    .epoch(epoch)
                    .eventPublisherId(eventPublisherId)
                    .build();

            applyRollback(filesToUse, rollbackContext);
        }
    }

//    @Command(description = "Rollback ledger state data to a previous epoch")
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
        RollbackConfig rollbackConfig;
        
        rollbackConfig = rollbackLoader.loadRollbackConfigFromMultipleFiles(rollbackFiles);

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
}
