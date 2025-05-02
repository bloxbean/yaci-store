package com.bloxbean.cardano.yaci.store.governanceaggr.util;

import com.bloxbean.cardano.client.governance.GovId;
import com.bloxbean.cardano.yaci.core.model.governance.VoterType;
import com.bloxbean.cardano.yaci.core.util.HexUtil;
import com.bloxbean.cardano.yaci.store.governance.domain.VotingProcedure;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DRepUtil {

    public static String getDRepId(VotingProcedure votingProcedure) {
        try {
            if (votingProcedure.getVoterType() == VoterType.DREP_KEY_HASH)
                return GovId.drepFromKeyHash(HexUtil.decodeHexString(votingProcedure.getVoterHash()));
            else if (votingProcedure.getVoterType() == VoterType.DREP_SCRIPT_HASH)
                return GovId.drepFromScriptHash(HexUtil.decodeHexString(votingProcedure.getVoterHash()));
            else
                return null;
        } catch (Exception e) {
            log.error("Error while getting DRep Id from Voter Hash: {}", votingProcedure.getVoterHash(), e);
            return null;
        }
    }
}
