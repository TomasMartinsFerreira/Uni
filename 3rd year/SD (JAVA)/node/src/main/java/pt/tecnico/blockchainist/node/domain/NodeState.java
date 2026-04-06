package pt.tecnico.blockchainist.node.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import pt.tecnico.blockchainist.contract.Block;
import pt.tecnico.blockchainist.contract.SignedCreateWalletRequest;
import pt.tecnico.blockchainist.contract.SignedDeleteWalletRequest;
import pt.tecnico.blockchainist.contract.SignedTransferRequest;
import pt.tecnico.blockchainist.contract.Transaction;
import pt.tecnico.blockchainist.node.InitKeys;
import pt.tecnico.blockchainist.node.TransactionAction.NodeTransactionAction;
import utils.GlobalUtil;



public class NodeState {
    
    private ConcurrentHashMap<String,Wallet> wallets;
    private List<NodeBlock> ledger;
    private final ConcurrentHashMap<String, String> txResults;
    private final InitKeys validClients;

    // Object to notify BLockRetriver when transactions are broadcasted
    private final Object blockRetrieverNotifier = new Object();
    private final AtomicInteger pendingTransactionsCount = new AtomicInteger(0);

    // class acting as a structure for a Wallet
    public static class Wallet {

        private String walletId;
        private String userId;
        private long balance;
        private boolean isLocal = false;
        private long provisional_balance; // optimization for local transactions
                                         
        private final Object walletLock = new Object();

        private int pendingCount = 0;
        
        public Wallet(String walletId, String userId, long balance, boolean isLocal) {
            this.walletId = walletId;
            this.userId = userId;
            this.balance = balance;
            this.isLocal = isLocal;
            this.provisional_balance = balance;
        }

        public boolean isPending() {
            return this.pendingCount > 0;
        }

        public boolean isLocalWallet() {
            return this.isLocal;
        }

        public void incrementPendingCount() {
            this.pendingCount++;
        }
        
        public void decrementPendingCount() {
            this.pendingCount--;
        }

        public long getBalance() {
            return this.balance;
        }

        public void setBalance(long balance) {
            this.balance = balance;
        }

        public long getProvisionalBalance() {
            return this.provisional_balance;
        }

        public void setProvisionalBalance(long balance) {
            this.provisional_balance = balance;
        }

        public void decrementProvisionalBalance(long amount) {
            this.provisional_balance = this.provisional_balance - amount;
        }

        public void addProvisionalBalance(long amount) {
            this.provisional_balance = this.provisional_balance + amount;
        }

        public Object getLock() {
          return this.walletLock;
        }
    }

    /**
     * Initializes the node state with the central bank wallet with a 
     * walletId of "bc" with 1000 coins
     */
    public NodeState(InitKeys validClients) {
        GlobalUtil.debug("Initializing NodeState");
        this.validClients = validClients;
        this.wallets = new ConcurrentHashMap<>();
        this.ledger = new ArrayList<>();
        this.txResults = new ConcurrentHashMap<>();
        this.addWalletState("bc", "BC", 1000, true);
    }

    /**
    * obtain the lock for sync in between grpc processes and the pulling thread(BlockRetriver)
    */
    public Object getBlockRetrieverNotifier() {
        return blockRetrieverNotifier;
    } 

    ///// methods so we can stop the fastFetching if we already found our block
    // causes us to activate the fastFetching
    public void incrementPendingTransactions() {
        pendingTransactionsCount.incrementAndGet();
    }

    public void decrementPendingTransactions() {
        pendingTransactionsCount.decrementAndGet();
    }

    public boolean hasPendingTransactions() {
        return pendingTransactionsCount.get() > 0;
    }
    /////

    /**
     * Adds a wallet to the NodeState, if the wallet already exists it returns -1, otherwise it returns 0
     */
    public int addWalletState(String walletId, String userId, long balance, boolean isLocal) {
        
        Wallet wallet = this.wallets.get(walletId);
        if (wallet != null) {
            return -1;
        }
        wallets.put(walletId, new Wallet(walletId, userId, balance, isLocal));
        return 0;
    }
        
    /**
     * Changes the balance of a wallet, if the wallet does not have enough funds it returns -1, otherwise it returns 0
     */
    public int changeBalanceState(String walletId, long amount) {
        
        Wallet wallet = this.wallets.get(walletId);
        long new_balance = wallet.getBalance() + amount;
        if (new_balance < 0) {
            return -1;
        }
        wallet.setBalance(new_balance);

        return 0;
    }

    /**
     * Deletes a wallet from the NodeState
     */
    public void deleteWalletState(String walletId) {
        this.wallets.remove(walletId);
    }
    
