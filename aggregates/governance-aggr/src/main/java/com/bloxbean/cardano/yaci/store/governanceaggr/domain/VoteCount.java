package com.bloxbean.cardano.yaci.store.governanceaggr.domain;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VoteCount {
    int yes;
    int no;
    int abstain;

    public VoteCount() {
        this.yes = 0;
        this.no = 0;
        this.abstain = 0;
    }

    public void addYes() {
        this.yes++;
    }

    public void addNo() {
        this.no++;
    }

    public void addAbstain() {
        this.abstain++;
    }

    public void subtractYes() {
        this.yes--;
    }

    public void subtractNo() {
        this.no--;
    }

    public void subtractAbstain() {
        this.abstain--;
    }
}
