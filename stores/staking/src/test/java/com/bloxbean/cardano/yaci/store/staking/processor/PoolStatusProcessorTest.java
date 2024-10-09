package com.bloxbean.cardano.yaci.store.staking.processor;

import com.bloxbean.cardano.yaci.store.staking.domain.PoolRegistration;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PoolStatusProcessorTest {

    @Test
    public void testSorting() {
        PoolRegistration poolRegistration1 = new PoolRegistration();
        poolRegistration1.setPoolId("pool1");
        poolRegistration1.setCost(BigInteger.ONE);
        poolRegistration1.setTxHash("tx1");
        poolRegistration1.setTxIndex(0);
        poolRegistration1.setCertIndex(0);

        PoolRegistration poolRegistration2 = new PoolRegistration();
        poolRegistration2.setPoolId("pool2");
        poolRegistration2.setCost(BigInteger.ONE);
        poolRegistration2.setTxHash("tx2");
        poolRegistration2.setTxIndex(2);
        poolRegistration2.setCertIndex(0);

        PoolRegistration poolRegistration3 = new PoolRegistration();
        poolRegistration3.setPoolId("pool3");
        poolRegistration3.setCost(BigInteger.ONE);
        poolRegistration3.setTxHash("tx3");
        poolRegistration3.setTxIndex(3);
        poolRegistration3.setCertIndex(10);

        PoolRegistration poolRegistration4 = new PoolRegistration();
        poolRegistration4.setPoolId("pool4");
        poolRegistration4.setCost(BigInteger.ONE);
        poolRegistration4.setTxHash("tx4");
        poolRegistration4.setTxIndex(1);
        poolRegistration4.setCertIndex(0);

        var poolRegistrations = new ArrayList<PoolRegistration>();
        poolRegistrations.add(poolRegistration2);
        poolRegistrations.add(poolRegistration3);
        poolRegistrations.add(poolRegistration1);
        poolRegistrations.add(poolRegistration4);
//        var poolRegistrations = (poolRegistration2, poolRegistration3, poolRegistration1, poolRegistration4);

        poolRegistrations.sort(Comparator.comparingLong(e -> e.getTxIndex()));

       poolRegistrations.forEach(poolRegistration -> System.out.println(poolRegistration.getPoolId() + ":" + poolRegistration.getTxIndex() + " : " + poolRegistration.getCertIndex()));
    }

    @Test
    public void testSorting2() {
        PoolRegistration poolRegistration1 = new PoolRegistration();
        poolRegistration1.setPoolId("pool1");
        poolRegistration1.setCost(BigInteger.ONE);
        poolRegistration1.setTxHash("tx1");
        poolRegistration1.setTxIndex(10);
        poolRegistration1.setCertIndex(0);

        PoolRegistration poolRegistration2 = new PoolRegistration();
        poolRegistration2.setPoolId("pool2");
        poolRegistration2.setCost(BigInteger.ONE);
        poolRegistration2.setTxHash("tx2");
        poolRegistration2.setTxIndex(2);
        poolRegistration2.setCertIndex(5);

        PoolRegistration poolRegistration3 = new PoolRegistration();
        poolRegistration3.setPoolId("pool3");
        poolRegistration3.setCost(BigInteger.ONE);
        poolRegistration3.setTxHash("tx3");
        poolRegistration3.setTxIndex(2);
        poolRegistration3.setCertIndex(3);

        PoolRegistration poolRegistration4 = new PoolRegistration();
        poolRegistration4.setPoolId("pool4");
        poolRegistration4.setCost(BigInteger.ONE);
        poolRegistration4.setTxHash("tx4");
        poolRegistration4.setTxIndex(1);
        poolRegistration4.setCertIndex(10);

        PoolRegistration poolRegistration5 = new PoolRegistration();
        poolRegistration5.setPoolId("pool5");
        poolRegistration5.setCost(BigInteger.ONE);
        poolRegistration5.setTxHash("tx4");
        poolRegistration5.setTxIndex(1);
        poolRegistration5.setCertIndex(5);

        var poolRegistrations = new ArrayList<PoolRegistration>();
        poolRegistrations.add(poolRegistration2);
        poolRegistrations.add(poolRegistration3);
        poolRegistrations.add(poolRegistration1);
        poolRegistrations.add(poolRegistration4);
        poolRegistrations.add(poolRegistration5);
//        var poolRegistrations = (poolRegistration2, poolRegistration3, poolRegistration1, poolRegistration4);

        poolRegistrations.sort(Comparator.comparingLong(PoolRegistration::getTxIndex)
                .thenComparingLong(PoolRegistration::getCertIndex));

        poolRegistrations.forEach(poolRegistration -> System.out.println(poolRegistration.getPoolId() + ":" + poolRegistration.getTxIndex() + " : " + poolRegistration.getCertIndex()));
    }

}