    /** 
    * processes a received block by executing its transactions sequentially
    * updates the node's state, stores each transaction's execution result in txResults
    * and notifies waiting client threads as soon as their transaction completes
    * finally, appends the fully processed block to the local ledger
    */
    public void executeTransactions(Block block) {
        
        NodeBlock nodeBlock = new NodeBlock(block.getBlockId());
        
        for (Transaction txMsg : block.getTransactionsList()) {
            NodeTransactionAction transactionAction = NodeTransactionAction.fromGrpc(txMsg);
            String clientTxId = transactionAction.getClientTxId();

            String resultMessage = "SUCCESS";
            byte[] dataToVerify = null;
            byte[] signatureBytes = null;
            String userId = null;

           switch (txMsg.getOperationCase()) {
                case CREATE_WALLET:
                    SignedCreateWalletRequest createReq = txMsg.getCreateWallet();
                    dataToVerify = createReq.getCreateWalletRequest().toByteArray();
                    signatureBytes = createReq.getSignature().getSignatureValue().toByteArray();
                    userId = createReq.getCreateWalletRequest().getUserId();
                    break;

                case DELETE_WALLET:
                    SignedDeleteWalletRequest deleteReq = txMsg.getDeleteWallet();
                    dataToVerify = deleteReq.getDeleteWalletRequest().toByteArray();
                    signatureBytes = deleteReq.getSignature().getSignatureValue().toByteArray();
                    userId = deleteReq.getDeleteWalletRequest().getUserId();
                    break;

                case TRANSFER:
                    SignedTransferRequest transferReq = txMsg.getTransfer();
                    dataToVerify = transferReq.getTransferRequest().toByteArray();
                    signatureBytes = transferReq.getSignature().getSignatureValue().toByteArray();
                    userId = transferReq.getTransferRequest().getSrcUserId();
                    break;

                case OPERATION_NOT_SET:
                    continue;
            }

            if (!validClients.verifySignature(userId, dataToVerify, signatureBytes)) {
                GlobalUtil.debug("Invalid signature for transaction " + clientTxId + " from user " + userId);
                continue;
            }
            GlobalUtil.debug("Valid signature for transaction " + clientTxId + " from user " + userId);

            
            try {
                transactionAction.execute(this);
            } catch (Exception e) {
                GlobalUtil.debug("Transaction " + clientTxId + " failed: " + e.getMessage());
                e.printStackTrace();
                resultMessage = e.getMessage();
            }
            if (clientTxId != null && !clientTxId.isEmpty()) {
                txResults.put(clientTxId, resultMessage);
                synchronized(this) { this.notifyAll(); }
            }

            nodeBlock.addTransaction(transactionAction);
        }

        this.ledger.add(nodeBlock);
    }


    public synchronized String waitTransaction(String clientTxId) throws InterruptedException {
        while (!this.txResults.containsKey(clientTxId)) {
            this.wait();
        }

        return this.txResults.get(clientTxId);
    }

    public Wallet getWallet(String walletId) {
        return this.wallets.get(walletId);
    }

    public void setTransactionTxResult(String clientTxId, String result) {
        if (clientTxId != null && !clientTxId.isEmpty()) {
            this.txResults.put(clientTxId, result);
        }
    }

    /**
     * blocks the current thread until the transaction with the specified clientTxId is processed.
     * it continuously waits until the transaction result is available in txResults.
     */
    public String checkDuplicates(String clientTxId) {
        if (clientTxId == null || clientTxId.isEmpty()) {
            return null;
        }
        
        return this.txResults.get(clientTxId);
    }

    /**
    * validates client requests before forwarding to the sequencer
    * ReadBalanceRequests are tested only on testReadRequest
    */
    public void testTransactionRequest(NodeTransactionAction transactionAction) throws IllegalArgumentException {
      if (transactionAction == null) {
        throw new IllegalArgumentException("Action cannot be null");
      }
      transactionAction.validate();
    }

    // same as the above but this is only called when reading the balance ( readBalance() )
    public void testReadRequest(String walletId) throws IllegalArgumentException {
      if (walletId == null || walletId.isEmpty()) {
        throw new IllegalArgumentException("Invalid ReadBalanceRequest: walletId must be non-empty");
      }
    }
 

