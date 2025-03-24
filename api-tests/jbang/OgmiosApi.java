//usr/bin/env jbang "$0" "$@" ; exit $?

//DEPS org.junit.jupiter:junit-jupiter-engine:5.10.1
//DEPS org.junit.platform:junit-platform-console:1.8.2
//DEPS org.hamcrest:hamcrest-library:2.1
//DEPS com.bloxbean.cardano:cardano-client-lib:0.7.0-beta2
//DEPS com.bloxbean.cardano:cardano-client-backend-blockfrost:0.7.0-beta2

import java.io.PrintWriter;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import org.junit.platform.console.options.CommandLineOptions;
import org.junit.platform.console.tasks.ConsoleTestExecutor;

import java.nio.file.Paths;
import java.util.Collections;
import com.bloxbean.cardano.client.address.AddressProvider;
import com.bloxbean.cardano.client.api.exception.ApiException;
import com.bloxbean.cardano.client.api.model.Amount;
import com.bloxbean.cardano.client.api.model.Result;
import com.bloxbean.cardano.client.api.model.Utxo;
import com.bloxbean.cardano.client.backend.api.DefaultUtxoSupplier;
import com.bloxbean.cardano.client.function.helper.ScriptUtxoFinders;
import com.bloxbean.cardano.client.plutus.blueprint.PlutusBlueprintUtil;
import com.bloxbean.cardano.client.plutus.blueprint.model.PlutusVersion;
import com.bloxbean.cardano.client.plutus.spec.*;
import com.bloxbean.cardano.client.quicktx.QuickTxBuilder;
import com.bloxbean.cardano.client.quicktx.ScriptTx;

import com.bloxbean.cardano.client.account.Account;
import com.bloxbean.cardano.client.backend.api.BackendService;
import com.bloxbean.cardano.client.backend.blockfrost.service.BFBackendService;
import com.bloxbean.cardano.client.common.model.Networks;
import com.bloxbean.cardano.client.crypto.cip1852.DerivationPath;
import com.bloxbean.cardano.client.function.helper.SignerProviders;
import com.bloxbean.cardano.client.plutus.spec.BigIntPlutusData;
import com.bloxbean.cardano.client.plutus.spec.PlutusData;
import com.bloxbean.cardano.client.quicktx.Tx;

import java.math.BigInteger;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import static org.junit.jupiter.api.Assertions.assertTrue;

