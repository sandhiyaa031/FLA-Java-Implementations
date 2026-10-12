import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * TASK 2: NFA TO DFA CONVERSION (SUBSET / POWERSET CONSTRUCTION)
 *
 * Reads an NFA (epsilon-transitions are allowed) and builds an equivalent DFA:
 *   - every DFA state is a set of NFA states;
 *   - the DFA start state is epsilon-closure({q0});
 *   - delta_D(S, a) = epsilon-closure( union of delta_N(q, a) for q in S );
 *   - a DFA state is final if it contains at least one NFA final state.
 * Only subsets reachable from the start state are built.
 *
 * The program prints every step, the resulting DFA, and then checks the two machines
 * against each other on every string up to a certain length.
 *
 * Usage:  java NFAtoDFA                 (keyboard)
 *         java NFAtoDFA input-file.txt  (file)
 */
public class NFAtoDFA {

    public static void main(String[] args) {
        Util.banner("TASK 2: NFA TO DFA CONVERSION",
                "Builds an equivalent DFA from an NFA using the subset construction.");
        try {
            InputSource in = new InputSource(args);
            NFA nfa = NFA.read(in);

            Util.heading("The NFA");
            nfa.printDefinition("N");

            DFA dfa = convert(nfa);

            verify(nfa, dfa);

            while (true) {
                String line = in.nextLine(
                        "\nEnter a string to test on both machines ('eps' = empty, 'end' to stop):");
                if (line == null || InputSource.isEnd(line)) {
                    break;
                }
                String w = InputSource.parseWord(line);
                boolean byNfa = nfa.accepts(w);
                boolean byDfa = dfa.accepts(w);
                System.out.println("  NFA: " + (byNfa ? "ACCEPTED" : "REJECTED")
                        + "   DFA: " + (byDfa ? "ACCEPTED" : "REJECTED")
                        + "   DFA path: " + dfaPath(dfa, w));
                System.out.println("  Result: " + Util.quote(w) + " is " + (byDfa ? "ACCEPTED" : "REJECTED")
                        + (byNfa == byDfa ? " (both machines agree)" : " (MACHINES DISAGREE - error)"));
            }
        } catch (InputException e) {
            System.out.println();
            System.out.println("Input error: " + e.getMessage());
            System.exit(1);
        } catch (IOException e) {
            System.out.println("Could not read the input: " + e.getMessage());
            System.exit(1);
        }
    }