    /**
    *adds the wallet to our state
    */
    public void createWallet(String userId, String walletId) throws Exception {

        GlobalUtil.debug("Creating wallet");

        boolean isLocal = validClients.isOrgUser(userId);

        // verifying first if the wallet previously existed
        int value = this.addWalletState(walletId, userId, 0, isLocal);
        if (value == -1) {
            GlobalUtil.debug("Wallet " + walletId + " already exists");
            throw new Exception("Wallet " + walletId + " already exists");

        }
        GlobalUtil.debug("Wallet created sucessfuly");
    }

    /** 
    * attempts to delete the wallet from state
    * throws exception if not possible
    */
    public void deleteWallet(String userId, String walletId) throws Exception {
        
        GlobalUtil.debug("Deleting wallet");
        Wallet wallet = this.wallets.get(walletId);

        if (wallet == null) {
            GlobalUtil.debug("Wallet: " + walletId + " doesnt exist");
            throw new Exception("Wallet " + walletId + " does not exist");
        }

        if (!userId.equals(wallet.userId)) {
            GlobalUtil.debug("Wallet : " + walletId + "does not belong to " + userId);
            throw new Exception("Wallet " + walletId + " does not belong to user " + userId);
        }

        if (wallet.getBalance() != 0) {
          GlobalUtil.debug("Wallet is not empty");
          throw new Exception("Wallet is not empty");
        }

        this.deleteWalletState(walletId);
        GlobalUtil.debug("Wallet deleted sucessfuly");
    }


    /**
    * transfers amount betweem srcWalletId and DstWalletId
    * throws exception in case of an invalid paramether
    */
    public void transfer(String srcUserId, String srcWalletId, String dstWalletId, Long amount) throws Exception {

        GlobalUtil.debug("Transfering from " + srcWalletId + " to " + dstWalletId);

        Wallet dstWallet = this.wallets.get(dstWalletId);
        Wallet srcWallet = this.wallets.get(srcWalletId);

        if (dstWallet == null) {
            if (srcWallet != null && srcWallet.isLocalWallet()) {
                synchronized(srcWallet.getLock()) {
                    srcWallet.addProvisionalBalance(amount);
                }
            }
            GlobalUtil.debug("Wallet: " + dstWalletId + " doesnt exist");
            throw new Exception("Wallet " + dstWalletId + " does not exist");
        }
        if (srcWallet == null) {
            GlobalUtil.debug("Wallet: " + srcWalletId + " doesnt exist");
            throw new Exception("Wallet " + srcWalletId + " does not exist");
        }
        if (!srcUserId.equals(srcWallet.userId)) {
            GlobalUtil.debug("Wallet : " + srcWalletId + "doesn't belong to " + srcUserId);
            throw new Exception("Wallet " + srcWalletId + " does not belong to user " + srcUserId);
        }
        if (amount < 0) return;
        
        Object firsLock = srcWalletId.compareTo(dstWalletId) < 0 ? this.getWallet(srcWalletId).getLock() : this.getWallet(dstWalletId).getLock();
        Object secondLock = srcWalletId.compareTo(dstWalletId) < 0 ? this.getWallet(dstWalletId).getLock() : this.getWallet(srcWalletId).getLock();

        synchronized(firsLock) {
            synchronized(secondLock) {

                // trys to remove amount from sender
                int value_sender = this.changeBalanceState(srcWalletId, -amount);
                if (value_sender == -1) {
                    if (srcWallet.isLocalWallet()) {
                        srcWallet.addProvisionalBalance(amount);
                    }
                    GlobalUtil.debug("Wallet: " + srcWalletId + " doesnt have enough funds");
                    throw new Exception("Wallet " + srcWalletId + " does not have enough funds");
                }
                // transfers amount to dstWalletId
                this.changeBalanceState(dstWalletId, amount);
                
                if (dstWallet.isLocalWallet() && !srcWallet.isLocalWallet()) {
                    dstWallet.addProvisionalBalance(amount);
                }
            }
        }
        GlobalUtil.debug("Transfer executed sucessfuly");
    }



    /** 
    * reads balance from a walletId
    */
    public long readBalance(String walletId) throws Exception {
        
        GlobalUtil.debug("Reading balance");
        Wallet wallet = this.wallets.get(walletId);
        if (wallet == null) {
            throw new Exception("Wallet " + walletId + " does not exist");
        }
        return wallet.getBalance();
    }


    /**
    * converts the current ledger state to a Transaction list, so it can be sent over to the client
    */
    public List<Transaction> getBlockchainState() {

        GlobalUtil.debug("Obtaining blockchain state");
        
        List<Transaction> transactions = new ArrayList<>();

        for (NodeBlock block : this.ledger) {
            for (NodeTransactionAction action : block.getTransactions()) {
                transactions.add(action.restoreTransactionMsg());
            }
        }

        return transactions;
    }
}
