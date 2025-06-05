package com.bloxbean.cardano.yaci.store.test.e2e.gov;

import com.bloxbean.cardano.yaci.core.model.governance.GovActionId;
import com.bloxbean.cardano.yaci.core.model.governance.GovActionType;
import com.bloxbean.cardano.yaci.store.adapot.job.storage.impl.AdaPotJobRepository;
import com.bloxbean.cardano.yaci.store.adapot.storage.AdaPotStorage;
import com.bloxbean.cardano.yaci.store.adapot.storage.impl.repository.RewardRestRepository;
import com.bloxbean.cardano.yaci.store.client.governance.ProposalStateClient;
import com.bloxbean.cardano.yaci.store.common.domain.GovActionStatus;
import com.bloxbean.cardano.yaci.store.events.domain.RewardRestType;
import com.bloxbean.cardano.yaci.store.governance.storage.GovActionProposalStorage;
import com.bloxbean.cardano.yaci.store.governanceaggr.storage.impl.repository.GovActionProposalStatusRepository;
import com.bloxbean.cardano.yaci.store.test.e2e.common.BaseE2ETest;
import com.bloxbean.cardano.yaci.store.test.e2e.common.TransactionHelper;
import groovy.util.logging.Slf4j;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Slf4j
public class InfoProposalLifeCycleIT extends BaseE2ETest {
    private final static Logger log = LoggerFactory.getLogger(InfoProposalLifeCycleIT.class);

    private static TransactionHelper transactionHelper;
    private static int GOV_ACTION_LIFETIME = 3; //TODO: Fetch this from devkit genesis file

    @Autowired
    private ProposalStateClient proposalStateClient; //TODO: Replace with REST call to proposal status endpoint

    @Autowired
    private GovActionProposalStorage govActionProposalStorage;

    @Autowired
    private GovActionProposalStatusRepository proposalStatusRepository;

    @Autowired
    private AdaPotStorage adaPotStorage;

    @Autowired
    private AdaPotJobRepository adaPotJobRepository;

    @Autowired
    private RewardRestRepository rewardRestRepository;

    @BeforeAll
    static void setup() {
        transactionHelper = new TransactionHelper(backendService);
        System.out.println("Resetting the network before running tests ...");

        Map<String, String> devNetConfig = new HashMap<>();
        devNetConfig.put("conwayHardForkAtEpoch", "0");
        devNetConfig.put("shiftStartTimeBehind", "false");
        devNetConfig.put("epochLength", "20");
        devNetConfig.put("govActionLifetime", String.valueOf(GOV_ACTION_LIFETIME));

        devNetConfig.put("dvtTreasuryWithdrawal", "0");
        devNetConfig.put("ccThresholdNumerator", "0");
        devNetConfig.put("committeeMinSize", "0");
        devNetConfig.put("constitutionScript", ""); //To disable constitution scirpt

        createDevNet(devNetConfig);
    }


    /**
     * Create Info proposal and check for expiry
     */
    @Test
    void createInfoProposal_shouldExpireAndrefundDeposit() {
        //Register stake address
        transactionHelper.registerStakeAddress(account0, account0.stakeAddress());

        //Create a new info proposal
        var proposalTxResult = transactionHelper.createInfoProposal(account0, account0.stakeAddress(), null);
        assertThat(proposalTxResult.isSuccessful()).isTrue();

        //check db if the proposal has been created or not
        var govActionProposal = govActionProposalStorage.findByGovActionIds(List.of(new GovActionId(proposalTxResult.getValue(), 0))).stream()
                .findFirst().orElse(null);
        assertThat(govActionProposal).isNotNull();

        int createEpoch = govActionProposal.getEpoch();
        int expectdExpiryEpoch = createEpoch + GOV_ACTION_LIFETIME + 1;

        //Wait till adaPot job for expectedExpiryEpoch is done.
        waitTillAdaPotJobDone(adaPotJobRepository, expectdExpiryEpoch);

        //Find proposal entry at different epochs
        var govActionCreateEpoch = proposalStateClient.getProposalsByStatusAndEpoch(GovActionStatus.ACTIVE, createEpoch + 1);
        var govActionCreateEpochPlusOne = proposalStateClient.getProposalsByStatusAndEpoch(GovActionStatus.ACTIVE, createEpoch + 2);

        assertThat(govActionCreateEpoch).hasSize(1);
        assertThat(govActionCreateEpochPlusOne).hasSize(1);

        var list = proposalStatusRepository.findAll();
        log.info(String.valueOf(list));

        //Get expired proposal and verify
        var govActionExpiredEpoch = proposalStateClient.getProposalsByStatusAndEpoch(GovActionStatus.EXPIRED, expectdExpiryEpoch);

        assertThat(govActionExpiredEpoch).hasSize(1);
        assertThat(govActionExpiredEpoch.get(0).getTxHash()).isEqualTo(proposalTxResult.getValue());
        assertThat(govActionExpiredEpoch.get(0).getIndex()).isEqualTo(0);
        assertThat(govActionExpiredEpoch.get(0).getGovAction().getType()).isEqualTo(GovActionType.INFO_ACTION);

        //Wait till adaPot job for expectedExpiryEpoch + 1 is done.
        waitTillAdaPotJobDone(adaPotJobRepository, (long) expectdExpiryEpoch + 1);

        //check proposal refund in expired_epoch + 1
        var proposalRefund = rewardRestRepository.findBySpendableEpochAndType(expectdExpiryEpoch + 1, RewardRestType.proposal_refund)
                .stream()
                .filter(rewardRestEntity -> rewardRestEntity.getAddress().equals(account0.stakeAddress()))
                .findFirst()
                .orElse(null);

        assertThat(proposalRefund).isNotNull();
    }

}
