package com.bloxbean.cardano.yaci.store.governanceaggr.storage.impl.mapper;

import com.bloxbean.cardano.yaci.store.governanceaggr.domain.CommitteeVote;
import com.bloxbean.cardano.yaci.store.governanceaggr.storage.impl.model.CommitteeVoteEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface CommitteeVoteMapper {
    CommitteeVoteEntity toCommitteeVotesEntity(CommitteeVote committeeVote);
    CommitteeVote toCommitteeVotes(CommitteeVoteEntity committeeVoteEntity);
}
