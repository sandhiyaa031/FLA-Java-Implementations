# FLA Project – Java Implementation

Five Java programs for Formal Languages and Automata:

| # | Task | Program | Method |
|---|------|---------|--------|
| 1 | DFA simulation | `DFASimulation` | Applies δ symbol by symbol and checks whether δ*(q0, w) ∈ F |
| 2 | NFA → DFA conversion | `NFAtoDFA` | Subset (powerset) construction, with ε-closure |
| 3 | DFA state reduction | `DFAMinimization` | Remove unreachable states, table-filling, merge equivalent states |
| 4 | Intersection of two DFAs | `DFAIntersection` | Product construction, F = F1 × F2 |
| 5 | PDA simulation | `PDASimulation` | Search over all configurations, with the stack shown at every step |

Every program prints each step of its algorithm, so the output can be used directly in a report or a viva.

---

## 1. Folder structure

```
FLA_Project/
├── src/                       Java source code
│   ├── DFASimulation.java     Task 1 (main program)
│   ├── NFAtoDFA.java          Task 2 (main program)
│   ├── DFAMinimization.java   Task 3 (main program)
│   ├── DFAIntersection.java   Task 4 (main program)
│   ├── PDASimulation.java     Task 5 (main program)
│   ├── DFA.java               DFA class shared by tasks 1–4
│   ├── NFA.java               NFA class (with ε-transitions) for task 2
│   ├── InputSource.java       Reads input from a file or the keyboard
│   ├── InputException.java    Error for invalid input
│   ├── TablePrinter.java      Prints aligned tables
│   └── Util.java              Small shared helpers
├── samples/                   Ready-made input files (2–3 per task)
├── sample_output/             The output of every program on every sample
└── README.md
```

## 2. Requirements

Java 8 or newer (JDK). The code uses only the standard library and plain ASCII output, so it works in the Windows Command Prompt, PowerShell, Linux and macOS terminals, and IDEs such as Eclipse, IntelliJ and NetBeans.

## 3. Compile and run

From the `FLA_Project` folder:

```
javac -d out src/*.java
```

Run a program **with a sample file**:

```
java -cp out DFASimulation   samples/1a_dfa_ends_with_abb.txt
java -cp out NFAtoDFA        samples/2b_enfa_a_b_c.txt
java -cp out DFAMinimization samples/3b_dfa_minimize_textbook.txt
java -cp out DFAIntersection samples/4a_intersection_even_a_ends_b.txt
java -cp out PDASimulation   samples/5a_pda_anbn.txt
```

Run a program **with keyboard input** (leave out the file name); it asks for each value:

```
java -cp out DFASimulation
```

To run everything at once: `run_all.bat` (Windows) or `./run_all.sh` (Linux/macOS). The results go to `sample_output/`.

On Windows you can write `src\*.java` and `samples\1a_dfa_ends_with_abb.txt`; both slash styles work.

## 4. Input format

The keyboard and the file use the **same format**: a file simply contains what you would type, in order. Blank lines and lines starting with `#` are ignored (use them for comments).

General rules:
* States are names without spaces, for example `q0`, `A`, `even`.
* Input symbols and stack symbols are **single characters**.
* Lists may be separated by spaces or commas, and may be wrapped in braces: `q0 q1`, `q0,q1` and `{q0, q1}` all work.
* `eps` means ε, the empty string (Linz writes λ). `λ`, `ε`, `lambda` and `epsilon` are also accepted.
* `none` means an empty set of final states.
* `end` finishes the list of transitions and the list of test strings.

### Task 1 – DFA (also used by tasks 3 and 4)

```
A B C D          <- states
a b              <- input alphabet
A                <- start state
D                <- final states
A a B            <- transitions: state symbol next-state  (δ(A, a) = B)
A b A
...
end
abb              <- strings to test, one per line
eps              <- the empty string
end
```

A transition may be left out. If the DFA needs a missing transition, the string is rejected (the same as a dead state). Giving two different targets for the same state and symbol is reported as an error, because a DFA must be deterministic.

### Task 2 – NFA

