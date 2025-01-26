package com.bloxbean.cardano.yaci.store.governanceaggr.service;

import com.bloxbean.cardano.yaci.core.model.governance.DrepType;
import com.bloxbean.cardano.yaci.store.governance.domain.DelegationVote;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Table;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.bloxbean.cardano.yaci.store.governance.jooq.Tables.DELEGATION_VOTE;
import static org.jooq.impl.DSL.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class DelegationVoteDataService {
    private final DSLContext dsl;

    @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
    public List<DelegationVote> getDelegationVotesByDRepTypeAndAddressList(List<String> addressList, DrepType drepType, Integer epoch) {
        Field<Integer> rn = rowNumber()
                .over(partitionBy(DELEGATION_VOTE.ADDRESS).orderBy(DELEGATION_VOTE.SLOT.desc(), DELEGATION_VOTE.TX_INDEX.desc(), DELEGATION_VOTE.CERT_INDEX))
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
                        .and(field("drep_type").eq(drepType.name())))
                .fetchInto(DelegationVote.class);

        return result;
    }
}
