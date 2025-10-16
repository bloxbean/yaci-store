package com.bloxbean.cardano.yaci.store.mcp.server.aggregation;

import com.bloxbean.cardano.yaci.store.mcp.server.model.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

/**
 * Unit tests for McpUtxoAggregationService.
 * Tests the aggregation tools for UTXO balance queries.
 */
@ExtendWith(MockitoExtension.class)
class McpUtxoAggregationServiceTest {

    @Mock
    private NamedParameterJdbcTemplate jdbcTemplate;

    @InjectMocks
    private McpUtxoAggregationService service;

    @Test
    void testGetBalanceSummary_singleAddress() {
        // Given
        String address = "addr1qxy123...";
        UtxoBalanceSummary expected = new UtxoBalanceSummary(
            42L,
            new BigDecimal("1500000000"),
            15,
            1000000L,
            2000000L
        );

        when(jdbcTemplate.queryForObject(
            anyString(),
            any(Map.class),
            any(RowMapper.class)
        )).thenReturn(expected);

        // When
        UtxoBalanceSummary result = service.getBalanceSummary(address);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.utxoCount()).isEqualTo(42L);
        assertThat(result.totalLovelace()).isEqualByComparingTo(new BigDecimal("1500000000"));
        assertThat(result.activeEpochs()).isEqualTo(15);
    }

    @Test
    void testGetBalanceSummary_multipleAddresses() {
        // Given
        String addresses = "addr1qxy123...,addr1abc456...,addr1def789...";
        UtxoBalanceSummary expected = new UtxoBalanceSummary(
            100L,
            new BigDecimal("5000000000"),
            20,
            500000L,
            3000000L
        );

        when(jdbcTemplate.queryForObject(
            anyString(),
            any(Map.class),
            any(RowMapper.class)
        )).thenReturn(expected);

        // When
        UtxoBalanceSummary result = service.getBalanceSummary(addresses);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.utxoCount()).isEqualTo(100L);
        assertThat(result.totalLovelace()).isEqualByComparingTo(new BigDecimal("5000000000"));
    }

    @Test
    void testGetBalanceAtEpoch() {
        // Given
        String address = "addr1qxy123...";
        int epoch = 400;
        HistoricalBalanceSummary expected = new HistoricalBalanceSummary(
            "epoch",
            400L,
            38L,
            new BigDecimal("1200000000")
        );

        when(jdbcTemplate.queryForObject(
            anyString(),
            any(Map.class),
            any(RowMapper.class)
        )).thenReturn(expected);

        // When
        HistoricalBalanceSummary result = service.getBalanceAtEpoch(address, epoch);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.timeType()).isEqualTo("epoch");
        assertThat(result.timeValue()).isEqualTo(400L);
        assertThat(result.utxoCount()).isEqualTo(38L);
        assertThat(result.totalLovelace()).isEqualByComparingTo(new BigDecimal("1200000000"));
    }

    @Test
    void testGetBalanceAtSlot() {
        // Given
        String address = "addr1qxy123...";
        long slot = 50000000L;
        HistoricalBalanceSummary expected = new HistoricalBalanceSummary(
            "slot",
            50000000L,
            40L,
            new BigDecimal("1300000000")
        );

        when(jdbcTemplate.queryForObject(
            anyString(),
            any(Map.class),
            any(RowMapper.class)
        )).thenReturn(expected);

        // When
        HistoricalBalanceSummary result = service.getBalanceAtSlot(address, slot);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.timeType()).isEqualTo("slot");
        assertThat(result.timeValue()).isEqualTo(50000000L);
        assertThat(result.utxoCount()).isEqualTo(40L);
    }

    @Test
    void testGetBalanceHistory() {
        // Given
        String address = "addr1qxy123...";
        int startEpoch = 400;
        int endEpoch = 410;
        List<BalanceHistoryPoint> expected = Arrays.asList(
            new BalanceHistoryPoint(400, 38L, new BigDecimal("1200000000")),
            new BalanceHistoryPoint(401, 39L, new BigDecimal("1250000000")),
            new BalanceHistoryPoint(402, 40L, new BigDecimal("1300000000"))
        );

        when(jdbcTemplate.query(
            anyString(),
            any(Map.class),
            any(RowMapper.class)
        )).thenReturn(expected);

        // When
        List<BalanceHistoryPoint> result = service.getBalanceHistory(address, startEpoch, endEpoch);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(3);
        assertThat(result.get(0).epoch()).isEqualTo(400);
        assertThat(result.get(1).epoch()).isEqualTo(401);
        assertThat(result.get(2).epoch()).isEqualTo(402);
    }

    @Test
    void testGetAssetBalance() {
        // Given
        String address = "addr1qxy123...";
        String assetUnit = "21575be1...446973636f696e";
        AssetBalanceSummary expected = new AssetBalanceSummary(
            assetUnit,
            1,
            new BigDecimal("1000")
        );

        when(jdbcTemplate.queryForObject(
            anyString(),
            any(Map.class),
            any(RowMapper.class)
        )).thenReturn(expected);

        // When
        AssetBalanceSummary result = service.getAssetBalance(address, assetUnit);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.assetUnit()).isEqualTo(assetUnit);
        assertThat(result.holderCount()).isEqualTo(1);
        assertThat(result.totalQuantity()).isEqualByComparingTo(new BigDecimal("1000"));
    }

    @Test
    void testGetStakeAddressPortfolio() {
        // Given
        String stakeAddress = "stake1uxy123...";
        Map<String, Object> adaResult = Map.of(
            "utxo_count", 50L,
            "total_lovelace", new BigDecimal("2000000000")
        );
        List<AssetHolding> assets = Arrays.asList(
            new AssetHolding(
                "21575be1...446973636f696e",
                "21575be1...",
                "Discoin",
                new BigDecimal("500")
            )
        );

        when(jdbcTemplate.queryForMap(anyString(), any(Map.class)))
            .thenReturn(adaResult);
        when(jdbcTemplate.query(anyString(), any(Map.class), any(RowMapper.class)))
            .thenReturn(assets);

        // When
        StakeAddressPortfolio result = service.getStakeAddressPortfolio(stakeAddress);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.stakeAddress()).isEqualTo(stakeAddress);
        assertThat(result.utxoCount()).isEqualTo(50L);
        assertThat(result.totalLovelace()).isEqualByComparingTo(new BigDecimal("2000000000"));
        assertThat(result.assets()).hasSize(1);
        assertThat(result.assets().get(0).assetName()).isEqualTo("Discoin");
    }
}
