package com.bloxbean.cardano.yaci.store.test.e2e.common;

import com.bloxbean.cardano.client.account.Account;
import com.bloxbean.cardano.client.api.UtxoSupplier;
import com.bloxbean.cardano.client.api.model.Result;
import com.bloxbean.cardano.client.api.model.Utxo;
import com.bloxbean.cardano.client.backend.api.BackendService;
import com.bloxbean.cardano.client.backend.api.DefaultUtxoSupplier;
import com.bloxbean.cardano.client.backend.blockfrost.service.BFBackendService;
import com.bloxbean.cardano.client.backend.model.TransactionContent;
import com.bloxbean.cardano.client.common.model.Networks;
import com.bloxbean.cardano.client.crypto.cip1852.DerivationPath;
import com.bloxbean.cardano.client.util.JsonUtil;
import com.bloxbean.cardano.yaci.core.model.governance.GovActionId;
import com.bloxbean.cardano.yaci.store.adapot.job.domain.AdaPotJobStatus;
import com.bloxbean.cardano.yaci.store.adapot.job.storage.impl.AdaPotJobRepository;
import com.bloxbean.cardano.yaci.store.governanceaggr.storage.impl.model.GovActionProposalStatusEntity;
import com.bloxbean.cardano.yaci.store.governanceaggr.storage.impl.repository.GovActionProposalStatusRepository;
import org.awaitility.core.ConditionTimeoutException;

import java.time.Duration;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static org.awaitility.Awaitility.await;

public class BaseE2ETest {
    public static final String DEVKIT_ADMIN_BASE_URL = DevKitAdminClient.DEFAULT_BASE_URL;
    public static final String DEFAULT_MNEMONICS = "test test test test test test test test test test test test test test test test test test test test test test test sauce";

    public static final BackendService backendService = new BFBackendService("http://localhost:8080/api/v1/", "Dummy");
    protected static final DevKitAdminClient devKitAdminClient = new DevKitAdminClient(DEVKIT_ADMIN_BASE_URL);

    protected Account account0 = new Account(Networks.testnet(), DEFAULT_MNEMONICS);
    protected Account account1 = new Account(Networks.testnet(), DEFAULT_MNEMONICS, DerivationPath.createExternalAddressDerivationPathForAccount(1));

    public UtxoSupplier getUTXOSupplier() {
        return new DefaultUtxoSupplier(backendService.getUtxoService());
    }

    protected static void topUpFund(String address, long adaAmount) {
        devKitAdminClient.topUpFund(address, adaAmount);
    }

    protected static void resetDevNet() {
        devKitAdminClient.resetDevNet();
    }

    protected static void createDevNet(Map<String, String> config) {
        devKitAdminClient.createDevNet(config);
    }

    protected int getCurrentEpoch() {
        return devKitAdminClient.getCurrentEpoch();
    }

    protected static void assertDevKitAdminAvailable() {
        devKitAdminClient.assertAdminAvailable();
    }

    protected static void waitForEpoch(int epoch) {
        devKitAdminClient.waitForEpoch(epoch);
    }

