package com.bloxbean.cardano.yaci.store.governancerules.rule.it;

import com.bloxbean.cardano.client.account.Account;
import com.bloxbean.cardano.client.api.model.Result;
import com.bloxbean.cardano.client.backend.api.BackendService;
import com.bloxbean.cardano.client.backend.blockfrost.common.Constants;
import com.bloxbean.cardano.client.backend.blockfrost.service.BFBackendService;
import com.bloxbean.cardano.client.common.model.Networks;
import com.bloxbean.cardano.client.function.helper.SignerProviders;
import com.bloxbean.cardano.client.governance.DRepId;
import com.bloxbean.cardano.client.quicktx.QuickTxBuilder;
import com.bloxbean.cardano.client.quicktx.Tx;
import com.bloxbean.cardano.client.spec.UnitInterval;
import com.bloxbean.cardano.client.transaction.spec.ProtocolParamUpdate;
import com.bloxbean.cardano.client.transaction.spec.ProtocolVersion;
import com.bloxbean.cardano.client.transaction.spec.Withdrawal;
import com.bloxbean.cardano.client.transaction.spec.governance.*;
import com.bloxbean.cardano.client.transaction.spec.governance.actions.*;
import com.bloxbean.cardano.client.util.HexUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;

import static com.bloxbean.cardano.client.common.ADAConversionUtil.adaToLovelace;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Disabled
class GovernanceTxIT extends QuickTxBaseIT {
    BackendService backendService;
    Account sender1;
    Account sender2;
    Account sender3;

    String sender1Addr;
    String sender2Addr;
    String sender3Addr;

    QuickTxBuilder quickTxBuilder;

    @Override
    public BackendService getBackendService() {
        if (BLOCKFROST.equals(backendType)) {
            return new BFBackendService(Constants.BLOCKFROST_SANCHONET_URL, bfProjectId);
        } else
            return super.getBackendService();
    }

    @BeforeEach
    void setup() {
        backendService = getBackendService();
        quickTxBuilder = new QuickTxBuilder(backendService);
//        addr_test1qp6swhj5utlzqwew2weq3djvlm009hx0ufefv8rwc4vhm8ezgm6wzyfv9z6qzxherk26fy0slf39y4vf5u69thhvnmzqa7e3eg
        String senderMnemonic = "advance balance awkward desert artefact drama explain hour butter benefit front lottery cross system pause capable cost fog this lizard garlic carpet oil unaware";
        sender1 = new Account(Networks.testnet(), senderMnemonic);
        sender1Addr = sender1.baseAddress();
//        addr_test1qq4jfvufrfvs4x4xyxlwk80nlvujnkrj6c94xxcarr8vl3nfc3tu0mavhq5vg2gfz9eec720z5r7qf2rhw83w5arnfestjum5l
        String sender2Mnemonic = "roast solar help ramp door dream pattern online runway yard animal shell true squeeze doll best noise moon smart group spray script spirit carbon";
        sender2 = new Account(Networks.testnet(), sender2Mnemonic);
        sender2Addr = sender2.baseAddress();
        String sender3Mnemonic = "test test test test test test test test test test test test test test test test test test test test test test test sauce";
        sender3 = new Account(Networks.testnet(), sender3Mnemonic);
        sender3Addr = sender3.baseAddress();
    }

    @Test
    void registerDrep() {
        var anchor =
            new Anchor(
                    "https://shorturl.at/vBIJ8",
                    HexUtil.decodeHexString(
                            "6dd65423ea0754ddf8a1a142dfc8152797b6fb4a4cd174a0cd3028f681a0c755"));
        QuickTxBuilder quickTxBuilder = new QuickTxBuilder(backendService);

        Tx drepRegTx = new Tx().registerDRep(sender2, anchor).from(sender2.baseAddress());

        Result<String> result =
                quickTxBuilder
                        .compose(drepRegTx)
                        .withSigner(SignerProviders.signerFrom(sender2))
                        .withSigner(SignerProviders.signerFrom(sender2.drepHdKeyPair()))
                        .complete();

        System.out.println("DRepId : " + sender2.drepId());

        System.out.println(result);
        assertTrue(result.isSuccessful());
        waitForTransaction(result);

        checkIfUtxoAvailable(result.getValue(), sender2.baseAddress());
    }

