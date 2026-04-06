package pt.tecnico.blockchainist.node.grpc;

import java.util.List;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import pt.tecnico.blockchainist.contract.CreateWalletResponse;
import pt.tecnico.blockchainist.contract.DeleteWalletResponse;
import pt.tecnico.blockchainist.contract.GetBlockchainStateRequest;
import pt.tecnico.blockchainist.contract.GetBlockchainStateResponse;
import pt.tecnico.blockchainist.contract.NodeServiceGrpc;
import pt.tecnico.blockchainist.contract.ReadBalanceRequest;
import pt.tecnico.blockchainist.contract.ReadBalanceResponse;
import pt.tecnico.blockchainist.contract.SignedCreateWalletRequest;
import pt.tecnico.blockchainist.contract.SignedDeleteWalletRequest;
import pt.tecnico.blockchainist.contract.SignedTransferRequest;
import pt.tecnico.blockchainist.contract.Transaction;
import pt.tecnico.blockchainist.contract.TransferResponse;
import pt.tecnico.blockchainist.node.InitKeys;
import pt.tecnico.blockchainist.node.TransactionAction.NodeTransactionAction;
import pt.tecnico.blockchainist.node.domain.NodeState;
import pt.tecnico.blockchainist.node.domain.NodeState.Wallet;
import utils.GlobalUtil;


public class NodeClientService extends NodeServiceGrpc.NodeServiceImplBase {
    
    private NodeState state;
    private NodeSequencerService sequencer;
    private InitKeys validClients;


    public NodeClientService(NodeState state, NodeSequencerService sequencer, InitKeys validClients) {
        this.state = state;
        this.sequencer = sequencer;
        this.validClients = validClients;
    }

    /**
     *  Handles the gRPC request to create a new wallet, checking for duplicate clientTxIds, validating the arguments, 
     *  broadcasting the transaction, then waiting for the transaction to be executed before responding to the client.
    */
    @Override
    public void createWallet(SignedCreateWalletRequest request, StreamObserver<CreateWalletResponse> responseObserver) {
        
        GlobalUtil.debug("Received createWallet request for user " + request.getCreateWalletRequest().getUserId() + " and wallet " + request.getCreateWalletRequest().getWalletId());
        
        try {
            if (!validClients.verifySignature(request.getCreateWalletRequest().getUserId(), request.getCreateWalletRequest().toByteArray(), request.getSignature().getSignatureValue().toByteArray())) {
                responseObserver.onError(Status.INVALID_ARGUMENT
                .withDescription("Invalid signature for user: " + request.getCreateWalletRequest().getUserId())
                .asRuntimeException());
                return;
            }
            GlobalUtil.debug("Valid signature for user: " + request.getCreateWalletRequest().getUserId());

            Transaction transaction = Transaction.newBuilder()
                                    .setCreateWallet(request)
                                    .build();

            NodeTransactionAction transactionAction = NodeTransactionAction.fromGrpc(transaction);
            String clientTxId = transactionAction.getClientTxId();

            String finalResult = state.checkDuplicates(clientTxId);

            if (finalResult == null) {

                state.testTransactionRequest(transactionAction);
                sequencer.broadcast(transaction);

                state.incrementPendingTransactions();
                synchronized(state.getBlockRetrieverNotifier()) {
                    state.getBlockRetrieverNotifier().notifyAll();
                }

                try {
                    finalResult = state.waitTransaction(clientTxId); 
                } finally {
                    state.decrementPendingTransactions();
                }
            }

            if ("SUCCESS".equals(finalResult)) {
                responseObserver.onNext(CreateWalletResponse.newBuilder().build());
                responseObserver.onCompleted();
            } else {
                responseObserver.onError(Status.INVALID_ARGUMENT
                    .withDescription(finalResult)
                    .asRuntimeException());
            }

        } catch (IllegalArgumentException e) {
            responseObserver.onError(Status.INVALID_ARGUMENT
                .withDescription(e.getMessage())
                .asRuntimeException());
            return;

        } catch (Exception e) {
            responseObserver.onError(Status.INTERNAL
                .withDescription(e.getMessage())
                .asRuntimeException());
        }
    }


