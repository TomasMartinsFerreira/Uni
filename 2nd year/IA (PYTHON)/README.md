# 🧩 NURUOMINO – Artificial Intelligence Project

## 📖 Project Overview

This project is a Python solver for the **NURUOMINO** puzzle. The goal is to fill a grid by placing one of four unique shapes (**tetrominos**) into predefined regions while following specific AI search constraints.

### 🎮 The Rules

* 
**Tetrominos:** Use only the **L, I, T, and S** shapes.

* 
**Placement:** Exactly one tetromino must be placed in each region.

* 
**Adjacency:** Two identical shapes (considering rotations and reflections) cannot be orthogonally adjacent.

* 
**Connectivity:** All filled cells must be orthogonally connected to form a single continuous shape.

* 
**No 2x2 Squares:** Filled cells cannot form a solid  block.

---

## 🛠️ Implementation & AI Techniques

* 
**Search Algorithms:** Supports Depth-First Search, Greedy Search, and .

* 
**State Management:** A custom `NuruominoState` class to track grid configurations.

* 
**Pré-loading** Uses `ac3` to test interaction between regions before starting a search algorithm.

* 
**Data Handling:** Utilized `numpy` for efficient array operations and representation.

---

## 🚀 How to test it

Run `run_tests.py` to run all the tests in sample-nuruominoboars.

Run `test.py` to see how a test looks like in a board.
(Change FILENAME for the test you want to see)

Run `nuruomino.py` to run a test.
(Make sure the syntax looks the same as the sample tests)


## 📊 Evaluation Results

* 
**Execution (75%):** Correctness verified via automated GitLab tests.

* 
**Video Report (25%):** Technical presentation explaining heuristic logic and AI concepts.

* 
**Group Project:** Developed by a team of 2 students.

* **Final Score:** **19.5 / 20**