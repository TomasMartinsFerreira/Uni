# 🔗Blockchain - Distributed Systems (SD) 
**Academic Year:** 2025/26  
**Final Grade:** ⭐ 18.00 / 20  
*Collaborative project inspired by the Hyperledger Fabric architecture.*

---

## 📝 Overview
This project involves the development of a **Permissioned Blockchain** infrastructure designed to support a simple cryptocurrency. Unlike public blockchains, this system operates within a known set of organizations and uses an **Order-Execute** model (similar to Hyperledger Fabric) to ensure total ordering of transactions through a centralized **Sequencer** service.

## 🏗️ System Architecture
The system is composed of three distinct entities interacting via **gRPC**:

*   **Clients:** Submit transaction requests and queries. Supports both blocking and non-blocking interactions.
*   **Fabric Peers (Nodes):** Replicated servers that maintain the **World State** and a local copy of the **Blockchain**.
*   **Sequencer:** A centralized ordering service that implements **Atomic Broadcast** by grouping transactions into blocks based on count ($N$) or time ($T$) thresholds.

---

## 🚀 Key Features

### 🛡️ Permissioned Governance
*   Identities of all participants (Clients, Nodes, Sequencer) are pre-known.
*   The system supports a pre-existing "Central Bank" (BC) wallet with an initial balance.

### ⛓️ Atomic Broadcast & Block Management
*   **Total Ordering:** The Sequencer ensures every node processes transactions in the exact same sequence.
*   **Dynamic Synchronization:** Nodes can join the network at any time; they automatically synchronize by fetching missing blocks from the Sequencer to reconstruct the current state.

### ⚡ Performance & Consistency
*   **Linearizability:** Ensures strong consistency for all global operations.
*   **Causal Ordering (Optimization):** Specifically for transfer operations, the system can utilize causal ordering to confirm transactions to the client before they are finalized in a block, reducing perceived latency.

### 🔐 Security & Integrity
*   **Digital Signatures:** All transactions are signed by the issuing user, and every block produced by the Sequencer is digitally signed.
*   **Signature Validation:** Nodes verify the integrity of every block and transaction before updating the local ledger.

---

## 💻 Technical Stack
*   **Language:** Java
*   **Framework:** gRPC (Remote Procedure Calls)
*   **Data Serialization:** Protocol Buffers (.proto)
*   **Build Tool:** Maven

---

## 💡 Engineering Takeaways
*   **Distributed Coordination:** Mastery of the "Order-Execute" lifecycle and its advantages in permissioned environments.
*   **Fault Tolerance:** Implementation of client-side failover logic where clients automatically switch nodes if a failure is detected, without violating consistency.
*   **Network Programming:** Advanced use of gRPC metadata to simulate network delays and non-blocking stubs for asynchronous command processing.