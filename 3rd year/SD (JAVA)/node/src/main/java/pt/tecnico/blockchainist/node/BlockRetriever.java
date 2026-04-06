package pt.tecnico.blockchainist.node;
import java.io.InputStream;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;

import pt.tecnico.blockchainist.contract.Block;
import pt.tecnico.blockchainist.contract.DeliverBlockResponse;
import pt.tecnico.blockchainist.contract.SignedBlockResponse;
import pt.tecnico.blockchainist.node.domain.NodeState;
import pt.tecnico.blockchainist.node.grpc.NodeSequencerService;
import utils.GlobalUtil;


public class BlockRetriever extends Thread {
    
    private final NodeSequencerService sequencerService;
    private final NodeState state;
    private int nextBlockId = 1;
    private boolean first_iter = true;
    private final Object syncLock;
    private double avgTime = 500;

    private final PublicKey publicKey;

    
    public BlockRetriever(NodeSequencerService sequencerService, NodeState state, Object lock) {
        this.sequencerService = sequencerService;
        this.state = state;
        this.syncLock = lock;
        try {
            this.publicKey = InitKeys.loadPublicKey("seq/Seq.pub");
        } catch (Exception e) {
            throw new RuntimeException("Failed to load public key", e);
        }
    } 
    
    
    @Override
    public void run() {
        GlobalUtil.debug("starting block synchronization loop...");

        try {
            java.security.Signature sig = java.security.Signature.getInstance("SHA256withRSA");
            sig.initVerify(publicKey);

                
            while (!isInterrupted()) {

                SignedBlockResponse signedResponse = sequencerService.deliverBlock(nextBlockId);
                if (signedResponse == null) {

                  if (first_iter) {
                      synchronized (syncLock) {
                          this.first_iter = false;
                          GlobalUtil.debug("Initial sync completed with sequencer"); 
                          syncLock.notifyAll();
                    }
                  }

                  try {
                    synchronized(state.getBlockRetrieverNotifier()) {
                      long waitTime;
                      
                      if (state.hasPendingTransactions()) {
                          waitTime = (long) (avgTime * 0.5);
                      } else {
                          waitTime = (long) avgTime;
                      }
                      if (waitTime < 500) waitTime = 500;
                      state.getBlockRetrieverNotifier().wait(waitTime);
                    }
                  } catch (InterruptedException e) {
                    interrupt(); 
                  }
                  continue;
                }

                DeliverBlockResponse response = signedResponse.getBlockResponse();
                pt.tecnico.blockchainist.contract.Signature receivedSignature = signedResponse.getSignature();
                
                sig.update(response.toByteArray());
                boolean isValid = sig.verify(receivedSignature.getSignatureValue().toByteArray());
                
                // will just send right away, in case the message was intercepted
                if (response !=null && !isValid) {
                    GlobalUtil.debug("Response from unkown identity\n Ignoring...");
                    continue;
                }

                if (response != null && response.hasBlock()) {
                    this.avgTime = response.getAvgTime();
                    Block block = response.getBlock();
                    GlobalUtil.debug("Block " + nextBlockId + " received");
                    
                    this.state.executeTransactions(block);
                    this.nextBlockId++;
                }
            }
        } catch (Exception e) {
            GlobalUtil.debug("Error in BlockRetriever: " + e.getMessage());
        }
    }
}
