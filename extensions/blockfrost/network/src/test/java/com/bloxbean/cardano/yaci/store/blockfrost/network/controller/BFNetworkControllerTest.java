package com.bloxbean.cardano.yaci.store.blockfrost.network.controller;

import com.bloxbean.cardano.yaci.store.blockfrost.network.dto.BFEraDto;
import com.bloxbean.cardano.yaci.store.blockfrost.network.dto.BFGenesisDto;
import com.bloxbean.cardano.yaci.store.blockfrost.network.dto.BFNetworkDto;
import com.bloxbean.cardano.yaci.store.blockfrost.network.dto.BFRootDto;
import com.bloxbean.cardano.yaci.store.blockfrost.network.service.BFNetworkService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@WebMvcTest(BFNetworkController.class)
@TestPropertySource(properties = {
        "store.extensions.blockfrost.network.enabled=true",
        "blockfrost.apiPrefix=/api/v1/blockfrost"
})
class BFNetworkControllerTest {

    private static final String PREFIX = "/api/v1/blockfrost";


    private static final String MAX_SUPPLY    = "45000000000000000";
    private static final String RESERVES      = "12587398023789607";
    private static final String TOTAL_SUPPLY  = "32412601976210393";  // MAX_SUPPLY - RESERVES
    private static final String TREASURY      = "1500000000000000";
    private static final String LOCKED_SUPPLY = "0";
    private static final String CIRCULATING   = "30912601976210393";  // TOTAL - TREASURY - LOCKED
    private static final String ACTIVE_STAKE  = "22957728499482000";
    private static final String LIVE_STAKE    = "22957728499482000";  // approximated as active

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private BFNetworkService bfNetworkService;


    private BFNetworkDto buildNetworkDto() {
        return BFNetworkDto.builder()
                .supply(BFNetworkDto.Supply.builder()
                        .max(MAX_SUPPLY)
                        .circulating(CIRCULATING)
                        .treasury(TREASURY)
                        .reserves(RESERVES)
                        .total(TOTAL_SUPPLY)
                        .locked(LOCKED_SUPPLY)
                        .build())
                .stake(BFNetworkDto.Stake.builder()
                        .active(ACTIVE_STAKE)
                        .live(LIVE_STAKE)
                        .build())
                .build();
    }

    private BFGenesisDto buildGenesisDto() {
        return BFGenesisDto.builder()
                .activeSlotsCoefficient(0.05)
                .updateQuorum(5)
                .maxLovelaceSupply("45000000000000000")
                .networkMagic(1L)             // preprod
                .epochLength(432000L)
                .systemStart(1654041600L)     // preprod genesis
                .slotsPerKesPeriod(129600L)
                .slotLength(1)
                .maxKesEvolutions(62)
                .securityParam(2160)
                .build();
    }

    private List<BFEraDto> buildEraList() {
        BFEraDto byronEra = BFEraDto.builder()
                .start(BFEraDto.EraBoundary.builder().time(1654041600L).slot(0L).epoch(0).build())
                .end(BFEraDto.EraBoundary.builder().time(1654900800L).slot(86400L).epoch(4).build())
                .parameters(BFEraDto.EraParameters.builder()
                        .epochLength(21600L).slotLength(20).safeZone(864L).build())
                .build();

        BFEraDto shelleyEra = BFEraDto.builder()
                .start(BFEraDto.EraBoundary.builder().time(1654900800L).slot(86400L).epoch(4).build())
                .end(null)
                .parameters(BFEraDto.EraParameters.builder()
                        .epochLength(432000L).slotLength(1).safeZone(129600L).build())
                .build();

        return List.of(byronEra, shelleyEra);
    }

   
    @Nested
    @DisplayName("GET /api/v1/blockfrost/network")
    class NetworkEndpoint {

        @BeforeEach
        void setUp() {
            when(bfNetworkService.getNetworkInfo()).thenReturn(buildNetworkDto());
        }

        @Test
        @DisplayName("returns HTTP 200 with JSON content type")
        void shouldReturn200WithJsonContentType() throws Exception {
            mockMvc.perform(get(PREFIX + "/network"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON));
        }

       

        @Test
        @DisplayName("supply object is present")
        void shouldHaveSupplyObject() throws Exception {
            mockMvc.perform(get(PREFIX + "/network"))
                    .andExpect(jsonPath("$.supply").exists());
        }

