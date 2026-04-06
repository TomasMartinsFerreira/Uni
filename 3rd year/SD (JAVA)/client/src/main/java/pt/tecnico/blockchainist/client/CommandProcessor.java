package pt.tecnico.blockchainist.client;


import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import pt.tecnico.blockchainist.client.grpc.ClientNodeService;
import pt.tecnico.blockchainist.contract.CreateWalletResponse;
import pt.tecnico.blockchainist.contract.DeleteWalletResponse;
import pt.tecnico.blockchainist.contract.ReadBalanceResponse;
import pt.tecnico.blockchainist.contract.Transaction;
import pt.tecnico.blockchainist.contract.TransferResponse;


public class CommandProcessor {

    private static final String SPACE = " ";
    private static final String CREATE_BLOCKING = "C";
    private static final String CREATE_ASYNC = "c";
    private static final String DELETE_BLOCKING = "E";
    private static final String DELETE_ASYNC = "e";
    private static final String BALANCE_BLOCKING = "S";
    private static final String BALANCE_ASYNC = "s";
    private static final String TRANSFER_BLOCKING = "T";
    private static final String TRANSFER_ASYNC = "t";
    private static final String DEBUG_BLOCKCHAIN_STATE = "B";
    private static final String PAUSE = "P";
    private static final String EXIT = "X";

    private static final Pattern ID_PATTERN = Pattern.compile("^[a-zA-Z0-9]+$");

    private final AtomicLong commandCounter = new AtomicLong(0);
    private final ArrayList<ClientNodeService> nodes;

    boolean exit = false;

    public CommandProcessor(ArrayList<ClientNodeService> nodes) {
        this.nodes = nodes;
    }

    void userInputLoop() {

        Scanner scanner = new Scanner(System.in);

        while (!exit) {
            System.out.print("\n> ");
            String line = scanner.nextLine().trim();
            String[] split = line.split(SPACE);
            try {
                switch (split[0]) {
                    case CREATE_BLOCKING:
                        this.create(split, true);
                        break;

                    case CREATE_ASYNC:
                        this.create(split, false);
                        break;

                    case DELETE_BLOCKING:
                        this.delete(split, true);
                        break;

                    case DELETE_ASYNC:
                        this.delete(split, false);
                        break;

                    case BALANCE_BLOCKING:
                        this.balance(split, true);
                        break;

                    case BALANCE_ASYNC:
                        this.balance(split, false);
                        break;

                    case TRANSFER_BLOCKING:
                        this.transfer(split, true);
                        break;

                    case TRANSFER_ASYNC:
                        this.transfer(split, false);
                        break;

                    case DEBUG_BLOCKCHAIN_STATE:
                        this.debugBlockchainState(split);
                        break;

                    case PAUSE:
                        this.pause(split);
                        break;

                    case EXIT:
                        exit = true;
                        break;

                    default:
                        printUsage();
                        break;
                }
            } catch (IllegalArgumentException e) {
                System.err.println("Error: " + e.getMessage());
                printUsage();
            }
        }
        scanner.close();
    }

