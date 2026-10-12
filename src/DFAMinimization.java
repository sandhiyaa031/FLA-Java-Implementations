import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * TASK 3: DFA STATE REDUCTION (MINIMIZATION)
 *
 * Reads a DFA and produces the equivalent DFA with the fewest states:
 *   Step 1  remove the states that cannot be reached from the start state;
 *   Step 2  table-filling algorithm: mark every pair of DISTINGUISHABLE states
 *           (round 0: one state final, the other not; later rounds: on some symbol the
 *           pair moves to a pair that is already marked);
 *   Step 3  the unmarked pairs are EQUIVALENT states; group them into classes
 *           (checked against the partition-refinement method, which must agree);
 *   Step 4  merge every class into one state of the minimized DFA;
 *   Step 5  verify that the original and minimized DFAs accept the same language.
 *
 * Usage:  java DFAMinimization                 (keyboard)
 *         java DFAMinimization input-file.txt  (file)
 */
public class DFAMinimization {

    public static void main(String[] args) {
        Util.banner("TASK 3: DFA STATE REDUCTION (MINIMIZATION)",
                "Finds and merges equivalent states to get the minimal DFA.");
        try {
            InputSource in = new InputSource(args);
            DFA original = DFA.read(in, "");

            Util.heading("The given DFA");
            original.printDefinition("M");

            DFA minimized = minimize(original);

            Util.heading("Verification: L(M) = L(M')?");
            String diff = DFA.distinguishingString(original, minimized);
            if (diff == null) {
                System.out.println("  Every reachable pair of states (one from M, one from M') was checked:");
                System.out.println("  no string is accepted by one machine and rejected by the other.");
                System.out.println("  So L(M) = L(M'). States: " + original.getStates().size()
                        + " -> " + minimized.getStates().size() + ".");
            } else {
                System.out.println("  ERROR: the machines differ on " + Util.quote(diff));
            }

            while (true) {
                String line = in.nextLine(
                        "\nEnter a string to test on both DFAs ('eps' = empty, 'end' to stop):");
                if (line == null || InputSource.isEnd(line)) {
                    break;
                }
                String w = InputSource.parseWord(line);
                boolean a = original.accepts(w);
                boolean b = minimized.accepts(w);
                System.out.println("  Original M: " + (a ? "ACCEPTED" : "REJECTED")
                        + "   Minimized M': " + (b ? "ACCEPTED" : "REJECTED"));
                System.out.println("  Result: " + Util.quote(w) + " is " + (b ? "ACCEPTED" : "REJECTED")
                        + (a == b ? " (both DFAs agree)" : " (DFAs DISAGREE - error)"));
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

    static DFA minimize(DFA input) {
        List<Character> sigma = input.getAlphabet();

        // ---- Step 0: make the DFA complete (the algorithm needs every transition)
        DFA dfa = input;
        if (!input.isComplete()) {
            dfa = input.complete();
            String trap = dfa.getStates().get(dfa.getStates().size() - 1);
            Util.heading("Step 0: complete the DFA");
            System.out.println("  Some transitions were undefined. They now go to a new non-final trap state "
                    + trap + ",");
            System.out.println("  which does not change the language:");
            dfa.printTable();
        }

        // ---- Step 1: unreachable states
        Util.heading("Step 1: remove unreachable states");
        Set<String> reachable = dfa.reachableStates();
        List<String> removed = new ArrayList<String>();
        for (String q : dfa.getStates()) {
            if (!reachable.contains(q)) {
                removed.add(q);
            }
        }
        System.out.println("  States reachable from " + dfa.getStart() + ": " + Util.setString(reachable));
        if (removed.isEmpty()) {
            System.out.println("  Every state is reachable; nothing to remove.");
        } else {
            System.out.println("  Removed (no path from the start state): " + Util.setString(removed));
        }
        List<String> q = new ArrayList<String>();
        for (String s : dfa.getStates()) {          // keep the user's order
            if (reachable.contains(s)) {
                q.add(s);
            }
        }
        int n = q.size();
        Map<String, Integer> idx = new LinkedHashMap<String, Integer>();
        for (int i = 0; i < n; i++) {
            idx.put(q.get(i), i);
        }
        int[][] next = new int[n][sigma.size()];
        boolean[] isFinal = new boolean[n];
        for (int i = 0; i < n; i++) {
            isFinal[i] = dfa.isFinal(q.get(i));
            for (int k = 0; k < sigma.size(); k++) {
                next[i][k] = idx.get(dfa.next(q.get(i), sigma.get(k)));
            }
        }

        // ---- Step 2: table filling
        Util.heading("Step 2: table-filling algorithm (mark distinguishable pairs)");
        int[][] round = new int[n][n];            // -1 = unmarked, otherwise the round it was marked in
        for (int[] r : round) {
            Arrays.fill(r, -1);
        }
        System.out.println("  Round 0: mark every pair where one state is final and the other is not.");
        List<String> marked0 = new ArrayList<String>();
        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                if (isFinal[i] != isFinal[j]) {
                    round[i][j] = 0;
                    marked0.add("(" + q.get(i) + "," + q.get(j) + ")");
                }
            }
        }
        System.out.println("    marked: " + (marked0.isEmpty() ? "none" : join(marked0)));
        int r = 0;
        while (true) {
            r++;
            List<int[]> newly = new ArrayList<int[]>();
            List<String> reasons = new ArrayList<String>();
            for (int i = 0; i < n; i++) {
                for (int j = i + 1; j < n; j++) {
                    if (round[i][j] >= 0) {
                        continue;
                    }
                    for (int k = 0; k < sigma.size(); k++) {
                        int a = Math.min(next[i][k], next[j][k]);
                        int b = Math.max(next[i][k], next[j][k]);
                        if (a != b && round[a][b] >= 0 && round[a][b] < r) {
                            newly.add(new int[] {i, j});
                            reasons.add("(" + q.get(i) + "," + q.get(j) + "): on '" + sigma.get(k) + "' they go to ("
                                    + q.get(a) + "," + q.get(b) + "), which is already marked");
                            break;
                        }
                    }
                }
            }
            if (newly.isEmpty()) {
                System.out.println("  Round " + r + ": no new pair can be marked, so the algorithm stops.");
                break;
            }
            System.out.println("  Round " + r + ":");
            for (int[] p : newly) {
                round[p[0]][p[1]] = r;
            }
            for (String s : reasons) {
                System.out.println("    mark " + s);
            }
        }

        System.out.println();
        System.out.println("  Final table (X = distinguishable, = = equivalent; number = round it was marked):");
        if (n >= 2) {
            String[] header = new String[n];
            header[0] = "";
            for (int j = 0; j < n - 1; j++) {
                header[j + 1] = q.get(j);
            }
            TablePrinter table = new TablePrinter(header);
            for (int i = 1; i < n; i++) {
                Object[] row = new Object[n];
                row[0] = q.get(i);
                for (int j = 0; j < n - 1; j++) {
                    if (j < i) {
                        row[j + 1] = round[j][i] >= 0 ? "X" + round[j][i] : "=";
                    } else {
                        row[j + 1] = "";
                    }
                }
                table.addRow(row);
            }
            table.print();
        } else {
            System.out.println("  (only one state, no pairs to compare)");
        }

        // ---- Step 3: equivalence classes
        Util.heading("Step 3: equivalence classes (unmarked pairs are equivalent)");
        int[] classOf = new int[n];
        Arrays.fill(classOf, -1);
        List<List<Integer>> classes = new ArrayList<List<Integer>>();
        for (int i = 0; i < n; i++) {
            if (classOf[i] >= 0) {
                continue;
            }
            List<Integer> cls = new ArrayList<Integer>();
            for (int j = i; j < n; j++) {
                if (classOf[j] < 0 && (j == i || round[i][j] < 0)) {
                    cls.add(j);
                    classOf[j] = classes.size();
                }
            }
            classes.add(cls);
        }
        List<String> classNames = new ArrayList<String>();
        for (List<Integer> cls : classes) {
            String name;
            if (cls.size() == 1) {
                name = q.get(cls.get(0));
            } else {
                StringBuilder sb = new StringBuilder("{");
                for (int k = 0; k < cls.size(); k++) {
                    sb.append(k > 0 ? "," : "").append(q.get(cls.get(k)));
                }
                name = sb.append('}').toString();
            }
            classNames.add(name);
            System.out.println("  " + name + (cls.size() > 1 ? "   <- these states are equivalent and are merged"
                    : "   <- distinguishable from every other state"));
        }

        // cross-check with partition refinement
        List<List<Integer>> partition = partitionRefinement(n, next, isFinal, q);
        if (samePartition(classOf, partition, n)) {
            System.out.println("  The partition-refinement method gives the same classes. (cross-check passed)");
        } else {
            System.out.println("  WARNING: the partition-refinement method gives different classes.");
        }

        // ---- Step 4: build the minimized DFA
        Util.heading("Step 4: the minimized DFA");
        System.out.println("  Each class becomes one state; its transitions are those of any member");
        System.out.println("  (all members agree, because they are equivalent).");
        Map<String, Map<Character, String>> delta = new LinkedHashMap<String, Map<Character, String>>();
        Set<String> finals = new LinkedHashSet<String>();
        for (int c = 0; c < classes.size(); c++) {
            int rep = classes.get(c).get(0);
            Map<Character, String> row = new LinkedHashMap<Character, String>();
            for (int k = 0; k < sigma.size(); k++) {
                row.put(sigma.get(k), classNames.get(classOf[next[rep][k]]));
            }
            delta.put(classNames.get(c), row);
            if (isFinal[rep]) {
                finals.add(classNames.get(c));
            }
        }
        String start = classNames.get(classOf[idx.get(dfa.getStart())]);
        DFA min = new DFA(classNames, sigma, start, finals, delta);
        min.printDefinition("M'");
        System.out.println();
        System.out.println("  Number of states: " + input.getStates().size() + " (original)"
                + (removed.isEmpty() ? "" : " -> " + n + " (after removing unreachable)")
                + " -> " + classNames.size() + " (minimized).");
        if (min.getStates().size() == input.getStates().size()) {
            System.out.println("  The given DFA was already minimal.");
        }
        return min;
    }

    /**
     * Partition-refinement (Moore) method: start from {non-final, final} and keep splitting
     * a block when two of its states go to different blocks on some symbol.
     */
    static List<List<Integer>> partitionRefinement(int n, int[][] next, boolean[] isFinal, List<String> names) {
        System.out.println();
        System.out.println("  Cross-check with the partition-refinement method:");
        List<List<Integer>> p = new ArrayList<List<Integer>>();
        List<Integer> nonFinal = new ArrayList<Integer>();
        List<Integer> fin = new ArrayList<Integer>();
        for (int i = 0; i < n; i++) {
            (isFinal[i] ? fin : nonFinal).add(i);
        }
        if (!nonFinal.isEmpty()) {
            p.add(nonFinal);
        }
        if (!fin.isEmpty()) {
            p.add(fin);
        }
        int k = 0;
        System.out.println("    P" + k + " = " + show(p, names));
        while (true) {
            int[] block = new int[n];
            for (int b = 0; b < p.size(); b++) {
                for (int s : p.get(b)) {
                    block[s] = b;
                }
            }
            List<List<Integer>> refined = new ArrayList<List<Integer>>();
            for (List<Integer> b : p) {
                Map<String, List<Integer>> groups = new LinkedHashMap<String, List<Integer>>();
                for (int s : b) {
                    StringBuilder sig = new StringBuilder();
                    for (int x = 0; x < next[s].length; x++) {
                        sig.append(block[next[s][x]]).append(',');
                    }
                    List<Integer> g = groups.get(sig.toString());
                    if (g == null) {
                        g = new ArrayList<Integer>();
                        groups.put(sig.toString(), g);
                    }
                    g.add(s);
                }
                refined.addAll(groups.values());
            }
            k++;
            System.out.println("    P" + k + " = " + show(refined, names));
            if (refined.size() == p.size()) {
                System.out.println("    P" + k + " = P" + (k - 1) + ", so the partition is final.");
                return refined;
            }
            p = refined;
        }
    }

    private static boolean samePartition(int[] classOf, List<List<Integer>> partition, int n) {
        for (List<Integer> block : partition) {
            for (int s : block) {
                if (classOf[s] != classOf[block.get(0)]) {
                    return false;
                }
            }
        }
        // same number of blocks as classes => identical partitions
        Set<Integer> distinct = new LinkedHashSet<Integer>();
        for (int i = 0; i < n; i++) {
            distinct.add(classOf[i]);
        }
        return distinct.size() == partition.size();
    }

    private static String show(List<List<Integer>> p, List<String> names) {
        StringBuilder sb = new StringBuilder("{ ");
        for (int b = 0; b < p.size(); b++) {
            if (b > 0) {
                sb.append(", ");
            }
            sb.append('{');
            for (int i = 0; i < p.get(b).size(); i++) {
                sb.append(i > 0 ? "," : "").append(names.get(p.get(b).get(i)));
            }
            sb.append('}');
        }
        return sb.append(" }").toString();
    }

    private static String join(List<String> items) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < items.size(); i++) {
            sb.append(i > 0 ? " " : "").append(items.get(i));
        }
        return sb.toString();
    }
}
