package pt.tecnico.blockchainist.sequencer.domain;

import pt.tecnico.blockchainist.contract.Transaction;
import pt.tecnico.blockchainist.contract.Block;
import pt.tecnico.blockchainist.sequencer.TransactionAction.SequencerTransactionAction;
import pt.tecnico.blockchainist.sequencer.domain.SequencerState.SequencerBlock;
import utils.GlobalUtil;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.ArrayList;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;





public class SequencerState {

  private final List<SequencerBlock> ledger = new ArrayList<>();
  private final Set<String> tx = ConcurrentHashMap.newKeySet();
  private SequencerBlock currentBlock;


  private static final int MAX_BLOCK_SIZE = 4;
  private static final int TIMEOUT_SECONDS = 5;
  private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
  private ScheduledFuture<?> block_timeout = null;
  
  private final ReadWriteLock rwLock = new ReentrantReadWriteLock();

  private long lastBlockTimestamp = System.currentTimeMillis();
  private double runningAvgTime = 0;

  public static class SequencerBlock {
    private final int block_sequence_number;
    private final List<SequencerTransactionAction> transactions;

    public SequencerBlock(int block_sequence_number) {
      this.block_sequence_number = block_sequence_number;
      this.transactions = new ArrayList<>();
    }

    public void addAction(SequencerTransactionAction action) {
      this.transactions.add(action);
    }

    public List<SequencerTransactionAction> getActions() {
      return transactions;
    }

    public int getSize() {
      return transactions.size();
    }

    public int getSequenceNumber() {
      return block_sequence_number;
    }

    public Block blockMsg() {
      return Block.newBuilder()
        .setBlockId(this.block_sequence_number)
        .addAllTransactions(this.transactions.stream()
          .map(SequencerTransactionAction::restoreTransactionMsg)
          .collect(Collectors.toList()))
        .build();
    }
  }

  // intializing Sequencer state
  public SequencerState() {
    this.currentBlock = new SequencerBlock(1);
  }


  public synchronized void addTransaction(Transaction transaction) {
    // obtain SequencerTransactionAction to store in ledger
    SequencerTransactionAction transactionAction = SequencerTransactionAction.fromGrpc(transaction);
    String clientTxId = transactionAction.getClientTxId();

    if (clientTxId == null || clientTxId.isEmpty() || !tx.add(clientTxId)) {
      return;
    }
    // after verifications start the timer
    if (currentBlock.getSize() == 0) {
      this.startBlockTimeOut();
    }

    currentBlock.addAction(transactionAction);

    if (currentBlock.getSize() >= MAX_BLOCK_SIZE) {
      addBlockToLedger();
    }
  }


  public synchronized void startBlockTimeOut() {
    
    if (block_timeout != null && !block_timeout.isDone()) {
      block_timeout.cancel(false);
    }
    block_timeout = scheduler.schedule(() -> {
      addBlockToLedger();
    }, TIMEOUT_SECONDS, TimeUnit.SECONDS);
  }



  public synchronized void addBlockToLedger() {

    if (currentBlock.getSize() == 0) return;

    rwLock.writeLock().lock();
    try {
      if (block_timeout != null) {
        block_timeout.cancel(false);
        block_timeout = null;
      }
      if (currentBlock.getSize() != 0) {
        
        updateAverageBlockTime();

        ledger.add(currentBlock);
        GlobalUtil.debug("added new block to ledger with seq = " + currentBlock.getSequenceNumber());

        int next_block_id = currentBlock.getSequenceNumber() + 1;
        this.currentBlock = new SequencerBlock(next_block_id);
      }

    } finally {
      rwLock.writeLock().unlock();
    }
  }


  private void updateAverageBlockTime() {

    long currentTime = System.currentTimeMillis();
    long timeSinceLastBlock = currentTime - lastBlockTimestamp;

    if (ledger.isEmpty()) {
        runningAvgTime = Math.min(timeSinceLastBlock, TIMEOUT_SECONDS * 1000);
    } else {
        // this is better for the network
        // if we had (runningAvgTime + timeSinceLastBlock)/2 - it would cause the network to have the nodes trying to retriveBlock more frequently, causing more preassure into the sequencer when the traffic is already high
        runningAvgTime = runningAvgTime + (timeSinceLastBlock - runningAvgTime) / (ledger.size() + 1);
    }
    
    lastBlockTimestamp = currentTime;
  }


  public double getAverageTime() {

    rwLock.readLock().lock();
    try {
      return runningAvgTime;
    } finally {
      rwLock.readLock().unlock();
    }
  }

  /**
  * retrives transaction in case of DeliverTransactionRequest in SequencerNodeService
  */
  public Block retriveBlock(int sequence_number) {
    
    rwLock.readLock().lock();
    try {
      GlobalUtil.debug("retriving block with" + sequence_number);
      // finds the specific Block associated to the sequence_number
      int pos = sequence_number - 1;
      if (pos >= 0 && pos < ledger.size()) {
        return ledger.get(pos).blockMsg();
      }
      return null;
    } finally {
      rwLock.readLock().unlock();
    }
  }

}