    private void create(String[] split, boolean isBlocking) {
        this.checkCreateCommandArgs(split);

        Long commandNumber = this.commandCounter.incrementAndGet();

        String userId = split[1];
        String walletId = split[2];
        Integer nodeIndex = Integer.parseInt(split[3]);
        Integer nodeDelay = Integer.parseInt(split[4]);
        String clientTxId = UUID.randomUUID().toString();

        if (isBlocking) {
            int currentNodeIndex = nodeIndex;
            boolean success = false;
            boolean second_iter = false;
            
            while (!success) {
                if (currentNodeIndex >= this.nodes.size()) {
                    currentNodeIndex = 0;
                    second_iter = true;
                }
                if (second_iter && currentNodeIndex == nodeIndex ) {
                    System.err.println("Error: All nodes have failed or are unavailable.");
                    exit = true; 
                    break;
                }
                
                try {
                    String r = this.nodes.get(currentNodeIndex).createWallet(userId, walletId, nodeDelay.toString(), clientTxId);
                    System.out.println("OK " + commandNumber);
                    System.out.println(r.toString());
                    success = true;
                    
                } catch (StatusRuntimeException e) {
                    Status status = e.getStatus();
                    if (status.getCode() == Status.Code.DEADLINE_EXCEEDED || status.getCode() == Status.Code.UNAVAILABLE) {
                        currentNodeIndex++;
                    } else {
                        System.err.println(status.getDescription());
                        break;
                    }
                } catch (RuntimeException e) {
                    System.err.println("Error: " + e.getMessage());
                    break;
                }
            }
        } else {
            this.nodes.get(nodeIndex).createWalletAsync(userId, walletId,  nodeDelay.toString(), clientTxId, new StreamObserver<CreateWalletResponse>() {
                int currentNodeIndex = nodeIndex;
                boolean second_iter = false;

                @Override
                public void onNext(CreateWalletResponse response) {
                    System.out.println("OK " + commandNumber);
                    System.out.println(response.toString());
                    System.out.print("> ");
                }

                @Override
                public void onError(Throwable throwable) {
                    Status status = Status.fromThrowable(throwable);
                    
                    if (status.getCode() == Status.Code.DEADLINE_EXCEEDED || status.getCode() == Status.Code.UNAVAILABLE) {
                        currentNodeIndex++;
                        
                        if (currentNodeIndex >= nodes.size()) {
                            currentNodeIndex = 0;
                            second_iter = true;
                        }
                        
                        if (second_iter && currentNodeIndex == nodeIndex) {
                            System.err.println("Error: All nodes have failed or are unavailable.");
                            System.out.print("> ");
                        } else {
                            nodes.get(currentNodeIndex).createWalletAsync(userId, walletId,  nodeDelay.toString(), clientTxId, this);
                        }
                    } else {
                        System.err.println(status.getDescription());
                        System.out.print("\n> ");
                    }
                }

                @Override
                public void onCompleted() {}
            });
        }
    }

    private void delete(String[] split, boolean isBlocking) {
        this.checkDeleteCommandArgs(split);

        Long commandNumber = this.commandCounter.incrementAndGet();

        String userId = split[1];
        String walletId = split[2];
        Integer nodeIndex = Integer.parseInt(split[3]);
        Integer nodeDelay = Integer.parseInt(split[4]);
        String clientTxId = UUID.randomUUID().toString();

        if (isBlocking) {
            int currentNodeIndex = nodeIndex;
            boolean success = false;
            boolean second_iter = false;
            
            while (!success) {
                if (currentNodeIndex >= this.nodes.size()) {
                    currentNodeIndex = 0;
                    second_iter = true;
                }
                if (second_iter && currentNodeIndex == nodeIndex ) {
                    System.err.println("Error: All nodes have failed or are unavailable.");
                    exit = true; 
                    break;
                }
                
                try {
                    String r = this.nodes.get(currentNodeIndex).deleteWallet(userId, walletId, nodeDelay.toString(), clientTxId);
                    System.out.println("OK " + commandNumber);
                    System.out.println(r.toString());
                    success = true;
                    
                } catch (StatusRuntimeException e) {
                    Status status = e.getStatus();
                    if (status.getCode() == Status.Code.DEADLINE_EXCEEDED || status.getCode() == Status.Code.UNAVAILABLE) {
                        currentNodeIndex++;
                    } else {
                        System.err.println(status.getDescription());
                        break;
                    }
                }
            }
        } else {
            this.nodes.get(nodeIndex).deleteWalletAsync(userId, walletId, nodeDelay.toString(), clientTxId, new StreamObserver<DeleteWalletResponse>() {
                int currentNodeIndex = nodeIndex;
                boolean second_iter = false;

                @Override
                public void onNext(DeleteWalletResponse response) {
                    System.out.println("OK " + commandNumber);
                    System.out.println(response.toString());
                    System.out.print("> ");
                }

                @Override
                public void onError(Throwable throwable) {
                    Status status = ((StatusRuntimeException) throwable).getStatus();
                    
                    if (status.getCode() == Status.Code.DEADLINE_EXCEEDED || status.getCode() == Status.Code.UNAVAILABLE) {
                        currentNodeIndex++; 
                        
                        if (currentNodeIndex >= nodes.size()) {
                            currentNodeIndex = 0;
                            second_iter = true;
                        }
                        
                        if (second_iter && currentNodeIndex == nodeIndex) {
                            System.err.println("Error: All nodes have failed or are unavailable.");
                            System.out.print("> ");
                        } else {
                            nodes.get(currentNodeIndex).deleteWalletAsync(userId, walletId, nodeDelay.toString(), clientTxId, this);
                        }
                    } else {
                        System.err.println(status.getDescription());
                        System.out.print("\n> ");
                    }
                }

                @Override
                public void onCompleted() {}
            });
        }
    }

