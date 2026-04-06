package pt.tecnico.blockchainist.sequencer.grpc;

import java.io.InputStream;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;

import com.google.protobuf.ByteString;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import pt.tecnico.blockchainist.contract.Block;
import pt.tecnico.blockchainist.contract.BroadcastRequest;
import pt.tecnico.blockchainist.contract.BroadcastResponse;
import pt.tecnico.blockchainist.contract.DeliverBlockRequest;
import pt.tecnico.blockchainist.contract.DeliverBlockResponse;
import pt.tecnico.blockchainist.contract.SequencerServiceGrpc;
import pt.tecnico.blockchainist.contract.Signature;
import pt.tecnico.blockchainist.contract.SignedBlockResponse;
import pt.tecnico.blockchainist.contract.Transaction;
import pt.tecnico.blockchainist.sequencer.domain.SequencerState;
import utils.GlobalUtil;




public class SequencerNodeService extends SequencerServiceGrpc.SequencerServiceImplBase {

    private SequencerState state;
    private PrivateKey privateKey;

    // constructor that injects the state into the gRPC service
    public SequencerNodeService(SequencerState state) {
      this.state = state;
      try {
        this.privateKey = loadPrivateKey("Seq.priv");
      } catch (Exception e) {
          System.err.println("Failed to load private key: " + e.getMessage());
          e.printStackTrace();
      }
    }

    public byte[] readResource(String path) throws Exception {
      try (InputStream is = getClass().getClassLoader().getResourceAsStream(path)) {
        if (is == null)
          throw new IllegalArgumentException("File not found: " + path);
        return is.readAllBytes();
      }
    }

    public PrivateKey loadPrivateKey(String resourcePath) throws Exception {
      byte[] keyBytes = readResource(resourcePath);
      PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
      KeyFactory kf = KeyFactory.getInstance("RSA");
      return kf.generatePrivate(spec);
    }

    /**
    * gRPC service implementation for the Sequencer-Node
    * handles incoming requests to sequence new transactions and deliver existing ones
    */
    @Override
    public void broadcast(BroadcastRequest request, StreamObserver<BroadcastResponse> responseObserver) {

        GlobalUtil.debug("Broadcasting in sequencer");

        Transaction transaction = request.getTransaction();
        // stores transaction in ledger
        this.state.addTransaction(transaction);

        BroadcastResponse response = BroadcastResponse.newBuilder()
                                                      .build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
 
    /**
     * Handles the gRPC request to deliver a specific block by its ID, returning the requested block and the average assembly time.
    */
    @Override
    public void deliverBlock(DeliverBlockRequest request, StreamObserver<SignedBlockResponse> responseObserver) {

      GlobalUtil.debug("Delivering a block");

      int block_id = request.getBlockId();

      Block block = this.state.retriveBlock(block_id);

      if (block == null) {

        responseObserver.onError(Status.NOT_FOUND
            .withDescription("Block with id " + block_id + " doesnt exist")
            .asRuntimeException());
        return;
      }

      DeliverBlockResponse response = DeliverBlockResponse.newBuilder()
                                                          .setBlock(block)
                                                          .setAvgTime(this.state.getAverageTime())
                                                          .build();

      // TODO: maybe the error should be returned with the signature, to verify if it was the server replying
      try {
        java.security.Signature sig = java.security.Signature.getInstance("SHA256withRSA");
  
        sig.initSign(this.privateKey);

        sig.update(response.toByteArray());
        byte[] signatureBytes = sig.sign();

        Signature signature = Signature.newBuilder()
                                      .setSignatureValue(ByteString.copyFrom((signatureBytes)))
                                      .build();
        SignedBlockResponse signedBlockResponse = SignedBlockResponse.newBuilder()
                                                                    .setBlockResponse(response)                          
                                                                    .setSignature(signature)
                                                                    .build();
        responseObserver.onNext(signedBlockResponse);
        responseObserver.onCompleted();

      } catch (Exception e) {
        GlobalUtil.debug("Error signing block: " + e.getMessage());
        responseObserver.onError(Status.INTERNAL
                        .withDescription("Failed to sign the block response")
                        .asRuntimeException());
      }
    }

}
