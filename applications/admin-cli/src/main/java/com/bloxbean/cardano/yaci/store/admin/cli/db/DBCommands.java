package com.bloxbean.cardano.yaci.store.admin.cli.db;

import com.bloxbean.cardano.yaci.store.admin.cli.Groups;
import com.bloxbean.cardano.yaci.store.dbutils.index.model.IndexDefinition;
import com.bloxbean.cardano.yaci.store.dbutils.index.model.TableIndex;
import com.bloxbean.cardano.yaci.store.dbutils.index.service.IndexService;
import com.bloxbean.cardano.yaci.store.dbutils.index.util.IndexLoader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.shell.command.annotation.Command;
import org.springframework.shell.command.annotation.Option;

import java.util.List;

import static com.bloxbean.cardano.yaci.store.admin.cli.common.ConsoleWriter.*;

@Command(group = Groups.DB_CMD_GROUP)
@RequiredArgsConstructor
@Slf4j
public class DBCommands {
    private final static String INDEX_FILE = "index.yml";
    private final static String EXTRA_INDEX_FILE = "extra-index.yml";

    private final IndexService indexService;

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

        List<TableIndex> existsList = resultPair.getFirst();
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

}
