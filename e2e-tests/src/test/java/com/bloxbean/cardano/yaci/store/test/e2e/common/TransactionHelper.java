package com.bloxbean.cardano.yaci.store.test.e2e.common;

import com.bloxbean.cardano.client.account.Account;
import com.bloxbean.cardano.client.api.model.Result;
import com.bloxbean.cardano.client.api.model.Utxo;
import com.bloxbean.cardano.client.backend.api.BackendService;
import com.bloxbean.cardano.client.backend.api.DefaultUtxoSupplier;
import com.bloxbean.cardano.client.function.helper.SignerProviders;
import com.bloxbean.cardano.client.quicktx.QuickTxBuilder;
import com.bloxbean.cardano.client.quicktx.Tx;
import com.bloxbean.cardano.client.transaction.spec.governance.Anchor;
import com.bloxbean.cardano.client.transaction.spec.governance.actions.GovAction;
import com.bloxbean.cardano.client.transaction.spec.governance.actions.InfoAction;
import com.bloxbean.cardano.client.util.HexUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class TransactionHelper {
    private final static Logger log = LoggerFactory.getLogger(TransactionHelper.class);

    private BackendService backendService;

    public TransactionHelper(BackendService backendService) {
        this.backendService = backendService;
    }

    public Result<String> createProposal(Account proposalCreator, String returnStakeAddress, GovAction govAction, Anchor anchor) {
        QuickTxBuilder quickTxBuilder = new QuickTxBuilder(backendService);

        Tx tx = new Tx()
                .createProposal(govAction, returnStakeAddress, anchor)
                .from(proposalCreator.baseAddress());

        log.info("return address: " + returnStakeAddress);

        Result<String> result = quickTxBuilder.compose(tx)
                .withSigner(SignerProviders.drepKeySignerFrom(proposalCreator))
                .withSigner(SignerProviders.signerFrom(proposalCreator))
                .completeAndWait(s -> System.out.println(s));

        log.info(result.toString());
        assertTrue(result.isSuccessful());
        checkIfUtxoAvailable(result.getValue(), proposalCreator.baseAddress());
        return result;
    }


    public Result<String> createInfoProposal(Account proposalCreator, String returnStakeAddress, Anchor anchor) {
        QuickTxBuilder quickTxBuilder = new QuickTxBuilder(backendService);

        var govAction = new InfoAction();
        if (anchor == null)
            anchor = new Anchor("https://xyz.com",
                    HexUtil.decodeHexString("cafef700c0039a2efb056a665b3a8bcd94f8670b88d659f7f3db68340f6f0937"));

        Tx tx = new Tx()
                .createProposal(govAction, returnStakeAddress, anchor)
                .from(proposalCreator.baseAddress());

        log.info("return address: " + returnStakeAddress);

        Result<String> result = quickTxBuilder.compose(tx)
                .withSigner(SignerProviders.drepKeySignerFrom(proposalCreator))
                .withSigner(SignerProviders.signerFrom(proposalCreator))
                .completeAndWait(s -> System.out.println(s));

        log.info(result.toString());
        assertTrue(result.isSuccessful());
        checkIfUtxoAvailable(result.getValue(), proposalCreator.baseAddress());
        return result;
    }

    public Result<String> registerStakeAddress(Account txSubmitter, String addressToRegister) {
        QuickTxBuilder quickTxBuilder = new QuickTxBuilder(backendService);
        Tx tx = new Tx()
                .registerStakeAddress(addressToRegister)
                .from(txSubmitter.baseAddress());

        log.info("Registering stake address for address: " + addressToRegister);
        Result<String> result = quickTxBuilder.compose(tx)
                .withSigner(SignerProviders.signerFrom(txSubmitter))
                .completeAndWait(msg -> System.out.println(msg));

        if (result.isSuccessful())
            checkIfUtxoAvailable(result.getValue(), txSubmitter.baseAddress());

        return result;
    }

    public Result<String> deregisterStakeAddresss(Account txSubmitter, Account stakeAddrAccount) {
        QuickTxBuilder quickTxBuilder = new QuickTxBuilder(backendService);
        Tx tx = new Tx()
                .deregisterStakeAddress(stakeAddrAccount.stakeAddress())
                .from(txSubmitter.baseAddress());

        Result<String> result = quickTxBuilder.compose(tx)
                .withSigner(SignerProviders.signerFrom(txSubmitter))
                .withSigner(SignerProviders.stakeKeySignerFrom(txSubmitter))
                .completeAndWait(msg -> System.out.println(msg));

        assertTrue(result.isSuccessful());
        checkIfUtxoAvailable(result.getValue(), txSubmitter.baseAddress());

        return result;
    }

    protected void checkIfUtxoAvailable(String txHash, String address) {
        Optional<Utxo> utxo = Optional.empty();
        int count = 0;
        while (utxo.isEmpty()) {
            if (count++ >= 20)
                break;
            List<Utxo> utxos = new DefaultUtxoSupplier(backendService.getUtxoService()).getAll(address);
            utxo = utxos.stream().filter(u -> u.getTxHash().equals(txHash))
                    .findFirst();
            log.info("Try to get new output... txhash: " + txHash);
            try {
                Thread.sleep(1000);
            } catch (Exception e) {
            }
        }
    }
}
