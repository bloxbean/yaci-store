package com.bloxbean.cardano.yaci.store.governanceaggr.service;

import com.bloxbean.cardano.yaci.core.model.governance.DrepType;
import com.bloxbean.cardano.yaci.store.governance.domain.DelegationVote;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Table;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.bloxbean.cardano.yaci.store.governance.jooq.Tables.DELEGATION_VOTE;
import static com.bloxbean.cardano.yaci.store.staking.jooq.Tables.STAKE_REGISTRATION;
import static org.jooq.impl.DSL.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class DelegationVoteDataService {
    private static final String STAKE_DEREGISTRATION_TYPE = "STAKE_DEREGISTRATION";

    private final DSLContext dsl;

    /**
     * Resolve the effective vote delegation of each supplied reward account as of {@code epoch} and
     * return only the accounts whose effective delegation is {@code drepType}.
     *
     * <p>The latest delegation certificate is not necessarily in effect: deregistering a stake
     * credential removes the whole account from ledger state, including its DRep delegation, and a
     * later re-registration starts a fresh account with no delegation. Ledger's
     * {@code defaultStakePoolVote} therefore falls back to {@code DefaultNo} once the account is
     * gone, so a delegation followed by a deregistration must not be reported here. Only a new
     * delegation certificate can restore one.
     *
     * <p>Certificates are ordered by {@code (slot, tx_index, cert_index)} because deregistration
     * and re-delegation can occur in the same epoch, the same transaction, and even in adjacent
     * certificates of that transaction.
     *
     * @param addressList bech32 reward accounts to resolve
     * @param drepType    virtual DRep type to filter on, typically ABSTAIN or NO_CONFIDENCE
     * @param epoch       snapshot epoch; certificates from later epochs are ignored
     * @return latest delegation per account, restricted to accounts still registered with that delegation
     */
    @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
    public List<DelegationVote> getDelegationVotesByDRepTypeAndAddressList(List<String> addressList, DrepType drepType, Integer epoch) {
        Field<Integer> rn = rowNumber()
                .over(partitionBy(DELEGATION_VOTE.ADDRESS).orderBy(DELEGATION_VOTE.SLOT.desc(), DELEGATION_VOTE.TX_INDEX.desc(), DELEGATION_VOTE.CERT_INDEX.desc()))
                .as("rn");

        Table<?> subquery = dsl.select(DELEGATION_VOTE.TX_HASH,
                        DELEGATION_VOTE.CERT_INDEX,
                        DELEGATION_VOTE.TX_INDEX,
                        DELEGATION_VOTE.SLOT,
                        DELEGATION_VOTE.ADDRESS,
                        DELEGATION_VOTE.DREP_HASH,
                        DELEGATION_VOTE.DREP_ID,
                        DELEGATION_VOTE.DREP_TYPE.as("drep_type"),
                        DELEGATION_VOTE.CREDENTIAL,
                        DELEGATION_VOTE.CRED_TYPE,
                        DELEGATION_VOTE.EPOCH, rn)
                .from(DELEGATION_VOTE)
                .where(
                        DELEGATION_VOTE.EPOCH.le(param("epoch", epoch))
                                .and(DELEGATION_VOTE.ADDRESS.in(addressList)))
                .asTable("d");

        var result = dsl.select()
                .from(subquery)
                .where(field(name("rn"), Integer.class).eq(1)
                        .and(field("drep_type").eq(drepType.name()))
                        .and(notDeregisteredAfterDelegation(subquery, epoch)))
                .fetchInto(DelegationVote.class);

        return result;
    }

    /**
     * Anti-join rejecting a delegation that any deregistration of the same account follows within
     * the snapshot. Re-registration is deliberately not consulted: it does not restore the removed
     * delegation, and a delegation issued after re-registration already wins the ranking.
     */
    private Condition notDeregisteredAfterDelegation(Table<?> rankedDelegations, Integer epoch) {
        var deregistration = STAKE_REGISTRATION.as("sd");

        Field<String> address = rankedDelegations.field(DELEGATION_VOTE.ADDRESS);
        Field<Long> slot = rankedDelegations.field(DELEGATION_VOTE.SLOT);
        Field<Integer> txIndex = rankedDelegations.field(DELEGATION_VOTE.TX_INDEX);
        Field<Integer> certIndex = rankedDelegations.field(DELEGATION_VOTE.CERT_INDEX);

        Condition afterDelegation = deregistration.SLOT.gt(slot)
                .or(deregistration.SLOT.eq(slot).and(deregistration.TX_INDEX.gt(txIndex)))
                .or(deregistration.SLOT.eq(slot)
                        .and(deregistration.TX_INDEX.eq(txIndex))
                        .and(deregistration.CERT_INDEX.gt(certIndex)));

        return notExists(selectOne()
                .from(deregistration)
                .where(deregistration.ADDRESS.eq(address)
                        .and(deregistration.TYPE.eq(STAKE_DEREGISTRATION_TYPE))
                        .and(deregistration.EPOCH.le(param("dereg_epoch", epoch)))
                        .and(afterDelegation)));
    }
}
