package pt.tecnico.blockchainist.client.grpc;

import java.io.InputStream;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.google.protobuf.ByteString;
import com.google.protobuf.Message;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Metadata;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.MetadataUtils;
import io.grpc.stub.StreamObserver;
import pt.tecnico.blockchainist.contract.CreateWalletRequest;
import pt.tecnico.blockchainist.contract.CreateWalletResponse;
import pt.tecnico.blockchainist.contract.DeleteWalletRequest;
import pt.tecnico.blockchainist.contract.DeleteWalletResponse;
import pt.tecnico.blockchainist.contract.GetBlockchainStateRequest;
import pt.tecnico.blockchainist.contract.GetBlockchainStateResponse;
import pt.tecnico.blockchainist.contract.NodeServiceGrpc;
import pt.tecnico.blockchainist.contract.ReadBalanceRequest;
import pt.tecnico.blockchainist.contract.ReadBalanceResponse;
import pt.tecnico.blockchainist.contract.Signature;
import pt.tecnico.blockchainist.contract.SignedCreateWalletRequest;
import pt.tecnico.blockchainist.contract.SignedDeleteWalletRequest;
import pt.tecnico.blockchainist.contract.SignedTransferRequest;
import pt.tecnico.blockchainist.contract.Transaction;
import pt.tecnico.blockchainist.contract.TransferRequest;
import pt.tecnico.blockchainist.contract.TransferResponse;



public class ClientNodeService {

  private final ManagedChannel channel;
  private final NodeServiceGrpc.NodeServiceBlockingStub stub;
  private final NodeServiceGrpc.NodeServiceStub asyncStub;
  private PrivateKey privateKey;

  // Ensure that the name of the header key to match the header key used by the sender.
   static final Metadata.Key<String> MY_HEADER_KEY =
      Metadata.Key.of("node-delay", Metadata.ASCII_STRING_MARSHALLER);

  /**
   * constructor for the ClientNodeService class, 
   * which initializes the gRPC channel and stub to communicate with the NodeService.
   */
  public ClientNodeService(String host, int port, String organization) {

    final String target = host + ":" + port;  
    this.channel = ManagedChannelBuilder.forTarget(target).usePlaintext().build();
    this.stub = NodeServiceGrpc.newBlockingStub(this.channel);
    this.asyncStub = NodeServiceGrpc.newStub(this.channel);
  }

