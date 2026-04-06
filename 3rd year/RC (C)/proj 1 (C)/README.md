# Reliable File Transfer (UDP ARQ) - Computer Networks

## 📊 Project Overview
This project implements a reliable file transfer system over **UDP** using a custom **Sliding Window ARQ** algorithm. It ensures data integrity and order even in environments with packet loss and reordering.

## 🚀 Key Features
*   **Generalized ARQ:** Supports Stop-and-Wait, Go-Back-N, and Selective Repeat.
*   **Sliding Window:** Configurable window sizes (up to 32) for both Sender and Receiver.
*   **Fast Retransmit:** Triggers immediate recovery after 3 duplicate ACKs.
*   **Selective ACKs:** Uses a 32-bit mask to avoid redundant data retransmission.
*   **Random Access I/O:** Uses `fseek` to handle out-of-order data chunks directly on disk.

## 🛠️ Technical Specifications
*   **Language:** C
*   **Protocol:** UDP (User Datagram Protocol)
*   **Packet Size:** 1000 bytes of data per chunk.
*   **Timeouts:** 1s for Sender retransmission; 4s for Receiver termination.
*   **Build Tool:** GNU Make.

## 🧪 Debugging & Tools
*   **Fault Injection:** Used `LD_PRELOAD` to simulate packet loss and latency.
*   **Visualization:** Generated Message Sequence Charts (MSC) to analyze packet flow.

## 🎯 Grade & Recognition
This project received a solid score of **17.50 out of 20** 📈  