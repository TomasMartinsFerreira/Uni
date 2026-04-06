package pt.tecnico.blockchainist.node.TransactionAction;


import pt.tecnico.blockchainist.contract.CreateWalletRequest;
import pt.tecnico.blockchainist.contract.DeleteWalletRequest;
import pt.tecnico.blockchainist.contract.Signature;
import pt.tecnico.blockchainist.contract.SignedCreateWalletRequest;
import pt.tecnico.blockchainist.contract.SignedDeleteWalletRequest;
import pt.tecnico.blockchainist.contract.SignedTransferRequest;
import pt.tecnico.blockchainist.contract.Transaction;
import pt.tecnico.blockchainist.contract.TransferRequest;
import pt.tecnico.blockchainist.node.domain.NodeState;


/*
NodeTransactionAction:
* - this file creates the transactions and its needed methods
* - main purpose: Not storing in the ledger the TransactionMsg
* - it implements a Hierarchical Design Pattern
* - \\ The RestoreTransactionMsg method is Inherited from BaseAction
            - it allows us to restor our Transaction msg so it can be sent

* -- execute() - is here so we could remove our previous switch case complexity from the NodeState( now we simply call the NodeTransactionAction.execute() )
* -- validate() - same idea as the execute, now the verification on the NodeState ( testTransactionRequest() ), only needs to call validate
*
*-- FromGrpc() is the most important part since its the one in charge of creating the specific class
*/ 



public abstract class NodeTransactionAction {

    protected final String clientTxId;

    protected NodeTransactionAction(String clientTxId) {
        this.clientTxId = clientTxId;
    }

    public String getClientTxId() {
        return clientTxId;
    }

    public static NodeTransactionAction fromGrpc(Transaction msg) {
        if (msg.hasCreateWallet()) return new CreateWalletAction(msg);
        if (msg.hasDeleteWallet()) return new DeleteWalletAction(msg);
        if (msg.hasTransfer()) return new TransferAction(msg);
        throw new IllegalArgumentException("Unknown transaction type");
    }

    public abstract void validate() throws IllegalArgumentException;

    public abstract void execute(NodeState state) throws Exception;

    public abstract Transaction restoreTransactionMsg();
}


// in case we are handling a CreateWalletRequest
class CreateWalletAction extends NodeTransactionAction {

    private final String userId;
    private final String walletId;
    private final Signature signature;

    public CreateWalletAction(Transaction msg) {
        super(msg.getCreateWallet().getCreateWalletRequest().getClientTxId());
        this.userId = msg.getCreateWallet().getCreateWalletRequest().getUserId();
        this.walletId = msg.getCreateWallet().getCreateWalletRequest().getWalletId();
        this.signature = msg.getCreateWallet().getSignature();
    }

    @Override
    public void validate() throws IllegalArgumentException{
      if (this.userId.isEmpty() || this.walletId.isEmpty()) {
         throw new IllegalArgumentException("Invalid CreateWalletRequest: userId and walletId must be non-empty");
      }
    }

    @Override
    public Transaction restoreTransactionMsg() {
        return Transaction.newBuilder()
                .setCreateWallet(SignedCreateWalletRequest.newBuilder()
                    .setCreateWalletRequest(CreateWalletRequest.newBuilder()
                        .setUserId(userId)
                        .setWalletId(walletId)
                        .setClientTxId(clientTxId)
                        .build())
                    .setSignature(signature)
                    .build())
                .build();
    }

    @Override
    public void execute(NodeState state) throws Exception {
      state.createWallet(this.userId, this.walletId);
    }
}


// in case we are handling a DeleteWalletRequest
class DeleteWalletAction extends NodeTransactionAction {

    private final String userId;
    private final String walletId;
    private final Signature signature;

    public DeleteWalletAction(Transaction msg) {
        super(msg.getDeleteWallet().getDeleteWalletRequest().getClientTxId());
        this.userId = msg.getDeleteWallet().getDeleteWalletRequest().getUserId();
        this.walletId = msg.getDeleteWallet().getDeleteWalletRequest().getWalletId();
        this.signature = msg.getDeleteWallet().getSignature();
    }

    
    @Override
    public void validate() throws IllegalArgumentException {
      if (this.userId.isEmpty() || this.walletId.isEmpty()) {
         throw new IllegalArgumentException("Invalid DeleteWalletRequest: userId and walletId must be non-empty");
      }
    }

    @Override
    public Transaction restoreTransactionMsg() {
        return Transaction.newBuilder()
                .setDeleteWallet(SignedDeleteWalletRequest.newBuilder()
                    .setDeleteWalletRequest(DeleteWalletRequest.newBuilder()
                        .setUserId(userId)
                        .setWalletId(walletId)
                        .setClientTxId(clientTxId)
                        .build())
                    .setSignature(signature)
                    .build())
                .build();
    }

    @Override
    public void execute(NodeState state) throws Exception{
      state.deleteWallet(this.userId, this.walletId);
    }
}


// in case we are handling a TransferRequest
class TransferAction extends NodeTransactionAction {

    private final String srcUserId;
    private final String srcWalletId;
    private final String dstWalletId;
    private final long amount;
    private final Signature signature;

    public TransferAction(Transaction msg) {
        super(msg.getTransfer().getTransferRequest().getClientTxId());
        TransferRequest t = msg.getTransfer().getTransferRequest();
        this.srcUserId = t.getSrcUserId();
        this.srcWalletId = t.getSrcWalletId();
        this.dstWalletId = t.getDstWalletId();
        this.amount = t.getValue();
        this.signature = msg.getTransfer().getSignature();
    } 

    public String getSourceWallet() {
        return this.srcWalletId;
    }
    public String getDestinationWallet() {
        return this.dstWalletId;
    }
    public long getAmount() {
        return this.amount;
    }

    @Override
    public void validate() throws IllegalArgumentException {
      if (this.srcUserId.isEmpty() || this.srcWalletId.isEmpty() || dstWalletId.isEmpty()) {
        throw new IllegalArgumentException("Invalid TransferRequest: srcUserId, srcWalletId and dstWalletId must be non-empty");
      }
      if (this.amount <= 0) {
        throw new IllegalArgumentException("Invalid TransferRequest: value must be bigger then 0");
      }
    }


    @Override
    public Transaction restoreTransactionMsg() {
        return Transaction.newBuilder()
                .setTransfer(SignedTransferRequest.newBuilder()
                    .setTransferRequest(TransferRequest.newBuilder()
                        .setSrcUserId(srcUserId)
                        .setSrcWalletId(srcWalletId)
                        .setDstWalletId(dstWalletId)
                        .setValue(amount)
                        .setClientTxId(clientTxId)
                        .build())
                    .setSignature(signature)
                    .build())
                .build();
    }

    @Override
    public void execute(NodeState state) throws Exception {
      state.transfer(this.srcUserId, this.srcWalletId, this.dstWalletId, this.amount);
    }
}
