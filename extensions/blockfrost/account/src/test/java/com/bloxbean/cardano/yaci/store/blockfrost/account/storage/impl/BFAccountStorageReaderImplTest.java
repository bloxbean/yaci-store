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
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static com.bloxbean.cardano.yaci.store.staking.jooq.Tables.STAKE_REGISTRATION;
import static org.assertj.core.api.Assertions.assertThat;

class BFAccountStorageReaderImplTest {

    private static final String STAKE_ADDRESS = "stake_test1upyxd0pgmepw5je3s7tekv0puwx33au8nsaq6y08f9yptecct8flv";
    // Preprod pool owner that never registered and never received an output.
    private static final String OWNER_ADDRESS = "stake_test1uzly0q2cnpxrjrqm9vpnr9dwkr0j945gulhhgs3dx33l47sfnz8a7";
    private static final String OWNER_KEY_HASH = "be478158984c390c1b2b033195aeb0df22d688e7ef74422d3463fafa";
    private static final List<String> ALL_PROBES = List.of(
            "address_utxo", "pool_reward_account", "mir", "gov_return_address", "pool_owner", "treasury_withdrawal");
    private static final DSLContext CREATE = DSL.using(SQLDialect.POSTGRES);

    @Test
    void getAccountInfo_neverRegisteredButSeenInOutputs_returnsUnregisteredAccount() {
        var db = new FakeDb(List.of(), "address_utxo");

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
    void getAccountInfo_neverSeenOnChain_returnsEmptyAfterEverySource() {
        var db = new FakeDb(List.of());

        assertThat(db.reader(true).getAccountInfo(STAKE_ADDRESS)).isEmpty();
        assertThat(db.probes()).containsExactlyElementsOf(concat("address", ALL_PROBES));
    }

    @Test
    void getAccountInfo_saveAddressEnabled_probesAddressTableFirst() {
        var db = new FakeDb(List.of(), "address");

        assertThat(db.reader(true).getAccountInfo(STAKE_ADDRESS)).isPresent();
        // The address table survives UTXO pruning, so a hit there settles it without address_utxo.
        assertThat(db.probes()).containsExactly("address");
    }

    @Test
    void getAccountInfo_saveAddressDisabled_skipsAddressTable() {
        var db = new FakeDb(List.of(), "address");

        assertThat(db.reader(false).getAccountInfo(STAKE_ADDRESS)).isEmpty();
        assertThat(db.probes()).containsExactlyElementsOf(ALL_PROBES);
    }

    @Test
    void getAccountInfo_poolRewardAccountOnlyInCertificate_returnsUnregisteredAccount() {
        var db = new FakeDb(List.of(), "pool_reward_account");

        assertThat(db.reader(false).getAccountInfo(OWNER_ADDRESS))
                .hasValueSatisfying(account -> assertThat(account.registered()).isFalse());
        assertThat(db.probes()).containsExactly("address_utxo", "pool_reward_account");
    }

    @Test
    void getAccountInfo_mirTargetOnly_returnsUnregisteredAccount() {
        var db = new FakeDb(List.of(), "mir");

        assertThat(db.reader(false).getAccountInfo(OWNER_ADDRESS)).isPresent();
        assertThat(db.probes()).endsWith("mir");
    }

    @Test
    void getAccountInfo_proposalReturnAddressOnly_returnsUnregisteredAccount() {
        var db = new FakeDb(List.of(), "gov_return_address");

        assertThat(db.reader(false).getAccountInfo(OWNER_ADDRESS)).isPresent();
        assertThat(db.probes()).endsWith("gov_return_address");
    }

    @Test
    void getAccountInfo_poolOwnerOnly_matchesOwnerKeyHash() {
        var db = new FakeDb(List.of(), "pool_owner");

        assertThat(db.reader(false).getAccountInfo(OWNER_ADDRESS)).isPresent();
        assertThat(db.probes()).endsWith("pool_owner");
        // pool_owners holds key hashes, not stake addresses.
        assertThat(db.probeSql("pool_owner")).contains(OWNER_KEY_HASH).doesNotContain(OWNER_ADDRESS);
    }

    @Test
    void getAccountInfo_treasuryWithdrawalTargetOnly_matchesRewardAccountHex() {
        var db = new FakeDb(List.of(), "treasury_withdrawal");

        assertThat(db.reader(false).getAccountInfo(OWNER_ADDRESS)).isPresent();
        // Withdrawals are keyed by the hex of the reward account: header e0 (testnet, key hash) + hash.
        assertThat(db.probeSql("treasury_withdrawal"))
                .contains("TREASURY_WITHDRAWALS_ACTION")
                .contains("\"e0" + OWNER_KEY_HASH + "\"");
    }

    @Test
    void getAccountInfo_scriptStakeAddress_skipsPoolOwnerProbe() {
        // Script credential: header f0. Pool owners are always key hashes.
        var db = new FakeDb(List.of());

        assertThat(db.reader(false).getAccountInfo("stake_test17pcy992j0jy9c95frpyccupklvjz3tcr959zf0wl45wsa5s5fhk5g"))
                .isEmpty();
        assertThat(db.probes()).doesNotContain("pool_owner").endsWith("treasury_withdrawal");
    }

    @Test
    void getAccountInfo_malformedAddress_returnsEmptyWithoutDecodingProbes() {
        var db = new FakeDb(List.of());

        assertThat(db.reader(false).getAccountInfo("not-a-stake-address")).isEmpty();
        assertThat(db.probes()).doesNotContain("pool_owner", "treasury_withdrawal");
    }

    @Test
    void getAccountInfo_withRegistrationHistory_doesNotProbeOutputs() {
        var db = new FakeDb(List.of(registration("STAKE_REGISTRATION", 10, 100L),
                registration("STAKE_DEREGISTRATION", 12, 200L)));

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

    private static List<String> concat(String first, List<String> rest) {
        var all = new ArrayList<String>();
        all.add(first);
        all.addAll(rest);
        return all;
    }

    /** Which source an existence probe reads, named after the column it matches. */
    private static String probeName(String sql) {
        if (sql.contains("\"address_utxo\"")) return "address_utxo";
        if (sql.contains("\"pool_owners\"")) return "pool_owner";
        if (sql.contains("\"pool_registration\"")) return "pool_reward_account";
        if (sql.contains("\"mir\"")) return "mir";
        if (sql.contains("\"details\"")) return "treasury_withdrawal";
        if (sql.contains("\"gov_action_proposal\"")) return "gov_return_address";
        return "address";
    }

    /**
     * Answers each query of {@code getAccountInfo} by the table it reads: the given registration rows,
     * a hit only for the named probe sources, no MIR rows, and a {@code NULL} sum for every aggregate.
     */
    private static final class FakeDb {
        private final List<Record> registrations;
        private final List<String> hits;
        private final List<String> sqls = new ArrayList<>();

        FakeDb(List<Record> registrations, String... hits) {
            this.registrations = registrations;
            this.hits = List.of(hits);
        }

        String probeSql(String name) {
            return sqls.stream()
                    .filter(sql -> isExistsProbe(sql) && probeName(sql).equals(name))
                    .findFirst().orElseThrow();
        }

        BFAccountStorageReaderImpl reader(boolean saveAddress) {
            DSLContext dsl = DSL.using(new MockConnection(ctx -> new MockResult[]{answer(ctx.sql() + " -- binds " + Arrays.toString(ctx.bindings()))}),
                    SQLDialect.POSTGRES);
            return new BFAccountStorageReaderImpl(dsl, UtxoStoreProperties.builder().saveAddress(saveAddress).build());
        }

        /** Tables probed for existence, in order. */
        List<String> probes() {
            return sqls.stream()
                    .filter(BFAccountStorageReaderImplTest::isExistsProbe)
                    .map(BFAccountStorageReaderImplTest::probeName)
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
                boolean hit = hits.contains(probeName(sql));
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
