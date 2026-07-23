package com.bloxbean.cardano.yaci.store.blockfrost.transaction.storage;

import com.bloxbean.cardano.yaci.store.blockfrost.transaction.dto.BFAmountDto;
import com.bloxbean.cardano.yaci.store.blockfrost.transaction.storage.impl.model.*;

import java.math.BigInteger;
import java.util.List;
import java.util.Optional;

public interface BFTransactionStorageReader {

    Optional<TxRaw> findTransactionByHash(String txHash);

    Optional<TxUtxosRaw> findTxUtxos(String txHash);

    Optional<String> findTxCborHex(String txHash);

    List<TxRedeemerRaw> findTxRedeemers(String txHash);

    Optional<TxRedeemerPricesRaw> findRedeemerPrices(String txHash);

    List<TxStakeRaw> findTxStakes(String txHash);

    List<TxDelegationRaw> findTxDelegations(String txHash);

    List<TxWithdrawalRaw> findTxWithdrawals(String txHash);

    List<TxPoolUpdateRaw> findTxPoolUpdates(String txHash);

    List<TxPoolRetireRaw> findTxPoolRetires(String txHash);

    /**
     * Returns output amounts for a transaction (excluding collateral returns for valid txs).
     * Lovelace is always first (aggregated across all outputs into a single entry).
     * Non-lovelace tokens are summed by unit across all outputs — i.e. the same token
     * appearing in multiple outputs yields a single merged entry in the list.
     */
    List<BFAmountDto> findTxOutputAmounts(String txHash);

    BigInteger sumDrepDeposit(String txHash);
}
