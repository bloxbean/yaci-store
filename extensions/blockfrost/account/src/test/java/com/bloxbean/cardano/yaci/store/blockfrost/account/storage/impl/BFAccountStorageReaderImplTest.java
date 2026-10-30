package com.bloxbean.cardano.yaci.store.blockfrost.account.storage.impl;

import com.bloxbean.cardano.yaci.store.blockfrost.account.storage.impl.model.AccountInfo;
import com.bloxbean.cardano.yaci.store.utxo.UtxoStoreProperties;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.Result;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.jooq.tools.jdbc.MockConnection;
import org.jooq.tools.jdbc.MockResult;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.bloxbean.cardano.yaci.store.staking.jooq.Tables.STAKE_REGISTRATION;
import static org.assertj.core.api.Assertions.assertThat;

class BFAccountStorageReaderImplTest {

    private static final String STAKE_ADDRESS = "stake_test1upyxd0pgmepw5je3s7tekv0puwx33au8nsaq6y08f9yptecct8flv";
    private static final DSLContext CREATE = DSL.using(SQLDialect.POSTGRES);

    @Test
    void getAccountInfo_neverRegisteredButSeenInOutputs_returnsUnregisteredAccount() {
        var db = new FakeDb(List.of(), false, true);

        Optional<AccountInfo> info = db.reader(false).getAccountInfo(STAKE_ADDRESS);

        // Blockfrost answers 200 for a stake address db-sync has seen, registered or not.
        assertThat(info).hasValueSatisfying(account -> {
            assertThat(account.stakeAddress()).isEqualTo(STAKE_ADDRESS);
            assertThat(account.registered()).isFalse();
            assertThat(account.active()).isFalse();
            assertThat(account.activeEpoch()).isNull();
            assertThat(account.poolId()).isNull();
            assertThat(account.drepId()).isNull();
            assertThat(account.controlledAmount()).isEqualTo(BigInteger.ZERO);
        });
        // Pool, DRep and epoch_stake lookups are gated on a registration and must not run.
        assertThat(db.sqls).noneMatch(sql -> sql.contains("\"delegation\"")
                || sql.contains("\"delegation_vote\"")
                || sql.contains("\"epoch_stake\""));
    }

    @Test
    void getAccountInfo_neverSeenOnChain_returnsEmpty() {
        var db = new FakeDb(List.of(), false, false);

        assertThat(db.reader(true).getAccountInfo(STAKE_ADDRESS)).isEmpty();
        assertThat(db.probes()).containsExactly("address", "address_utxo");
    }

    @Test
    void getAccountInfo_saveAddressEnabled_probesAddressTableFirst() {
        var db = new FakeDb(List.of(), true, false);

        assertThat(db.reader(true).getAccountInfo(STAKE_ADDRESS)).isPresent();
        // The address table survives UTXO pruning, so a hit there settles it without address_utxo.
        assertThat(db.probes()).containsExactly("address");
    }

    @Test
    void getAccountInfo_saveAddressDisabled_skipsAddressTable() {
        var db = new FakeDb(List.of(), true, false);

        assertThat(db.reader(false).getAccountInfo(STAKE_ADDRESS)).isEmpty();
        assertThat(db.probes()).containsExactly("address_utxo");
    }

    @Test
    void getAccountInfo_withRegistrationHistory_doesNotProbeOutputs() {
        var db = new FakeDb(List.of(registration("STAKE_REGISTRATION", 10, 100L),
                registration("STAKE_DEREGISTRATION", 12, 200L)), false, false);

        Optional<AccountInfo> info = db.reader(true).getAccountInfo(STAKE_ADDRESS);

        assertThat(info).hasValueSatisfying(account -> {
            assertThat(account.registered()).isFalse();
            assertThat(account.activeEpoch()).isEqualTo(10);
        });
        assertThat(db.probes()).isEmpty();
    }

    private static Record registration(String type, int epoch, long slot) {
        Record rec = CREATE.newRecord(STAKE_REGISTRATION.TYPE, STAKE_REGISTRATION.EPOCH, STAKE_REGISTRATION.SLOT,
                STAKE_REGISTRATION.TX_INDEX, STAKE_REGISTRATION.CERT_INDEX);
        rec.set(STAKE_REGISTRATION.TYPE, type);
        rec.set(STAKE_REGISTRATION.EPOCH, epoch);
        rec.set(STAKE_REGISTRATION.SLOT, slot);
        rec.set(STAKE_REGISTRATION.TX_INDEX, 0);
        rec.set(STAKE_REGISTRATION.CERT_INDEX, 0);
        return rec;
    }