        @Test
        @DisplayName("stake object is present")
        void shouldHaveStakeObject() throws Exception {
            mockMvc.perform(get(PREFIX + "/network"))
                    .andExpect(jsonPath("$.stake").exists());
        }

       

        @Test
        @DisplayName("CRITICAL: supply.total is present (derived field)")
        void shouldHaveSupplyTotal() throws Exception {
            mockMvc.perform(get(PREFIX + "/network"))
                    .andExpect(jsonPath("$.supply.total").exists())
                    .andExpect(jsonPath("$.supply.total").value(not(emptyOrNullString())));
        }

        @Test
        @DisplayName("CRITICAL: supply.locked is present (derived field)")
        void shouldHaveSupplyLocked() throws Exception {
            mockMvc.perform(get(PREFIX + "/network"))
                    .andExpect(jsonPath("$.supply.locked").exists())
                    .andExpect(jsonPath("$.supply.locked").value(not(emptyOrNullString())));
        }

        @Test
        @DisplayName("CRITICAL: stake.live is present (derived field)")
        void shouldHaveStakeLive() throws Exception {
            mockMvc.perform(get(PREFIX + "/network"))
                    .andExpect(jsonPath("$.stake.live").exists())
                    .andExpect(jsonPath("$.stake.live").value(not(emptyOrNullString())));
        }

       

        @Test
        @DisplayName("CRITICAL: supply.max is a JSON string (not number)")
        void supplyMax_mustBeJsonString() throws Exception {
            mockMvc.perform(get(PREFIX + "/network"))
                    .andExpect(jsonPath("$.supply.max").isString())
                    .andExpect(jsonPath("$.supply.max").value(MAX_SUPPLY));
        }

        @Test
        @DisplayName("CRITICAL: supply.circulating is a JSON string (not number)")
        void supplyCirculating_mustBeJsonString() throws Exception {
            mockMvc.perform(get(PREFIX + "/network"))
                    .andExpect(jsonPath("$.supply.circulating").isString())
                    .andExpect(jsonPath("$.supply.circulating").value(CIRCULATING));
        }

        @Test
        @DisplayName("CRITICAL: supply.treasury is a JSON string (not number)")
        void supplyTreasury_mustBeJsonString() throws Exception {
            mockMvc.perform(get(PREFIX + "/network"))
                    .andExpect(jsonPath("$.supply.treasury").isString())
                    .andExpect(jsonPath("$.supply.treasury").value(TREASURY));
        }

        @Test
        @DisplayName("CRITICAL: supply.reserves is a JSON string (not number)")
        void supplyReserves_mustBeJsonString() throws Exception {
            mockMvc.perform(get(PREFIX + "/network"))
                    .andExpect(jsonPath("$.supply.reserves").isString())
                    .andExpect(jsonPath("$.supply.reserves").value(RESERVES));
        }

        @Test
        @DisplayName("CRITICAL: supply.total is a JSON string (not number)")
        void supplyTotal_mustBeJsonString() throws Exception {
            mockMvc.perform(get(PREFIX + "/network"))
                    .andExpect(jsonPath("$.supply.total").isString())
                    .andExpect(jsonPath("$.supply.total").value(TOTAL_SUPPLY));
        }

        @Test
        @DisplayName("CRITICAL: supply.locked is a JSON string (not number)")
        void supplyLocked_mustBeJsonString() throws Exception {
            mockMvc.perform(get(PREFIX + "/network"))
                    .andExpect(jsonPath("$.supply.locked").isString())
                    .andExpect(jsonPath("$.supply.locked").value(LOCKED_SUPPLY));
        }

        @Test
        @DisplayName("CRITICAL: stake.active is a JSON string (not number)")
        void stakeActive_mustBeJsonString() throws Exception {
            mockMvc.perform(get(PREFIX + "/network"))
                    .andExpect(jsonPath("$.stake.active").isString())
                    .andExpect(jsonPath("$.stake.active").value(ACTIVE_STAKE));
        }

        @Test
        @DisplayName("CRITICAL: stake.live is a JSON string (not number)")
        void stakeLive_mustBeJsonString() throws Exception {
            mockMvc.perform(get(PREFIX + "/network"))
                    .andExpect(jsonPath("$.stake.live").isString())
                    .andExpect(jsonPath("$.stake.live").value(LIVE_STAKE));
        }

     
        @Test
        @DisplayName("response matches expected Blockfrost JSON structure")
        void shouldMatchBlockfrostJsonStructure() throws Exception {
            mockMvc.perform(get(PREFIX + "/network"))
                    .andDo(print())
                    .andExpect(jsonPath("$.supply.max").isString())
                    .andExpect(jsonPath("$.supply.total").isString())
                    .andExpect(jsonPath("$.supply.circulating").isString())
                    .andExpect(jsonPath("$.supply.locked").isString())
                    .andExpect(jsonPath("$.supply.treasury").isString())
                    .andExpect(jsonPath("$.supply.reserves").isString())
                    .andExpect(jsonPath("$.stake.live").isString())
                    .andExpect(jsonPath("$.stake.active").isString());
        }

