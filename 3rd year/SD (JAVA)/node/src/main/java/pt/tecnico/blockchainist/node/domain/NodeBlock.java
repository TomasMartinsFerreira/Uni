package pt.tecnico.blockchainist.node.domain;

import pt.tecnico.blockchainist.node.TransactionAction.NodeTransactionAction;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a block within the node's local ledger, with a unique block ID 
 * and a list of transaction actions.
 */
public class NodeBlock {
    private final int blockId;
    private final List<NodeTransactionAction> transactions;

    public NodeBlock(int blockId) {
        this.blockId = blockId;
        this.transactions = new ArrayList<>();
    }

    public void addTransaction(NodeTransactionAction tx) { this.transactions.add(tx); }

    public int getBlockId() { return blockId; }
    public List<NodeTransactionAction> getTransactions() { return transactions; }
    
    public int getSize() { return transactions.size(); }
}