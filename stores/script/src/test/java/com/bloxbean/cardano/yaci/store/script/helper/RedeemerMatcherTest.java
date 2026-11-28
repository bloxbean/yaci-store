package com.bloxbean.cardano.yaci.store.script.helper;

import com.bloxbean.cardano.yaci.core.model.*;
import com.bloxbean.cardano.yaci.core.model.governance.GovActionId;
import com.bloxbean.cardano.yaci.core.model.governance.Voter;
import com.bloxbean.cardano.yaci.core.model.governance.VoterType;
import com.bloxbean.cardano.yaci.core.model.governance.VotingProcedure;
import com.bloxbean.cardano.yaci.core.model.governance.VotingProcedures;
import com.bloxbean.cardano.yaci.helper.model.Transaction;
import com.bloxbean.cardano.yaci.store.client.utxo.UtxoClient;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class RedeemerMatcherTest {

    private final RedeemerDatumMatcher matcher = new RedeemerDatumMatcher(mock(UtxoClient.class));

    @Test
    void findSpendScriptFromRedeemer() {
        //TODO -
        //Test data
        // txHash: 0d66c70aa9efa6bc1a635191abef9a0cd386a42b3f43c3199f3b794986995865 (with script ref in input)
    }

    //Mainnet tx 0392f5b4f3f93a0d6e2d12779a16f5b2da0fca0f26340ed85ba2205fe017e415: the mint map is not in policy id order
    @Test
    void findScriptsForRedeemers_mintRedeemersFollowPolicyIdOrder() {
        String policy460e = "460e2da186289ba263e644aa4e1b852329895d1b8fa5f83a4b160ba9";
        String policy1bc6 = "1bc6b83356e3ae9519997a7c4f2d3bdf85d058ee21551eb4b937eb26";
        Map<String, PlutusScript> scripts = scripts(policy460e, policy1bc6);

        TransactionBody body = body()
                .mint(List.of(mint(policy460e), mint(policy1bc6)))
                .build();

        List<ScriptContext> contexts = matcher.findScriptsForRedeemers(tx(body, RedeemerTag.Mint, 2), scripts);

        assertThat(contexts.get(0).getPlutusScript()).isSameAs(scripts.get(policy1bc6));
        assertThat(contexts.get(1).getPlutusScript()).isSameAs(scripts.get(policy460e));
    }

    //Mainnet tx 0072d81f31868f5bf4833f06dfc094017e429f50e5df721b92b19bb5358e77c1: the withdrawal map is not in credential order
    @Test
    void findScriptsForRedeemers_rewardRedeemersFollowCredentialHashOrder() {
        String scriptD8b6 = "d8b69fc53637bcfadbc4469083f706bc293f4d9d2296646c5ca167bb";
        String script3b3a = "3b3aca6e0ed3af06fcc51c30db622b9dfcd56bcf30213512ecd49f76";
        Map<String, PlutusScript> scripts = scripts(scriptD8b6, script3b3a);

        Map<String, BigInteger> withdrawals = new LinkedHashMap<>();
        withdrawals.put("f1" + scriptD8b6, BigInteger.ZERO);
        withdrawals.put("f1" + script3b3a, BigInteger.ZERO);
        TransactionBody body = body().withdrawals(withdrawals).build();

        List<ScriptContext> contexts = matcher.findScriptsForRedeemers(tx(body, RedeemerTag.Reward, 2), scripts);

        assertThat(contexts.get(0).getPlutusScript()).isSameAs(scripts.get(script3b3a));
        assertThat(contexts.get(1).getPlutusScript()).isSameAs(scripts.get(scriptD8b6));
    }

    @Test
    void findScriptsForRedeemers_rewardRedeemersPutScriptCredentialsBeforeKeyCredentials() {
        String keyHash = "11".repeat(28);
        String scriptHash = "22".repeat(28);
        Map<String, PlutusScript> scripts = scripts(scriptHash);

        //Key credential first in CBOR, with a lower hash and a lower header byte (e1 < f1)
        Map<String, BigInteger> withdrawals = new LinkedHashMap<>();
        withdrawals.put("e1" + keyHash, BigInteger.ZERO);
        withdrawals.put("f1" + scriptHash, BigInteger.ZERO);
        TransactionBody body = body().withdrawals(withdrawals).build();

        List<ScriptContext> contexts = matcher.findScriptsForRedeemers(tx(body, RedeemerTag.Reward, 1), scripts);

        assertThat(contexts.get(0).getPlutusScript()).isSameAs(scripts.get(scriptHash));
    }

    @Test
    void findScriptsForRedeemers_votingRedeemersFollowLedgerVoterOrder() {
        String drepKey = "aa".repeat(28);
        String pool = "bb".repeat(28);
        String drepScript = "cc".repeat(28);
        String committeeScript = "dd".repeat(28);
        Map<String, PlutusScript> scripts = scripts(drepScript, committeeScript);

        Map<Voter, Map<GovActionId, VotingProcedure>> voting = new LinkedHashMap<>();
        voting.put(new Voter(VoterType.DREP_KEY_HASH, drepKey), Map.of());
        voting.put(new Voter(VoterType.STAKING_POOL_KEY_HASH, pool), Map.of());
        voting.put(new Voter(VoterType.DREP_SCRIPT_HASH, drepScript), Map.of());
        voting.put(new Voter(VoterType.CONSTITUTIONAL_COMMITTEE_HOT_SCRIPT_HASH, committeeScript), Map.of());
        TransactionBody body = body().votingProcedures(new VotingProcedures(voting)).build();

        List<ScriptContext> contexts = matcher.findScriptsForRedeemers(tx(body, RedeemerTag.Voting, 2), scripts);

        assertThat(contexts.get(0).getPlutusScript()).isSameAs(scripts.get(committeeScript));
        assertThat(contexts.get(1).getPlutusScript()).isSameAs(scripts.get(drepScript));
    }

    @Test
    void findScriptsForRedeemers_votingRedeemersOfSameVoterTypeFollowHashOrder() {
        String drepScriptEe = "ee".repeat(28);
        String drepScriptCc = "cc".repeat(28);
        Map<String, PlutusScript> scripts = scripts(drepScriptEe, drepScriptCc);

        Map<Voter, Map<GovActionId, VotingProcedure>> voting = new LinkedHashMap<>();
        voting.put(new Voter(VoterType.DREP_SCRIPT_HASH, drepScriptEe), Map.of());
        voting.put(new Voter(VoterType.DREP_SCRIPT_HASH, drepScriptCc), Map.of());
        TransactionBody body = body().votingProcedures(new VotingProcedures(voting)).build();

        List<ScriptContext> contexts = matcher.findScriptsForRedeemers(tx(body, RedeemerTag.Voting, 2), scripts);

        assertThat(contexts.get(0).getPlutusScript()).isSameAs(scripts.get(drepScriptCc));
        assertThat(contexts.get(1).getPlutusScript()).isSameAs(scripts.get(drepScriptEe));
    }

    @Test
    void findScriptsForRedeemers_votingIndexCountsKeyVoters() {
        String committeeKey = "11".repeat(28);
        String drepScript = "22".repeat(28);
        Map<String, PlutusScript> scripts = scripts(drepScript);

        //Committee key voter sorts before the DRep script voter, so the DRep script redeemer has index 1
        Map<Voter, Map<GovActionId, VotingProcedure>> voting = new LinkedHashMap<>();
        voting.put(new Voter(VoterType.DREP_SCRIPT_HASH, drepScript), Map.of());
        voting.put(new Voter(VoterType.CONSTITUTIONAL_COMMITTEE_HOT_KEY_HASH, committeeKey), Map.of());
        TransactionBody body = body().votingProcedures(new VotingProcedures(voting)).build();

        Transaction tx = tx(body, RedeemerTag.Voting, 0);
        tx.getWitnesses().getRedeemers().add(Redeemer.builder().tag(RedeemerTag.Voting).index(1).build());

        List<ScriptContext> contexts = matcher.findScriptsForRedeemers(tx, scripts);

        assertThat(contexts).hasSize(1);
        assertThat(contexts.get(0).getPlutusScript()).isSameAs(scripts.get(drepScript));
    }

    @Test
    void findScriptsForRedeemers_returnsEmptyContextWhenVotingIndexIsOutOfRange() {
        String drepScript = "22".repeat(28);
        Map<Voter, Map<GovActionId, VotingProcedure>> voting = new LinkedHashMap<>();
        voting.put(new Voter(VoterType.DREP_SCRIPT_HASH, drepScript), Map.of());
        TransactionBody body = body().votingProcedures(new VotingProcedures(voting)).build();

        List<ScriptContext> contexts = matcher.findScriptsForRedeemers(tx(body, RedeemerTag.Voting, 2), scripts(drepScript));

        assertThat(contexts.get(1).getPlutusScript()).isNull();
        assertThat(contexts.get(1).getRedeemer().getIndex()).isEqualTo(1);
    }

    @Test
    void findScriptsForRedeemers_returnsEmptyContextWhenRewardIndexIsOutOfRange() {
        String scriptHash = "22".repeat(28);
        TransactionBody body = body().withdrawals(Map.of("f1" + scriptHash, BigInteger.ZERO)).build();

        List<ScriptContext> contexts = matcher.findScriptsForRedeemers(tx(body, RedeemerTag.Reward, 2), scripts(scriptHash));

        assertThat(contexts.get(1).getPlutusScript()).isNull();
        assertThat(contexts.get(1).getRedeemer().getIndex()).isEqualTo(1);
    }

    private static TransactionBody.TransactionBodyBuilder body() {
        return TransactionBody.builder().inputs(Set.of());
    }

    private static Transaction tx(TransactionBody body, RedeemerTag tag, int redeemerCount) {
        List<Redeemer> redeemers = new ArrayList<>();
        for (int i = 0; i < redeemerCount; i++)
            redeemers.add(Redeemer.builder().tag(tag).index(i).build());

        return Transaction.builder()
                .txHash("tx")
                .body(body)
                .witnesses(Witnesses.builder().redeemers(redeemers).build())
                .build();
    }

    private static Amount mint(String policyId) {
        return Amount.builder().policyId(policyId).assetName("token").quantity(BigInteger.ONE).build();
    }

    private static Map<String, PlutusScript> scripts(String... hashes) {
        Map<String, PlutusScript> scripts = new HashMap<>();
        for (String hash : hashes)
            scripts.put(hash, PlutusScript.builder().type(PlutusScriptType.PlutusScriptV2).content(hash).build());
        return scripts;
    }
}
