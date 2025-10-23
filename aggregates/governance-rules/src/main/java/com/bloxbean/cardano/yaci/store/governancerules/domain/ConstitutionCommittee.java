package com.bloxbean.cardano.yaci.store.governancerules.domain;

import com.bloxbean.cardano.yaci.core.types.UnitInterval;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConstitutionCommittee {
    private List<CommitteeMember> members;
    private ConstitutionCommitteeState state;
    private UnitInterval threshold;
}
