package pt.tecnico.blockchainist.node.grpc;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;

import pt.tecnico.blockchainist.contract.BroadcastRequest;
import pt.tecnico.blockchainist.contract.DeliverBlockRequest;
import pt.tecnico.blockchainist.contract.SignedBlockResponse;
import pt.tecnico.blockchainist.contract.SequencerServiceGrpc;
import pt.tecnico.blockchainist.contract.Transaction;

import pt.tecnico.blockchainist.node.domain.NodeState;
import utils.GlobalUtil;




public class NodeSequencerService {

  private final NodeState state;
  private final ManagedChannel channel;
  private final SequencerServiceGrpc.SequencerServiceBlockingStub stub;

  
  public NodeSequencerService(NodeState state, String host, String port) {

    GlobalUtil.debug("Initializing NodeSequencerService");
    
    final String sequencer = host + ":" + port;
    this.channel = ManagedChannelBuilder.forTarget(sequencer).usePlaintext().build();
    this.stub = SequencerServiceGrpc.newBlockingStub(this.channel);
    this.state = state;
  } 


  /**
   * Handles the gRPC request to broadcast a transaction by sending it to the sequencer and returning the sequence number for that transaction.
  */
  public void broadcast(Transaction transaction) {

    GlobalUtil.debug("Broadcasting transaction");
    
    try {
      BroadcastRequest request = BroadcastRequest.newBuilder()
                                 .setTransaction(transaction)
                                 .build();
      this.stub.broadcast(request);

    } catch (StatusRuntimeException e) {
      GlobalUtil.debug("Failed to broadcast transaction in NodeSequencerService: " +
                        e.getStatus().getDescription());
    }
  }


  /**
   * Handles the gRPC request to fetch a specific block by sending its block ID to the sequencer and returning the corresponding block response
  */
  public SignedBlockResponse deliverBlock(int block_id) {

    GlobalUtil.debug("Fetching block " + block_id + " from sequencer...");
    try {
      DeliverBlockRequest request = DeliverBlockRequest.newBuilder()
                                                       .setBlockId(block_id)
                                                       .build();
      return this.stub.deliverBlock(request);

    } catch (StatusRuntimeException e) {
      GlobalUtil.debug("Failed to retrive Block from sequencer: " +
                          e.getStatus().getDescription());
      return null;
    }
  }

}
