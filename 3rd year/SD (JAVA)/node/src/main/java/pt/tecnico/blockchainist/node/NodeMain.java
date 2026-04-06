package pt.tecnico.blockchainist.node;

import java.io.IOException;

import io.grpc.BindableService;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.ServerInterceptors;
import pt.tecnico.blockchainist.node.domain.NodeState;
import pt.tecnico.blockchainist.node.grpc.HeaderNodeInterceptor;
import pt.tecnico.blockchainist.node.grpc.NodeClientService;
import pt.tecnico.blockchainist.node.grpc.NodeSequencerService;
import utils.GlobalUtil;




public class NodeMain {

  public static void main(String[] args) {

    GlobalUtil.debug(NodeMain.class.getSimpleName());

    if (args.length != 3) {
      GlobalUtil.debug("Argument(s) missing!");
      printUsage();
      return;
    }

    int port = -1;

    try {
      port = Integer.parseInt(args[0]);

    } catch (NumberFormatException e) {
      GlobalUtil.debug("Invalid port");
      printUsage();
      return;
    }
    final String organization = args[1];
    final String sequencer_address = args[2];
    
    if (port > 65535 || port < 0) {
      GlobalUtil.debug("Port number out of range (0-65535): " + port);
      return;
    } 

    String[] split = args[2].split(":");
    if (split.length != 2) {
      GlobalUtil.debug("Invalid argument: " + args[2]);
      printUsage();
      return;
    }

    String sequencer = split[0];
    int port_tmp = -1;
    try {
      port_tmp = Integer.parseInt(split[1]);
    } catch (NumberFormatException e) {
      GlobalUtil.debug("Invalid port (" + split[1] + ") in argument: " + args[2]);
      printUsage();
      return;
    }
    String port_sequencer = split[1];
    
    Object syncLock = new Object();
    InitKeys validClients = new InitKeys(organization);
    NodeState nodeState = new NodeState(validClients);
    NodeSequencerService seq = new NodeSequencerService(nodeState, sequencer, port_sequencer);

    // stating the thread that will, in 1sec, intervals fetch blocks
    BlockRetriever retriever = new BlockRetriever(seq, nodeState, syncLock);
    retriever.start();
    
    // waiting for the node to fetch the current sequencer state before starting to process client requests
    synchronized (syncLock) {
      GlobalUtil.debug("Waiting for the node initialization");
      try {
        syncLock.wait();
      } catch (InterruptedException e) {}
    }

    BindableService impl = new NodeClientService(nodeState, seq, validClients);

    Server node = ServerBuilder.forPort(port)
                  .addService(ServerInterceptors.intercept(impl, new HeaderNodeInterceptor()))
                  .build();

    try {
      node.start();

    } catch (IOException e) {
      GlobalUtil.debug("failed to start node" + e.getMessage());
      return;
    }

    System.out.println("Node started working for " + organization);
    
    try {
      node.awaitTermination();
    } catch (InterruptedException e) {
      GlobalUtil.debug("Caught InterruptedException in NodeMain" + e.getMessage());
      return;
    }
  
  }

  private static void printUsage() {
    GlobalUtil.debug("Usage: mvn exec:java -Dexec.args=\"<port> <organization> <sequencer_address>\"");
  }

}
