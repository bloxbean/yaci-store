package com.bloxbean.cardano.yaci.store.snapshot.handler;

import com.bloxbean.cardano.yaci.store.snapshot.manifest.ConsistencyPoint;
import com.bloxbean.cardano.yaci.store.snapshot.spec.SnapshotTableSpec;

import java.sql.Connection;
import java.sql.SQLException;

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
     * @param connection      JDBC connection to the target database, inside the caller's transaction
     * @param schema          validated target schema name
     * @param point           the snapshot consistency point
     * @param eventPublisherId {@code store.event-publisher-id} of the restored instance
     * @param cursorBlocksToKeep {@code store.cardano.cursor-no-of-blocks-to-keep}
     */
    record Context(Connection connection,
                   String schema,
                   ConsistencyPoint point,
                   long eventPublisherId,
                   int cursorBlocksToKeep,
                   SnapshotTableSpec spec) {}
}
