import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * TASK 4: INTERSECTION OF TWO DFAs (PRODUCT CONSTRUCTION)
 *
 * Given M1 = (Q1, Sigma, d1, p0, F1) and M2 = (Q2, Sigma, d2, q0, F2), the product DFA is
 *   M = (Q1 x Q2, Sigma, d, (p0, q0), F1 x F2)   with   d((p, q), a) = (d1(p, a), d2(q, a)).
 * M runs both machines at the same time and accepts exactly when BOTH accept, so
 * L(M) = L(M1) intersect L(M2). Only the pairs reachable from (p0, q0) are built.
 *
 * Usage:  java DFAIntersection                 (keyboard)
 *         java DFAIntersection input-file.txt  (file)
 */
public class DFAIntersection {

    public static void main(String[] args) {
        Util.banner("TASK 4: INTERSECTION OF TWO DFAs",
                "Builds a DFA for L = L1 intersect L2 with the product construction.");
        try {
            InputSource in = new InputSource(args);
            in.hint("\nFirst DFA (M1):");
            DFA m1 = DFA.read(in, "DFA 1");
            in.hint("\nSecond DFA (M2):");
            DFA m2 = DFA.read(in, "DFA 2");

            Util.heading("DFA 1");
            m1.printDefinition("M1");
            Util.heading("DFA 2");
            m2.printDefinition("M2");

            DFA product = intersect(m1, m2);

            verify(m1, m2, product);

            while (true) {
                String line = in.nextLine(
                        "\nEnter a string to test ('eps' = empty string, 'end' to stop):");
                if (line == null || InputSource.isEnd(line)) {
                    break;
                }
                String w = InputSource.parseWord(line);
                boolean a = m1.accepts(w);
                boolean b = m2.accepts(w);
                boolean c = product.accepts(w);
                System.out.println("  M1: " + (a ? "ACCEPTED" : "REJECTED") + "   M2: " + (b ? "ACCEPTED" : "REJECTED")
                        + "   Product path: " + NFAtoDFA.dfaPath(product, w));
                System.out.println("  Result: " + Util.quote(w) + " is " + (c ? "ACCEPTED" : "REJECTED")
                        + " by L1 intersect L2" + (c == (a && b) ? "" : " (ERROR: does not match M1 and M2)"));
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

    static DFA intersect(DFA m1, DFA m2) {
        // ---- Step 1: same alphabet, every transition defined
        Util.heading("Step 1: prepare the two DFAs");
        List<Character> sigma = new ArrayList<Character>(m1.getAlphabet());
        for (char c : m2.getAlphabet()) {
            if (!sigma.contains(c)) {
                sigma.add(c);
            }
        }
        boolean sameAlphabet = sigma.size() == m1.getAlphabet().size() && sigma.size() == m2.getAlphabet().size();
        if (sameAlphabet) {
            System.out.println("  Both DFAs use the alphabet " + chars(sigma) + ".");
        } else {
            System.out.println("  The alphabets differ, so both DFAs are extended to " + chars(sigma) + ".");
            System.out.println("  A symbol a DFA did not know leads it to a trap state (it rejects).");
        }
        DFA a = m1.withAlphabet(sigma);
        DFA b = m2.withAlphabet(sigma);
        boolean changed = !sameAlphabet || !a.isComplete() || !b.isComplete();
        if (!a.isComplete()) {
            a = a.complete();
            System.out.println("  M1 had undefined transitions; they now go to trap state "
                    + a.getStates().get(a.getStates().size() - 1) + ".");
        }
        if (!b.isComplete()) {
            b = b.complete();
            System.out.println("  M2 had undefined transitions; they now go to trap state "
                    + b.getStates().get(b.getStates().size() - 1) + ".");
        }
        if (!changed) {
            System.out.println("  Both DFAs are complete, so no changes are needed.");
        }

        // ---- Step 2: product construction
        Util.heading("Step 2: product construction");
        System.out.println("  Start state: (" + a.getStart() + ", " + b.getStart() + ")");
        System.out.println("  Rule: delta((p, q), x) = (delta1(p, x), delta2(q, x))");
        List<String[]> pairs = new ArrayList<String[]>();
        Map<String, Integer> id = new HashMap<String, Integer>();
        Deque<Integer> queue = new ArrayDeque<Integer>();
        String[] startPair = {a.getStart(), b.getStart()};
        pairs.add(startPair);
        id.put(name(startPair), 0);
        queue.add(0);
        Map<String, Map<Character, String>> delta = new LinkedHashMap<String, Map<Character, String>>();
        while (!queue.isEmpty()) {
            int i = queue.poll();
            String[] pq = pairs.get(i);
            String from = name(pq);
            Map<Character, String> row = new LinkedHashMap<Character, String>();
            for (char x : sigma) {
                String[] target = {a.next(pq[0], x), b.next(pq[1], x)};
                String to = name(target);
                boolean isNew = !id.containsKey(to);
                if (isNew) {
                    id.put(to, pairs.size());
                    pairs.add(target);
                    queue.add(pairs.size() - 1);
                }
                row.put(x, to);
                System.out.println("    delta(" + from + ", " + x + ") = (delta1(" + pq[0] + ", " + x + "), delta2("
                        + pq[1] + ", " + x + ")) = " + to + (isNew ? "   (new state)" : ""));
            }
            delta.put(from, row);
        }

        // ---- Step 3: final states
        Util.heading("Step 3: final states");
        System.out.println("  For intersection, (p, q) is final when p is in F1 AND q is in F2.");
        List<String> names = new ArrayList<String>();
        List<String> finals = new ArrayList<String>();
        for (String[] pq : pairs) {
            names.add(name(pq));
            if (a.isFinal(pq[0]) && b.isFinal(pq[1])) {
                finals.add(name(pq));
            }
        }
        System.out.println("  F = " + Util.setString(finals));

        DFA product = new DFA(names, sigma, name(startPair), finals, delta);
        Util.heading("Step 4: the product DFA for L1 intersect L2");
        product.printDefinition("M");
        System.out.println();
        System.out.println("  Q1 x Q2 has " + a.getStates().size() + " x " + b.getStates().size() + " = "
                + a.getStates().size() * b.getStates().size() + " pairs; " + pairs.size()
                + " of them are reachable from the start state and were built.");

        String shortest = shortestAccepted(product);
        if (shortest == null) {
            System.out.println("  No final state is reachable, so L1 intersect L2 is EMPTY.");
        } else {
            System.out.println("  L1 intersect L2 is not empty; its shortest string is " + Util.quote(shortest) + ".");
        }
        return product;
    }

    /** Checks product(w) == M1(w) AND M2(w) for every string up to a fixed length. */
    static void verify(final DFA m1, final DFA m2, final DFA product) {
        Util.heading("Verification: L(M) = L(M1) intersect L(M2)?");
        int maxLength = Util.testLength(product.getAlphabet().size(), 200000, 14);
        String bad = Util.forEachWord(product.getAlphabet(), maxLength, new Util.WordVisitor() {
            public boolean visit(String w) {
                return product.accepts(w) == (m1.accepts(w) && m2.accepts(w));
            }
        });
        long count = Util.wordCount(product.getAlphabet().size(), maxLength);
        if (bad == null) {
            System.out.println("  Tested all " + count + " strings of length 0 to " + maxLength
                    + ": the product DFA accepts a string exactly when both M1 and M2 accept it.");
        } else {
            System.out.println("  ERROR: mismatch on " + Util.quote(bad));
        }
    }

    /** Breadth-first search for the shortest accepted string; null if the language is empty. */
    static String shortestAccepted(DFA d) {
        Map<String, String> parent = new HashMap<String, String>();
        Map<String, Character> via = new HashMap<String, Character>();
        Deque<String> queue = new ArrayDeque<String>();
        parent.put(d.getStart(), null);
        queue.add(d.getStart());
        while (!queue.isEmpty()) {
            String q = queue.poll();
            if (d.isFinal(q)) {
                StringBuilder sb = new StringBuilder();
                for (String s = q; parent.get(s) != null; s = parent.get(s)) {
                    sb.append(via.get(s));
                }
                return sb.reverse().toString();
            }
            for (char c : d.getAlphabet()) {
                String r = d.next(q, c);
                if (r != null && !parent.containsKey(r)) {
                    parent.put(r, q);
                    via.put(r, c);
                    queue.add(r);
                }
            }
        }
        return null;
    }

    private static String name(String[] pair) {
        return "(" + pair[0] + "," + pair[1] + ")";
    }

    private static String chars(List<Character> sigma) {
        List<String> s = new ArrayList<String>();
        for (char c : sigma) {
            s.add(String.valueOf(c));
        }
        return Util.setString(s);
    }
}
