package com.bloxbean.cardano.yaci.store.live.service;

import com.bloxbean.cardano.client.transaction.spec.Transaction;
import com.bloxbean.cardano.client.transaction.util.TransactionUtil;
import com.bloxbean.cardano.yaci.helper.LocalClientProvider;
import com.bloxbean.cardano.yaci.helper.LocalTxMonitorClient;
import com.bloxbean.cardano.yaci.helper.model.MempoolStatus;
import com.bloxbean.cardano.yaci.store.live.BlocksWebSocketHandler;
import com.bloxbean.cardano.yaci.store.live.dto.MempoolTx;
import com.bloxbean.cardano.yaci.store.live.dto.MempoolTxs;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

@Service
@ConditionalOnBean(LocalClientProvider.class)
@ConditionalOnProperty(
        value="cardano.mempool.monitoring.enabled",
        havingValue = "true"
)
@Slf4j
public class LocalTxMonitorService {
    private final LocalClientProvider localClientProvider;
    private final LocalTxMonitorClient localTxMonitorClient;
    private BlocksWebSocketHandler socketHandler;

    public LocalTxMonitorService(LocalClientProvider localClientProvider, BlocksWebSocketHandler blocksWebSocketHandler) {
        this.localClientProvider = localClientProvider;
        this.localTxMonitorClient = localClientProvider.getTxMonitorClient();
        this.socketHandler = blocksWebSocketHandler;

        //start();
    }

    public void start() {
        while (true) {
            try {
                List<byte[]> txBytesList = localTxMonitorClient.acquireAndGetMempoolTransactionsAsMono().block();

                List<MempoolTx> mempoolTxList = new ArrayList<>();
                for (byte[] txBytes : txBytesList) {
                    String txHash = TransactionUtil.getTxHash(txBytes);
                    Transaction transaction = Transaction.deserialize(txBytes);
                    BigInteger totalFee = transaction.getBody().getFee();
                    BigInteger totalOutput = transaction.getBody().getOutputs()
                            .stream()
                            .map(transactionOutput -> transactionOutput.getValue().getCoin())
                            .reduce(BigInteger::add).orElse(BigInteger.ZERO);
                    MempoolTx mempoolTx = MempoolTx.builder()
                            .txHash(txHash)
                            .totalFee(totalFee)
                            .totalOutput(totalOutput)
                            .build();
                    mempoolTxList.add(mempoolTx);
                }

                MempoolTxs mempoolTxs = MempoolTxs.builder()
                        .transactions(mempoolTxList)
                        .build();

                log.info("sending mempool txs >> " + mempoolTxs);
                socketHandler.broadcastMempoolTxs(mempoolTxs);

                MempoolStatus mempoolStatus = localTxMonitorClient.getMempoolSizeAndCapacity().block();
                log.info("Mem Pool >> " + mempoolStatus);
            } catch (Exception e) {
                log.error("Error in mempool tx monitoring", e);
                try {
                    Thread.sleep(10000);
                } catch (InterruptedException ex) {
                }
            }
        }
    }
}