Same as the DFA, but a transition line may list **several targets**, and the symbol may be `eps`:

```
q0 0 q0 q1       <- δ(q0, 0) = {q0, q1}
q0 eps q2        <- ε-transition from q0 to q2
```

### Task 4 – two DFAs

The first DFA (ending with `end`), then the second DFA (ending with `end`), then the test strings. If the two alphabets differ, both DFAs are extended to the union of the alphabets; a symbol a DFA does not know makes it reject.

### Task 5 – PDA

```
q0 q1 q2         <- states
a b              <- input alphabet (Σ)
a Z              <- stack alphabet (Γ)
q0               <- start state
Z                <- initial stack symbol
q2               <- final states ('none' if acceptance is by empty stack only)
F                <- acceptance: F = by final state, E = by empty stack
q0 a Z q0 aZ     <- transitions: state input pop next-state push
q0 b a q1 eps
...
end
aabb             <- strings to test
end
```

`q0 a Z q0 aZ` means **δ(q0, a, Z) contains (q0, aZ)**: in state q0, read `a`, pop `Z`, push `aZ`, and go to q0.
* The pushed string is written with its **leftmost symbol on top** (the usual textbook convention), so `aZ` puts `a` above `Z`.
* push `eps` = push nothing (a pure pop).
* input `eps` = a move that reads no input.
* pop `eps` = a move that does not look at or pop the stack (an extension; textbook PDAs always pop one symbol).

## 5. How each program works

### Task 1 – DFA simulation
A DFA is M = (Q, Σ, δ, q0, F). The program starts in q0 and, for each symbol of w, moves to δ(current state, symbol). It prints a table of every step and the path, for example `A --a--> B --b--> C --b--> D`. The string is **accepted** if the last state δ*(q0, w) is in F. It is **rejected** if that state is not final, if a symbol is not in Σ, or if a needed transition is undefined.

### Task 2 – NFA to DFA (subset construction)
1. If the NFA has ε-moves, compute the ε-closure of every state (the state plus everything reachable by ε-moves alone).
2. The DFA start state is D0 = ε-closure({q0}).
3. For each new DFA state S and each symbol a: compute move(S, a) = ⋃ δ(q, a) over q ∈ S, then T = ε-closure(move(S, a)). If T has not been seen before it becomes a new DFA state. This repeats until no new states appear, so only reachable subsets are built.
4. A DFA state is final if its subset contains at least one NFA final state.
5. The empty set {}, if reached, is the dead state.

The program prints every move/closure computation, the DFA table with the NFA subset of each state, and then checks that the NFA and DFA give the same answer on every string up to a length chosen to keep the test fast (for example, all 32,767 strings of length 0–14 over {a, b}).

### Task 3 – DFA state reduction
0. If some transitions are undefined, a trap state is added first, because minimization needs a complete DFA.
1. **Unreachable states** (no path from q0) are removed.
2. **Table-filling:** two states p and q are *distinguishable* if some string leads one to a final state and the other to a non-final state.
   * Round 0 marks every pair where exactly one state is final.
   * Each following round marks (p, q) if, for some symbol a, the pair (δ(p, a), δ(q, a)) is already marked.
   * It stops when a round marks nothing new. The program prints the reason for every mark and the final triangular table (`X1` = marked in round 1, `=` = never marked).
3. Unmarked pairs are **equivalent**. They form classes, which the program cross-checks against the **partition-refinement** method (P0 = {non-final, final}, then split blocks until nothing changes). Both methods must give the same classes.
4. Each class becomes one state of the minimized DFA. The class containing q0 is the start state, and classes of final states are final.
5. The program then proves L(M) = L(M′) exactly by exploring every reachable pair of states of the two machines.

### Task 4 – Intersection (product construction)
For M1 = (Q1, Σ, δ1, p0, F1) and M2 = (Q2, Σ, δ2, q0, F2) the product DFA is

M = (Q1 × Q2, Σ, δ, (p0, q0), F1 × F2), where δ((p, q), a) = (δ1(p, a), δ2(q, a)).