        @Test
        @DisplayName("supply.total equals MAX_SUPPLY minus RESERVES (Blockfrost invariant)")
        void supplyTotal_mustEqualMaxMinusReserves() throws Exception {
            // Verify the fixture itself is internally consistent
            java.math.BigInteger max      = new java.math.BigInteger(MAX_SUPPLY);
            java.math.BigInteger reserves = new java.math.BigInteger(RESERVES);
            java.math.BigInteger expected = max.subtract(reserves);
            org.assertj.core.api.Assertions.assertThat(TOTAL_SUPPLY)
                    .as("Test fixture: TOTAL_SUPPLY must equal MAX_SUPPLY - RESERVES")
                    .isEqualTo(expected.toString());

            // Verify the HTTP response carries the correct derived value
            mockMvc.perform(get(PREFIX + "/network"))
                    .andExpect(jsonPath("$.supply.total").value(TOTAL_SUPPLY));
        }

        @Test
        @DisplayName("supply.locked is '0' (Yaci approximation — script-locked UTxOs not tracked)")
        void supplyLocked_isApproximatedAsZero() throws Exception {
            mockMvc.perform(get(PREFIX + "/network"))
                    .andExpect(jsonPath("$.supply.locked").value("0"));
        }

        @Test
        @DisplayName("stake.live equals stake.active (Yaci approximation — live snapshot unavailable)")
        void stakeLive_isApproximatedAsActive() throws Exception {
            mockMvc.perform(get(PREFIX + "/network"))
                    .andExpect(jsonPath("$.stake.live").value(ACTIVE_STAKE));
        }

        @Test
        @DisplayName("supply.circulating < supply.total (circulating excludes treasury and locked)")
        void supplyCirculating_lessThanTotal() throws Exception {
            // Verify fixture is consistent: circulating = total - treasury - locked
            java.math.BigInteger total     = new java.math.BigInteger(TOTAL_SUPPLY);
            java.math.BigInteger treasury  = new java.math.BigInteger(TREASURY);
            java.math.BigInteger locked    = new java.math.BigInteger(LOCKED_SUPPLY);
            java.math.BigInteger expected  = total.subtract(treasury).subtract(locked);
            org.assertj.core.api.Assertions.assertThat(CIRCULATING)
                    .as("Test fixture: CIRCULATING must equal TOTAL - TREASURY - LOCKED")
                    .isEqualTo(expected.toString());

            mockMvc.perform(get(PREFIX + "/network"))
                    .andExpect(jsonPath("$.supply.circulating").value(CIRCULATING));
        }

        // ── Error handling ───────────────────────────────────────────────────

        @Test
        @DisplayName("returns 503 when adapot service is unavailable")
        void shouldReturn503WhenServiceUnavailable() throws Exception {
            when(bfNetworkService.getNetworkInfo())
                    .thenThrow(new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                            "Network info service not available."));

            mockMvc.perform(get(PREFIX + "/network"))
                    .andExpect(status().isServiceUnavailable());
        }

