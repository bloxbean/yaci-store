package com.bloxbean.cardano.yaci.store.snapshot.handler;

import com.bloxbean.cardano.yaci.store.snapshot.manifest.ConsistencyPoint;
import com.bloxbean.cardano.yaci.store.snapshot.spec.SnapshotTableSpec;
import com.bloxbean.cardano.yaci.store.snapshot.util.Identifiers;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/**
 * The escape hatch, not the default.
 *
 * <p>A handler exists for operational lifecycle state whose shape is not a projection of exported
 * rows — {@code cursor_}, {@code era}, job state. Adding an ordinary table or column must never
 * require one: that is what the declarative DIRECT/MAPPED/SQL modes are for.
 *
 * <p>Handlers run after all table data is loaded, in dependency order, against a plain JDBC
 * connection to the target schema.
 */
public interface SnapshotHandler {

    /** Stable name referenced from {@code import.handler} in a specification. */
    String name();

    /**
     * @return a short description of what was written, for the import report
     */
    String apply(Context context) throws SQLException;

    /**
     * Ids of the specifications whose rows this handler reads. When the target does not import one
     * of them (its store is disabled), the importer still extracts that table's files and stages the
     * rows the handler needs, see {@link Context#relation}.
     */
    default List<String> reads() {
        return List.of();
    }

    /**
     * @param connection      JDBC connection to the target database, inside the caller's transaction
     * @param schema          validated target schema name
     * @param point           the snapshot consistency point
     * @param eventPublisherId {@code store.event-publisher-id} of the restored instance
     * @param cursorBlocksToKeep {@code store.cardano.cursor-no-of-blocks-to-keep}
     * @param staged          target table to the qualified scratch relation that holds its rows, for
     *                        the {@link #reads()} tables this import does not load
     */
    record Context(Connection connection,
                   String schema,
                   ConsistencyPoint point,
                   long eventPublisherId,
                   int cursorBlocksToKeep,
                   SnapshotTableSpec spec,
                   Map<String, String> staged) {

        public Context(Connection connection, String schema, ConsistencyPoint point, long eventPublisherId,
                       int cursorBlocksToKeep, SnapshotTableSpec spec) {
            this(connection, schema, point, eventPublisherId, cursorBlocksToKeep, spec, Map.of());
        }

        /** The qualified relation to read {@code table} from: the imported table, or its staged rows. */
        public String relation(String table) {
            String staging = staged == null ? null : staged.get(table);
            return staging != null ? staging : Identifiers.quote(schema) + "." + Identifiers.quote(table);
        }
    }
}
