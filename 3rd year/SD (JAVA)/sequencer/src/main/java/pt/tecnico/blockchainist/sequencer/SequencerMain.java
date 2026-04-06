package pt.tecnico.blockchainist.sequencer;

import utils.GlobalUtil;

import pt.tecnico.blockchainist.sequencer.domain.SequencerState;
import pt.tecnico.blockchainist.sequencer.grpc.*;

import java.io.IOException;


import io.grpc.BindableService;
import io.grpc.Server;
import io.grpc.ServerBuilder;




public class SequencerMain {

    public static void main(String[] args) {

        GlobalUtil.debug(SequencerMain.class.getSimpleName());

        if (args.length != 1) {
            GlobalUtil.debug("Wrong format");
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
        if (port > 65535 || port < 0) {
            GlobalUtil.debug("Port number out of range (0-65535): " + port);
            return;
        } 

        SequencerState sequencerState = new SequencerState();

        BindableService impl = new SequencerNodeService(sequencerState);

        Server sequencer = ServerBuilder.forPort(port)
                           .addService(impl)
                           .build();
        try {
            sequencer.start();
            // sequencerState.startBlockTimeOut();
        } catch (IOException e) {
            GlobalUtil.debug("Caught IOException when starting the sequencer");
            return;
        }

        System.out.println("Sequencer started working sucessfuly");

        try {
            sequencer.awaitTermination();
        } catch (InterruptedException e) {
            GlobalUtil.debug("Caught InterruptedException in SequencerMain");
        }
    }



    private static void printUsage() {
        GlobalUtil.debug("Usage: mvn exec:java -Dexec.args=\"<port>\"");
    }

}
