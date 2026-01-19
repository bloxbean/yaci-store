package com.bloxbean.cardano.yaci.store.adminui.service;

import com.bloxbean.cardano.yaci.store.adminui.dto.IndexStatusDto;
import com.bloxbean.cardano.yaci.store.dbutils.index.model.IndexDefinition;
import com.bloxbean.cardano.yaci.store.dbutils.index.model.TableIndex;
import com.bloxbean.cardano.yaci.store.dbutils.index.service.IndexService;
import com.bloxbean.cardano.yaci.store.dbutils.index.util.IndexLoader;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Service to retrieve index status using IndexService from dbutils.
 * This checks only app-specific indexes defined in index.yml and extra-index.yml.
 */
@Service
@Slf4j
public class IndexStatusService {

    private static final String INDEX_FILE = "index.yml";
    private static final String EXTRA_INDEX_FILE = "extra-index.yml";

    @Autowired(required = false)
    private IndexService indexService;

    public List<IndexStatusDto> getIndexStatuses() {
        if (indexService == null) {
            log.debug("IndexService not available");
            return Collections.emptyList();
        }

        try {
            List<IndexStatusDto> results = new ArrayList<>();

            // Load and verify indexes from both files
            processIndexFile(INDEX_FILE, results);
            processIndexFile(EXTRA_INDEX_FILE, results);

            // Sort by table name, then index name
            results.sort(Comparator.comparing(IndexStatusDto::getTableName)
                    .thenComparing(IndexStatusDto::getName));

            return results;
        } catch (Exception e) {
            log.warn("Could not get index status: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    private void processIndexFile(String indexFile, List<IndexStatusDto> results) {
        try {
            IndexLoader indexLoader = new IndexLoader();
            List<IndexDefinition> indexDefinitions = indexLoader.loadIndexes(indexFile);

            if (indexDefinitions == null || indexDefinitions.isEmpty()) {
                log.debug("No indexes found in {}", indexFile);
                return;
            }

            Pair<List<TableIndex>, List<TableIndex>> verifyResult =
                    indexService.verifyIndexes(indexDefinitions);

            // Add existing indexes
            for (TableIndex tableIndex : verifyResult.getFirst()) {
                results.add(IndexStatusDto.builder()
                        .name(tableIndex.getIndex())
                        .tableName(tableIndex.getTableName())
                        .exists(true)
                        .columns(Collections.emptyList())  // Column info not available from verifyIndexes
                        .build());
            }

            // Add missing indexes
            for (TableIndex tableIndex : verifyResult.getSecond()) {
                results.add(IndexStatusDto.builder()
                        .name(tableIndex.getIndex())
                        .tableName(tableIndex.getTableName())
                        .exists(false)
                        .columns(Collections.emptyList())
                        .build());
            }

        } catch (IllegalArgumentException e) {
            // File not found - this is OK, not all deployments have both files
            log.debug("Index file not found: {}", indexFile);
        } catch (Exception e) {
            log.warn("Error processing index file {}: {}", indexFile, e.getMessage());
        }
    }
}