  public byte[] readResource(String path) throws Exception {
      try (InputStream is = getClass().getClassLoader().getResourceAsStream(path)) {
        if (is == null)
          throw new RuntimeException("File not found: " + path);
        return is.readAllBytes();
      } catch (RuntimeException e) {
        throw e;
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }

    public PrivateKey loadPrivateKey(String resourcePath) throws Exception {
      byte[] keyBytes = readResource(resourcePath);
      PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
      KeyFactory kf = KeyFactory.getInstance("RSA");
      return kf.generatePrivate(spec);
    }

    public Signature signRequest(String userId, Message request) throws Exception {
      try {
        this.privateKey = loadPrivateKey(userId + ".priv");
        java.security.Signature sig = java.security.Signature.getInstance("SHA256withRSA");
        sig.initSign(this.privateKey);
        sig.update(request.toByteArray());
        byte[] signatureBytes = sig.sign();

        Signature signature = Signature.newBuilder()
              .setSignatureValue(ByteString.copyFrom(signatureBytes))
              .build();
        return signature;
      } catch (RuntimeException e) {
        throw e;
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }

  /**
   * Creates a new wallet for a user by 
   * sending a CreateWalletRequest to the NodeService and returns the response as a string.
   */
  public String createWallet(String userId, String walletId, String nodeDelay, String clientTxId) {


    CreateWalletRequest request = CreateWalletRequest.newBuilder()
                                    .setUserId(userId)
                                    .setWalletId(walletId)
                                    .setClientTxId(clientTxId)
                                    .build();
    try {
        SignedCreateWalletRequest signedRequest = SignedCreateWalletRequest.newBuilder()
                                    .setCreateWalletRequest(request)
                                    .setSignature(signRequest(userId, request))
                                    .build();

      Metadata metadata = new Metadata();
      metadata.put(MY_HEADER_KEY, nodeDelay);

      NodeServiceGrpc.NodeServiceBlockingStub stubWithMetadata = 
        this.stub.withInterceptors(MetadataUtils.newAttachHeadersInterceptor(metadata));

      CreateWalletResponse response = stubWithMetadata.withDeadlineAfter(20, TimeUnit.SECONDS).createWallet(signedRequest);
      return response.toString();
      } catch (StatusRuntimeException e) {
        throw e;
      } catch (RuntimeException e) {
        throw e;
      } catch (Exception e) {
        throw Status.INTERNAL.withDescription("Error: " + e.getMessage()).asRuntimeException();
      }
  }

  /**
   * Asynchronous version of the createWallet method, 
   * which sends a CreateWalletRequest to the NodeService and handles the response using a StreamObserver.
   */
  public void createWalletAsync(String userId, String walletId, String nodeDelay, String clientTxId, StreamObserver<CreateWalletResponse> responseObserver) {

      CreateWalletRequest request = CreateWalletRequest.newBuilder()
                                    .setUserId(userId)
                                    .setWalletId(walletId)
                                    .setClientTxId(clientTxId)
                                    .build();

      try {
        SignedCreateWalletRequest signedRequest = SignedCreateWalletRequest.newBuilder()
                                        .setCreateWalletRequest(request)
                                        .setSignature(signRequest(userId, request))
                                        .build();
        
        Metadata metadata = new Metadata();
        metadata.put(MY_HEADER_KEY, nodeDelay);
        NodeServiceGrpc.NodeServiceStub asyncStubWithMetadata = 
          this.asyncStub.withInterceptors(MetadataUtils.newAttachHeadersInterceptor(metadata));

        asyncStubWithMetadata.withDeadlineAfter(20, TimeUnit.SECONDS).createWallet(signedRequest, responseObserver);
      } catch (Exception e) {
          responseObserver.onError(Status.INTERNAL
            .withDescription("Error: " + e.getMessage())
            .withCause(e)
            .asRuntimeException());
      }
  }

  /**
   * Deletes a wallet for a user by 
   * sending a DeleteWalletRequest to the NodeService and returns the response as a string.
   */
  public String deleteWallet(String userId, String walletId,String nodeDelay, String clientTxId) {

    DeleteWalletRequest request = DeleteWalletRequest.newBuilder()
                                  .setUserId(userId)
                                  .setWalletId(walletId)
                                  .setClientTxId(clientTxId)
                                  .build();
    try {
        SignedDeleteWalletRequest signedRequest = SignedDeleteWalletRequest.newBuilder()
                                    .setDeleteWalletRequest(request)
                                    .setSignature(signRequest(userId, request))
                                    .build();

      Metadata metadata = new Metadata();
      metadata.put(MY_HEADER_KEY, nodeDelay);

      NodeServiceGrpc.NodeServiceBlockingStub stubWithMetadata = 
        this.stub.withInterceptors(MetadataUtils.newAttachHeadersInterceptor(metadata));

      DeleteWalletResponse response = stubWithMetadata.withDeadlineAfter(20, TimeUnit.SECONDS).deleteWallet(signedRequest);
      return response.toString();
      } catch (StatusRuntimeException e) {
        throw e;
      } catch (Exception e) {
        throw Status.INTERNAL.withDescription("Error: " + e.getMessage()).asRuntimeException();
      }
  }
  /**
   * Asynchronous version of the deleteWallet method, 
   * which sends a DeleteWalletRequest to the NodeService and handles the response using a StreamObserver.
   */
  public void deleteWalletAsync(String userId, String walletId, String nodeDelay, String clientTxId, StreamObserver<DeleteWalletResponse> responseObserver) {

    DeleteWalletRequest request = DeleteWalletRequest.newBuilder()
                                  .setUserId(userId)
                                  .setWalletId(walletId)
                                  .setClientTxId(clientTxId)
                                  .build();
      try {
        SignedDeleteWalletRequest signedRequest = SignedDeleteWalletRequest.newBuilder()
                                        .setDeleteWalletRequest(request)
                                        .setSignature(signRequest(userId, request))
                                        .build();
        
        Metadata metadata = new Metadata();
        metadata.put(MY_HEADER_KEY, nodeDelay);
        NodeServiceGrpc.NodeServiceStub asyncStubWithMetadata = 
          this.asyncStub.withInterceptors(MetadataUtils.newAttachHeadersInterceptor(metadata));

        asyncStubWithMetadata.withDeadlineAfter(20, TimeUnit.SECONDS).deleteWallet(signedRequest, responseObserver);
      } catch (Exception e) {
          responseObserver.onError(Status.INTERNAL
            .withDescription("Error: " + e.getMessage())
            .withCause(e)
            .asRuntimeException());
      }
  }

  /**
   * Reads the balance of a wallet by 
   * sending a ReadBalanceRequest to the NodeService and returns the response as a string.
   */
  public String readBalance(String walletId,String nodeDelay) {

    ReadBalanceRequest request = ReadBalanceRequest.newBuilder()
                                  .setWalletId(walletId)
                                  .build();
                              
    Metadata metadata = new Metadata();
    metadata.put(MY_HEADER_KEY, nodeDelay);
    NodeServiceGrpc.NodeServiceBlockingStub stubWithMetadata = 
      this.stub.withInterceptors(MetadataUtils.newAttachHeadersInterceptor(metadata));
                                
    ReadBalanceResponse response = stubWithMetadata.withDeadlineAfter(20, TimeUnit.SECONDS).readBalance(request);
    return String.valueOf(response.getBalance());
  }

  /**
   * Asynchronous version of the readBalance method, 
   * which sends a ReadBalanceRequest to the NodeService and handles the response using a StreamObserver.
   */
  public void readBalanceAsync(String walletId,String nodeDelay, StreamObserver<ReadBalanceResponse> responseObserver) {

    ReadBalanceRequest request = ReadBalanceRequest.newBuilder()
                                  .setWalletId(walletId)
                                  .build();

    Metadata metadata = new Metadata();
    metadata.put(MY_HEADER_KEY, nodeDelay);
    NodeServiceGrpc.NodeServiceStub asyncStubWithMetadata = 
      this.asyncStub.withInterceptors(MetadataUtils.newAttachHeadersInterceptor(metadata));

    asyncStubWithMetadata.withDeadlineAfter(20, TimeUnit.SECONDS).readBalance(request, responseObserver);
  }

  /**
   * Transfers an amount from a source wallet to a destination wallet by 
   * sending a TransferRequest to the NodeService and returns the response as a string.
   */
  public String transfer(String sourceUserId, String sourceWalletId, String destinationWalletId, Long amount, String nodeDelay, String clientTxId) {

    TransferRequest request = TransferRequest.newBuilder()
                              .setSrcUserId(sourceUserId)
                              .setSrcWalletId(sourceWalletId)
                              .setDstWalletId(destinationWalletId)
                              .setValue(amount)
                              .setClientTxId(clientTxId)
                              .build();
    try {
        SignedTransferRequest signedRequest = SignedTransferRequest.newBuilder()
                                    .setTransferRequest(request)
                                    .setSignature(signRequest(sourceUserId, request))
                                    .build();

      Metadata metadata = new Metadata();
      metadata.put(MY_HEADER_KEY, nodeDelay);

      NodeServiceGrpc.NodeServiceBlockingStub stubWithMetadata = 
        this.stub.withInterceptors(MetadataUtils.newAttachHeadersInterceptor(metadata));

      TransferResponse response = stubWithMetadata.withDeadlineAfter(20, TimeUnit.SECONDS).transfer(signedRequest);
      return response.toString();
      } catch (StatusRuntimeException e) {
        throw e;
      } catch (Exception e) {
        throw Status.INTERNAL.withDescription("Error: " + e.getMessage()).asRuntimeException();
      }
  }

  public void transferAsync(String sourceUserId, String sourceWalletId, String destinationWalletId, Long amount, String nodeDelay, String clientTxId, StreamObserver<TransferResponse> responseObserver) {

    TransferRequest request = TransferRequest.newBuilder()
                              .setSrcUserId(sourceUserId)
                              .setSrcWalletId(sourceWalletId)
                              .setDstWalletId(destinationWalletId)
                              .setValue(amount)
                              .setClientTxId(clientTxId)
                              .build();

      try {
        SignedTransferRequest signedRequest = SignedTransferRequest.newBuilder()
                                        .setTransferRequest(request)
                                        .setSignature(signRequest(sourceUserId, request))
                                        .build();
        
        Metadata metadata = new Metadata();
        metadata.put(MY_HEADER_KEY, nodeDelay);
        NodeServiceGrpc.NodeServiceStub asyncStubWithMetadata = 
          this.asyncStub.withInterceptors(MetadataUtils.newAttachHeadersInterceptor(metadata));

        asyncStubWithMetadata.withDeadlineAfter(20, TimeUnit.SECONDS).transfer(signedRequest, responseObserver);
      } catch (Exception e) {
           responseObserver.onError(Status.INTERNAL
            .withDescription("Error: " + e.getMessage())
            .withCause(e)
            .asRuntimeException());
      }
  }

  /**
   * Retrieves the current state of the blockchain by 
   * sending a GetBlockchainStateRequest to the NodeService and returns a list of transactions.
   */
  public List<Transaction> getBlockchainState() {

    GetBlockchainStateRequest request = GetBlockchainStateRequest.newBuilder()
                                        .build();
                                                                              
    GetBlockchainStateResponse response = this.stub.withDeadlineAfter(20, TimeUnit.SECONDS).getBlockchainState(request);
    return response.getTransactionsList();
  }

  public void shutdownConnection() {
    this.channel.shutdown();
  }

}