    @Test
    void deRegisterDrep() {
        QuickTxBuilder quickTxBuilder = new QuickTxBuilder(backendService);
        Tx tx = new Tx()
                .unregisterDRep(sender1.drepCredential())
                .from(sender1Addr);

        Result<String> result = quickTxBuilder.compose(tx)
                .withSigner(SignerProviders.drepKeySignerFrom(sender1))
                .withSigner(SignerProviders.signerFrom(sender1))
                .complete();

        System.out.println(result);
        assertTrue(result.isSuccessful());
        waitForTransaction(result);

        checkIfUtxoAvailable(result.getValue(), sender1Addr);
    }

    @Test
    void updateDrep() {
        QuickTxBuilder quickTxBuilder = new QuickTxBuilder(backendService);

        var anchor = new Anchor("https://xyz.com",
                HexUtil.decodeHexString("cafef700c0039a2efb056a665b3a8bcd94f8670b88d659f7f3db68340f6f0937"));

        Tx drepRegTx = new Tx()
                .updateDRep(sender1.drepCredential(), anchor)
                .from(sender1Addr);

        Result<String> result = quickTxBuilder.compose(drepRegTx)
                .withSigner(SignerProviders.drepKeySignerFrom(sender1))
                .withSigner(SignerProviders.signerFrom(sender1))
                .completeAndWait(s -> System.out.println(s));

        System.out.println("DRepId : " + sender1.drepId());
        System.out.println(result);
        assertTrue(result.isSuccessful());
        checkIfUtxoAvailable(result.getValue(), sender1Addr);
    }

    @Test
    void createProposal_infoAction() {
        QuickTxBuilder quickTxBuilder = new QuickTxBuilder(backendService);

        var govAction = new InfoAction();
        var anchor = new Anchor("https://xyz.com",
                HexUtil.decodeHexString("cafef700c0039a2efb056a665b3a8bcd94f8670b88d659f7f3db68340f6f0937"));

        Tx tx = new Tx()
                .createProposal(govAction, sender1.stakeAddress(), anchor)
                .from(sender1Addr);

        Result<String> result = quickTxBuilder.compose(tx)
                .withSigner(SignerProviders.drepKeySignerFrom(sender1))
                .withSigner(SignerProviders.signerFrom(sender1))
                .completeAndWait(s -> System.out.println(s));

        System.out.println(result);
        assertTrue(result.isSuccessful());
        checkIfUtxoAvailable(result.getValue(), sender1Addr);
    }

    @Test
    void createProposal_newConstitutionAction() {
        QuickTxBuilder quickTxBuilder = new QuickTxBuilder(backendService);

        var anchor = new Anchor("https://bit.ly/2kBHHHL",
                HexUtil.decodeHexString("cdfef700c0039a2efb056a665b3a8bcd94f8670b88d659f7f3db68340f6f0937"));
        var govAction = new NewConstitution();
        govAction.setPrevGovActionId(new GovActionId("bd5d786d745ec7c1994f8cff341afee513c7cdad73e8883d540ff71c41763fd1", 0));
        govAction.setConstitution(Constitution.builder()
                .anchor(anchor)
                .build());

        Tx tx = new Tx()
                .createProposal(govAction, sender1.stakeAddress(), anchor)
                .from(sender1Addr);

        Result<String> result = quickTxBuilder.compose(tx)
                .withSigner(SignerProviders.drepKeySignerFrom(sender1))
                .withSigner(SignerProviders.signerFrom(sender1))
                .completeAndWait(s -> System.out.println(s));

        System.out.println(result);
        assertTrue(result.isSuccessful());
        checkIfUtxoAvailable(result.getValue(), sender1Addr);
    }

    @Test
    void createProposal_noConfidence() {
        QuickTxBuilder quickTxBuilder = new QuickTxBuilder(backendService);

        var noConfidence = new NoConfidence();
        noConfidence.setPrevGovActionId(new GovActionId("e86050ac376fc4df7c76635f648c963f44702e13beb81a5c9971a418013c74dc", 0));
        var anchor = new Anchor("https://xyz.com",
                HexUtil.decodeHexString("cafef700c0039a2efb056a665b3a8bcd94f8670b88d659f7f3db68340f6f0937"));

        Tx tx = new Tx()
                .createProposal(noConfidence, sender1.stakeAddress(), anchor)
                .from(sender1Addr);

        Result<String> result = quickTxBuilder.compose(tx)
                .withSigner(SignerProviders.drepKeySignerFrom(sender1))
                .withSigner(SignerProviders.signerFrom(sender1))
                .completeAndWait(s -> System.out.println(s));

        System.out.println(result);
        assertTrue(result.isSuccessful());
        checkIfUtxoAvailable(result.getValue(), sender1Addr);
    }

