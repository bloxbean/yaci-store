package com.bloxbean.cardano.yaci.store.blockfrost.governance.service;

import com.bloxbean.cardano.yaci.store.blockfrost.governance.storage.BFGovernanceStorageReader;
import com.bloxbean.cardano.yaci.store.blockfrost.governance.storage.impl.model.BFProposal;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BFProposalGovernanceDescriptionTest {

    private static final String TX_HASH = "ab".repeat(32);

    private final ObjectMapper objectMapper = new ObjectMapper();
    private BFGovernanceStorageReader storageReader;
    private BFGovernanceService governanceService;

    @BeforeEach
    void setUp() {
        storageReader = mock(BFGovernanceStorageReader.class);
        governanceService = new BFGovernanceService(
                storageReader, objectMapper, new BFGovernanceDescriptionTransformer(objectMapper));
    }

    @Test
    void mapsInfoActionWithoutContents() throws Exception {
        mockProposal("INFO_ACTION", objectMapper.readTree("{\"type\":\"INFO_ACTION\"}"));

        JsonNode description = governanceService.getProposal(TX_HASH, 0).getGovernanceDescription();

        assertThat(description).isEqualTo(objectMapper.readTree("{\"tag\":\"InfoAction\"}"));
    }

    @Test
    void mapsParameterChangeUsingLedgerJsonNamesAndValues() throws Exception {
        mockProposal("PARAMETER_CHANGE_ACTION", objectMapper.readTree("""
                {
                  "govActionId": null,
                  "protocolParamUpdate": {
                    "nopt": 500,
                    "poolPledgeInfluence": {"numerator": 3, "denominator": 10},
                    "maxTxExMem": 17500000,
                    "maxTxExSteps": 10000000000,
                    "costModels": {"2": "9f0102ff"}
                  },
                  "policyHash": "policy"
                }
                """));

        JsonNode contents = governanceService.getProposal(TX_HASH, 0)
                .getGovernanceDescription().get("contents");

        assertThat(contents.get(0).isNull()).isTrue();
        assertThat(contents.get(1).toString()).isEqualTo(objectMapper.readTree("""
                {
                  "stakePoolTargetNum": 500,
                  "poolPledgeInfluence": 0.3,
                  "costModels": {"PlutusV3": [1, 2]},
                  "maxTxExecutionUnits": {"memory": 17500000, "steps": 10000000000}
                }
                """).toString());
        assertThat(contents.get(2).asText()).isEqualTo("policy");
    }

    @Test
    void mapsRationalsUsingLedgerExactDecimalRules() throws Exception {
        mockProposal("PARAMETER_CHANGE_ACTION", objectMapper.readTree("""
                {
                  "govActionId": null,
                  "protocolParamUpdate": {
                    "poolPledgeInfluence": {"numerator": 3, "denominator": 6},
                    "expansionRate": {"numerator": 1, "denominator": 3},
                    "treasuryGrowthRate": {"numerator": 2, "denominator": 6},
                    "minFeeRefScriptCostPerByte": {
                      "numerator": 1,
                      "denominator": 95367431640625
                    }
                  },
                  "policyHash": null
                }
                """));

        JsonNode protocolParams = governanceService.getProposal(TX_HASH, 0)
                .getGovernanceDescription().get("contents").get(1);

        assertThat(protocolParams.toString()).isEqualTo(objectMapper.readTree("""
                {
                  "poolPledgeInfluence": 0.5,
                  "monetaryExpansion": {"numerator": 1, "denominator": 3},
                  "treasuryCut": {"numerator": 1, "denominator": 3},
                  "minFeeRefScriptCostPerByte": {
                    "numerator": 1,
                    "denominator": 95367431640625
                  }
                }
                """).toString());
    }

    @Test
    void mapsAndOrdersCommitteeCredentialsLikeCardanoLedger() throws Exception {
        mockProposal("UPDATE_COMMITTEE", objectMapper.readTree("""
                {
                  "govActionId": null,
                  "membersForRemoval": [
                    {"type": "ADDR_KEYHASH", "hash": "00"},
                    {"type": "SCRIPTHASH", "hash": "ff"}
                  ],
                  "newMembersAndTerms": {
                    "{\\\"type\\\":\\\"ADDR_KEYHASH\\\",\\\"hash\\\":\\\"aa\\\"}": 900
                  },
                  "threshold": {"numerator": 1, "denominator": 2}
                }
                """));

        JsonNode contents = governanceService.getProposal(TX_HASH, 0)
                .getGovernanceDescription().get("contents");

        assertThat(contents.get(1)).isEqualTo(objectMapper.readTree("""
                [{"scriptHash":"ff"},{"keyHash":"00"}]
                """));
        assertThat(contents.get(2).get("keyHash-aa").asInt()).isEqualTo(900);
        assertThat(contents.get(3).decimalValue()).isEqualByComparingTo("0.5");
    }

    private void mockProposal(String type, JsonNode details) {
        BFProposal proposal = BFProposal.builder()
                .txHash(TX_HASH)
                .index(0)
                .type(type)
                .details(details)
                .build();
        when(storageReader.findProposalByTxHashAndIndex(TX_HASH, 0)).thenReturn(Optional.of(proposal));
    }
}