    /**
     *  Handles the gRPC request to delete a wallet, checking for duplicate clientTxIds, validating the arguments, 
     *  broadcasting the transaction, then waiting for the transaction to be executed before responding to the client.
     */
    @Override
    public void deleteWallet(SignedDeleteWalletRequest request, StreamObserver<DeleteWalletResponse> responseObserver) {
        
        GlobalUtil.debug("Received deleteWallet request");
        try {
            if (!validClients.verifySignature(request.getDeleteWalletRequest().getUserId(), request.getDeleteWalletRequest().toByteArray(), request.getSignature().getSignatureValue().toByteArray())) {
                responseObserver.onError(Status.INVALID_ARGUMENT
                .withDescription("Invalid signature for user: " + request.getDeleteWalletRequest().getUserId())
                .asRuntimeException());
            }
            GlobalUtil.debug("Valid signature for user: " + request.getDeleteWalletRequest().getUserId());


            Transaction transaction = Transaction.newBuilder()
                                    .setDeleteWallet(request)
                                    .build();

            NodeTransactionAction transactionAction = NodeTransactionAction.fromGrpc(transaction);
            String clientTxId = transactionAction.getClientTxId();

            String finalResult = state.checkDuplicates(clientTxId);

            if (finalResult == null) {
                String walletId = request.getDeleteWalletRequest().getWalletId();

                Wallet wallet = state.getWallet(walletId);
                if (wallet == null) {
                    throw new IllegalArgumentException("Wallet " + walletId + " does not exist.");
                }

                Object walletLock = wallet.getLock();
                synchronized(walletLock) {
                    state.testTransactionRequest(transactionAction);
                    wallet.incrementPendingCount();

                    sequencer.broadcast(transaction);
                    
                    state.incrementPendingTransactions();
                    synchronized(state.getBlockRetrieverNotifier()) {
                        state.getBlockRetrieverNotifier().notifyAll();
                    }
                }
                try {
                    finalResult = state.waitTransaction(clientTxId); 
                } finally {
                    state.decrementPendingTransactions();

                    synchronized(walletLock) {
                        wallet.decrementPendingCount();
                    }
                }
            }

            if ("SUCCESS".equals(finalResult)) {
                responseObserver.onNext(DeleteWalletResponse.newBuilder().build());
                responseObserver.onCompleted();
            } else {
                responseObserver.onError(Status.INVALID_ARGUMENT
                    .withDescription(finalResult)
                    .asRuntimeException());
            }

        } catch (IllegalArgumentException e) {
            responseObserver.onError(Status.INVALID_ARGUMENT
            .withDescription(e.getMessage())
            .asRuntimeException());
        } catch (Exception e) {
            responseObserver.onError(Status.INTERNAL
            .withDescription(e.getMessage())
            .asRuntimeException());
        }
    }