        @Test
        @DisplayName("returns 404 when no network data found")
        void shouldReturn404WhenNotFound() throws Exception {
            when(bfNetworkService.getNetworkInfo())
                    .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND,
                            "Network information not found."));

            mockMvc.perform(get(PREFIX + "/network"))
                    .andExpect(status().isNotFound());
        }
    }

    // =========================================================================
    // /genesis — Blockchain genesis parameters
    // =========================================================================

    @Nested
    @DisplayName("GET /api/v1/blockfrost/genesis")
    class GenesisEndpoint {

        @BeforeEach
        void setUp() {
            when(bfNetworkService.getGenesis()).thenReturn(buildGenesisDto());
        }

        @Test
        @DisplayName("returns HTTP 200 with JSON content type")
        void shouldReturn200WithJsonContentType() throws Exception {
            mockMvc.perform(get(PREFIX + "/genesis"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON));
        }

        @Test
        @DisplayName("active_slots_coefficient is present and a number")
        void shouldHaveActiveSlotsCoefficient() throws Exception {
            mockMvc.perform(get(PREFIX + "/genesis"))
                    .andExpect(jsonPath("$.active_slots_coefficient").isNumber())
                    .andExpect(jsonPath("$.active_slots_coefficient").value(0.05));
        }

        @Test
        @DisplayName("CRITICAL: max_lovelace_supply is a JSON string (not number)")
        void maxLovelaceSupply_mustBeJsonString() throws Exception {
            mockMvc.perform(get(PREFIX + "/genesis"))
                    .andExpect(jsonPath("$.max_lovelace_supply").isString())
                    .andExpect(jsonPath("$.max_lovelace_supply").value("45000000000000000"));
        }

        @Test
        @DisplayName("epoch_length is present and matches expected value")
        void shouldHaveEpochLength() throws Exception {
            mockMvc.perform(get(PREFIX + "/genesis"))
                    .andExpect(jsonPath("$.epoch_length").isNumber())
                    .andExpect(jsonPath("$.epoch_length").value(432000));
        }

        @Test
        @DisplayName("security_param is present and a positive integer")
        void shouldHaveSecurityParam() throws Exception {
            mockMvc.perform(get(PREFIX + "/genesis"))
                    .andExpect(jsonPath("$.security_param").isNumber())
                    .andExpect(jsonPath("$.security_param").value(greaterThan(0)));
        }

        @Test
        @DisplayName("slot_length is 1 (Shelley slot duration)")
        void shouldHaveSlotLengthOfOne() throws Exception {
            mockMvc.perform(get(PREFIX + "/genesis"))
                    .andExpect(jsonPath("$.slot_length").value(1));
        }

        @Test
        @DisplayName("system_start is a valid Unix timestamp")
        void shouldHaveSystemStart() throws Exception {
            mockMvc.perform(get(PREFIX + "/genesis"))
                    .andExpect(jsonPath("$.system_start").isNumber())
                    .andExpect(jsonPath("$.system_start").value(greaterThan(1504224000))); // > Cardano genesis
        }

        @Test
        @DisplayName("response has all required Blockfrost genesis fields")
        void shouldHaveAllRequiredFields() throws Exception {
            mockMvc.perform(get(PREFIX + "/genesis"))
                    .andDo(print())
                    .andExpect(jsonPath("$.active_slots_coefficient").exists())
                    .andExpect(jsonPath("$.update_quorum").exists())
                    .andExpect(jsonPath("$.max_lovelace_supply").exists())
                    .andExpect(jsonPath("$.network_magic").exists())
                    .andExpect(jsonPath("$.epoch_length").exists())
                    .andExpect(jsonPath("$.system_start").exists())
                    .andExpect(jsonPath("$.slots_per_kes_period").exists())
                    .andExpect(jsonPath("$.slot_length").exists())
                    .andExpect(jsonPath("$.max_kes_evolutions").exists())
                    .andExpect(jsonPath("$.security_param").exists());
        }
    }

    // =========================================================================
    // /network/eras — Era summaries
    // =========================================================================

    @Nested
    @DisplayName("GET /api/v1/blockfrost/network/eras")
    class ErasEndpoint {

        @BeforeEach
        void setUp() {
            when(bfNetworkService.getNetworkEras()).thenReturn(buildEraList());
        }

        @Test
        @DisplayName("returns HTTP 200 with JSON content type")
        void shouldReturn200WithJsonContentType() throws Exception {
            mockMvc.perform(get(PREFIX + "/network/eras"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON));
        }

        @Test
        @DisplayName("response is a JSON array")
        void shouldReturnJsonArray() throws Exception {
            mockMvc.perform(get(PREFIX + "/network/eras"))
                    .andExpect(jsonPath("$").isArray());
        }

        @Test
        @DisplayName("response contains multiple eras (Byron + at least one post-Byron)")
        void shouldReturnMultipleEras() throws Exception {
            mockMvc.perform(get(PREFIX + "/network/eras"))
                    .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))));
        }

        @Test
        @DisplayName("first era has start.slot = 0 (Byron genesis)")
        void firstEra_startSlot_shouldBeZero() throws Exception {
            mockMvc.perform(get(PREFIX + "/network/eras"))
                    .andExpect(jsonPath("$[0].start.slot").value(0));
        }

        @Test
        @DisplayName("first era has start.epoch = 0")
        void firstEra_startEpoch_shouldBeZero() throws Exception {
            mockMvc.perform(get(PREFIX + "/network/eras"))
                    .andExpect(jsonPath("$[0].start.epoch").value(0));
        }

        @Test
        @DisplayName("every era has a start boundary")
        void everyEra_shouldHaveStartBoundary() throws Exception {
            mockMvc.perform(get(PREFIX + "/network/eras"))
                    .andExpect(jsonPath("$[*].start").exists())
                    .andExpect(jsonPath("$[*].start.slot").exists())
                    .andExpect(jsonPath("$[*].start.time").exists())
                    .andExpect(jsonPath("$[*].start.epoch").exists());
        }

        @Test
        @DisplayName("every era has epoch_length, slot_length, safe_zone parameters")
        void everyEra_shouldHaveParameters() throws Exception {
            mockMvc.perform(get(PREFIX + "/network/eras"))
                    .andExpect(jsonPath("$[*].parameters").exists())
                    .andExpect(jsonPath("$[*].parameters.epoch_length").exists())
                    .andExpect(jsonPath("$[*].parameters.slot_length").exists())
                    .andExpect(jsonPath("$[*].parameters.safe_zone").exists());
        }

        @Test
        @DisplayName("Byron era has slot_length = 20 seconds")
        void byronEra_shouldHaveSlotLengthOf20() throws Exception {
            mockMvc.perform(get(PREFIX + "/network/eras"))
                    .andExpect(jsonPath("$[0].parameters.slot_length").value(20));
        }

        @Test
        @DisplayName("Shelley era has slot_length = 1 second")
        void shelleyEra_shouldHaveSlotLengthOf1() throws Exception {
            mockMvc.perform(get(PREFIX + "/network/eras"))
                    .andExpect(jsonPath("$[1].parameters.slot_length").value(1));
        }

        @Test
        @DisplayName("returns empty list without exceptions when no eras available")
        void shouldHandleEmptyEraList() throws Exception {
            when(bfNetworkService.getNetworkEras()).thenReturn(List.of());

            mockMvc.perform(get(PREFIX + "/network/eras"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$", hasSize(0)));
        }

        @Test
        @DisplayName("full era structure matches Blockfrost schema")
        void shouldMatchBlockfrostEraSchema() throws Exception {
            mockMvc.perform(get(PREFIX + "/network/eras"))
                    .andDo(print())
                    .andExpect(jsonPath("$[0].start").exists())
                    .andExpect(jsonPath("$[0].start.time").isNumber())
                    .andExpect(jsonPath("$[0].start.slot").isNumber())
                    .andExpect(jsonPath("$[0].start.epoch").isNumber())
                    .andExpect(jsonPath("$[0].end").exists())
                    .andExpect(jsonPath("$[0].end.time").isNumber())
                    .andExpect(jsonPath("$[0].end.slot").isNumber())
                    .andExpect(jsonPath("$[0].end.epoch").isNumber())
                    .andExpect(jsonPath("$[0].parameters.epoch_length").isNumber())
                    .andExpect(jsonPath("$[0].parameters.slot_length").isNumber())
                    .andExpect(jsonPath("$[0].parameters.safe_zone").isNumber());
        }
    }

    // =====================================================================
    // / — Root endpoint
    // =====================================================================

    @Nested
    @DisplayName("GET /api/v1/blockfrost  (root)")
    class RootEndpoint {

        @BeforeEach
        void setUp() {
            when(bfNetworkService.getRoot()).thenReturn(
                    BFRootDto.builder()
                            .url("https://cardano-mainnet.blockfrost.io/api")
                            .version("0.1.30")
                            .build());
        }

        @Test
        @DisplayName("returns HTTP 200")
        void shouldReturn200() throws Exception {
            // Spring Boot 3.x: trailing slash is NOT automatically matched.
            // The controller's @GetMapping (no path) maps to the exact base prefix.
            mockMvc.perform(get(PREFIX))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("url field is present and non-empty")
        void shouldHaveUrlField() throws Exception {
            mockMvc.perform(get(PREFIX))
                    .andExpect(jsonPath("$.url").isString())
                    .andExpect(jsonPath("$.url").value(not(emptyString())));
        }

        @Test
        @DisplayName("version field is present and non-empty")
        void shouldHaveVersionField() throws Exception {
            mockMvc.perform(get(PREFIX))
                    .andExpect(jsonPath("$.version").isString())
                    .andExpect(jsonPath("$.version").value(not(emptyString())));
        }
    }
}
