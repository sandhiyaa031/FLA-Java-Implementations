import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A deterministic finite automaton M = (Q, Sigma, delta, q0, F).
 *
 * The transition function may be partial: a missing transition means that the DFA
 * rejects (it behaves as if the transition led to a non-final dead state).
 * {@link #complete()} makes the dead state explicit.
 */
public class DFA {

    private final List<String> states;
    private final List<Character> alphabet;
    private final String start;
    private final Set<String> finals;
    private final Map<String, Map<Character, String>> delta;

    public DFA(List<String> states, List<Character> alphabet, String start,
               Collection<String> finals, Map<String, Map<Character, String>> transitions) {
        this.states = Collections.unmodifiableList(new ArrayList<String>(states));
        this.alphabet = Collections.unmodifiableList(new ArrayList<Character>(alphabet));
        this.start = start;
        this.finals = Collections.unmodifiableSet(new LinkedHashSet<String>(finals));
        if (!this.states.contains(start)) {
            throw new IllegalArgumentException("start state " + start + " is not in Q");
        }
        for (String f : this.finals) {
            if (!this.states.contains(f)) {
                throw new IllegalArgumentException("final state " + f + " is not in Q");
            }
        }
        Map<String, Map<Character, String>> copy = new LinkedHashMap<String, Map<Character, String>>();
        for (String q : this.states) {
            Map<Character, String> row = new LinkedHashMap<Character, String>();
            Map<Character, String> source = transitions.get(q);
            if (source != null) {
                for (char c : this.alphabet) {
                    String target = source.get(c);
                    if (target != null) {
                        if (!this.states.contains(target)) {
                            throw new IllegalArgumentException("target " + target + " is not in Q");
                        }
                        row.put(c, target);
                    }
                }
            }
            copy.put(q, row);
        }
        this.delta = copy;
    }

    public List<String> getStates() {
        return states;
    }

    public List<Character> getAlphabet() {
        return alphabet;
    }

    public String getStart() {
        return start;
    }

    public Set<String> getFinals() {
        return finals;
    }

    public boolean isFinal(String q) {
        return finals.contains(q);
    }

    /** delta(q, c), or null if that transition is undefined. */
    public String next(String q, char c) {
        Map<Character, String> row = delta.get(q);
        return row == null ? null : row.get(c);
    }

    /** The undefined transitions, written as "delta(q, a)". */
    public List<String> undefinedTransitions() {
        List<String> missing = new ArrayList<String>();
        for (String q : states) {
            for (char c : alphabet) {
                if (next(q, c) == null) {
                    missing.add("delta(" + q + ", " + c + ")");
                }
            }
        }
        return missing;
    }

    public boolean isComplete() {
        return undefinedTransitions().isEmpty();
    }

    /** A state name that is not already used, based on the given name. */
    public String freshName(String base) {
        String name = base;
        int i = 1;
        while (states.contains(name)) {
            name = base + i;
            i++;
        }
        return name;
    }

    /**
     * Returns an equivalent complete DFA: every undefined transition is sent to a new
     * non-final trap state that loops to itself on every symbol. Returns this DFA if it
     * is already complete.
     */
    public DFA complete() {
        if (isComplete()) {
            return this;
        }
        String trap = freshName("TRAP");
        List<String> newStates = new ArrayList<String>(states);
        newStates.add(trap);
        Map<String, Map<Character, String>> newDelta = new LinkedHashMap<String, Map<Character, String>>();
        for (String q : newStates) {
            Map<Character, String> row = new LinkedHashMap<Character, String>();
            for (char c : alphabet) {
                String target = q.equals(trap) ? null : next(q, c);
                row.put(c, target == null ? trap : target);
            }
            newDelta.put(q, row);
        }
        return new DFA(newStates, alphabet, start, finals, newDelta);
    }

    /**
     * The same DFA over a larger alphabet. Transitions on the new symbols are undefined
     * (so strings containing them are rejected), which keeps the language unchanged.
     */
    public DFA withAlphabet(List<Character> largerAlphabet) {
        if (!largerAlphabet.containsAll(alphabet)) {
            throw new IllegalArgumentException("the new alphabet must contain the old one");
        }
        return new DFA(states, largerAlphabet, start, finals, delta);
    }

    /** Runs the DFA on w. Symbols outside the alphabet and undefined moves reject. */
    public boolean accepts(String w) {
        String q = start;
        for (int i = 0; i < w.length(); i++) {
            q = next(q, w.charAt(i));
            if (q == null) {
                return false;
            }
        }
        return finals.contains(q);
    }

    /** The states reachable from the start state, in breadth-first order. */
    public Set<String> reachableStates() {
        Set<String> seen = new LinkedHashSet<String>();
        Deque<String> queue = new ArrayDeque<String>();
        seen.add(start);
        queue.add(start);
        while (!queue.isEmpty()) {
            String q = queue.poll();
            for (char c : alphabet) {
                String r = next(q, c);
                if (r != null && seen.add(r)) {
                    queue.add(r);
                }
            }
        }
        return seen;
    }

    /** Prints the formal 5-tuple and the transition table. */
    public void printDefinition(String name) {
        List<String> symbols = new ArrayList<String>();
        for (char c : alphabet) {
            symbols.add(String.valueOf(c));
        }
        System.out.println(name + " = (Q, Sigma, delta, q0, F) where");
        System.out.println("  Q     = " + Util.setString(states) + "   (" + states.size() + " states)");
        System.out.println("  Sigma = " + Util.setString(symbols));
        System.out.println("  q0    = " + start);
        System.out.println("  F     = " + Util.setString(finals));
        System.out.println("  delta (transition table):");
        printTable();
    }

    /** Prints the transition table. "->" marks the start state and "*" the final states. */
    public void printTable() {
        String[] header = new String[alphabet.size() + 2];
        header[0] = "";
        header[1] = "State";
        for (int i = 0; i < alphabet.size(); i++) {
            header[i + 2] = String.valueOf(alphabet.get(i));
        }
        TablePrinter table = new TablePrinter(header);
        boolean partial = false;
        for (String q : states) {
            Object[] row = new Object[alphabet.size() + 2];
            row[0] = (q.equals(start) ? "->" : "") + (finals.contains(q) ? "*" : "");
            row[1] = q;
            for (int i = 0; i < alphabet.size(); i++) {
                String target = next(q, alphabet.get(i));
                if (target == null) {
                    partial = true;
                }
                row[i + 2] = target == null ? "-" : target;
            }
            table.addRow(row);
        }
        table.print();
        System.out.println("  (-> marks the start state, * marks a final state"
                + (partial ? ", - means undefined" : "") + ")");
    }

    /**
     * Returns the shortest string accepted by exactly one of the two DFAs, or null if
     * they accept exactly the same language. This is an exact test: it explores every
     * reachable pair of states (p, q) of the two machines.
     */
    public static String distinguishingString(DFA a, DFA b) {
        List<Character> union = new ArrayList<Character>(a.alphabet);
        for (char c : b.alphabet) {
            if (!union.contains(c)) {
                union.add(c);
            }
        }
        DFA x = a.withAlphabet(union).complete();
        DFA y = b.withAlphabet(union).complete();
        Map<String, String> parent = new HashMap<String, String>();
        Map<String, Character> via = new HashMap<String, Character>();
        Map<String, String[]> pairOf = new HashMap<String, String[]>();
        Deque<String> queue = new ArrayDeque<String>();
        String startKey = x.start + "\u0000" + y.start;
        parent.put(startKey, null);
        pairOf.put(startKey, new String[] {x.start, y.start});
        queue.add(startKey);
        while (!queue.isEmpty()) {
            String key = queue.poll();
            String[] pair = pairOf.get(key);
            if (x.isFinal(pair[0]) != y.isFinal(pair[1])) {
                StringBuilder sb = new StringBuilder();
                String k = key;
                while (parent.get(k) != null) {
                    sb.append(via.get(k));
                    k = parent.get(k);
                }
                return sb.reverse().toString();
            }
            for (char c : union) {
                String p = x.next(pair[0], c);
                String q = y.next(pair[1], c);
                String nextKey = p + "\u0000" + q;
                if (!pairOf.containsKey(nextKey)) {
                    pairOf.put(nextKey, new String[] {p, q});
                    parent.put(nextKey, key);
                    via.put(nextKey, c);
                    queue.add(nextKey);
                }
            }
        }
        return null;
    }

    /**
     * Reads a DFA in the common input format:
     * states, alphabet, start state, final states, then one transition per line
     * ("state symbol next-state") ending with the word "end".
     */
    public static DFA read(InputSource in, String label) throws IOException {
        String p = label.isEmpty() ? "" : "[" + label + "] ";
        List<String> states = in.parseStates(in.require(p + "Enter the states (separated by spaces):"));
        List<Character> alphabet = in.parseAlphabet(
                in.require(p + "Enter the input alphabet (single characters):"), "input");
        String start = in.parseSingleState(in.require(p + "Enter the start state:"), states);
        Set<String> finals = in.parseStateSet(in.require(p + "Enter the final states ('none' if there are none):"), states);

        in.hint(p + "Enter the transitions, one per line, as:  state symbol next-state");
        in.hint(p + "Example: q0 a q1   means delta(q0, a) = q1.   Type 'end' when finished.");
        Map<String, Map<Character, String>> delta = new LinkedHashMap<String, Map<Character, String>>();
        for (String q : states) {
            delta.put(q, new LinkedHashMap<Character, String>());
        }
        while (true) {
            String line = in.require(p + "  transition:");
            if (InputSource.isEnd(line)) {
                break;
            }
            List<String> t = InputSource.transitionTokens(line);
            if (t.size() != 3) {
                throw in.error("a DFA transition needs 3 parts (state symbol next-state), got: " + line);
            }
            String from = in.parseState(t.get(0), states);
            if (t.get(1).length() != 1 || !alphabet.contains(t.get(1).charAt(0))) {
                if (InputSource.isEpsilon(t.get(1))) {
                    throw in.error("a DFA cannot have epsilon-transitions (use the NFA to DFA program)");
                }
                throw in.error("'" + t.get(1) + "' is not a symbol of the input alphabet");
            }
            char symbol = t.get(1).charAt(0);
            String to = in.parseState(t.get(2), states);
            String old = delta.get(from).get(symbol);
            if (old != null && !old.equals(to)) {
                throw in.error("not deterministic: delta(" + from + ", " + symbol + ") was already given as "
                        + old + ", now " + to);
            }
            delta.get(from).put(symbol, to);
        }
        return new DFA(states, alphabet, start, finals, delta);
    }
}
