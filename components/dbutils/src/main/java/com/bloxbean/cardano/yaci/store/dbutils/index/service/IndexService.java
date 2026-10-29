package com.bloxbean.cardano.yaci.store.dbutils.index.service;

import com.bloxbean.cardano.yaci.store.dbutils.index.model.IndexDefinition;
import com.bloxbean.cardano.yaci.store.dbutils.index.model.TableIndex;
import com.bloxbean.cardano.yaci.store.dbutils.index.util.DatabaseUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.util.Pair;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class IndexService {

    private final JdbcTemplate jdbcTemplate;
    private final DatabaseUtils databaseUtils;

    public Pair<List<IndexDefinition.Index>, List<IndexDefinition.Index>> applyIndexes(List<IndexDefinition> indexDefinitions) {
        var dbTypeOpt = DatabaseUtils.getDbType(jdbcTemplate.getDataSource());
        if (dbTypeOpt.isEmpty()) {
            log.error("Apply Index failure. Couldn't detect database type.");
        }

        log.info("Db type : {}", dbTypeOpt.get());

        List<IndexDefinition.Index> success = new ArrayList<>();
        List<IndexDefinition.Index> failure = new ArrayList<>();
        Set<String> tablesToAnalyze = new LinkedHashSet<>();

        for (IndexDefinition indexDefinition : indexDefinitions) {
            String tableName = indexDefinition.getTable();
            if (databaseUtils.tableExists(tableName)) {
                for (IndexDefinition.Index index : indexDefinition.getIndexes()) {
                    String sql = generateCreateIndexSQL(tableName, index, dbTypeOpt.get());
                    if (sql != null) {
                        log.info("Applying index on table {}, index name {} ", indexDefinition.getTable(), index.getName());
                        try {
                            log.debug(sql);
                            jdbcTemplate.execute(sql);
                            success.add(index);
                            if (dbTypeOpt.get() == DatabaseUtils.DbType.postgres && isExpressionIndex(index))
                                tablesToAnalyze.add(tableName);
                        } catch (Exception e) {
                            log.warn("Failed to apply index on table {}, index name {} ", indexDefinition.getTable(), index.getName());
                            log.warn("Index query: {}", sql);
                            failure.add(index);
                        }
                    } else {
                        log.warn("Failed to apply index on table {}, index name {} ", indexDefinition.getTable(), index.getName());
                        log.warn("Index query is null");
                        failure.add(index);
                    }
                }
            }
        }

        //PostgreSQL keeps statistics of an index expression, which the planner needs to estimate its matches,
        //only from an ANALYZE of the table, and autovacuum may not analyze a large table for a long time
        for (String tableName : tablesToAnalyze) {
            log.info("Analyzing table {} for the statistics of its expression indexes", tableName);
            try {
                jdbcTemplate.execute("ANALYZE " + tableName);
            } catch (Exception e) {
                log.warn("Failed to analyze table {}", tableName, e);
            }
        }

        return Pair.of(success, failure);
    }

    private static boolean isExpressionIndex(IndexDefinition.Index index) {
        return index.getColumns() != null && index.getColumns().stream().anyMatch(column -> column.contains("("));
    }

    private String generateCreateIndexSQL(String tableName, IndexDefinition.Index index, DatabaseUtils.DbType dbType) {
        String indexName = index.getName();
        String columns = String.join(", ", index.getColumns());

        switch (dbType) {
            case postgres:
                if (index.getExcludes() != null && index.getExcludes().contains(DatabaseUtils.DbType.postgres.toString()))
                    return null;

                String using = index.getType() != null? "USING " + index.getType().trim() : "";
                String where = index.getWhere() != null && !index.getWhere().trim().isEmpty()
                        ? " WHERE " + index.getWhere().trim()
                        : "";
                return String.format("CREATE INDEX IF NOT EXISTS %s ON %s %s (%s) %s;", indexName, tableName, using, columns, where);
            case h2:
                if (index.getExcludes() != null && index.getExcludes().contains(DatabaseUtils.DbType.h2.toString()))
                    return null;

                return String.format("CREATE INDEX IF NOT EXISTS %s ON %s (%s);", indexName, tableName, columns);
            case mysql:
                if (index.getExcludes() != null && index.getExcludes().contains(DatabaseUtils.DbType.mysql.toString()))
                    return null;

                return String.format("CREATE INDEX %s ON %s (%s);", indexName, tableName, columns);
            default:
                log.error("Unsupported DB Type. It shouldn't reach here. DB Type: " + dbType);
                return null;
        }
    }

    public Pair<List<TableIndex>, List<TableIndex>> verifyIndexes(List<IndexDefinition> indexDefinitions) {
        var dbTypeOpt = DatabaseUtils.getDbType(jdbcTemplate.getDataSource());
        if (dbTypeOpt.isEmpty()) {
            log.error("Apply Index failure. Couldn't detect database type.");
        }

        log.debug("Db type : {}", dbTypeOpt.get());

        List<TableIndex> exists = new ArrayList<>();
        List<TableIndex> notExists = new ArrayList<>();

        for (IndexDefinition indexDefinition : indexDefinitions) {
            String tableName = indexDefinition.getTable();
            for (IndexDefinition.Index index : indexDefinition.getIndexes()) {
                if (databaseUtils.indexExists(tableName, index.getName()))
                    exists.add(new TableIndex(tableName, index.getName()));
                else
                    notExists.add(new TableIndex(tableName, index.getName()));
            }
        }

        return Pair.of(exists, notExists);
    }
}