    /** Performs the subset construction, printing each step, and returns the DFA. */
    static DFA convert(NFA nfa) {
        List<Character> sigma = nfa.getAlphabet();
        int step = 1;

        if (nfa.hasEpsilon()) {
            Util.heading("Step " + step++ + ": epsilon-closure of every NFA state");
            System.out.println("  epsilon-closure(q) = q together with every state reachable from q");
            System.out.println("  using epsilon-transitions only.");
            TablePrinter closures = new TablePrinter("State q", "epsilon-closure(q)");
            for (String q : nfa.getStates()) {
                closures.addRow(q, nfa.name(nfa.closure(nfa.singleton(q))));
            }
            closures.print();
        }

        Util.heading("Step " + step++ + ": subset construction");
        List<BitSet> dStates = new ArrayList<BitSet>();
        Map<BitSet, Integer> idOf = new HashMap<BitSet, Integer>();
        List<int[]> table = new ArrayList<int[]>();
        Deque<Integer> unmarked = new ArrayDeque<Integer>();

        BitSet start = nfa.startSet();
        dStates.add(start);
        idOf.put(start, 0);
        unmarked.add(0);
        if (nfa.hasEpsilon()) {
            System.out.println("  Start state D0 = epsilon-closure({" + nfa.getStart() + "}) = " + nfa.name(start));
        } else {
            System.out.println("  Start state D0 = {" + nfa.getStart() + "}");
        }

        while (!unmarked.isEmpty()) {
            int i = unmarked.poll();
            BitSet s = dStates.get(i);
            System.out.println();
            System.out.println("  Processing D" + i + " = " + nfa.name(s) + ":");
            int[] row = new int[sigma.size()];
            for (int k = 0; k < sigma.size(); k++) {
                char a = sigma.get(k);
                BitSet moved = nfa.move(s, a);
                BitSet target = nfa.closure(moved);
                boolean isNew = !idOf.containsKey(target);
                if (isNew) {
                    idOf.put(target, dStates.size());
                    dStates.add(target);
                    unmarked.add(idOf.get(target));
                }
                int j = idOf.get(target);
                row[k] = j;
                StringBuilder sb = new StringBuilder("    on '" + a + "': move(" + nfa.name(s) + ", " + a + ") = "
                        + nfa.name(moved));
                if (nfa.hasEpsilon()) {
                    sb.append(", epsilon-closure = ").append(nfa.name(target));
                }
                sb.append("  ->  D").append(j).append(isNew ? "  (new state)" : "");
                if (target.isEmpty() && isNew) {
                    sb.append("  [the empty set: a dead state]");
                }
                System.out.println(sb);
            }
            // rows are added in the order states are processed, which equals their index
            while (table.size() <= i) {
                table.add(null);
            }
            table.set(i, row);
        }

        Util.heading("Step " + step++ + ": final states of the DFA");
        System.out.println("  A DFA state is final if its subset contains an NFA final state "
                + Util.setString(nfa.getFinals()) + ".");
        List<String> dfaFinals = new ArrayList<String>();
        for (int i = 0; i < dStates.size(); i++) {
            List<String> f = nfa.finalsIn(dStates.get(i));
            if (!f.isEmpty()) {
                dfaFinals.add("D" + i);
                System.out.println("  D" + i + " = " + nfa.name(dStates.get(i)) + " is final (contains "
                        + joinList(f) + ")");
            }
        }
        if (dfaFinals.isEmpty()) {
            System.out.println("  No reachable subset contains a final state: the language is empty.");
        }

        // build the DFA object
        List<String> names = new ArrayList<String>();
        for (int i = 0; i < dStates.size(); i++) {
            names.add("D" + i);
        }
        Map<String, Map<Character, String>> delta = new LinkedHashMap<String, Map<Character, String>>();
        for (int i = 0; i < dStates.size(); i++) {
            Map<Character, String> r = new LinkedHashMap<Character, String>();
            for (int k = 0; k < sigma.size(); k++) {
                r.put(sigma.get(k), "D" + table.get(i)[k]);
            }
            delta.put("D" + i, r);
        }
        DFA dfa = new DFA(names, sigma, "D0", dfaFinals, delta);

        Util.heading("Step " + step + ": the equivalent DFA");
        String[] header = new String[sigma.size() + 3];
        header[0] = "";
        header[1] = "DFA state";
        header[2] = "NFA states";
        for (int k = 0; k < sigma.size(); k++) {
            header[k + 3] = String.valueOf(sigma.get(k));
        }
        TablePrinter t = new TablePrinter(header);
        for (int i = 0; i < dStates.size(); i++) {
            Object[] r = new Object[sigma.size() + 3];
            r[0] = (i == 0 ? "->" : "") + (dfaFinals.contains("D" + i) ? "*" : "");
            r[1] = "D" + i;
            r[2] = nfa.name(dStates.get(i));
            for (int k = 0; k < sigma.size(); k++) {
                r[k + 3] = "D" + table.get(i)[k];
            }
            t.addRow(r);
        }
        t.print();
        System.out.println("  (-> marks the start state, * marks a final state)");
        System.out.println();
        dfa.printDefinition("D");
        System.out.println();
        System.out.println("  The NFA has " + nfa.getStates().size() + " states; the DFA has " + dStates.size()
                + " reachable states (the subset construction can give at most 2^"
                + nfa.getStates().size() + " = " + (1L << Math.min(62, nfa.getStates().size())) + ").");
        Integer empty = idOf.get(new BitSet());
        if (empty != null) {
            System.out.println("  D" + empty + " = {} is the dead (trap) state: once entered, the DFA never accepts.");
        }
        return dfa;
    }

    /** Compares the NFA and the DFA on every string up to a length chosen to keep the test fast. */
    static void verify(final NFA nfa, final DFA dfa) {
        Util.heading("Verification: L(NFA) = L(DFA)?");
        int maxLength = Util.testLength(nfa.getAlphabet().size(), 200000, 14);
        String mismatch = Util.forEachWord(nfa.getAlphabet(), maxLength, new Util.WordVisitor() {
            public boolean visit(String w) {
                return nfa.accepts(w) == dfa.accepts(w);
            }
        });
        long count = Util.wordCount(nfa.getAlphabet().size(), maxLength);
        if (mismatch == null) {
            System.out.println("  Tested all " + count + " strings of length 0 to " + maxLength
                    + ": the NFA and the DFA accept exactly the same ones.");
        } else {
            System.out.println("  ERROR: the machines disagree on " + Util.quote(mismatch));
        }
    }

    static String dfaPath(DFA dfa, String w) {
        String q = dfa.getStart();
        StringBuilder sb = new StringBuilder(q);
        for (int i = 0; i < w.length(); i++) {
            q = dfa.next(q, w.charAt(i));
            if (q == null) {
                return sb.append(" --").append(w.charAt(i)).append("--> (symbol not in alphabet)").toString();
            }
            sb.append(" --").append(w.charAt(i)).append("--> ").append(q);
        }
        return sb.toString();
    }

    private static String joinList(List<String> items) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < items.size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(items.get(i));
        }
        return sb.toString();
    }
}