    protected void waitForTransaction(Result<String> result) {
        try {
            if (result.isSuccessful()) { //Wait for transaction to be mined
                int count = 0;
                while (count < 60) {
                    Result<TransactionContent> txnResult = backendService.getTransactionService().getTransaction(result.getValue());
                    if (txnResult.isSuccessful()) {
                        System.out.println(JsonUtil.getPrettyJson(txnResult.getValue()));
                        break;
                    } else {
                        System.out.println("Waiting for transaction to be mined ....");
                    }

                    count++;
                    Thread.sleep(2000);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
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
            System.out.println("Try to get new output... txhash: " + txHash);
            try {
                Thread.sleep(1000);
            } catch (Exception e) {}
        }
    }

    protected void waitTillAdaPotJobDone(AdaPotJobRepository adaPotJobRepository, long epoch) {
        waitTillAdaPotJobDone(adaPotJobRepository, epoch, () -> "");
    }

    protected void waitTillAdaPotJobDone(AdaPotJobRepository adaPotJobRepository, long epoch, Supplier<String> extraDiagnostics) {
        try {
            await().atMost(Duration.ofSeconds(100))
                    .until(() -> adaPotJobRepository.findById(epoch)
                            .filter(adaPotJobEntity -> adaPotJobEntity.getStatus() == AdaPotJobStatus.COMPLETED)
                            .isPresent());
        } catch (ConditionTimeoutException e) {
            throw new AssertionError(buildAdaPotJobTimeoutMessage(adaPotJobRepository, epoch, extraDiagnostics), e);
        }
    }

    protected Supplier<String> proposalStatusDiagnostics(GovActionProposalStatusRepository proposalStatusRepository,
                                                         GovActionId govActionId) {
        return () -> proposalStatusRepository.findAll().stream()
                .filter(status -> status.getGovActionTxHash().equals(govActionId.getTransactionId())
                        && status.getGovActionIndex() == govActionId.getGov_action_index())
                .sorted(Comparator.comparing(GovActionProposalStatusEntity::getEpoch))
                .map(this::formatProposalStatus)
                .collect(Collectors.joining("; ", "Proposal status rows: ", ""));
    }

    protected static Map<String, String> permissiveGovernanceConfig(int epochLength, int govActionLifetime) {
        Map<String, String> config = baseGovernanceConfig(epochLength, govActionLifetime);

        putPoolThresholds(config, "0");
        putDRepThresholds(config, "0");
        config.put("dvtTreasuryWithdrawal", "0");
        config.put("ccThresholdNumerator", "0");
        config.put("committeeMinSize", "0");
        config.put("constitutionScript", "");

        return config;
    }

    protected static Map<String, String> votedGovernanceConfig(int epochLength, int govActionLifetime) {
        Map<String, String> config = baseGovernanceConfig(epochLength, govActionLifetime);

        putPoolThresholds(config, "0.51");
        putDRepThresholds(config, "0.51");
        config.put("dvtTreasuryWithdrawal", "0.51");
        config.put("ccThresholdNumerator", "1");
        config.put("committeeMinSize", "1");
        config.put("constitutionScript", "");

        return config;
    }

    protected static Map<String, String> bootstrapGovernanceConfig(int epochLength, int govActionLifetime) {
        Map<String, String> config = permissiveGovernanceConfig(epochLength, govActionLifetime);
        config.put("protocolMajorVer", "9");
        return config;
    }

    private static Map<String, String> baseGovernanceConfig(int epochLength, int govActionLifetime) {
        Map<String, String> config = new LinkedHashMap<>();
        config.put("conwayHardForkAtEpoch", "0");
        config.put("shiftStartTimeBehind", "false");
        config.put("epochLength", String.valueOf(epochLength));
        config.put("govActionLifetime", String.valueOf(govActionLifetime));
        return config;
    }

    private static void putPoolThresholds(Map<String, String> config, String value) {
        config.put("pvtcommitteeNormal", value);
        config.put("pvtCommitteeNoConfidence", value);
        config.put("pvtHardForkInitiation", value);
        config.put("pvtMotionNoConfidence", value);
        config.put("pvtPPSecurityGroup", value);
    }

    private static void putDRepThresholds(Map<String, String> config, String value) {
        config.put("dvtMotionNoConfidence", value);
        config.put("dvtCommitteeNormal", value);
        config.put("dvtCommitteeNoConfidence", value);
        config.put("dvtUpdateToConstitution", value);
        config.put("dvtHardForkInitiation", value);
        config.put("dvtPPNetworkGroup", value);
        config.put("dvtPPEconomicGroup", value);
        config.put("dvtPPTechnicalGroup", value);
        config.put("dvtPPGovGroup", value);
    }

    private String buildAdaPotJobTimeoutMessage(AdaPotJobRepository adaPotJobRepository, long epoch, Supplier<String> extraDiagnostics) {
        StringBuilder message = new StringBuilder();
        message.append("Timed out waiting for AdaPot job epoch ")
                .append(epoch)
                .append(" to reach COMPLETED.");
        message.append("\nDevKit current epoch: ").append(safeCurrentEpoch());
        message.append("\nTarget AdaPot job: ")
                .append(adaPotJobRepository.findById(epoch).map(this::formatAdaPotJob).orElse("not found"));
        message.append("\nRecent AdaPot jobs: ").append(recentAdaPotJobs(adaPotJobRepository));

        String extra = extraDiagnostics == null ? "" : extraDiagnostics.get();
        if (extra != null && !extra.isBlank()) {
            message.append("\n").append(extra);
        }

        return message.toString();
    }

    private String recentAdaPotJobs(AdaPotJobRepository adaPotJobRepository) {
        return adaPotJobRepository.findAll().stream()
                .sorted(Comparator.comparingLong(job -> -job.getEpoch()))
                .limit(10)
                .map(this::formatAdaPotJob)
                .collect(Collectors.joining("; "));
    }

    private String formatAdaPotJob(com.bloxbean.cardano.yaci.store.adapot.job.storage.impl.AdaPotJobEntity job) {
        return "epoch=" + job.getEpoch()
                + ", type=" + job.getType()
                + ", status=" + job.getStatus()
                + ", slot=" + job.getSlot()
                + ", error=" + job.getErrorMessage();
    }

    private String formatProposalStatus(GovActionProposalStatusEntity status) {
        return "epoch=" + status.getEpoch()
                + ", txHash=" + status.getGovActionTxHash()
                + ", index=" + status.getGovActionIndex()
                + ", type=" + status.getType()
                + ", status=" + status.getStatus()
                + ", votingStats=" + status.getVotingStats();
    }

    private String safeCurrentEpoch() {
        try {
            return String.valueOf(getCurrentEpoch());
        } catch (RuntimeException e) {
            return "unavailable (" + e.getMessage() + ")";
        }
    }
}
