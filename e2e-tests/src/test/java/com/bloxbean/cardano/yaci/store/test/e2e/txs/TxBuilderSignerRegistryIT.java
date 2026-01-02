package com.bloxbean.cardano.yaci.store.test.e2e.txs;

import com.bloxbean.cardano.client.address.AddressProvider;
import com.bloxbean.cardano.client.common.model.Networks;
import com.bloxbean.cardano.client.plutus.blueprint.PlutusBlueprintUtil;
import com.bloxbean.cardano.client.plutus.blueprint.model.PlutusVersion;
import com.bloxbean.cardano.client.plutus.spec.PlutusScript;
import com.bloxbean.cardano.client.util.HexUtil;
import com.bloxbean.cardano.yaci.store.test.e2e.common.BaseE2ETest;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class TxBuilderSignerRegistryIT extends BaseE2ETest {

    private static final String POOL_ID = "pool1pu5jlj4q9w9jlxeu370a3c9myx47md5j5m2str0naunn2q3lkdy";

    // PlutusV3 Hello World script (same as SubmitEvaluateTxTest) - compiled code format
    private static final String HELLO_WORLD_COMPILED_CODE = "590169010100323232323232323225333002323232323253330073370e900118049baa0011323232533300a3370e900018061baa005132533300f00116132533333301300116161616132533301130130031533300d3370e900018079baa004132533300e3371e6eb8c04cc044dd5004a4410d48656c6c6f2c20576f726c642100100114a06644646600200200644a66602a00229404c94ccc048cdc79bae301700200414a2266006006002602e0026eb0c048c04cc04cc04cc04cc04cc04cc04cc04cc040dd50051bae301230103754602460206ea801054cc03924012465787065637420536f6d6528446174756d207b206f776e6572207d29203d20646174756d001616375c0026020002601a6ea801458c038c03c008c034004c028dd50008b1805980600118050009805001180400098029baa001149854cc00d2411856616c696461746f722072657475726e65642066616c736500136565734ae7155ceaab9e5573eae855d12ba401";
    private static PlutusScript plutusScript;
    private static String scriptAddress;
    private static String scriptCborHex;

    @BeforeAll
    static void setupAll() {
        initializeDevnetOnce();

        // Use the same hello world script as SubmitEvaluateTxTest
        plutusScript = PlutusBlueprintUtil.getPlutusScriptFromCompiledCode(
                HELLO_WORLD_COMPILED_CODE,
                PlutusVersion.v3
        );
        scriptAddress = AddressProvider.getEntAddress(plutusScript, Networks.testnet()).toBech32();
        scriptCborHex = plutusScript.getCborHex();
        System.out.println("PlutusV3 Script Address: " + scriptAddress);
        System.out.println("PlutusV3 Script CborHex: " + scriptCborHex);
    }

    @Test
    @Order(1)
    @DisplayName("Basic payment with remote signer (Build & Submit)")
    void buildTxPlan_withRemoteSignerRef_shouldSucceed() {
        topUpFund(account0.baseAddress(), 200);
        waitForFunds(account0.baseAddress());

        String txYaml = """
            version: 1.0
            context:
              signers:
                - ref: remote://ops
                  scope: payment
            transaction:
              - tx:
                  from: %s
                  intents:
                    - type: payment
                      address: %s
                      amounts:
                        - unit: lovelace
                          quantity: 1000000
            """.formatted(account0.baseAddress(), account1.baseAddress());

        TxPlanResponse response = buildAndSubmitTxFromYaml(txYaml); // Build AND submit
        assertTxPlanSuccess(response);
        assertThat(response.txHash).isNotNull();
        System.out.println("Basic Payment with Remote Signer: Build and submit successful");
        System.out.println("Tx hash: " + response.txHash);
    }

    @Test
    @Order(2)
    @DisplayName("Multi-output payment with remote signer")
    void buildTxPlan_multiOutputPayment_shouldSucceed() {
        topUpFund(account0.baseAddress(), 200);
        waitForFunds(account0.baseAddress());

        String txYaml = """
            version: 1.0
            context:
              signers:
                - ref: remote://ops
                  scope: payment
            transaction:
              - tx:
                  from: %s
                  intents:
                    - type: payment
                      address: %s
                      amounts:
                        - unit: lovelace
                          quantity: 1000000
                    - type: payment
                      address: %s
                      amounts:
                        - unit: lovelace
                          quantity: 2000000
            """.formatted(account0.baseAddress(), account1.baseAddress(), account1.baseAddress());

        TxPlanResponse response = buildAndSubmitTxFromYaml(txYaml);
        assertTxPlanSuccess(response);
        assertThat(response.txHash).isNotNull();
        System.out.println("Multi-output Payment with Remote Signer: Build and submit successful");
        System.out.println("Tx hash: " + response.txHash);
    }

    @Test
    @Order(7)
    @Disabled("Test pool ID does not exist in devnet. Requires pool registration setup.")
    @DisplayName("Stake delegation with stake scope")
    void buildTxPlan_stakeDelegation_shouldSucceed() {
        topUpFund(account1.baseAddress(), 300);
        waitForFunds(account1.baseAddress());

        String stakeAddress = account1.stakeAddress();

        String txYaml = """
            version: 1.0
            context:
              signers:
                - ref: remote://stake-ops
                  scope: payment,stake
            transaction:
              - tx:
                  from: %s
                  intents:
                    - type: stake_delegation
                      stake_address: %s
                      pool_id: %s
            """.formatted(account1.baseAddress(), stakeAddress, POOL_ID);

        TxPlanResponse response = buildAndSubmitTxFromYaml(txYaml);
        assertTxPlanSuccess(response);
        assertThat(response.txHash).isNotNull();
        System.out.println("✓ TC3 - Delegation with Stake Scope: Build and submit successful");
        System.out.println("  Tx hash: " + response.txHash);
    }

    @Test
    @Order(8)
    @Disabled("Test pool ID does not exist in devnet. Requires pool registration setup.")
    @DisplayName("Payment + Stake multi-scope transaction")
    void buildTxPlan_multiScope_shouldSucceed() {
        // Use account1 (stake-ops account) for both payment and stake
        topUpFund(account1.baseAddress(), 300);
        waitForFunds(account1.baseAddress());

        String txYaml = """
            version: 1.0
            context:
              signers:
                - ref: remote://stake-ops
                  scope: payment
                - ref: remote://stake-ops
                  scope: stake
            transaction:
              - tx:
                  from: %s
                  intents:
                    - type: payment
                      address: %s
                      amounts:
                        - unit: lovelace
                          quantity: 5000000
                    - type: stake_delegation
                      stake_address: %s
                      pool_id: %s
            """.formatted(account1.baseAddress(), account2.baseAddress(),
                         account1.stakeAddress(), POOL_ID);

        TxPlanResponse response = buildAndSubmitTxFromYaml(txYaml);
        assertTxPlanSuccess(response);
        assertThat(response.txHash).isNotNull();
        System.out.println("✓ TC4 - Multi-scope (payment+stake): Build and submit successful");
        System.out.println("  Tx hash: " + response.txHash);
    }

    @Test
    @Order(5)
    @DisplayName("Policy scope with remote signer")
    void buildTxPlan_policyScope_shouldSucceed() {
        topUpFund(account0.baseAddress(), 200);
        waitForFunds(account0.baseAddress());

        String txYaml = """
            version: 1.0
            context:
              signers:
                - ref: remote://ops
                  scope: payment
                - ref: remote://policy-ops
                  scope: policy
            transaction:
              - tx:
                  from: %s
                  intents:
                    - type: payment
                      address: %s
                      amounts:
                        - unit: lovelace
                          quantity: 2000000
            """.formatted(account0.baseAddress(), account1.baseAddress());

        TxPlanResponse response = buildAndSubmitTxFromYaml(txYaml);
        assertTxPlanSuccess(response);
        assertThat(response.txHash).isNotNull();
        System.out.println("✓ TC5 - Policy Scope with Remote Signer: Build and submit successful");
        System.out.println("  Tx hash: " + response.txHash);
    }

    @Test
    @Order(4)
    @DisplayName("Lock funds to PlutusV3 script address with datum (YAML)")
    void buildTxPlan_scriptTx_lock_shouldSucceed() {
        // First, fund account0 so we can send to script address
        topUpFund(account0.baseAddress(), 200);
        waitForFunds(account0.baseAddress());

        // Get the payment credential hash for the datum (same as SubmitEvaluateTxTest)
        // The hello world contract expects datum = Constr(0, [owner_payment_cred_hash])
        String paymentCredHash = HexUtil.encodeHexString(
                account0.getBaseAddress().getPaymentCredentialHash().get());

        // Lock funds to script address with datum using YAML TxPlan
        // PaymentIntent supports datum field for contract payments
        String txYaml = """
            version: 1.0
            context:
              signers:
                - ref: remote://ops
                  scope: payment
            transaction:
              - tx:
                  from: %s
                  intents:
                    - type: payment
                      address: %s
                      amounts:
                        - unit: lovelace
                          quantity: 20000000
                      datum:
                        constructor: 0
                        fields:
                          - bytes: "%s"
            """.formatted(account0.baseAddress(), scriptAddress, paymentCredHash);

        TxPlanResponse response = buildAndSubmitTxFromYaml(txYaml);
        assertTxPlanSuccess(response);
        assertThat(response.txHash).isNotNull();
        System.out.println("  Lock to PlutusV3 Script (YAML): Funds locked successfully");
        System.out.println("  Script address: " + scriptAddress);
        System.out.println("  Datum (owner payment cred hash): " + paymentCredHash);
        System.out.println("  Tx hash: " + response.txHash);
    }

    @Test
    @Order(6)
//    @Disabled("Script integrity hash mismatch: YaciStoreProtocolParamsSupplier epoch params don't include cost models. " +
//              "The /build-and-submit endpoint falls back to hardcoded cost models which differ from devnet. " +
//              "Fix requires: store.epoch to properly store and provide cost models from protocol params.")
    @DisplayName("TC6b: ScriptTx unlock with remote signer (PlutusV3)")
    void buildTxPlan_scriptTx_unlock_shouldSucceed() {
        // TC6a must run first to lock funds to script address
        // Wait for the locked funds to be synced to yaci-store
        waitForFunds(scriptAddress);

        // Now unlock using YAML TxPlan with scriptTx
        // ScriptTx requires fee_payer and collateral_payer for script execution
        // The hello world contract expects redeemer = Constr(0, ["Hello, World!"])
        // "Hello, World!" in hex = 48656c6c6f2c20576f726c6421
        String helloWorldHex = HexUtil.encodeHexString("Hello, World!".getBytes());

        // Get owner's payment credential hash for required_signers
        // The Hello World contract requires the owner's signature for validation
        String ownerPaymentCredHash = HexUtil.encodeHexString(
                account0.getBaseAddress().getPaymentCredentialHash().get());

        String txYaml = """
            version: 1.0
            context:
              fee_payer: %s
              collateral_payer: %s
              required_signers:
                - "%s"
              signers:
                - ref: remote://ops
                  scope: payment
            transaction:
              - scriptTx:
                  inputs:
                    - type: script_collect_from
                      address: %s
                      utxo_filter:
                        lovelace:
                          gte: 10000000
                        selection:
                          limit: 1
                      redeemer:
                        constructor: 0
                        fields:
                          - bytes: "%s"
                  intents:
                    - type: payment
                      address: %s
                      amounts:
                        - unit: lovelace
                          quantity: 10000000
                  scripts:
                    - type: validator
                      role: spend
                      version: v3
                      cbor_hex: "%s"
            """.formatted(
                account0.baseAddress(),    // fee_payer
                account0.baseAddress(),    // collateral_payer
                ownerPaymentCredHash,      // required_signers
                scriptAddress,             // script address
                helloWorldHex,             // redeemer bytes
                account1.baseAddress(),    // receiver
                scriptCborHex              // script cbor
            );

        TxPlanResponse response = buildAndSubmitTxFromYaml(txYaml);
        assertTxPlanSuccess(response);
        assertThat(response.txHash).isNotNull();
        System.out.println("  ScriptTx Unlock with Remote Signer: Build and submit successful");
        System.out.println("  Tx hash: " + response.txHash);
    }

    @Test
    @Order(3)
    @DisplayName("Stake registration with remote signer")
    void buildTxPlan_stakeRegistration_shouldSucceed() {
        // Use account1 (stake-ops account) for both payment and stake
        topUpFund(account1.baseAddress(), 300);
        waitForFunds(account1.baseAddress());

        String txYaml = """
            version: 1.0
            context:
              signers:
                - ref: remote://stake-ops
                  scope: payment,stake
            transaction:
              - tx:
                  from: %s
                  intents:
                    - type: stake_registration
                      stake_address: %s
            """.formatted(account1.baseAddress(), account1.stakeAddress());

        TxPlanResponse response = buildAndSubmitTxFromYaml(txYaml);
        assertTxPlanSuccess(response);
        assertThat(response.txHash).isNotNull();
        System.out.println("✓ TC7 - Stake Registration: Build and submit successful");
        System.out.println("  Tx hash: " + response.txHash);
    }

    @Test
    @Order(9)
    @DisplayName("Multiple remote signers in single transaction")
    void buildTxPlan_multipleRemoteSigners_shouldSucceed() {
        topUpFund(account0.baseAddress(), 300);
        waitForFunds(account0.baseAddress());

        String txYaml = """
            version: 1.0
            context:
              signers:
                - ref: remote://ops
                  scope: payment
                - ref: remote://stake-ops
                  scope: stake
            transaction:
              - tx:
                  from: %s
                  intents:
                    - type: payment
                      address: %s
                      amounts:
                        - unit: lovelace
                          quantity: 3000000
            """.formatted(account0.baseAddress(), account1.baseAddress());

        TxPlanResponse response = buildAndSubmitTxFromYaml(txYaml);
        assertTxPlanSuccess(response);
        assertThat(response.txHash).isNotNull();
        System.out.println(" Multiple Remote Signers: Build and submit successful");
        System.out.println("  Tx hash: " + response.txHash);
    }

    @Test
    @Order(10)
    @DisplayName("Mixed local and remote signers")
    void buildTxPlan_mixedSigners_shouldSucceed() {
        // Use account2 (local-alice's account) as the sender
        topUpFund(account2.baseAddress(), 300);
        waitForFunds(account2.baseAddress());

        String txYaml = """
            version: 1.0
            context:
              signers:
                - ref: account://local-alice
                  scope: payment
                - ref: remote://stake-ops
                  scope: stake
            transaction:
              - tx:
                  from: %s
                  intents:
                    - type: payment
                      address: %s
                      amounts:
                        - unit: lovelace
                          quantity: 2000000
            """.formatted(account2.baseAddress(), account1.baseAddress());

        TxPlanResponse response = buildAndSubmitTxFromYaml(txYaml);
        assertTxPlanSuccess(response);
        assertThat(response.txHash).isNotNull();
        System.out.println("  Mixed Local and Remote Signers: Build and submit successful");
        System.out.println("  Tx hash: " + response.txHash);
    }

    @Test
    @Order(11)
    @DisplayName("Variable resolution with remote signer")
    void buildTxPlan_variablesWithRemoteSigner_shouldSucceed() {
        topUpFund(account0.baseAddress(), 200);
        waitForFunds(account0.baseAddress());

        String txYaml = """
            version: 1.0
            variables:
              sender: %s
              receiver: %s
              amount: 5000000
              remote_ref: remote://ops
            context:
              signers:
                - ref: ${remote_ref}
                  scope: payment
            transaction:
              - tx:
                  from: ${sender}
                  intents:
                    - type: payment
                      address: ${receiver}
                      amounts:
                        - unit: lovelace
                          quantity: ${amount}
            """.formatted(account0.baseAddress(), account1.baseAddress());

        TxPlanResponse response = buildAndSubmitTxFromYaml(txYaml);
        assertTxPlanSuccess(response);
        assertThat(response.txHash).isNotNull();
        System.out.println("  Variables with Remote Signer: Build and submit successful");
        System.out.println("  Tx hash: " + response.txHash);
    }

    @Test
    @Order(12)
    @DisplayName("Context properties with remote signer")
    void buildTxPlan_contextProperties_shouldSucceed() {
        topUpFund(account0.baseAddress(), 200);
        waitForFunds(account0.baseAddress());

        String txYaml = """
            version: 1.0
            context:
              signers:
                - ref: remote://ops
                  scope: payment
            transaction:
              - tx:
                  from: %s
                  intents:
                    - type: payment
                      address: %s
                      amounts:
                        - unit: lovelace
                          quantity: 3000000
            """.formatted(account0.baseAddress(), account1.baseAddress());

        TxPlanResponse response = buildAndSubmitTxFromYaml(txYaml);
        assertTxPlanSuccess(response);
        assertThat(response.txHash).isNotNull();
        System.out.println("  Context Properties with Remote Signer: Build and submit successful");
        System.out.println("  Tx hash: " + response.txHash);
    }

    @Test
    @Order(13)
    @DisplayName("Multi-scope signer used for single scope")
    void buildTxPlan_multiScopeConfig_shouldSucceed() {
        // Use account1 (stake-ops account) as sender since that's the signer we're using
        topUpFund(account1.baseAddress(), 200);
        waitForFunds(account1.baseAddress());

        // Uses remote://stake-ops (configured with payment,stake) for payment only
        String txYaml = """
            version: 1.0
            context:
              signers:
                - ref: remote://stake-ops
                  scope: payment
            transaction:
              - tx:
                  from: %s
                  intents:
                    - type: payment
                      address: %s
                      amounts:
                        - unit: lovelace
                          quantity: 2000000
            """.formatted(account1.baseAddress(), account2.baseAddress());

        TxPlanResponse response = buildAndSubmitTxFromYaml(txYaml);
        assertTxPlanSuccess(response);
        assertThat(response.txHash).isNotNull();
        System.out.println("  Multi-scope signer used for single scope: Build and submit successful");
        System.out.println("  Tx hash: " + response.txHash);
    }

    @Test
    @Order(14)
    @DisplayName("Different signer ref URI formats")
    void buildTxPlan_refUriFormats_shouldSucceed() {
        topUpFund(account0.baseAddress(), 200);
        waitForFunds(account0.baseAddress());

        // Test both remote:// and account:// formats
        String txYaml = """
            version: 1.0
            context:
              signers:
                - ref: remote://ops
                  scope: payment
                - ref: account://local-alice
                  scope: payment
            transaction:
              - tx:
                  from: %s
                  intents:
                    - type: payment
                      address: %s
                      amounts:
                        - unit: lovelace
                          quantity: 1500000
            """.formatted(account0.baseAddress(), account1.baseAddress());

        TxPlanResponse response = buildAndSubmitTxFromYaml(txYaml);
        assertTxPlanSuccess(response);
        assertThat(response.txHash).isNotNull();
        System.out.println("  Different ref URI formats: Build and submit successful");
        System.out.println("  Tx hash: " + response.txHash);
    }

    @Test
    @Order(15)
    @DisplayName("Metadata transaction with remote signer")
    void buildTxPlan_metadataTransaction_shouldSucceed() {
        topUpFund(account0.baseAddress(), 200);
        waitForFunds(account0.baseAddress());

        // Note: metadata in MetadataIntent expects a YAML string, not a nested object
        String txYaml = """
            version: 1.0
            context:
              signers:
                - ref: remote://ops
                  scope: payment
            transaction:
              - tx:
                  from: %s
                  intents:
                    - type: payment
                      address: %s
                      amounts:
                        - unit: lovelace
                          quantity: 3000000
                    - type: metadata
                      metadata: |
                        674:
                          msg:
                            - E2E Test Transaction
                            - Testing TxPlan API with remote signer
                            - Metadata stored on-chain
            """.formatted(account0.baseAddress(), account1.baseAddress());

        TxPlanResponse response = buildAndSubmitTxFromYaml(txYaml);
        assertTxPlanSuccess(response);
        assertThat(response.txHash).isNotNull();
        System.out.println("  Metadata Transaction: Build and submit successful");
        System.out.println("  Tx hash: " + response.txHash);
    }

    @Test
    @Order(16)
    @DisplayName("Stake deregistration with remote signer")
    void buildTxPlan_stakeDeregistration_shouldSucceed() {
        // Deregister account1's stake address (which was registered by TC7)
        // Note: TC7 must run first (Order 4 < Order 16)
        topUpFund(account1.baseAddress(), 200);
        waitForFunds(account1.baseAddress());

        String deregisterYaml = """
            version: 1.0
            context:
              signers:
                - ref: remote://stake-ops
                  scope: payment
                - ref: remote://stake-ops
                  scope: stake
            transaction:
              - tx:
                  from: %s
                  intents:
                    - type: stake_deregistration
                      stake_address: %s
            """.formatted(account1.baseAddress(), account1.stakeAddress());

        TxPlanResponse response = buildAndSubmitTxFromYaml(deregisterYaml);
        assertTxPlanSuccess(response);
        assertThat(response.txHash).isNotNull();
        System.out.println("  Stake Deregistration: Build and submit successful");
        System.out.println("  Tx hash: " + response.txHash);
    }
}
