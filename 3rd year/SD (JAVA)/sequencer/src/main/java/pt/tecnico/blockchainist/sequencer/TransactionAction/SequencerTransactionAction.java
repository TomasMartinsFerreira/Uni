package pt.tecnico.blockchainist.sequencer.TransactionAction;

import pt.tecnico.blockchainist.contract.CreateWalletRequest;
import pt.tecnico.blockchainist.contract.DeleteWalletRequest;
import pt.tecnico.blockchainist.contract.Signature;
import pt.tecnico.blockchainist.contract.SignedCreateWalletRequest;
import pt.tecnico.blockchainist.contract.SignedDeleteWalletRequest;
import pt.tecnico.blockchainist.contract.SignedTransferRequest;
import pt.tecnico.blockchainist.contract.Transaction;
import pt.tecnico.blockchainist.contract.TransferRequest;

/*
NodeTransactionAction:
* - this file creates the transactions and its needed methods
* - main purpose: Not storing in the ledger the TransactionMsg
* - it implements a Hierarchical Design Pattern
*
* - \\ The RestoreTransactionMsg method is Inherited from BaseAction
*           - it allows us to restor our Transaction msg so it can be sent
*
*-- FromGrpc() is the most important part since its the one in charge of creating the specific class that we need to store
*/ 


public abstract class SequencerTransactionAction {

    protected final String clientTxId;

    protected SequencerTransactionAction(String clientTxId) {
        this.clientTxId = clientTxId;
    }

    public String getClientTxId() {
        return clientTxId;
    }
    
    public static SequencerTransactionAction fromGrpc(Transaction msg) {
        if (msg.hasCreateWallet()) return new WalletTransactionAction(msg);
        if (msg.hasDeleteWallet()) return new DeleteWalletTransactionAction(msg);
        if (msg.hasTransfer()) return new TransferTransactionAction(msg);
        throw new IllegalArgumentException("Unknown transaction type");
    }

    public abstract Transaction restoreTransactionMsg();
}


// Object to store the WalletCreation Transaction on the ledger
class WalletTransactionAction extends SequencerTransactionAction {

    private final String userId;
    private final String walletId;
    private final Signature signature;

    public WalletTransactionAction(Transaction msg) {
        super(msg.getCreateWallet().getCreateWalletRequest().getClientTxId());
        this.userId = msg.getCreateWallet().getCreateWalletRequest().getUserId();
        this.walletId = msg.getCreateWallet().getCreateWalletRequest().getWalletId();
        this.signature = msg.getCreateWallet().getSignature();
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
}


// Object to store the DeleteWallet Transaction on the ledger
class DeleteWalletTransactionAction extends SequencerTransactionAction {

    private final String userId;
    private final String walletId;
    private final Signature signature;

    public DeleteWalletTransactionAction(Transaction msg) {
        super(msg.getDeleteWallet().getDeleteWalletRequest().getClientTxId());
        this.userId = msg.getDeleteWallet().getDeleteWalletRequest().getUserId();
        this.walletId = msg.getDeleteWallet().getDeleteWalletRequest().getWalletId();
        this.signature = msg.getDeleteWallet().getSignature();
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
}


// Object to store the Transfer Transaction on the ledger
class TransferTransactionAction extends SequencerTransactionAction {

    private final String srcUserId;
    private final String srcWalletId;
    private final String dstWalletId;
    private final long amount;
    private final Signature signature;

    public TransferTransactionAction(Transaction msg) {
        super(msg.getTransfer().getTransferRequest().getClientTxId());
        TransferRequest t = msg.getTransfer().getTransferRequest();
        this.srcUserId = t.getSrcUserId();
        this.srcWalletId = t.getSrcWalletId();
        this.dstWalletId = t.getDstWalletId();
        this.amount = t.getValue();
        this.signature = msg.getTransfer().getSignature();
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
}