M runs both DFAs at the same time and accepts only when **both** accept, so L(M) = L1 ∩ L2. Only pairs reachable from (p0, q0) are built, and the program prints each one. It also reports whether L1 ∩ L2 is empty, giving the shortest string in it when it is not, and checks the product DFA against M1 and M2 on every short string. (For union, the only change would be F = {(p, q) : p ∈ F1 or q ∈ F2}.)

### Task 5 – PDA simulation
A PDA is M = (Q, Σ, Γ, δ, q0, z, F). A **configuration** (instantaneous description) is (state, unread input, stack). A PDA can be nondeterministic, so it may have several possible moves at once. The program therefore explores **all** computations using breadth-first search over configurations, remembering those already visited.

* By **final state**: accept if some computation reads the whole input and ends in a state of F.
* By **empty stack**: accept if some computation reads the whole input and empties the stack.

For an accepted string the program prints the accepting computation: every move used, the stack operation (read / pop / push), the state, the unread input, and the stack after each step, plus the chain of instantaneous descriptions, for example:

```
(q0, aabb, Z) |- (q0, abb, aZ) |- (q0, bb, aaZ) |- (q1, b, aZ) |- (q1, eps, Z) |- (q2, eps, Z)
```

For a rejected string it reports how many configurations were explored and how far into the input any computation got. The program also states whether the PDA is deterministic (a DPDA).

A PDA with endless ε-moves (for example, pushing forever) could make the search infinite. The search therefore stops at 1,000,000 configurations or a stack of 1,000 symbols. If it stops for that reason without finding an accepting computation, the answer is reported honestly as **UNDECIDED (search limit reached)**, never as a wrong "rejected".

## 6. Sample inputs and results

| File | What it is | Key result |
|------|-----------|------------|
| `1a_dfa_ends_with_abb` | DFA for strings ending in `abb` | abb, aabb, babb accepted; abba, ab, ε, abcb rejected |
| `1b_dfa_divisible_by_3` | Binary numbers divisible by 3 | 110 (6), 1001 (9), 10101 (21) accepted; 1011 (11), 100 (4) rejected |
| `2a_nfa_ends_with_01` | NFA, strings ending in `01` | 3-state DFA: {q0}, {q0,q1}, {q0,q2} |
| `2b_enfa_a_b_c` | ε-NFA for a\*b\*c\* | ε-closures; 4-state DFA including the dead state {} |
| `2c_nfa_third_from_end` | 3rd symbol from the end is `a` | 4-state NFA becomes an 8-state DFA (2³) |
| `3a_dfa_minimize` | 7 states, G unreachable | 3 states: {A,B}, {C,D,E}, F (language 0\*10\*) |
| `3b_dfa_minimize_textbook` | Classic Hopcroft–Ullman example | D unreachable; 5 states: {A,E}, {B,H}, C, F, G |
| `4a_intersection_even_a_ends_b` | Even number of a's ∩ ends with b | 4-state product, F = {(E,Y)} |
| `4b_intersection_contains_ab_odd_length` | Contains `ab` ∩ odd length | 6-state product; shortest string `aab` |
| `5a_pda_anbn` | aⁿbⁿ, n ≥ 0, final state | aabb, aaabbb, ε accepted; aab, abb, ba rejected |
| `5b_pda_even_palindromes` | ww^R, nondeterministic | abba, abaaba, ε accepted; aba, abab rejected |
| `5c_pda_balanced_parentheses` | Balanced ( ), empty stack | (())(), ε accepted; ()(, ())( rejected |

The full output of each run is in `sample_output/`.

## 7. Testing

Besides the samples, the programs were checked against independent implementations on randomly generated automata: 260 random DFAs, NFAs (with ε-moves) and DFA pairs (including partial DFAs and pairs with different alphabets), plus the three PDAs on every string up to length 10 — 57,674 string checks in total, with no mismatch. The number of DFA states produced by the subset construction and by minimization also matched the independently computed values in every case. All 3b results agree with the published textbook answer.

The programs also reject invalid input with a clear message: an unknown state or symbol, a DFA with two targets for the same state and symbol, a missing file, and so on.