    /**
     * Answers each query of {@code getAccountInfo} by the table it reads: the given registration rows,
     * the given probe results, no MIR rows, and a {@code NULL} sum for every aggregate.
     */
    private static final class FakeDb {
        private final List<Record> registrations;
        private final boolean inAddressTable;
        private final boolean inAddressUtxo;
        private final List<String> sqls = new ArrayList<>();

        FakeDb(List<Record> registrations, boolean inAddressTable, boolean inAddressUtxo) {
            this.registrations = registrations;
            this.inAddressTable = inAddressTable;
            this.inAddressUtxo = inAddressUtxo;
        }

        BFAccountStorageReaderImpl reader(boolean saveAddress) {
            DSLContext dsl = DSL.using(new MockConnection(ctx -> new MockResult[]{answer(ctx.sql())}),
                    SQLDialect.POSTGRES);
            return new BFAccountStorageReaderImpl(dsl, UtxoStoreProperties.builder().saveAddress(saveAddress).build());
        }

        /** Tables probed for existence, in order. */
        List<String> probes() {
            return sqls.stream()
                    .filter(BFAccountStorageReaderImplTest::isExistsProbe)
                    .map(sql -> sql.contains("\"address_utxo\"") ? "address_utxo" : "address")
                    .toList();
        }

        private MockResult answer(String sql) {
            sqls.add(sql);
            if (sql.contains("\"stake_registration\"")) {
                Result<Record> result = result(STAKE_REGISTRATION.TYPE, STAKE_REGISTRATION.EPOCH,
                        STAKE_REGISTRATION.SLOT, STAKE_REGISTRATION.TX_INDEX, STAKE_REGISTRATION.CERT_INDEX);
                result.addAll(registrations);
                return new MockResult(result.size(), result);
            }
            if (isExistsProbe(sql)) {
                boolean hit = sql.contains("\"address_utxo\"") ? inAddressUtxo : inAddressTable;
                Field<Boolean> exists = DSL.field("exists", Boolean.class);
                Result<Record> result = result(exists);
                Record rec = CREATE.newRecord(exists);
                rec.set(exists, hit);
                result.add(rec);
                return new MockResult(1, result);
            }
            if (sql.contains("\"mir\"")) {
                Field<String> pot = DSL.field(DSL.name("pot"), String.class);
                Field<BigDecimal> total = DSL.field(DSL.name("total"), BigDecimal.class);
                return new MockResult(0, CREATE.newResult(pot, total));
            }
            Field<BigDecimal> sum = DSL.field("sum", BigDecimal.class);
            Result<Record> result = result(sum);
            result.add(CREATE.newRecord(sum));
            return new MockResult(1, result);
        }
    }

    private static boolean isExistsProbe(String sql) {
        return sql.startsWith("select exists");
    }

    private static Result<Record> result(Field<?>... fields) {
        return CREATE.newResult(fields);
    }

    @Test
    void activeEpochStakeCondition_boundsThePartitionKey() {
        String sql = DSL.using(SQLDialect.POSTGRES).renderInlined(
                BFAccountStorageReaderImpl.activeEpochStakeCondition("stake_test1abc", 1444));

        // epoch_stake is range-partitioned on epoch, and both of its writers (the stake snapshot
        // and the genesis pools) store active_epoch = epoch + 2, so active_epoch >= N is the same
        // filter as epoch >= N - 2. Stated on the partition key, the lookup no longer plans and
        // probes every epoch partition.
        assertThat(sql)
                .contains("\"epoch_stake\".\"address\" = 'stake_test1abc'")
                .contains("\"epoch_stake\".\"epoch\" >= 1442")
                .doesNotContain("active_epoch");
    }

    @Test
    void activeEpochStakeCondition_keepsGenesisRowsAtEpochMinusOne() {
        String sql = DSL.using(SQLDialect.POSTGRES).renderInlined(
                BFAccountStorageReaderImpl.activeEpochStakeCondition("stake_test1abc", 1));

        // GenesisPoolProcessor writes a devnet's genesis delegators at epoch = -1 (active_epoch = 1),
        // so the bound must go negative rather than clamp at 0 and skip them.
        assertThat(sql).contains("\"epoch_stake\".\"epoch\" >= -1");
    }
}