    private void balance(String[] split, boolean isBlocking) {
        this.checkBalanceCommandArgs(split);

        Long commandNumber = this.commandCounter.incrementAndGet();

        String walletId = split[1];
        Integer nodeIndex = Integer.parseInt(split[2]);
        Integer nodeDelay = Integer.parseInt(split[3]);
        
        if (isBlocking) {
            int currentNodeIndex = nodeIndex;
            boolean success = false;
            boolean second_iter = false;
            
            while (!success) {
                if (currentNodeIndex >= this.nodes.size()) {
                    currentNodeIndex = 0;
                    second_iter = true;
                }
                if (second_iter && currentNodeIndex == nodeIndex ) {
                    System.err.println("Error: All nodes have failed or are unavailable.");
                    exit = true; 
                    break;
                }
                
                try {
                    String r = this.nodes.get(currentNodeIndex).readBalance(walletId, nodeDelay.toString());
                    System.out.println("OK " + commandNumber);
                    System.out.println(r.toString());
                    success = true;
                    
                } catch (StatusRuntimeException e) {
                    Status status = e.getStatus();
                    if (status.getCode() == Status.Code.DEADLINE_EXCEEDED || status.getCode() == Status.Code.UNAVAILABLE) {
                        currentNodeIndex++;
                    } else {
                        System.err.println(status.getDescription());
                        break;
                    }
                }
            }
        } else {
            this.nodes.get(nodeIndex).readBalanceAsync(walletId, nodeDelay.toString(), new StreamObserver<ReadBalanceResponse>() {
                int currentNodeIndex = nodeIndex;
                boolean second_iter = false;

                @Override
                public void onNext(ReadBalanceResponse response) {
                    System.out.println("OK " + commandNumber);
                    System.out.println(String.valueOf(response.getBalance()));
                    System.out.print("> ");
                }

                @Override
                public void onError(Throwable throwable) {
                    Status status = ((StatusRuntimeException) throwable).getStatus();
                    
                    if (status.getCode() == Status.Code.DEADLINE_EXCEEDED || status.getCode() == Status.Code.UNAVAILABLE) {
                        currentNodeIndex++;
                        
                        if (currentNodeIndex >= nodes.size()) {
                            currentNodeIndex = 0;
                            second_iter = true;
                        }
                        
                        if (second_iter && currentNodeIndex == nodeIndex) {
                            System.err.println("Error: All nodes have failed or are unavailable.");
                            System.out.print("> ");
                        } else {
                            nodes.get(currentNodeIndex).readBalanceAsync(walletId, nodeDelay.toString(), this);
                        }
                    } else {
                        System.err.println(status.getDescription());
                        System.out.print("\n> ");
                    }
                }

                @Override
                public void onCompleted() {}
            });
        }
    }

    private void transfer(String[] split, boolean isBlocking) {
        this.checkTransferCommandArgs(split);

        Long commandNumber = this.commandCounter.incrementAndGet();

        String sourceUserId = split[1];
        String sourceWalletId = split[2];
        String destinationWalletId = split[3];
        Long amount = Long.parseLong(split[4]);
        Integer nodeIndex = Integer.parseInt(split[5]);
        Integer nodeDelay = Integer.parseInt(split[6]);
        String clientTxId = UUID.randomUUID().toString();

        if (isBlocking) {
            int currentNodeIndex = nodeIndex;
            boolean success = false;
            boolean second_iter = false;
            
            while (!success) {
                if (currentNodeIndex >= this.nodes.size()) {
                    currentNodeIndex = 0;
                    second_iter = true;
                }
                if (second_iter && currentNodeIndex == nodeIndex ) {
                    System.err.println("Error: All nodes have failed or are unavailable.");
                    exit = true; 
                    break;
                }
                
                try {
                    String r = this.nodes.get(currentNodeIndex).transfer(sourceUserId, sourceWalletId, destinationWalletId, amount, nodeDelay.toString(), clientTxId);
                    System.out.println("OK " + commandNumber);
                    System.out.println(r);
                    success = true;
                    
                } catch (StatusRuntimeException e) {
                    Status status = e.getStatus();
                    if (status.getCode() == Status.Code.DEADLINE_EXCEEDED || status.getCode() == Status.Code.UNAVAILABLE) {
                        currentNodeIndex++;
                    } else {
                        System.err.println(status.getDescription());
                        break;
                    }
                }
            }
        } else {
            this.nodes.get(nodeIndex).transferAsync(sourceUserId, sourceWalletId, destinationWalletId, amount, nodeDelay.toString(), clientTxId, new StreamObserver<TransferResponse>() {
                
                int currentNodeIndex = nodeIndex;
                boolean second_iter = false;

                @Override
                public void onNext(TransferResponse response) {
                    System.out.println("OK " + commandNumber);
                    System.out.println(response.toString());
                    System.out.print("> ");
                }

                @Override
                public void onError(Throwable throwable) {
                    Status status = ((StatusRuntimeException) throwable).getStatus();
                    
                    if (status.getCode() == Status.Code.DEADLINE_EXCEEDED || status.getCode() == Status.Code.UNAVAILABLE) {
                        currentNodeIndex++;
                        
                        if (currentNodeIndex >= nodes.size()) {
                            currentNodeIndex = 0;
                            second_iter = true;
                        }
                        
                        if (second_iter && currentNodeIndex == nodeIndex) {
                            System.err.println("Error: All nodes have failed or are unavailable.");
                            System.out.print("> ");
                        } else {
                            nodes.get(currentNodeIndex).transferAsync(sourceUserId, sourceWalletId, destinationWalletId, amount, nodeDelay.toString(), clientTxId, this);
                        }
                    } else {
                        System.err.println(status.getDescription());
                        System.out.print("\n> ");
                    }
                }

                @Override
                public void onCompleted() {}
            });
        }
    }