    @Test
    void createProposal_parameterChangeAction() {
        QuickTxBuilder quickTxBuilder = new QuickTxBuilder(backendService);

        var parameterChange = new ParameterChangeAction();
//        parameterChange.setPrevGovActionId(new GovActionId("529736be1fac33431667f2b66231b7b66d4c7a3975319ddac7cfb17dcb5c4145", 0));
        parameterChange.setProtocolParamUpdate(ProtocolParamUpdate.builder()
                .minPoolCost(adaToLovelace(100))
                .build()
        );
        var anchor = new Anchor("https://xyz.com",
                HexUtil.decodeHexString("cafef700c0039a2efb056a665b3a8bcd94f8670b88d659f7f3db68340f6f0937"));

        Tx tx = new Tx()
                .createProposal(parameterChange, sender1.stakeAddress(), anchor)
                .from(sender1Addr);

        Result<String> result = quickTxBuilder.compose(tx)
                .withSigner(SignerProviders.drepKeySignerFrom(sender1))
                .withSigner(SignerProviders.signerFrom(sender1))
                .completeAndWait(s -> System.out.println(s));

        System.out.println(result);
        assertTrue(result.isSuccessful());
        checkIfUtxoAvailable(result.getValue(), sender1Addr);
    }

    @Test
    void createProposal_treasuryWithdrawalAction() {
        QuickTxBuilder quickTxBuilder = new QuickTxBuilder(backendService);

        var treasuryWithdrawalsAction = new TreasuryWithdrawalsAction();
        treasuryWithdrawalsAction.addWithdrawal(new Withdrawal("stake_test1ur6l9f5l9jw44kl2nf6nm5kca3nwqqkccwynnjm0h2cv60ccngdwa", adaToLovelace(20)));
        var anchor = new Anchor("https://xyz.com",
                HexUtil.decodeHexString("daeef700c0039a2efb056a665b3a8bcd94f8670b88d659f7f3db68340f6f0937"));

        Tx tx = new Tx()
                .createProposal(treasuryWithdrawalsAction,sender1.stakeAddress(), anchor)
                .from(sender1Addr);

        Result<String> result = quickTxBuilder.compose(tx)
                .withSigner(SignerProviders.drepKeySignerFrom(sender1))
                .withSigner(SignerProviders.signerFrom(sender1))
                .completeAndWait(s -> System.out.println(s));

        System.out.println(result);
        assertTrue(result.isSuccessful());
        checkIfUtxoAvailable(result.getValue(), sender1Addr);
    }

    @Test
    void createProposal_updateCommittee() {
        QuickTxBuilder quickTxBuilder = new QuickTxBuilder(backendService);

        var updateCommittee = new UpdateCommittee();
        updateCommittee.setPrevGovActionId(new GovActionId("b3ce0371310a07a797657d19453d953bb352b6841c2f5c5e0bd2557189ef5c3a", 0));
        updateCommittee.setQuorumThreshold(new UnitInterval(BigInteger.valueOf(1), BigInteger.valueOf(3)));

        var anchor = new Anchor("https://xyz.com",
                HexUtil.decodeHexString("daeef700c0039a2efb056a665b3a8bcd94f8670b88d659f7f3db68340f6f0937"));

        Tx tx = new Tx()
                .createProposal(updateCommittee, sender1.stakeAddress(), anchor)
                .from(sender1Addr);

        Result<String> result = quickTxBuilder.compose(tx)
                .withSigner(SignerProviders.drepKeySignerFrom(sender1))
                .withSigner(SignerProviders.signerFrom(sender1))
                .completeAndWait(s -> System.out.println(s));

        System.out.println(result);
        assertTrue(result.isSuccessful());
        checkIfUtxoAvailable(result.getValue(), sender1Addr);
    }

    @Test
    void createProposal_hardforkInitiation() {
        QuickTxBuilder quickTxBuilder = new QuickTxBuilder(backendService);

        var hardforkInitiation = new HardForkInitiationAction();
        hardforkInitiation.setPrevGovActionId(new GovActionId("416f7f01c548a85546aa5bbd155b34bb2802df68e08db4e843ef6da764cd8f7e", 0));
        hardforkInitiation.setProtocolVersion(new ProtocolVersion(9, 3));

        var anchor = new Anchor("https://xyz.com",
                HexUtil.decodeHexString("daeef700c0039a2efb056a665b3a8bcd94f8670b88d659f7f3db68340f6f0937"));

        Tx tx = new Tx()
                .createProposal(hardforkInitiation,sender1.stakeAddress(), anchor)
                .from(sender1Addr);

        Result<String> result = quickTxBuilder.compose(tx)
                .withSigner(SignerProviders.drepKeySignerFrom(sender1))
                .withSigner(SignerProviders.signerFrom(sender1))
                .completeAndWait(s -> System.out.println(s));

        System.out.println(result);
        assertTrue(result.isSuccessful());
        checkIfUtxoAvailable(result.getValue(), sender1Addr);
    }

