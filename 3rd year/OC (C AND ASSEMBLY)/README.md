# Computer Organization (OC)

## 📊 Project Grades (Group Lab Sessions)

| Grade                      | Score         |
|----------------------------|---------------|
| **Lab 1: System Modeling** | ⭐ 20.00     |
| **Lab 2: TLB Simulator**   | ⭐ 20.00     |
| **Lab 3: ILP & Pipeline**  | ⭐ 20.00     |
| **Final Average**          | 🏆 **20.00** |

---

## 🚀 Project Highlights

### 🧠 Lab 1: System Modeling and Profiling
**Goal:** Experimentally determine cache characteristics and optimize matrix multiplication performance.

*   **Cache Modeling:** Identified L1/L2 capacity, block size, and associativity using PAPI.
*   **Performance Profiling:** Analyzed cache misses (`PAPI_L1_DCM`) and memory access patterns.
*   **Code Optimization:** Implemented **Matrix Transposition** and **Tiled (Blocked) Multiplication** to improve data locality and bypass hardware bottlenecks.

### ⚡ Lab 2: TLB Cache Simulator
**Goal:** Implement a high-performance 2-level Translation Lookaside Buffer (TLB) simulator in C.

*   **Virtual Memory:** Simulated the translation process from virtual addresses to physical memory.
*   **Cache Logic:** Developed **Fully Associative** mapping and **LRU (Least Recently Used)** eviction policies.
*   **Systems Programming:** Wrote robust, low-level C code to handle hits, misses, and write-back policies.

### 🏎️ Lab 3: Instruction Level Parallelism (ILP)
**Goal:** Optimize assembly code execution through pipeline analysis and hazard mitigation.

*   **Pipeline Analysis:** Identified Data, Structural, and Control hazards in a standard 5-stage pipeline.
*   **Hazard Mitigation:** Utilized data forwarding and branch prediction ("Not Taken" policy) to reduce clock cycle stalls.
*   **Advanced Optimization:** Applied **Loop Unrolling** to achieve significant SpeedUp and lower the Cycles Per Instruction (CPI).

---

## 💡 Key Takeaways

*   **Hardware-Software Synergy:** Deep understanding of how high-level code interacts with Caches, TLBs, and CPU Pipelines.
*   **Low-Level Mastery:** Proficiency in **C** and **RISC-V Assembly** for performance-critical applications.
*   **Quantitative Analysis:** Ability to measure SpeedUp, CPI, and Hit-Rates to justify architectural design choices.
*   **Optimization Mindset:** Expertise in restructuring algorithms (Tiling, Unrolling) to maximize hardware efficiency.