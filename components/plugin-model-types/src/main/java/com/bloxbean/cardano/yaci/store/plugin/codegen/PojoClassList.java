package com.bloxbean.cardano.yaci.store.plugin.codegen;

import com.bloxbean.cardano.yaci.store.assets.domain.TxAsset;
import com.bloxbean.cardano.yaci.store.blocks.domain.Block;
import com.bloxbean.cardano.yaci.store.common.domain.AddressUtxo;
import com.bloxbean.cardano.yaci.store.common.domain.TxInput;
import com.bloxbean.cardano.yaci.store.events.*;
import com.bloxbean.cardano.yaci.store.events.internal.CommitEvent;
import com.bloxbean.cardano.yaci.store.events.internal.EpochTransitionCommitEvent;
import com.bloxbean.cardano.yaci.store.events.internal.PreCommitEvent;
import com.bloxbean.cardano.yaci.store.events.internal.PreEpochTransitionEvent;
import com.bloxbean.cardano.yaci.store.governance.domain.*;
import com.bloxbean.cardano.yaci.store.metadata.domain.TxMetadataEvent;
import com.bloxbean.cardano.yaci.store.metadata.domain.TxMetadataLabel;
import com.bloxbean.cardano.yaci.store.script.domain.*;
import com.bloxbean.cardano.yaci.store.staking.domain.*;
import com.bloxbean.cardano.yaci.store.staking.domain.event.*;
import com.bloxbean.cardano.yaci.store.transaction.domain.Txn;
import com.bloxbean.cardano.yaci.store.transaction.domain.TxnWitness;
import com.bloxbean.cardano.yaci.store.transaction.domain.Withdrawal;
import com.bloxbean.cardano.yaci.store.transaction.domain.event.TxnEvent;
import com.bloxbean.cardano.yaci.store.utxo.domain.AddressUtxoEvent;

import java.util.List;

public class PojoClassList {
    public static List<Class> eventClasses = List.of(
            RollbackEvent.class,
            CommitEvent.class,
            PreCommitEvent.class,
            PreEpochTransitionEvent.class,
            EpochTransitionCommitEvent.class,
            AuxDataEvent.class,
            BlockEvent.class,
            CertificateEvent.class,
            EpochChangeEvent.class,
            GovernanceEvent.class,
            MintBurnEvent.class,
            ScriptEvent.class,
            TransactionEvent.class,
            UpdateEvent.class,
            TxMetadataEvent.class,
            DatumEvent.class,
            TxScriptEvent.class,
            PoolRegistrationEvent.class,
            PoolRetiredEvent.class,
            PoolRetirementEvent.class,
            StakeRegDeregEvent.class,
            StakingDepositEvent.class,
            TxnEvent.class,
            AddressUtxoEvent.class
    );

    public static List<Class> domainClasses = List.of(
            TxAsset.class,
            Block.class,
            TxMetadataLabel.class,
            Datum.class,
            Script.class,
            TxScript.class,
            PoolRegistration.class,
            PoolRetirement.class,
            Pool.class,
            StakeRegistrationDetail.class,
            Delegation.class,
            Txn.class,
            TxnWitness.class,
            Withdrawal.class,
            AddressUtxo.class,
            TxInput.class,
            //governance
            CommitteeDeRegistration.class,
            CommitteeMember.class,
            CommitteeRegistration.class,
            DelegationVote.class,
            DRepRegistration.class,
            DRep.class,
            GovActionProposal.class,
            VotingProcedure.class
    );
}
