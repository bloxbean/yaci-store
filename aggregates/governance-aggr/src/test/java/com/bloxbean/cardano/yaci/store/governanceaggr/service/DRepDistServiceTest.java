package com.bloxbean.cardano.yaci.store.governanceaggr.service;

import com.bloxbean.cardano.client.common.model.Networks;
import com.bloxbean.cardano.yaci.core.model.Era;
import com.bloxbean.cardano.yaci.store.adapot.job.domain.AdaPotJobType;
import com.bloxbean.cardano.yaci.store.adapot.job.storage.AdaPotJobStorage;
import com.bloxbean.cardano.yaci.store.adapot.storage.PartitionManager;
import com.bloxbean.cardano.yaci.store.client.governance.ProposalStateClient;
import com.bloxbean.cardano.yaci.store.common.config.StoreProperties;
import com.bloxbean.cardano.yaci.store.common.domain.ProtocolParams;
import com.bloxbean.cardano.yaci.store.core.service.EraService;
import com.bloxbean.cardano.yaci.store.epoch.domain.EpochParam;
import com.bloxbean.cardano.yaci.store.epoch.processor.EraGenesisProtocolParamsUtil;
import com.bloxbean.cardano.yaci.store.epoch.storage.EpochParamStorage;
import com.bloxbean.cardano.yaci.store.governance.storage.GovActionProposalStorage;
import com.bloxbean.cardano.yaci.store.governanceaggr.GovernanceAggrProperties;
import com.bloxbean.cardano.yaci.store.governanceaggr.storage.GovActionProposalStatusStorage;
import com.bloxbean.cardano.yaci.store.governanceaggr.storage.impl.mapper.ProposalMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DRepDistServiceTest {

    private static final String OLD_DREP = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";
    private static final String NEW_DREP = "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb";

    @Mock
    private EraService eraService;
    @Mock
    private EpochParamStorage epochParamStorage;
    @Mock
    private GovActionProposalStatusStorage govActionProposalStatusStorage;
    @Mock
    private GovActionProposalStorage govActionProposalStorage;
    @Mock
    private ProposalStateClient proposalStateClient;
    @Mock
    private ProposalMapper proposalMapper;
    @Mock
    private EraGenesisProtocolParamsUtil eraGenesisProtocolParamsUtil;
    @Mock
    private DRepExpiryService dRepExpiryService;
    @Mock
    private AdaPotJobStorage adaPotJobStorage;
    @Mock
    private PartitionManager partitionManager;

    private JdbcTemplate jdbcTemplate;
    private DRepDistService dRepDistService;

    @BeforeEach
    void setUp() {
        var dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("org.h2.Driver");
        dataSource.setUrl("jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1");

        jdbcTemplate = new JdbcTemplate(dataSource);
        var namedJdbcTemplate = new NamedParameterJdbcTemplate(dataSource);

        var storeProperties = StoreProperties.builder()
                .protocolMagic(Networks.mainnet().getProtocolMagic())
                .build();

        dRepDistService = new DRepDistService(
                namedJdbcTemplate,
                eraService,
                epochParamStorage,
                govActionProposalStatusStorage,
                govActionProposalStorage,
                proposalStateClient,
                proposalMapper,
                storeProperties,
                eraGenesisProtocolParamsUtil,
                dRepExpiryService,
                adaPotJobStorage,
                GovernanceAggrProperties.builder().build(),
                partitionManager,
                new DRepPv9ClearEventCacheService(namedJdbcTemplate)
        );

        createSchema();
        mockCommonDependencies();
    }

    @Test
    void pv9ClearedAddresses_requiresOldDRepRegisteredWhenStaleDelegationWasMade() {
        // old_drep becomes active only at slot 12. A delegation before this slot should not be
        // treated as a stale reverse-entry candidate, because ledger could not have inserted the
        // stake address into old_drep's reverse set before old_drep existed.
        insertDRepRegistration("old-reg", "REG_DREP_CERT", OLD_DREP, "ADDR_KEYHASH", 12, 1);
        insertDRepRegistration("old-unreg", "UNREG_DREP_CERT", OLD_DREP, "ADDR_KEYHASH", 30, 3);
        insertDRepRegistration("new-reg", "REG_DREP_CERT", NEW_DREP, "ADDR_KEYHASH", 1, 1);

        // This address delegated to old_drep before old_drep was registered. Even though it later
        // delegates to new_drep, old_drep's unregistration must not clear it. It should still be
        // counted for new_drep.
        insertDelegation("before-old-registered-to-old", "stake-before-reg", OLD_DREP, 10, 1);
        insertDelegation("before-old-registered-to-new", "stake-before-reg", NEW_DREP, 20, 2);
        insertBalance("stake-before-reg", 1_000L);

        // This address delegated to old_drep after old_drep was registered, then moved to new_drep.
        // In PV9, old_drep may still keep a stale reverse entry for this address. When old_drep
        // unregisters, ledger clears the address's forward delegation, so yaci-store must exclude
        // this address from new_drep's distribution.
        insertDelegation("after-old-registered-to-old", "stake-after-reg", OLD_DREP, 14, 1);
        insertDelegation("after-old-registered-to-new", "stake-after-reg", NEW_DREP, 21, 2);
        insertBalance("stake-after-reg", 2_000L);

        dRepDistService.takeStakeSnapshot(4);

        Long newDRepAmount = jdbcTemplate.queryForObject("""
                SELECT amount
                FROM drep_dist
                WHERE epoch = 4
                  AND drep_hash = ?
                  AND drep_type = 'ADDR_KEYHASH'
                """, Long.class, NEW_DREP);

        // Only the pre-registration address remains counted. The post-registration address is
        // removed by the PV9 stale reverse-entry compensation.
        assertThat(newDRepAmount).isEqualTo(1_000L);
    }

    private void mockCommonDependencies() {
        when(eraService.getEraForEpoch(3)).thenReturn(Era.Conway);
        when(epochParamStorage.getProtocolParams(3)).thenReturn(Optional.of(EpochParam.builder()
                .epoch(3)
                .params(ProtocolParams.builder()
                        .protocolMajorVer(9)
                        .build())
                .build()));
        when(proposalStateClient.getProposalsByStatusAndEpoch(any(), anyInt())).thenReturn(List.of());
        when(govActionProposalStorage.findByEpoch(anyInt())).thenReturn(List.of());
        when(adaPotJobStorage.getJobByTypeAndEpoch(AdaPotJobType.REWARD_CALC, 4)).thenReturn(Optional.empty());
    }

    private void createSchema() {
        jdbcTemplate.execute("""
                CREATE TABLE drep_dist (
                    drep_hash varchar(56),
                    drep_type varchar(40),
                    drep_id varchar(255),
                    amount bigint,
                    epoch int,
                    active_until int,
                    expiry int,
                    update_datetime timestamp,
                    primary key (drep_hash, drep_type, epoch)
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE delegation_vote (
                    tx_hash varchar(64) not null,
                    cert_index int not null,
                    tx_index int not null,
                    address varchar(255),
                    drep_hash varchar(56),
                    drep_id varchar(255),
                    drep_type varchar(40),
                    epoch int,
                    credential varchar(56),
                    cred_type varchar(40),
                    slot bigint,
                    block bigint,
                    block_time bigint,
                    update_datetime timestamp,
                    primary key (tx_hash, cert_index)
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE drep_registration (
                    tx_hash varchar(64) not null,
                    cert_index int not null,
                    tx_index int not null,
                    type varchar(50),
                    deposit bigint,
                    drep_hash varchar(56),
                    drep_id varchar(255),
                    anchor_url varchar,
                    anchor_hash varchar(64),
                    cred_type varchar(40),
                    epoch int,
                    slot bigint,
                    block bigint,
                    block_time bigint,
                    update_datetime timestamp,
                    primary key (tx_hash, cert_index)
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE gov_action_proposal (
                    tx_hash varchar(64),
                    idx int,
                    deposit bigint,
                    return_address varchar(255),
                    epoch int
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE gov_action_proposal_status (
                    gov_action_tx_hash varchar(64),
                    gov_action_index int,
                    status varchar(50),
                    epoch int
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE reward_rest (
                    address varchar(255),
                    amount bigint,
                    slot bigint,
                    spendable_epoch int
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE reward (
                    address varchar(255),
                    amount bigint,
                    slot bigint,
                    spendable_epoch int,
                    type varchar(50)
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE stake_registration (
                    tx_hash varchar(64),
                    cert_index int,
                    tx_index int,
                    type varchar(50),
                    address varchar(255),
                    epoch int,
                    slot bigint
                )
                """);
        jdbcTemplate.execute("CREATE TABLE ss_last_withdrawal (address varchar(255), max_slot bigint)");
        jdbcTemplate.execute("CREATE TABLE ss_pool_rewards (address varchar(255), withdrawable_reward bigint)");
        jdbcTemplate.execute("CREATE TABLE ss_insta_spendable_rewards (address varchar(255), insta_withdrawable_reward bigint)");
        jdbcTemplate.execute("CREATE TABLE ss_gov_pool_refund_rewards (address varchar(255), pool_refund_withdrawable_reward bigint)");
        jdbcTemplate.execute("CREATE TABLE ss_max_slot_balances (address varchar(255), max_slot bigint)");
        jdbcTemplate.execute("CREATE TABLE stake_address_balance (address varchar(255), slot bigint, quantity bigint)");
        jdbcTemplate.execute("""
                CREATE TABLE drep_pv9_stale_clear_event_cache (
                    pv9_max_epoch int,
                    event_count bigint not null,
                    pv9_source_max_slot bigint not null,
                    update_datetime timestamp,
                    primary key (pv9_max_epoch)
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE drep_pv9_stale_clear_event (
                    pv9_max_epoch int not null,
                    address varchar(255) not null,
                    old_drep_hash varchar(56) not null,
                    old_drep_type varchar(40) not null,
                    stale_slot bigint not null,
                    stale_tx_index int not null,
                    stale_cert_index int not null,
                    unreg_epoch int not null,
                    unreg_slot bigint not null,
                    unreg_tx_index int not null,
                    unreg_cert_index int not null,
                    update_datetime timestamp
                )
                """);
    }

    private void insertDRepRegistration(String txHash, String type, String drepHash, String credType, long slot, int certIndex) {
        jdbcTemplate.update("""
                INSERT INTO drep_registration
                    (tx_hash, cert_index, tx_index, type, deposit, drep_hash, drep_id, cred_type, epoch, slot)
                VALUES (?, ?, 0, ?, 0, ?, ?, ?, 3, ?)
                """, txHash, certIndex, type, drepHash, drepHash, credType, slot);
    }

    private void insertDelegation(String txHash, String address, String drepHash, long slot, int certIndex) {
        jdbcTemplate.update("""
                INSERT INTO delegation_vote
                    (tx_hash, cert_index, tx_index, address, drep_hash, drep_id, drep_type, epoch, credential, cred_type, slot)
                VALUES (?, ?, 0, ?, ?, ?, 'ADDR_KEYHASH', 3, ?, 'ADDR_KEYHASH', ?)
                """, txHash, certIndex, address, drepHash, drepHash, address, slot);
    }

    private void insertBalance(String address, long amount) {
        jdbcTemplate.update("INSERT INTO ss_max_slot_balances (address, max_slot) VALUES (?, 100)", address);
        jdbcTemplate.update("INSERT INTO stake_address_balance (address, slot, quantity) VALUES (?, 100, ?)", address, amount);
    }
}