    /**
     *  Handles the gRPC request to transfer funds between wallets, checking for duplicate clientTxIds, validating the arguments, 
     *  broadcasting the transaction, then waiting for the transaction to be executed before responding to the client.
            - local execution only if:
             * 1: Both wallets are local (same organization)
             * 2: Neither wallet has pending transactions
             * 3: Source has sufficient provisional balance 
    */
    @Override
    public void transfer(SignedTransferRequest request, StreamObserver<TransferResponse> responseObserver) {
 
        GlobalUtil.debug("Received transfer request");
        try {
            if (!validClients.verifySignature(request.getTransferRequest().getSrcUserId(), request.getTransferRequest().toByteArray(), request.getSignature().getSignatureValue().toByteArray())) {
                    responseObserver.onError(Status.INVALID_ARGUMENT
                    .withDescription("Invalid signature for user: " + request.getTransferRequest().getSrcUserId())
                    .asRuntimeException());
            }  
            GlobalUtil.debug("Valid signature for user: " + request.getTransferRequest().getSrcUserId());

            Transaction transaction = Transaction.newBuilder()
                                    .setTransfer(request)
                                    .build();
 
            NodeTransactionAction transactionAction = NodeTransactionAction.fromGrpc(transaction);

            String clientTxId = transactionAction.getClientTxId();
            String finalResult = state.checkDuplicates(clientTxId);
            
            if (finalResult == null) {
 
                // String userId = request.getTransferRequest().getSrcUserId();
                String srcWalletId = request.getTransferRequest().getSrcWalletId();
                String dstWalletId = request.getTransferRequest().getDstWalletId();
                long amount = request.getTransferRequest().getValue();
 
                Wallet src = state.getWallet(srcWalletId);
                Wallet dst = state.getWallet(dstWalletId);
                if (src == null) {
                    throw new IllegalArgumentException("Wallet " + srcWalletId + " doesnt exist");
                }
                if (dst == null) {
                    throw new IllegalArgumentException("Wallet " + dstWalletId + " doesnt exist");
                }

                state.testTransactionRequest(transactionAction);

                boolean isDstLocal = dst.isLocalWallet();
 
                boolean isSourcePending = src.isPending();
                boolean isDestPending = dst.isPending();
                boolean hasNoPending = !isSourcePending && !isDestPending;
 
                Object firsLock = srcWalletId.compareTo(dstWalletId) < 0 ? src.getLock() : dst.getLock();
                Object secondLock = srcWalletId.compareTo(dstWalletId) < 0 ? dst.getLock() : src.getLock();
 
                boolean fastExecutionTriggered = false;

                synchronized(firsLock) {
                    synchronized(secondLock) {
 
                        if (isDstLocal && hasNoPending && src.getProvisionalBalance() >= amount) {
                            GlobalUtil.debug("Fast Transfer");
                            src.decrementProvisionalBalance(amount);
                            dst.addProvisionalBalance(amount);

                            fastExecutionTriggered = true;
                            finalResult = "SUCCESS";
                            state.setTransactionTxResult(clientTxId, finalResult); 

                            sequencer.broadcast(transaction);
                        
                        } else {
                            GlobalUtil.debug("Slow Transfer");
                            src.decrementProvisionalBalance(amount);

                            sequencer.broadcast(transaction);
                            
                            state.incrementPendingTransactions();
                            synchronized(state.getBlockRetrieverNotifier()) {
                                state.getBlockRetrieverNotifier().notifyAll();
                            }
                        }
                    }
                }
                if (!fastExecutionTriggered) {
                    try {
                        finalResult = state.waitTransaction(clientTxId);
                    } finally {
                        synchronized(firsLock) {
                            synchronized(secondLock) {
                                state.decrementPendingTransactions();
                            }
                        }
                    }
                }
            }

            if ("SUCCESS".equals(finalResult)) {
                responseObserver.onNext(TransferResponse.newBuilder().build());
                responseObserver.onCompleted();
            } else {
                responseObserver.onError(Status.INVALID_ARGUMENT
                    .withDescription(finalResult)
                    .asRuntimeException());
            }
 
        } catch (IllegalArgumentException e) {
            responseObserver.onError(Status.INVALID_ARGUMENT
            .withDescription(e.getMessage())
            .asRuntimeException());
        } catch (Exception e) {
            responseObserver.onError(Status.INTERNAL
            .withDescription(e.getMessage())
            .asRuntimeException());
        }
    }

    
    /**
     *  Handles the gRPC request to read the balance from a wallets, validating the arguments
     *  and obtaining the balance from the node, before responding to the client.
     */
    @Override
    public void readBalance(ReadBalanceRequest request, StreamObserver<ReadBalanceResponse> responseObserver) {

        GlobalUtil.debug("Received readBalance request");
        try {
            state.testReadRequest(request.getWalletId());

            long balance = this.state.readBalance(request.getWalletId());

            ReadBalanceResponse response = ReadBalanceResponse.newBuilder().setBalance(balance).build();
        
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (IllegalArgumentException e) {
            responseObserver.onError(Status.INVALID_ARGUMENT
            .withDescription(e.getMessage())
            .asRuntimeException());
        } catch (Exception e) {
            responseObserver.onError(Status.INTERNAL
            .withDescription(e.getMessage())
            .asRuntimeException());
        }
    }


    /**
     *  Handles the gRPC request to get the current transaction ledger 
     *  obtaining the transaction ledger from the node before responding to the client.
     */
    @Override
    public void getBlockchainState(GetBlockchainStateRequest request, StreamObserver<GetBlockchainStateResponse> responseObserver) {
        GlobalUtil.debug("Received getBlockchainState request");
        try {

            List<Transaction> transactions = state.getBlockchainState();

            GetBlockchainStateResponse response = GetBlockchainStateResponse.newBuilder()
                                                .addAllTransactions(transactions)
                                                .build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(Status.INTERNAL
            .withDescription(e.getMessage())
            .asRuntimeException());
        }
    }
}
