package com.bloxbean.cardano.yaci.store.snapshot.load;

import com.bloxbean.cardano.yaci.store.snapshot.spec.SnapshotTableSpec;
import com.bloxbean.cardano.yaci.store.snapshot.util.Identifiers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Pre-creates the PostgreSQL range partitions an imported epoch range needs.
 *
 * <p>{@code reward}, {@code epoch_stake} and {@code drep_dist} are range-partitioned by epoch, and
 * the runtime {@code PostgresPartitionManager} creates each partition lazily. Loading through the
 * parent without them would put every imported row in the default partition, and the first runtime
 * attempt to create a real partition for an imported epoch would then have to migrate those rows out
 * again. Creating them up front with the same naming avoids that entirely.
 *
 * <p>This is schema preparation for data the importer is about to write, not index management: no
 * admin-managed index is created, dropped or deferred here.
 */
public class PartitionPreparer {

    private static final Logger log = LoggerFactory.getLogger(PartitionPreparer.class);

    private final Connection conn;
    private final String schema;

    public PartitionPreparer(Connection conn, String schema) {
        this.conn = conn;
        this.schema = Identifiers.requireSqlIdentifier(schema, "target schema");
    }

    /**
     * @return number of partitions created
     */
    public int prepare(SnapshotTableSpec spec, int fromEpoch, int toEpoch) throws SQLException {
        SnapshotTableSpec.TargetPartitioning p = spec.importSpec().targetPartitioning();
        if (p == null) {
            return 0;
        }
        String parent = Identifiers.quote(schema) + "." + Identifiers.quote(spec.targetTable());
        int created = 0;
        for (int epoch = fromEpoch; epoch <= toEpoch; epoch++) {
            String name = p.partitionPrefix() + epoch;
            Identifiers.requireSqlIdentifier(name, "partition name");
            if (exists(name)) {
                continue;
            }
            String sql = "CREATE TABLE " + Identifiers.quote(schema) + "." + Identifiers.quote(name)
                    + " PARTITION OF " + parent + " FOR VALUES FROM (" + epoch + ") TO (" + (epoch + 1) + ")";
            try (Statement st = conn.createStatement()) {
                st.execute(sql);
                created++;
            } catch (SQLException e) {
                // Another partition already covering this range is a real problem worth surfacing.
                throw new SQLException("Unable to create partition " + name + " of " + spec.targetTable()
                        + ": " + e.getMessage(), e);
            }
        }
        if (created > 0) {
            log.info("Created {} partition(s) for {} covering epochs {}..{}",
                    created, spec.targetTable(), fromEpoch, toEpoch);
        }
        return created;
    }

    private boolean exists(String relname) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT 1 FROM pg_class c JOIN pg_namespace n ON n.oid=c.relnamespace"
                        + " WHERE n.nspname=? AND c.relname=?")) {
            ps.setString(1, schema);
            ps.setString(2, relname);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }
}
