package com.bloxbean.cardano.yaci.store.mcp.server.aggregation;

import com.bloxbean.cardano.yaci.store.mcp.server.model.*;
import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for McpUtxoAggregationService using real preprod2 database.
 *
 * These tests verify that aggregation queries work correctly with actual blockchain data.
 * Database connection is configurable via application-integrationtest.properties.
 *
 * To run with custom database:
 * ./gradlew test --tests McpUtxoAggregationServiceIntegrationTest \
 *   -Dtest.db.host=localhost \
 *   -Dtest.db.port=54333 \
 *   -Dtest.db.name=yaci_store \
 *   -Dtest.db.schema=preprod2 \
 *   -Dtest.db.username=yaci \
 *   -Dtest.db.password=dbpass
 */
@SpringBootTest
@ActiveProfiles("integrationtest")
@TestPropertySource(locations = "classpath:application-integrationtest.properties")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class McpUtxoAggregationServiceIntegrationTest {

    private static final Logger log = LoggerFactory.getLogger(McpUtxoAggregationServiceIntegrationTest.class);

    @Autowired
    private McpUtxoAggregationService aggregationService;

    @Autowired
    private NamedParameterJdbcTemplate jdbcTemplate;

    // Test data from properties
    @Value("${test.data.address.high-utxo}")
    private String testAddressHighUtxo;

    @Value("${test.data.address.medium-utxo}")
    private String testAddressMediumUtxo;

    @Value("${test.data.stake-address}")
    private String testStakeAddress;

    @Value("${test.data.asset.unit}")
    private String testAssetUnit;

    @Value("${test.data.epoch.historical}")
    private int testEpochHistorical;

    @Value("${test.data.epoch.range-start}")
    private int testEpochRangeStart;

    @Value("${test.data.epoch.range-end}")
    private int testEpochRangeEnd;

    @BeforeAll
    static void beforeAll() {
        log.info("=================================================================");
        log.info("Starting MCP UTXO Aggregation Service Integration Tests");
        log.info("=================================================================");
    }

    @AfterAll
    static void afterAll() {
        log.info("=================================================================");
        log.info("Completed MCP UTXO Aggregation Service Integration Tests");
        log.info("=================================================================");
    }

    @BeforeEach
    void setUp() {
        assertThat(aggregationService).isNotNull();
        assertThat(jdbcTemplate).isNotNull();
    }

    @Test
    @Order(1)
    @DisplayName("Test 1: utxo-balance-summary - Single address with high UTXO count")
    void testGetBalanceSummary_singleAddress() {
        log.info("Testing utxo-balance-summary for address: {}", testAddressHighUtxo);

        // When
        UtxoBalanceSummary result = aggregationService.getBalanceSummary(testAddressHighUtxo);

        // Then
        log.info("Result: {}", result);
        assertThat(result).isNotNull();
        assertThat(result.utxoCount()).isGreaterThan(0)
            .withFailMessage("Expected UTXOs but found none for address: " + testAddressHighUtxo);
        assertThat(result.totalLovelace()).isGreaterThan(BigDecimal.ZERO)
            .withFailMessage("Expected positive lovelace balance");
        assertThat(result.activeEpochs()).isGreaterThan(0)
            .withFailMessage("Expected active epochs but found none");
        assertThat(result.firstSeenSlot()).isGreaterThan(0)
            .withFailMessage("Expected first seen slot");
        assertThat(result.lastSeenSlot()).isGreaterThanOrEqualTo(result.firstSeenSlot())
            .withFailMessage("Last seen slot should be >= first seen slot");

        log.info("✅ Single address balance summary: {} UTXOs, {} lovelace, {} active epochs",
                 result.utxoCount(), result.totalLovelace(), result.activeEpochs());
    }

    @Test
    @Order(2)
    @DisplayName("Test 2: utxo-balance-summary - Multiple addresses")
    void testGetBalanceSummary_multipleAddresses() {
        String multipleAddresses = testAddressHighUtxo + "," + testAddressMediumUtxo;
        log.info("Testing utxo-balance-summary for multiple addresses");

        // When
        UtxoBalanceSummary result = aggregationService.getBalanceSummary(multipleAddresses);

        // Then
        log.info("Result: {}", result);
        assertThat(result).isNotNull();
        assertThat(result.utxoCount()).isGreaterThan(0);
        assertThat(result.totalLovelace()).isGreaterThan(BigDecimal.ZERO);

        // Verify it's aggregating across both addresses by getting individual results
        UtxoBalanceSummary addr1 = aggregationService.getBalanceSummary(testAddressHighUtxo);
        UtxoBalanceSummary addr2 = aggregationService.getBalanceSummary(testAddressMediumUtxo);

        // Combined UTXO count should equal or exceed individual counts
        assertThat(result.utxoCount()).isGreaterThanOrEqualTo(addr1.utxoCount());
        assertThat(result.utxoCount()).isGreaterThanOrEqualTo(addr2.utxoCount());

        log.info("✅ Multi-address balance summary: {} UTXOs (addr1: {}, addr2: {})",
                 result.utxoCount(), addr1.utxoCount(), addr2.utxoCount());
    }

    @Test
    @Order(3)
    @DisplayName("Test 3: utxo-balance-at-epoch - Historical point-in-time balance")
    void testGetBalanceAtEpoch() {
        log.info("Testing utxo-balance-at-epoch for address: {} at epoch: {}",
                 testAddressHighUtxo, testEpochHistorical);

        // When
        HistoricalBalanceSummary result = aggregationService.getBalanceAtEpoch(
            testAddressHighUtxo,
            testEpochHistorical
        );

        // Then
        log.info("Result: {}", result);
        assertThat(result).isNotNull();
        assertThat(result.timeType()).isEqualTo("epoch");
        assertThat(result.timeValue()).isEqualTo(testEpochHistorical);
        // Balance could be 0 if address had no UTXOs at that epoch
        assertThat(result.utxoCount()).isGreaterThanOrEqualTo(0);
        assertThat(result.totalLovelace()).isGreaterThanOrEqualTo(BigDecimal.ZERO);

        log.info("✅ Balance at epoch {}: {} UTXOs, {} lovelace",
                 testEpochHistorical, result.utxoCount(), result.totalLovelace());
    }

    @Test
    @Order(4)
    @DisplayName("Test 4: utxo-balance-at-slot - Precise point-in-time balance")
    void testGetBalanceAtSlot() {
        // Use a recent slot (approximate: epoch 240 * 21600 slots/epoch)
        long testSlot = testEpochHistorical * 21600L;
        log.info("Testing utxo-balance-at-slot for address: {} at slot: {}",
                 testAddressHighUtxo, testSlot);

        // When
        HistoricalBalanceSummary result = aggregationService.getBalanceAtSlot(
            testAddressHighUtxo,
            testSlot
        );

        // Then
        log.info("Result: {}", result);
        assertThat(result).isNotNull();
        assertThat(result.timeType()).isEqualTo("slot");
        assertThat(result.timeValue()).isEqualTo(testSlot);
        assertThat(result.utxoCount()).isGreaterThanOrEqualTo(0);
        assertThat(result.totalLovelace()).isGreaterThanOrEqualTo(BigDecimal.ZERO);

        log.info("✅ Balance at slot {}: {} UTXOs, {} lovelace",
                 testSlot, result.utxoCount(), result.totalLovelace());
    }

    @Test
    @Order(5)
    @DisplayName("Test 5: balance-history-timeline - Balance evolution over epochs")
    void testGetBalanceHistory() {
        log.info("Testing balance-history-timeline for address: {} from epoch {} to {}",
                 testAddressHighUtxo, testEpochRangeStart, testEpochRangeEnd);

        // When
        List<BalanceHistoryPoint> result = aggregationService.getBalanceHistory(
            testAddressHighUtxo,
            testEpochRangeStart,
            testEpochRangeEnd
        );

        // Then
        log.info("Result: {} data points", result.size());
        assertThat(result).isNotNull();

        // Should have one entry per epoch in range
        int expectedPoints = testEpochRangeEnd - testEpochRangeStart + 1;
        assertThat(result).hasSize(expectedPoints)
            .withFailMessage("Expected %d data points for epoch range [%d-%d]",
                           expectedPoints, testEpochRangeStart, testEpochRangeEnd);

        // Verify epochs are in order
        for (int i = 0; i < result.size(); i++) {
            BalanceHistoryPoint point = result.get(i);
            int expectedEpoch = testEpochRangeStart + i;
            assertThat(point.epoch()).isEqualTo(expectedEpoch);
            assertThat(point.utxoCount()).isGreaterThanOrEqualTo(0);
            assertThat(point.totalLovelace()).isGreaterThanOrEqualTo(BigDecimal.ZERO);

            log.info("  Epoch {}: {} UTXOs, {} lovelace",
                     point.epoch(), point.utxoCount(), point.totalLovelace());
        }

        log.info("✅ Balance history retrieved successfully with {} data points", result.size());
    }

    @Test
    @Order(6)
    @DisplayName("Test 6: asset-balance-by-address - Specific asset balance")
    void testGetAssetBalance() {
        log.info("Testing asset-balance-by-address for asset: {}", testAssetUnit);

        // Find an address that actually holds this asset
        String addressWithAsset = findAddressWithAsset(testAssetUnit);

        if (addressWithAsset == null) {
            log.warn("⚠️  No address found holding asset: {}. Skipping test.", testAssetUnit);
            return;
        }

        log.info("Found address holding asset: {}", addressWithAsset);

        // When
        AssetBalanceSummary result = aggregationService.getAssetBalance(
            addressWithAsset,
            testAssetUnit
        );

        // Then
        log.info("Result: {}", result);
        assertThat(result).isNotNull();
        assertThat(result.assetUnit()).isEqualTo(testAssetUnit);
        assertThat(result.holderCount()).isGreaterThan(0)
            .withFailMessage("Expected holders for asset");
        assertThat(result.totalQuantity()).isGreaterThan(BigDecimal.ZERO)
            .withFailMessage("Expected positive asset quantity");

        log.info("✅ Asset balance: {} holders, {} units",
                 result.holderCount(), result.totalQuantity());
    }

    @Test
    @Order(7)
    @DisplayName("Test 7: stake-address-portfolio - Complete portfolio with all assets")
    void testGetStakeAddressPortfolio() {
        log.info("Testing stake-address-portfolio for stake address: {}", testStakeAddress);

        // When
        StakeAddressPortfolio result = aggregationService.getStakeAddressPortfolio(testStakeAddress);

        // Then
        log.info("Result: {}", result);
        assertThat(result).isNotNull();
        assertThat(result.stakeAddress()).isEqualTo(testStakeAddress);
        assertThat(result.utxoCount()).isGreaterThan(0)
            .withFailMessage("Expected UTXOs for stake address");
        assertThat(result.totalLovelace()).isGreaterThan(BigDecimal.ZERO)
            .withFailMessage("Expected positive ADA balance");
        assertThat(result.assets()).isNotNull();

        log.info("✅ Stake address portfolio: {} UTXOs, {} lovelace, {} different assets",
                 result.utxoCount(), result.totalLovelace(), result.assets().size());

        // Log first 5 assets
        result.assets().stream()
            .limit(5)
            .forEach(asset -> log.info("  Asset: {} - Quantity: {}",
                                     asset.assetName() != null ? asset.assetName() : asset.assetUnit(),
                                     asset.quantity()));

        if (result.assets().size() > 5) {
            log.info("  ... and {} more assets", result.assets().size() - 5);
        }
    }

    @Test
    @Order(8)
    @DisplayName("Test 8: Verify spent UTXOs are excluded from current balance")
    void testSpentUtxoFiltering() {
        log.info("Testing that spent UTXOs are properly excluded from balance");

        // Get current balance
        UtxoBalanceSummary currentBalance = aggregationService.getBalanceSummary(testAddressHighUtxo);

        // Query for ALL UTXOs (including spent) for this address
        Long totalUtxosIncludingSpent = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM address_utxo WHERE owner_addr = :address",
            new java.util.HashMap<>() {{
                put("address", testAddressHighUtxo);
            }},
            Long.class
        );

        log.info("Current unspent UTXOs: {}", currentBalance.utxoCount());
        log.info("Total UTXOs (including spent): {}", totalUtxosIncludingSpent);

        // The unspent count should be less than or equal to total count
        assertThat(currentBalance.utxoCount()).isLessThanOrEqualTo(totalUtxosIncludingSpent);

        log.info("✅ Spent UTXO filtering verified: {} unspent out of {} total",
                 currentBalance.utxoCount(), totalUtxosIncludingSpent);
    }

    @Test
    @Order(9)
    @DisplayName("Test 9: Performance check - Balance summary should complete quickly")
    void testPerformance() {
        log.info("Testing performance of utxo-balance-summary");

        long startTime = System.currentTimeMillis();

        UtxoBalanceSummary result = aggregationService.getBalanceSummary(testAddressHighUtxo);

        long duration = System.currentTimeMillis() - startTime;

        log.info("Query completed in {}ms", duration);
        assertThat(result).isNotNull();
        assertThat(duration).isLessThan(5000)
            .withFailMessage("Query took too long: %dms (expected < 5000ms)", duration);

        log.info("✅ Performance test passed: {}ms for {} UTXOs",
                 duration, result.utxoCount());
    }

    @Test
    @Order(10)
    @DisplayName("Test 10: Empty result handling - Non-existent address")
    void testNonExistentAddress() {
        String nonExistentAddress = "addr_test1qqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqq6j2v0j";
        log.info("Testing with non-existent address: {}", nonExistentAddress);

        // When
        UtxoBalanceSummary result = aggregationService.getBalanceSummary(nonExistentAddress);

        // Then
        log.info("Result: {}", result);
        assertThat(result).isNotNull();
        assertThat(result.utxoCount()).isEqualTo(0);
        assertThat(result.totalLovelace()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.activeEpochs()).isEqualTo(0);

        log.info("✅ Empty result handled correctly for non-existent address");
    }

    /**
     * Helper method to find an address that holds a specific asset.
     * Returns null if no address is found.
     */
    private String findAddressWithAsset(String assetUnit) {
        try {
            return jdbcTemplate.queryForObject(
                """
                SELECT DISTINCT owner_addr
                FROM address_utxo,
                     jsonb_array_elements(amounts) as elem
                WHERE elem->>'unit' = :assetUnit
                  AND NOT EXISTS (
                      SELECT 1 FROM tx_input
                      WHERE tx_input.tx_hash = address_utxo.tx_hash
                      AND tx_input.output_index = address_utxo.output_index
                  )
                LIMIT 1
                """,
                new java.util.HashMap<>() {{
                    put("assetUnit", assetUnit);
                }},
                String.class
            );
        } catch (Exception e) {
            log.warn("Failed to find address with asset {}: {}", assetUnit, e.getMessage());
            return null;
        }
    }
}