    @Test
    void createVote() {
        QuickTxBuilder quickTxBuilder = new QuickTxBuilder(backendService);

        var anchor = new Anchor("https://xyz.com",
                HexUtil.decodeHexString("daeef700c0039a2efb056a665b3a8bcd94f8670b88d659f7f3db68340f6f0937"));

        var voter = new Voter(VoterType.DREP_KEY_HASH, sender1.drepCredential());
        Tx tx = new Tx()
                .createVote(voter, new GovActionId("5655fbb4ceafd34296fe58f6e3d28b8ff663a89e84aa0edd77bd02fe379cef4c", 0),
                        Vote.NO, anchor)
                .from(sender1Addr);

        Result<String> result = quickTxBuilder.compose(tx)
                .withSigner(SignerProviders.drepKeySignerFrom(sender1))
                .withSigner(SignerProviders.signerFrom(sender1))
                .completeAndWait(s -> System.out.println(s));

        System.out.println(result);
        assertTrue(result.isSuccessful());
        checkIfUtxoAvailable(result.getValue(), sender1Addr);
    }

    @Test
    void createVote_noAnchor() {
        QuickTxBuilder quickTxBuilder = new QuickTxBuilder(backendService);

        var voter = new Voter(VoterType.DREP_KEY_HASH, sender2.drepCredential());
        Tx tx = new Tx()
                .createVote(voter, new GovActionId("5655fbb4ceafd34296fe58f6e3d28b8ff663a89e84aa0edd77bd02fe379cef4c", 0),
                        Vote.YES)
                .from(sender1Addr);

        Result<String> result = quickTxBuilder.compose(tx)
                .withSigner(SignerProviders.drepKeySignerFrom(sender2))
                .withSigner(SignerProviders.signerFrom(sender1))
                .completeAndWait(s -> System.out.println(s));

        System.out.println(result);
        assertTrue(result.isSuccessful());
        checkIfUtxoAvailable(result.getValue(), sender1Addr);
    }

    @Test
    void voteDelegation() {
//        stakeAddressRegistration(sender2Addr);
        QuickTxBuilder quickTxBuilder = new QuickTxBuilder(backendService);

        DRep drep = DRepId.toDrep(sender1.drepId(), DRepType.ADDR_KEYHASH);
        System.out.println("Drep : " + sender1.drepId());

        Tx tx = new Tx()
                .delegateVotingPowerTo(sender2Addr, drep)
                .from(sender1Addr);

        Result<String> result = quickTxBuilder.compose(tx)
                .withSigner(SignerProviders.stakeKeySignerFrom(sender2))
                .withSigner(SignerProviders.signerFrom(sender1))
                .withTxInspector(transaction -> {
                    System.out.println(transaction);
                })
                .completeAndWait(s -> System.out.println(s));

        System.out.println(result);
        assertTrue(result.isSuccessful());
        checkIfUtxoAvailable(result.getValue(), sender1Addr);
    }


    //stake address registration
    void stakeAddressRegistration(String addressToRegister) {
        QuickTxBuilder quickTxBuilder = new QuickTxBuilder(backendService);
        Tx tx = new Tx()
                .registerStakeAddress(addressToRegister)
                .from(sender1Addr);

        System.out.println("Registering stake address for address: " + addressToRegister);
        Result<String> result = quickTxBuilder.compose(tx)
                .withSigner(SignerProviders.signerFrom(sender1))
                .completeAndWait(msg -> System.out.println(msg));

        assertTrue(result.isSuccessful());
        checkIfUtxoAvailable(result.getValue(), sender1Addr);
    }

    void registerDrep(Account drep, Anchor anchor) {
        QuickTxBuilder quickTxBuilder = new QuickTxBuilder(backendService);

        Tx drepRegTx = new Tx().registerDRep(drep, anchor).from(drep.baseAddress());

        Result<String> result =
                quickTxBuilder
                        .compose(drepRegTx)
                        .withSigner(SignerProviders.signerFrom(drep))
                        .withSigner(SignerProviders.signerFrom(drep.drepHdKeyPair()))
                        .complete();

        System.out.println("DRepId : " + drep.drepId());

        System.out.println(result);
        assertTrue(result.isSuccessful());
        waitForTransaction(result);

        checkIfUtxoAvailable(result.getValue(), drep.baseAddress());
    }

}