    private void debugBlockchainState(String[] split) {
        this.checkDebugBlockchainStateArgs(split);

        Long commandNumber = this.commandCounter.incrementAndGet();

        Integer nodeIndex = Integer.parseInt(split[1]);

        int currentNodeIndex = nodeIndex;
        boolean success = false;
        boolean second_iter = false;

        while (!success) {

            if (currentNodeIndex >= this.nodes.size()) {
                currentNodeIndex = 0;
                second_iter = true;
            }
            if (second_iter && currentNodeIndex == nodeIndex ) {
                System.err.println("Error: All nodes have failed or are unavailable.");
                exit = true;
                break;
            }
            try {
                List<Transaction> transactions = this.nodes.get(currentNodeIndex).getBlockchainState();
                System.out.println("OK " + commandNumber);
                System.out.println(transactions.toString());
                success = true;

            } catch (StatusRuntimeException e) {
                Status status = e.getStatus();
                if (status.getCode() == Status.Code.DEADLINE_EXCEEDED || status.getCode() == Status.Code.UNAVAILABLE) {
                    currentNodeIndex++;
                } else {
                    System.err.println(status.getDescription());
                    break;
                }
            }
        }
    }

    private void pause(String[] split) {
        this.checkPauseArgs(split);

        Integer time;

        time = Integer.parseInt(split[1]);

        try {
            Thread.sleep(time * 1000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private void checkCreateCommandArgs(String[] split) {
        // C|c <user_id> <wallet_id> <node_index> <node_delay>
        if (split.length != 5) {
            throw new IllegalArgumentException("Expected 5 arguments, got " + split.length);
        }

        if (!ID_PATTERN.matcher(split[1]).matches()) {
            throw new IllegalArgumentException("Expected User ID to be composed of ASCII alphanumeric characters, got \"" + split[1] + "\"");
        }

        if (!ID_PATTERN.matcher(split[2]).matches()) {
            throw new IllegalArgumentException("Expected Wallet ID to be composed of ASCII alphanumeric characters, got \"" + split[2] + "\"");
        }

        try {
            int nodeIndex = Integer.parseInt(split[3]);
            if (nodeIndex < 0 || nodeIndex >= this.nodes.size()) {
                throw new IllegalArgumentException("Node index must be between 0 and " + (this.nodes.size() - 1));
            }
            if (Integer.parseInt(split[4]) < 0) {
                throw new IllegalArgumentException("Node delay cannot be negative");
            }
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Expected initial balance, node number, and node delay to be integers");
        }
    }

    private void checkDeleteCommandArgs(String[] split) {
        // E|e <user_id> <wallet_id> <node_index> <node_delay>
        if (split.length != 5) {
            throw new IllegalArgumentException("Expected 5 arguments, got " + split.length);
        }

        if (!ID_PATTERN.matcher(split[1]).matches()) {
            throw new IllegalArgumentException("Expected User ID to be composed of ASCII alphanumeric characters, got \"" + split[1] + "\"");
        }

        if (!ID_PATTERN.matcher(split[2]).matches()) {
            throw new IllegalArgumentException("Expected Wallet ID to be composed of ASCII alphanumeric characters, got \"" + split[2] + "\"");
        }

        try {
            int nodeIndex = Integer.parseInt(split[3]);
            if (nodeIndex < 0 || nodeIndex >= this.nodes.size()) {
                throw new IllegalArgumentException("Node index must be between 0 and " + (this.nodes.size() - 1));
            }
            if (Integer.parseInt(split[4]) < 0) {
                throw new IllegalArgumentException("Node delay cannot be negative");
            }
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Expected node number and node delay to be integers");
        }
    }

    private void checkBalanceCommandArgs(String[] split) {
        // S|s <wallet_id> <node_index> <node_delay>
        if (split.length != 4) {
            throw new IllegalArgumentException("Expected 4 arguments, got " + split.length);
        }

        if (!ID_PATTERN.matcher(split[1]).matches()) {
            throw new IllegalArgumentException("Expected Wallet ID to be composed of ASCII alphanumeric characters, got \"" + split[1] + "\"");
        }

        try {
            int nodeIndex = Integer.parseInt(split[2]);
            if (nodeIndex < 0 || nodeIndex >= this.nodes.size()) {
                throw new IllegalArgumentException("Node index must be between 0 and " + (this.nodes.size() - 1));
            }
            if (Integer.parseInt(split[3]) < 0) {
                throw new IllegalArgumentException("Node delay cannot be negative");
            }
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Expected node number and node delay to be integers");
        }
    }

    private void checkTransferCommandArgs(String[] split) {
        // T|t <source_user_id> <source_wallet_id> <destination_wallet_id> <amount> <node_index> <node_delay>
        if (split.length != 7) {
            throw new IllegalArgumentException("Expected 7 arguments, got " + split.length);
        }

        if (!ID_PATTERN.matcher(split[1]).matches()) {
            throw new IllegalArgumentException("Expected Source User ID to be composed of ASCII alphanumeric characters, got \"" + split[1] + "\"");
        }

        if (!ID_PATTERN.matcher(split[2]).matches()) {
            throw new IllegalArgumentException("Expected Source Wallet ID to be composed of ASCII alphanumeric characters, got \"" + split[2] + "\"");
        }

        if (!ID_PATTERN.matcher(split[3]).matches()) {
            throw new IllegalArgumentException("Expected Destination Wallet ID to be composed of ASCII alphanumeric characters, got \"" + split[3] + "\"");
        }

        try {
            if (Long.parseLong(split[4]) < 0) {
                throw new IllegalArgumentException("Amount cannot be negative");
            }
            int nodeIndex = Integer.parseInt(split[5]);
            if (nodeIndex < 0 || nodeIndex >= this.nodes.size()) {
                throw new IllegalArgumentException("Node index must be between 0 and " + (this.nodes.size() - 1));
            }
            if (Integer.parseInt(split[6]) < 0) {
                throw new IllegalArgumentException("Node delay cannot be negative");
            }
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Expected amount, node number, and node delay to be integers");
        }
    }

    private void checkDebugBlockchainStateArgs(String[] split) {
        // B <node_index>
        if (split.length != 2) {
            throw new IllegalArgumentException("Expected 2 arguments, got " + split.length);
        }

        try {
            int nodeIndex = Integer.parseInt(split[1]);
            if (nodeIndex < 0 || nodeIndex >= this.nodes.size()) {
                throw new IllegalArgumentException("Node index must be between 0 and " + (this.nodes.size() - 1));
            }
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Expected node index to be an integer");
        }
    }

    private void checkPauseArgs(String[] split) {
        // P <integer>
        if (split.length != 2) {
            throw new IllegalArgumentException("Expected 2 arguments, got " + split.length);
        }

        try {
            if (Integer.parseInt(split[1]) < 0) {
                throw new IllegalArgumentException("Pause time cannot be negative");
            }
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Expected pause time to be an integer");
        }
    }

    private static void printUsage() {
        System.err.println("Usage:\n" +
                "- C|c <user_id> <wallet_id> <node_index> <node_delay>\n" +
                "- E|e <user_id> <wallet_id> <node_index> <node_delay>\n" +
                "- S|s <wallet_id> <node_index> <node_delay>\n" +
                "- T|t <source_user_id> <source_wallet_id> <destination_wallet_id> <amount> <node_index> <node_delay>\n" +
                "- B <node_index>\n" +
                "- P <integer>\n" +
                "- X\n");
    }
}