//This test only runs with Yaci DevKit
//It verifies if the Ogmios Tx Evaluation is working fine and also checks submit endpoint by submitting a script tx
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class OgmiosApi {

    public static void main(String... args) throws Exception {
        CommandLineOptions options = new CommandLineOptions();
        options.setSelectedClasses(Collections.singletonList(OgmiosApi.class.getName()));
        options.setReportsDir(Paths.get(System.getProperty("user.dir")));
        new ConsoleTestExecutor(options).execute(new PrintWriter(System.out));
    }

    String compiledCode = "590169010100323232323232323225333002323232323253330073370e900118049baa0011323232533300a3370e900018061baa005132533300f00116132533333301300116161616132533301130130031533300d3370e900018079baa004132533300e3371e6eb8c04cc044dd5004a4410d48656c6c6f2c20576f726c642100100114a06644646600200200644a66602a00229404c94ccc048cdc79bae301700200414a2266006006002602e0026eb0c048c04cc04cc04cc04cc04cc04cc04cc04cc040dd50051bae301230103754602460206ea801054cc03924012465787065637420536f6d6528446174756d207b206f776e6572207d29203d20646174756d001616375c0026020002601a6ea801458c038c03c008c034004c028dd50008b1805980600118050009805001180400098029baa001149854cc00d2411856616c696461746f722072657475726e65642066616c736500136565734ae7155ceaab9e5573eae855d12ba401";

    PlutusScript plutusScript = PlutusBlueprintUtil.getPlutusScriptFromCompiledCode(compiledCode, PlutusVersion.v3);

    String scriptAddress = AddressProvider.getEntAddress(plutusScript, Networks.testnet()).toBech32();

    public static String MNEMONIC = "test test test test test test test test test test test test test test test test test test test test test test test sauce";

    public final static Account sender1 = new Account(Networks.testnet(), MNEMONIC);
    public final static String sender1Addr = sender1.baseAddress();

    public final static Account sender2 = new Account(Networks.testnet(), MNEMONIC, DerivationPath.createExternalAddressDerivationPathForAccount(2));
    public final static String sender2Addr = sender2.baseAddress();

    public String receiver = "addr_test1qq8phk43ndg0zf2l4xc5vd7gu4f85swkm3dy7fjmfkf6q249ygmm3ascevccsq5l5ym6khc3je5plx9t5vsa06jvlzls8el07z";
    public String receiver2 = "addr_test1qq7a8p6zaxzgcmcjcy7ak8u5vn7qec9mjggzw6qg096nzlj6n7rflnv3x43vnv8q7q0h0ef4n6ncp5mljd2ljupwl79s5mqneq";

    public static BackendService backendService = new BFBackendService("http://localhost:8080/api/v1/", "Dummy Key");

    protected void checkIfUtxoAvailable(String txHash, String address) {
        Optional<Utxo> utxo = Optional.empty();
        int count = 0;
        while (utxo.isEmpty()) {
            if (count++ >= 20)
                break;
            List<Utxo> utxos = new DefaultUtxoSupplier(backendService.getUtxoService()).getAll(address);
            utxo = utxos.stream().filter(u -> u.getTxHash().equals(txHash))
                    .findFirst();
            System.out.println("Try to get new output... txhash: " + txHash);
            try {
                Thread.sleep(1000);
            } catch (Exception e) {}
        }
    }

    protected void lockFund(String scriptAddress, BigInteger scriptAmt, PlutusData plutusData) {
        Tx tx = new Tx();
        tx.payToContract(scriptAddress, Amount.lovelace(scriptAmt), plutusData)
                .from(sender2Addr);

        QuickTxBuilder quickTxBuilder = new QuickTxBuilder(backendService);
        Result<String> result = quickTxBuilder.compose(tx)
                .withSigner(SignerProviders.signerFrom(sender2))
                .completeAndWait(System.out::println);

        System.out.println(result.getResponse());
        checkIfUtxoAvailable(result.getValue(), scriptAddress);
    }

    @Test
    @Order(1)
    void helloworldContract_lock() {
        PlutusData datum = ConstrPlutusData.of(
                0, BytesPlutusData.of(sender1.getBaseAddress().getPaymentCredentialHash().get()));

        Tx tx = new Tx()
                .payToContract(scriptAddress, Amount.ada(10), datum)
                .from(sender1Addr);

        QuickTxBuilder quickTxBuilder = new QuickTxBuilder(backendService);
        Result<String> result = quickTxBuilder.compose(tx)
                .withSigner(SignerProviders.signerFrom(sender1))
                .completeAndWait(System.out::println);

        System.out.println(result);
    }

    @Test
    @Order(2)
    void helloworldContract_unlock() throws ApiException {
        PlutusData datum = ConstrPlutusData.of(
                0, BytesPlutusData.of(sender1.getBaseAddress().getPaymentCredentialHash().get()));

        //Find Script Utxo
        Utxo scriptUtxo  = ScriptUtxoFinders.findFirstByInlineDatum(
                new DefaultUtxoSupplier(backendService.getUtxoService()),
                scriptAddress, datum).orElseThrow();

        PlutusData redeemer = ConstrPlutusData.of(0, BytesPlutusData.of("Hello, World!"));

        ScriptTx sctipTx = new ScriptTx()
                .collectFrom(scriptUtxo, redeemer)
                .payToAddress(receiver, Amount.ada(10))
                .attachSpendingValidator(plutusScript);

        QuickTxBuilder quickTxBuilder = new QuickTxBuilder(backendService);
        Result<String> result = quickTxBuilder.compose(sctipTx)
                .feePayer(receiver)
                .collateralPayer(sender1Addr)
                .withSigner(SignerProviders.signerFrom(sender1))
                .withRequiredSigners(sender1.getBaseAddress())
                .completeAndWait(System.out::println);


        assertTrue(result.isSuccessful());
    }

}
